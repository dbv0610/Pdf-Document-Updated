package com.azg.pdf8.ui.main.create;

import android.graphics.BitmapFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.R
import com.azg.pdf8.adapter.CreatePdfAdapter
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.ads.ads.interstitial.InterstitialAdManager
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityCreateBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.dialog.DialogProcess
import com.azg.pdf8.dialog.PdfNameDialog
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.camera.CropImageActivity
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.BitmapManager
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.CreateViewModel
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.viewmodel.OnCreateFile
import com.dong.baselib.widget.click
import com.dong.baselib.widget.delay
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.util.Collections
import kotlin.random.Random

class CreateActivity : BaseActivity<ActivityCreateBinding>(ActivityCreateBinding::inflate) {
    private val viewModel: CreateViewModel by inject()
    private val docViewModel: DocumentViewModel by inject()

    companion object {
        var currentFlowBimap = MutableStateFlow<CreatePdf?>(null)
    }

    private val processDialog by lazy {
        DialogProcess(this@CreateActivity)
    }
    private val createDialog by lazy {
        DialogCreatePdf(this@CreateActivity, object : CreateEventHandle {
            override fun createImage() {
                NativeAdPreloadManager.preloadAd(
                    this@CreateActivity,
                    NativePlacement.PERMISSION,
                    2,
                    false
                )
                InterstitialAdManager.showInterAll(this@CreateActivity) {
                    launcherForResult<ChooseImageActivity>(
                        hashMapOf(
                            Constant.KEY_ACTION to Constant.IMAGE_ADD_LIST
                        )
                    ) { acResult ->
                        acResult.getResultData<MutableList<RecentDocument>>(Constant.IMAGE_ADD_LIST)
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
            }

            override fun scanDocument() {
                if (permission.checkGrantedCamera) {
                    InterstitialAdManager.showInterAll(this@CreateActivity) {
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
                } else {
                    requestCameraLauncher.launch(permission.cameraRequest)
                }
            }
        }).attachActivity(this@CreateActivity)
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

                BitmapManager.setEditBitmap(data.picture)
                BitmapManager.generateDesUri(baseContext)
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
                                val document = RecentDocument(
                                    mediaId = file.hashCode().toLong(),
                                    path = outPutFile.absolutePath,
                                    lastModified = System.currentTimeMillis(),
                                    lastTimeView = System.currentTimeMillis(),
                                    size = outPutFile.length(),
                                    type = DocumentType.Pdf
                                )
                                docViewModel.addToRecent(document.apply {
                                    lastTimeView = System.currentTimeMillis()
                                })
                                InterstitialAdManager.showInterAll(this@CreateActivity) {
                                    launchActivity<ReadPdfActivity>(
                                        hashMapOf(
                                            Constant.ARG_MEDIA_MODEL to document,
                                            Constant.RateWhenCreate to true
                                        )
                                    )
                                    finish()
                                }
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
                } ?: run {
                    toastShort(getString(R.string.can_create_pdf))
                    delay(1500) {
                        finish()
                    }
                }
            } else {
                getData<MutableList<RecentDocument>>(Constant.IMAGE_ADD_NEW)?.let { data ->
                    if (data.isNotEmpty()) {
                        viewModel.initListData(data)
                        binding.progressBar.gone()
                    } else {
                        binding.progressBar.visible()
                    }
                } ?: run {
                    toastShort(getString(R.string.can_create_pdf))
                    delay(1500) {
                        finish()
                    }
                }
            }
        } ?: run {
            getData<MutableList<RecentDocument>>(Constant.IMAGE_ADD_NEW)?.let { data ->
                if (data.isNotEmpty()) {
                    viewModel.initListData(data)
                    binding.progressBar.gone()
                } else {
                    binding.progressBar.visible()
                }
            } ?: run {
                toastShort(getString(R.string.can_create_pdf))
                delay(1500) {
                    finish()
                }
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.rcvData)
        adapter.dragStartListener = { viewHolder ->
            touchHelper.startDrag(viewHolder)
        }
        binding.bannerAdView
            .setBannerPlacement(this@CreateActivity, BannerPlacement.BANNER_ALL)
            .requestBanner()
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
            createNameDialog.showWith(getString(R.string.convert_to_pdf))
        }
    }
}