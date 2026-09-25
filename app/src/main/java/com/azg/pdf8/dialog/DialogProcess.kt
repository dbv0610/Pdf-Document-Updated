package com.azg.pdf8.dialog

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import com.azg.pdf8.databinding.DialogProcessBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.rotateViewByTime
import com.dong.baselib.widget.rotateViewWithTime

class DialogProcess(activity: Context) : BaseDialog<DialogProcessBinding>(
    activity,
    DialogProcessBinding::inflate,
    cancelAble = false
) {
    private var rotateAnimator: ObjectAnimator? = null

    override fun DialogProcessBinding.initView() {
        rotateAnimator = imgProcess.rotateViewWithTime(duration = 1500, isLoop = true)
    }

    override fun show() {
        binding.txtProgress.visibility = View.GONE
        super.show()
        rotateAnimator?.start()
    }

    /** Shows "[current]/[total]" under the spinner, e.g. pages written so far. */
    fun setProgress(current: Int, total: Int) {
        binding.txtProgress.text = "$current/$total"
        binding.txtProgress.visibility = View.VISIBLE
    }

    override fun dismiss() {
        // Keep the animator: the dialog instance is reused, and a nulled animator
        // left the spinner frozen on every show after the first.
        rotateAnimator?.cancel()
        super.dismiss()
    }
}