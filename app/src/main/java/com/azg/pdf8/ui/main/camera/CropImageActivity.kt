package com.azg.pdf8.ui.main.camera

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.AutoTransition
import androidx.transition.Transition
import com.azg.pdf8.R
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.databinding.ActivityCropImageBinding
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.ui.main.create.CreateActivity.Companion.currentFlowBimap
import com.azg.pdf8.utils.BitmapManager
import com.azg.pdf8.utils.BitmapManager.isValid
import com.azg.pdf8.utils.dpToPxInt
import com.azg.pdf8.viewmodel.CropImageViewModel
import com.azg.pdf8.widget.AspectRatio
import com.azg.pdf8.widget.AspectRatioTextView
import com.azg.pdf8.widget.getSnackBar
import com.dong.baselib.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.view.GestureCropImageView
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.view.TransformImageView.TransformImageListener
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView.ScrollingListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropImageActivity :
    BaseActivity<ActivityCropImageBinding>(ActivityCropImageBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    override fun initialize() {
        initiateRootViews()
        setupView()
        setImageData()
        setInitialState()
    }

    override fun ActivityCropImageBinding.setData() = Unit

    override fun ActivityCropImageBinding.onClick() = Unit

    private var mHorizontalProgressWheelView: HorizontalProgressWheelView? = null
    private var mGestureCropImageView: GestureCropImageView? = null
    private var mOverlayView: OverlayView? = null
    private var mControlsTransition: Transition? = null
    private var mCompressFormat: Bitmap.CompressFormat = DEFAULT_COMPRESS_FORMAT
    private var mCompressQuality = DEFAULT_COMPRESS_QUALITY
    private val mCropAspectRatioViews: ArrayList<ViewGroup> = ArrayList()
    private var mTextViewReset: TextView? = null
    private var mSeekbarScale: SeekBar? = null
    private var currentEditMode: EditMode = EditMode.CROP
    private val viewModel: CropImageViewModel by viewModels()
    private val snackBar: Snackbar by lazy {
        baseContext.getSnackBar(binding.rootView, binding.wrapperControls, "")
    }

    companion object {
        val cropResult = MutableStateFlow<CreatePdf?>(null)
        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val DEFAULT_COMPRESS_QUALITY: Int = 100
        private val DEFAULT_COMPRESS_FORMAT: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT: Int = 42
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT: Int = 15000
        private const val ROTATE_LEFT_ANGLE: Int = -90
        private const val ROTATE_RIGHT_ANGLE: Int = 90
        private const val MAX_SCALE_VALUE = 200f
        private const val ORIGINAL_ASPECT_RATIO_INDEX = 0
        private const val ORIGINAL_ROTATE_ANGLE = 0f
        private const val CROP_FRAME_STROKE_WIDTH_IN_DP = 2
        private const val CROP_GRID_STROKE_WIDTH_IN_DP = 1
        private const val MAX_RESULT_IMAGE_SIZE = 4096
    }

    enum class EditMode {
        SCALE, ROTATE, CROP
    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private fun setupView() {
        mControlsTransition = AutoTransition()
        mControlsTransition!!.setDuration(CONTROLS_ANIMATION_DURATION)

        binding.azStateAspectRatio.setOnClickListener {
            changeEditMode(EditMode.CROP)
        }
        binding.azStateRotate.setOnClickListener {
            changeEditMode(EditMode.ROTATE)
        }
        binding.azStateScale.setOnClickListener {
            changeEditMode(EditMode.SCALE)
        }
        binding.stateAzCancel.setOnClickListener { finishWithoutResult() }
        binding.stateAzSave.setOnClickListener { cropAndSaveImage() }

        setupAspectRatioWidget()
        setupRotateWidget()
        setupScaleWidget()
        binding.bannerAdView
            .setBannerPlacement(this@CropImageActivity, BannerPlacement.BANNER_ALL)
            .requestBanner()
    }

    private fun changeEditMode(editMode: EditMode) {
        if (editMode == currentEditMode) return
        currentEditMode = editMode
        setSelectedEditLayout(editMode)
    }

    private fun resetRotation() {
        mGestureCropImageView!!.postRotate(-mGestureCropImageView!!.currentAngle)
        mGestureCropImageView!!.resetCurrentZoom()
        mTextViewReset?.isVisible = false
        mHorizontalProgressWheelView?.reset()
    }

    private fun resetScale() {
        mSeekbarScale?.progress = MAX_SCALE_VALUE.toInt() / 2
        mGestureCropImageView!!.resetCurrentZoom()
        mTextViewReset?.isVisible = false
    }

    private fun setImageData() {
        processOptions()
        try {
            lifecycleScope.launch {
                currentFlowBimap.collect {
                    cropResult.value = it
                }
            }
            val editBitmap = BitmapManager.getEditBitmap()
            val desUri = BitmapManager.desUri

            if (editBitmap != null && editBitmap.isValid() && desUri != null) {
                mGestureCropImageView!!.setImageBitmap(
                    baseContext,
                    editBitmap,
                    desUri
                )
            } else {
                throw IllegalStateException("Edit bitmap or destination URI is not available")
            }
        } catch (e: Exception) {
            setResultError(e)
            backPressed()
        }
    }

    private fun processOptions() {
        // Bitmap compression options
        mCompressFormat = DEFAULT_COMPRESS_FORMAT
        mCompressQuality = DEFAULT_COMPRESS_QUALITY
        mGestureCropImageView!!.maxBitmapSize =
            com.yalantis.ucrop.view.CropImageView.DEFAULT_MAX_BITMAP_SIZE
        mGestureCropImageView!!.setMaxScaleMultiplier(MAX_SCALE_VALUE)
        mGestureCropImageView!!.setImageToWrapCropBoundsAnimDuration(com.yalantis.ucrop.view.CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION.toLong())
        // Overlay view options
        mOverlayView!!.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH)
        mOverlayView!!.setDimmedColor(resources.getColor(R.color.color_default_dimmed, null))
        mOverlayView!!.setCircleDimmedLayer(OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER)
        mOverlayView!!.setShowCropFrame(true)
        mOverlayView!!.setCropFrameColor(resources.getColor(R.color.color_default_crop_frame, null))
        mOverlayView!!.setCropFrameStrokeWidth(baseContext.dpToPxInt(CROP_FRAME_STROKE_WIDTH_IN_DP))
        mOverlayView!!.setShowCropGrid(OverlayView.DEFAULT_SHOW_CROP_GRID)
        mOverlayView!!.setCropGridRowCount(OverlayView.DEFAULT_CROP_GRID_ROW_COUNT)
        mOverlayView!!.setCropGridColumnCount(OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT)
        mOverlayView!!.setCropGridColor(resources.getColor(R.color.color_default_crop_grid, null))
        mOverlayView!!.setCropGridCornerColor(
            resources.getColor(
                R.color.color_default_crop_grid,
                null
            )
        )
        mOverlayView!!.setCropGridStrokeWidth(baseContext.dpToPxInt(CROP_GRID_STROKE_WIDTH_IN_DP))
        val aspectRationSelectedByDefault = ORIGINAL_ASPECT_RATIO_INDEX
        val aspectRatioList = AspectRatio.entries.toTypedArray()
        val targetAspectRatio =
            aspectRatioList[aspectRationSelectedByDefault].x / aspectRatioList[aspectRationSelectedByDefault].y
        mGestureCropImageView!!.targetAspectRatio =
            if (java.lang.Float.isNaN(targetAspectRatio)) com.yalantis.ucrop.view.CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio

        mGestureCropImageView!!.setMaxResultImageSizeX(MAX_RESULT_IMAGE_SIZE)
        mGestureCropImageView!!.setMaxResultImageSizeY(MAX_RESULT_IMAGE_SIZE)
    }

    private fun setInitialState() {
        setSelectedEditLayout(currentEditMode)
    }

    private fun setSelectedEditLayout(editMode: EditMode) {
        binding.azStateAspectRatio.isSelected = editMode == EditMode.CROP
        binding.azStateRotate.isSelected = editMode == EditMode.ROTATE
        binding.azStateScale.isSelected = editMode == EditMode.SCALE
        binding.scrollWrapperAspectRatio.isVisible = editMode == EditMode.CROP
        binding.layoutRotateWheel.rlRootView.isVisible = editMode == EditMode.ROTATE
        binding.layoutScaleWheel.flRootView.isVisible = editMode == EditMode.SCALE
        setAllowedGestures(editMode)
        if (editMode == EditMode.SCALE) {
            setButtonResetVisibility(mSeekbarScale?.progress != MAX_SCALE_VALUE.toInt() / 2)
        } else if (editMode == EditMode.ROTATE) {
            setButtonResetVisibility(mGestureCropImageView?.currentAngle != ORIGINAL_ROTATE_ANGLE)
        }
    }

    private fun setButtonResetVisibility(isVisibility: Boolean) {
        binding.textViewReset.isVisible = isVisibility
    }

    private fun setAllowedGestures(tab: EditMode) {
        mGestureCropImageView!!.isScaleEnabled = tab == EditMode.CROP || tab == EditMode.SCALE
        mGestureCropImageView!!.isRotateEnabled = tab == EditMode.CROP || tab == EditMode.ROTATE
    }

    private fun initiateRootViews() {
        mGestureCropImageView = binding.azCropView.cropImageView
        mOverlayView = binding.azCropView.overlayView

        mGestureCropImageView!!.setTransformImageListener(mImageListener)
        binding.azCropframe.setBackgroundColor(
            ContextCompat.getColor(
                baseContext, R.color.black
            )
        )
        mTextViewReset = binding.textViewReset
        mTextViewReset?.setOnClickListener {
            if (currentEditMode == EditMode.ROTATE) {
                resetRotation()
            } else {
                resetScale()
            }
        }
    }

    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {}

        override fun onScale(currentScale: Float) {}

        override fun onLoadComplete() {
            binding.azCropView.animate().alpha(1f).setDuration(300)
                .setInterpolator(AccelerateInterpolator())
        }

        override fun onLoadFailure(e: Exception) {
            setResultError(e)
            backPressed()
        }
    }

    private fun cropAndSaveImage() {
        binding.loadingView.isVisible = true
        mGestureCropImageView!!.cropAndSaveImage(
            mCompressFormat,
            mCompressQuality,
            object : BitmapCropCallback {
                override fun onBitmapCropped(
                    resultUri: Uri,
                    offsetX: Int,
                    offsetY: Int,
                    imageWidth: Int,
                    imageHeight: Int
                ) {
                    setResultUri(
                        resultUri
                    )
                }

                override fun onCropFailure(t: Throwable) {
                    setResultError(t)
                    backPressed()
                }
            })
    }

    private fun setResultUri(uri: Uri) {
        viewModel.loadBitmapFromUri(this@CropImageActivity, uri)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.bitmapLoaderState.collectLatest { state ->
                    when (state) {
                        is CropImageViewModel.BitmapLoaderState.SUCCESS -> {
                            withContext(Dispatchers.Main) {
                                cropResult.update {
                                    it?.copy(picture = state.bitmap)
                                }
                                finish()
                            }
                        }
                        is CropImageViewModel.BitmapLoaderState.ERROR -> {
                            snackBar.setText(R.string.some_errors_occurred_please_try_again).show()
                            backPressed()
                        }
                        CropImageViewModel.BitmapLoaderState.IDLE -> Unit
                        CropImageViewModel.BitmapLoaderState.LOADING -> Unit
                        else -> {}
                    }
                }
            }
        }
        setResult(RESULT_OK)
    }

    private fun finishWithoutResult() {
        backPressed()
    }

    private fun setResultError(throwable: Throwable?) {
        Log.d("tag", "Error:${throwable?.message}")
    }

    private fun setupAspectRatioWidget() {
        val aspectRationSelectedByDefault = 0
        val aspectRatioList = AspectRatio.entries.toTypedArray()
        var wrapperAspectRatio: LinearLayout
        var aspectRatioTextView: AspectRatioTextView
        var aspectRatioImageView: AppCompatImageView
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (aspectRatio in aspectRatioList) {
            wrapperAspectRatio =
                layoutInflater.inflate(R.layout.layout_az_aspect_ratio, null) as LinearLayout
            wrapperAspectRatio.layoutParams = lp
            aspectRatioTextView = wrapperAspectRatio.findViewById(R.id.tvAzAspectRatio)
            aspectRatioImageView = wrapperAspectRatio.findViewById(R.id.ivUcropAspectRation)
            aspectRatioTextView.setActiveColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.color_E84749
                )
            )
            aspectRatioTextView.setAspectRatio(aspectRatio)
            aspectRatioImageView.setImageResource(aspectRatio.drawable)

            binding.layoutAspectRatio.addView(wrapperAspectRatio)
            mCropAspectRatioViews.add(wrapperAspectRatio)
        }

        mCropAspectRatioViews[aspectRationSelectedByDefault].isSelected = true

        for (cropAspectRatioView in mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener { v: View ->
                if (v.isSelected) return@setOnClickListener
                val aspectRatioTextView1 =
                    v.findViewById<AspectRatioTextView>(R.id.tvAzAspectRatio)
                mGestureCropImageView!!.targetAspectRatio =
                    aspectRatioTextView1.getAspectRatio(v.isSelected)
                mGestureCropImageView!!.setImageToWrapCropBounds()
                for (cropAspectRatioView1 in mCropAspectRatioViews) {
                    cropAspectRatioView1.isSelected = cropAspectRatioView1 === v
                }
            }
        }
    }

    private fun setupRotateWidget() {
        mHorizontalProgressWheelView =
            findViewById(R.id.rotate_scroll_wheel)
        val mWrapperRightRotate = findViewById<View>(R.id.wrapper_right_rotate)
        val mWrapperLeftRotate = findViewById<View>(R.id.wrapper_left_rotate)

        fun rotateByAngle(angle: Int) {
            mGestureCropImageView!!.postRotate(angle.toFloat())
            mGestureCropImageView!!.setImageToWrapCropBounds()
            setButtonResetVisibility(mGestureCropImageView?.currentAngle != 0f)
        }

        mHorizontalProgressWheelView?.setScrollingListener(object : ScrollingListener {
            override fun onScroll(delta: Float, totalDistance: Float) {
                mTextViewReset?.isVisible = delta != 0f
                mGestureCropImageView!!.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
            }

            override fun onScrollEnd() {
                mGestureCropImageView!!.setImageToWrapCropBounds()
            }

            override fun onScrollStart() {
                mGestureCropImageView!!.cancelAllAnimations()
            }
        })

        mHorizontalProgressWheelView?.setMarkerColor(
            ContextCompat.getColor(
                baseContext, R.color.color_E84749
            )
        )

        mWrapperRightRotate.setOnClickListener {
            rotateByAngle(
                ROTATE_RIGHT_ANGLE
            )
        }

        mWrapperLeftRotate.setOnClickListener {
            rotateByAngle(
                ROTATE_LEFT_ANGLE
            )
        }
    }

    private fun setupScaleWidget() {
        mSeekbarScale = findViewById(R.id.seekbar_view_scale)
        mSeekbarScale?.progress = MAX_SCALE_VALUE.toInt() / 2
        mSeekbarScale?.max = MAX_SCALE_VALUE.toInt()
        mSeekbarScale?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var lastProgress = MAX_SCALE_VALUE.toInt() / 2
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val delta = progress - lastProgress
                    if (delta > MAX_SCALE_VALUE.toInt() / 2) {
                        mGestureCropImageView!!.zoomInImage(
                            mGestureCropImageView!!.currentScale +
                                    delta * ((mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    } else {
                        mGestureCropImageView!!.zoomOutImage(
                            mGestureCropImageView!!.currentScale +
                                    delta * ((mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    }
                    lastProgress = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mGestureCropImageView!!.cancelAllAnimations()
                lastProgress = seekBar.progress
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mGestureCropImageView!!.setImageToWrapCropBounds()
                setButtonResetVisibility(seekBar.progress != MAX_SCALE_VALUE.toInt() / 2)
            }
        })
    }

    override fun onStop() {
        if (mGestureCropImageView != null) {
            mGestureCropImageView!!.cancelAllAnimations()
        }
        super.onStop()
    }
}

