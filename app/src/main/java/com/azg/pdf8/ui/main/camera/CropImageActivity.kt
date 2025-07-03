package com.azg.pdf8.ui.main.camera

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.databinding.ActivityCropImageBinding
import com.azg.pdf8.databinding.ItemCropTypeBinding
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.ui.main.create.CreateActivity
import com.azg.pdf8.utils.crop.CropImageView
import com.azg.pdf8.utils.crop.callback.CropCallback
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.api.logD
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.base.BaseAdapter
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import com.dong.baselib.widget.white
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CropType {
    Crop, Rotate, Scale
}

class CropImageActivity :
    BaseActivity<ActivityCropImageBinding>(ActivityCropImageBinding::inflate) {
    override fun backPressed() {
        finish()
    }
    companion object {
        val cropResult = MutableStateFlow<CreatePdf?>(null)
    }

    enum class CropEdg(var xEdg: Int, var yEdg: Int, var src: Int) {
        ORIGINAL(-1, -1, R.drawable.ic_crop_orn),
        C_1_1(1, 1, R.drawable.ic_crop_1_1),
        C_4_3(4, 3, R.drawable.ic_crop_4_3),
        C_3_4(3, 4, R.drawable.ic_crop_3_4),
        C_3_2(3, 2, R.drawable.ic_crop_3_2),
        C_2_3(2, 3, R.drawable.ic_crop_2_3),
        C_16_9(16, 9, R.drawable.ic_crop_16_9),
        C_9_16(9, 16, R.drawable.ic_crop_9_16)
    }

    inner class CropEdgAdapter(val callback: (CropEdg) -> Unit = {}) :
        BaseAdapter<CropEdg, ItemCropTypeBinding>() {
        override fun createBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ) = ItemCropTypeBinding.inflate(inflater, parent, false)

        override fun ItemCropTypeBinding.bind(
            item: CropEdg,
            position: Int
        ) {
            ivScType.setImageResource(item.src)
            tvDetail.text = if (item.xEdg == -1 && item.yEdg == -1)
                getString(R.string.original) else "${item.xEdg}:${item.yEdg}"
            root.click {
                callback.invoke(item)
                currentPosition.value = position
            }
            lifecycle?.let { lf ->
                currentPosition.observe(lf) {
                    ivScType.setColorFilter(if (it == position) mainColor else white)
                    tvDetail.setTextColor(if (it == position) mainColor else white)
                }
            }
        }
    }

    private val cropEdgAdapter by lazy {
        CropEdgAdapter {
            binding.cropImage.setCustomRatio(it.xEdg, it.yEdg)
        }.attachLifecycle(this@CropImageActivity)
    }
    val currentDataType = MutableLiveData(CropType.Crop)
    override fun initialize() {
        lifecycleScope.launch {
            CreateActivity.currentFlowBimap.collect {
                it?.let {
                    cropResult.value = it
                    binding.cropImage.imageBitmap = it.picture
                }
            }
        }
        binding.rcvCropType.adapter = cropEdgAdapter
        cropEdgAdapter.submitList(CropEdg.entries)
        cropEdgAdapter.currentPosition.value = 0

        currentDataType.observe(this@CropImageActivity) {
            when (it) {
                CropType.Crop -> {
                    binding.rcvCropType.visible()
                    binding.lnRotate.gone()
                    binding.seekbarScale.gone()
                }
                CropType.Rotate -> {
                    binding.rcvCropType.gone()
                    binding.lnRotate.visible()
                    binding.seekbarScale.gone()
                }
                else -> {
                    binding.rcvCropType.gone()
                    binding.lnRotate.gone()
                    binding.seekbarScale.visible()
                }
            }
            binding.tvCrop.setTextColor(if (it == CropType.Crop) mainColor else white)
            binding.tvScale.setTextColor(if (it == CropType.Scale) mainColor else white)
            binding.tvRotate.setTextColor(if (it == CropType.Rotate) mainColor else white)
        }
    }

    var lastDegree = 0f
    override fun ActivityCropImageBinding.setData() {
        scrollRulerRotate.setOnRulerChangeListener {
            val current = it - 180
            val degrees = current - lastDegree
            cropImage.rotateAngleImage(degrees)
        }
        seekbarScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    cropImage.setImageScale(progress / 100f)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    override fun ActivityCropImageBinding.onClick() {
        binding.tvCrop.click {
            currentDataType.value = CropType.Crop
        }
        binding.tvScale.click {
            currentDataType.value = CropType.Scale
        }
        binding.tvRotate.click {
            currentDataType.value = CropType.Rotate
        }
        lnRtLeft.click {
            cropImage.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D)
        }
        lnRtRight.click {
            cropImage.rotateImage(CropImageView.RotateDegrees.ROTATE_90D)
        }
        icSave.click {
             cropImage.cropAsync(object : CropCallback{
                override fun onSuccess(cropped: Bitmap?) {
                    setResult(RESULT_OK)
                    cropped?.let {
                        cropResult.update {
                            it?.copy(picture = cropped)
                        }
                    }
                    finish()
                }

                override fun onError(e: Throwable?) {
                    toastShort("${e?.message}")
                }
            })
        }
        icClose.click {
            finish()
        }
    }
}

