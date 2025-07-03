package com.azg.pdf8.ui.main.create;

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.adapter.CreatePdfAdapter
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityCreateBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.dialog.DialogProcess
import com.azg.pdf8.dialog.PdfNameDialog
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.camera.CropImageActivity
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.CreateViewModel
import com.azg.pdf8.viewmodel.OnCreateFile
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import kotlin.random.Random

class CreateActivity : BaseActivity<ActivityCreateBinding>(ActivityCreateBinding::inflate) {
    private val viewModel: CreateViewModel by inject()

    companion object {
        var currentFlowBimap = MutableStateFlow<CreatePdf?>(null)
    }

    private val processDialog by lazy {
        DialogProcess(this@CreateActivity)
    }
    private val createDialog by lazy {
        DialogCreatePdf(this@CreateActivity, object : CreateEventHandle {
            override fun createImage() {
                launcherForResult<ChooseImageActivity>(
                    hashMapOf(
                        Constant.KEY_ACTION to Constant.IMAGE_ADD_LIST
                    )
                ) { acResult ->
                    acResult.getResultData<MutableList<DocumentModel>>(Constant.IMAGE_ADD_LIST)
                        ?.let { data ->
                            if (data.isNotEmpty()) {
                                viewModel.addDataToList(data)
                                binding.progressBar.gone()
                            } else {
                                binding.progressBar.visible()
                            }
                        }
                }
            }

            override fun scanDocument() {
                launcherForResult<CameraActivity>(hashMapOf(Constant.SCREEN_ACTION to Constant.CAPTURE_ADD)) {
                    it.getResultData<String>(Constant.CAPTURE_ADD)?.let { path ->
                        val randomId = Random.nextInt()
                        val bitmap = BitmapFactory.decodeFile(path)
                        val model = CreatePdf(
                            randomId, bitmap, randomId
                        )
                        viewModel.addData(model)
                        binding.progressBar.gone()
                    }
                }
            }
        })
    }
    private val adapter: CreatePdfAdapter by lazy {
        CreatePdfAdapter(
            onRemove = { item, listCurrent ->
                listCurrent.removeIf { it.defId == item.defId }
                adapter.submitList(listCurrent)
            },
            onEdit = { data ->
                currentFlowBimap.value = data
                launchActivity<CropImageActivity>()
            },
        )
    }
    val callback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            adapter.currentAdapterList.value.toMutableList().apply {
                Collections.swap(this, from, to)
            }.also { newList ->
                adapter.submitList(newList)
            }
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        override fun isLongPressDragEnabled() = false
    }
    private val createNameDialog by lazy {
        PdfNameDialog(this@CreateActivity) { newName ->
            CoroutineScope(Dispatchers.IO).launch {
                val listFile = adapter.currentList.toMutableList<CreatePdf>()
                val listBitmap = listFile.map { it.picture }
                val outPutFile = File(AppUtils.documentPath, "$newName.pdf")
                viewModel.createPdfFromBitmaps(
                    bitmaps = listBitmap, outPutFile, event = object : OnCreateFile {
                        override fun onStartCreate() {
                            super.onStartCreate()
                            runOnUiThread { processDialog.show() }
                        }

                        override fun onFinish(file: File?) {
                            super.onFinish(file)
                            runOnUiThread {
                                processDialog.dismiss()
                            }
                        }

                        override fun onError() {
                            super.onError()
                            runOnUiThread {
                                processDialog.dismiss()
                                toastShort(getString(R.string.can_create_pdf))
                            }
                        }
                    })
            }
        }
    }

    override fun backPressed() {
        finish()
    }

    override fun initialize() {
        getData<String>(Constant.KEY_ACTION)?.let { it ->
            if (it == "addNew") {
                getData<String>(Constant.IMAGE_PATH)?.let { model ->
                    val randomId = Random.nextInt()
                    val bitmap = BitmapFactory.decodeFile(model)
                    val model = CreatePdf(
                        randomId, bitmap, randomId
                    )
                    viewModel.addData(model)
                    binding.progressBar.gone()
                }
            } else {
                getData<MutableList<DocumentModel>>(Constant.IMAGE_ADD_NEW)?.let { data ->
                    if (data.isNotEmpty()) {
                        viewModel.initListData(data)
                        binding.progressBar.gone()
                    } else {
                        binding.progressBar.visible()
                    }
                }
            }
        } ?: run {
            getData<MutableList<DocumentModel>>(Constant.IMAGE_ADD_NEW)?.let { data ->
                if (data.isNotEmpty()) {
                    viewModel.initListData(data)
                    binding.progressBar.gone()
                } else {
                    binding.progressBar.visible()
                }
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.rcvData)
        adapter.dragStartListener = { viewHolder ->
            touchHelper.startDrag(viewHolder)
        }
    }

    override fun ActivityCreateBinding.setData() {
        lifecycleScope.launch {
            viewModel.listPhotoSelect.collect {
                adapter.submitList(it)
            }
        }
        rcvData.adapter = adapter
        lifecycleScope.launch {
            CropImageActivity.cropResult.collect {
                it?.let {
                    viewModel.updateDataInList(it)
                }
            }
        }
    }

    override fun ActivityCreateBinding.onClick() {
        icBack.click {
            backPressed()
        }
        btnAddImage.click {
            createDialog.show()
        }
        btnCreate.click {
            createNameDialog.show()
        }
    }
}