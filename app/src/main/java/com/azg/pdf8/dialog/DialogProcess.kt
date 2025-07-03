package com.azg.pdf8.dialog

import android.animation.ObjectAnimator
import android.content.Context
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
        super.show()
        rotateAnimator?.start()
    }

    override fun dismiss() {
        rotateAnimator?.cancel()
        rotateAnimator = null
        super.dismiss()
    }
}