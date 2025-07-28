package com.azg.pdf8.ui.main.create;

import android.content.Intent
import android.os.Parcelable
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.R;
import com.azg.pdf8.adapter.FolderGalleryAdapter
import com.azg.pdf8.adapter.PhotoGalleryAdapter
import com.azg.pdf8.adapter.ViewActionHandle
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivityChooseImageBinding
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.utils.ScanState
import com.azg.pdf8.viewmodel.ScanImageViewModel
import com.azg.pdf8.viewmodel.ScreenType
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.invisible
import com.dong.baselib.widget.visible
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChooseImageActivity :
    BaseActivity<ActivityChooseImageBinding>(ActivityChooseImageBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    private var screenAction = ""
    private var photoAdapter: PhotoGalleryAdapter? = null
    val viewModel: ScanImageViewModel by inject()
    private var isFirstRequest = false
    val adapter by lazy {
        FolderGalleryAdapter {
            viewModel.setCurrentPhotoModel(it)
            viewModel.clearSelected()
            binding.folderName.text = it.folderName
        }
    }
    val isSmallNative = remoteConfig.n110Config1.layout.contains("small")
    val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this@ChooseImageActivity,
            config = NativePlacement.PERMISSION,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
        )
    }

    override fun initialize() {
        screenAction = getData<String>(Constant.KEY_ACTION).toString()
        photoAdapter = PhotoGalleryAdapter(object : ViewActionHandle {
            override fun onSelect(recentDocument: RecentDocument) {
                super.onSelect(recentDocument)
                if (!isFirstRequest) {
                    requestAds()
                    isFirstRequest = true
                }
                viewModel.modifyItemSelect(recentDocument)
            }
        })
        lifecycleScope.launch {
            viewModel.listSelectedItem.collect {
                binding.icCheckSelect.isVisible = it.isNotEmpty()
                photoAdapter?.checkStateSelect(it.toMutableList())
            }
        }
        requestAds()
    }

    fun requestAds() {
        with(nativeAdsWrapper) {
            setupNativeAd("native_choose_image")
            requestAds()
        }
    }

    override fun ActivityChooseImageBinding.setData() {
        val gridLM = GridLayoutManager(this@ChooseImageActivity, 4)
        val linearLM = LinearLayoutManager(this@ChooseImageActivity)
        binding.rcvData.layoutManager = linearLM
        binding.rcvData.adapter = adapter
        lifecycleScope.launch {
            viewModel.scanState.collect { state ->
                when (state) {
                    is ScanState.Start -> binding.progressBar.visible()
                    is ScanState.Progress -> {
                        binding.progressBar.max = state.total
                        binding.progressBar.progress = state.processed
                    }
                    is ScanState.Success -> {
                        binding.progressBar.gone()
                        viewModel.groupFolder(state.list.toMutableList())
                    }
                    is ScanState.Error -> {
                        binding.progressBar.gone()
                        Toast.makeText(
                            this@ChooseImageActivity,
                            "Scan error: ${state.throwable.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        lifecycleScope.launch {
            combine(viewModel.listPhotoFolder, viewModel.screenType) { list, type ->
                if (type == ScreenType.Folder) list else null
            }.collectLatest { list ->
                if (list != null && list.isNotEmpty()) {
                    binding.rcvData.visible()
                    binding.lnNoData.gone()
                    binding.rcvData.layoutManager = linearLM
                    binding.rcvData.adapter = adapter
                    adapter.submitList(list)
                } else if (viewModel.screenType.value == ScreenType.Folder) {
                    binding.rcvData.invisible()
                    binding.lnNoData.visible()
                }
            }
        }

        lifecycleScope.launch {
            combine(viewModel.listSelectPhotoFolder, viewModel.screenType) { list, type ->
                if (type == ScreenType.Detail) list else null
            }.collectLatest { list ->
                if (list != null && list.isNotEmpty()) {
                    binding.rcvData.visible()
                    binding.lnNoData.gone()
                    binding.rcvData.layoutManager = gridLM
                    binding.rcvData.adapter = photoAdapter
                    photoAdapter?.submitList(list)
                } else if (viewModel.screenType.value == ScreenType.Detail) {
                    binding.rcvData.invisible()
                    binding.lnNoData.visible()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.screenType.collect { type ->
                binding.icDropDown.setImageResource(
                    if (type == ScreenType.Detail) R.drawable.ic_drop_down
                    else R.drawable.ic_drop_up
                )
            }
        }
        binding.lnSelectType.click {
            if (viewModel.screenType.value == ScreenType.Detail) {
                viewModel.setScType(ScreenType.Folder)
            } else {
                viewModel.setScType(ScreenType.Detail)
            }
        }
        viewModel.startScan(this@ChooseImageActivity)
    }

    override fun ActivityChooseImageBinding.onClick() {
        lnSelectType.click {
            if (viewModel.screenType.value == ScreenType.Detail) {
                viewModel.setScType(ScreenType.Folder)
            } else {
                viewModel.setScType(ScreenType.Detail)
            }
        }
        icCheckSelect.click {
            if (screenAction == Constant.IMAGE_ADD_LIST) {
                val lisCast = viewModel.listSelectedItem.value as? List<Parcelable>
                lisCast?.let {
                    val intentData = Intent()
                    intentData.putParcelableArrayListExtra(
                        Constant.IMAGE_ADD_LIST,
                        ArrayList(lisCast)
                    )
                    setResult(RESULT_OK, intentData)
                    finish()
                } ?: run {
                    Toast.makeText(
                        this@ChooseImageActivity,
                        getString(R.string.can_create_pdf),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (screenAction == "createNew") {
                launchActivity<CreateActivity>(mapOf(Constant.IMAGE_ADD_NEW to viewModel.listSelectedItem.value))
            }
        }
        icBack.click {
            backPressed()
        }
    }
}