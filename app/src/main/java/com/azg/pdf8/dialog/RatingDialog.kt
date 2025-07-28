package com.azg.pdf8.dialog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ads.control.admob.AppOpenManager
import com.azg.pdf8.R
import com.azg.pdf8.app.rateApp
import com.azg.pdf8.app.toastShort
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.base.SystemUtil
import com.azg.pdf8.databinding.DiallogRateAppBinding
import com.dong.baselib.widget.RatingBar
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class RatingDialog(val activity: AppCompatActivity) :
    BaseDialog<DiallogRateAppBinding>(activity, DiallogRateAppBinding::inflate, true) {
    private lateinit var reviewManagerInstance: ReviewManager
    private var reviewInfoInstance: ReviewInfo? = null
    override fun DiallogRateAppBinding.initView() {
        binding.tvRate.setOnClickListener {
            if (binding.ratingBar.getRating() == 0) {
                Toast.makeText(
                    context,
                    context.getText(R.string.please_rate_us),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (binding.ratingBar.getRating() >= 4) {
                requestReview()
                dismiss()
                SystemUtil.forceRated(activity)
            } else {
                SystemUtil.forceRated(activity)

                AppOpenManager.getInstance().disableAdResumeByClickAction()
                composeEmail()
            }
        }
        reviewManagerInstance = ReviewManagerFactory.create(context)
        reviewManagerInstance.requestReviewFlow().addOnCompleteListener {
            if (it.isSuccessful)
                reviewInfoInstance = it.result
        }
        binding.ratingBar.setRatingChangeListener(object : RatingBar.RatingChangeListener {
            override fun onRatingChanged(rating: Int) {
                when (rating) {
                    0 -> {
                        binding.llFocusItem.visible()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_00)

                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_0)
                        binding.tvRate.text = activity.getString(R.string.rate_us)
                    }
                    1 -> {
                        binding.llFocusItem.visible()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_01)
                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_1_2)
                        binding.tvRate.text = activity.getString(R.string.rate_us)
                    }
                    2 -> {
                        binding.llFocusItem.visible()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_02)
                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_1_2)
                        binding.tvRate.text = activity.getString(R.string.rate_us)
                    }
                    3 -> {
                        binding.llFocusItem.visible()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_03)
                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_3)
                        binding.tvRate.text = activity.getString(R.string.rate_us)
                    }
                    4 -> {
                        binding.llFocusItem.gone()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_04)
                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_4)
                        binding.tvRate.text = activity.getString(R.string.rate_on_google_play)
                    }
                    else -> {
                        binding.llFocusItem.gone()
                        binding.imgRate.setImageResource(R.drawable.img_ic_rate_05)
                        binding.tvDescription.text =
                            activity.getString(R.string.rating_description_5)
                        binding.tvRate.text = activity.getString(R.string.rate_on_google_play)
                    }
                }
            }
        })
    }

    private fun rateApp() {
        activity.rateApp {
            activity.toastShort(activity.getString(R.string.thanks_for_your_rating))
            onFinishRate()
            dismiss()
        }
    }

    private var onFinishRate: () -> Unit = {}
    private var onDismiss: () -> Unit = {}
    fun setFinishRate(onFinish: () -> Unit) = apply {
        this.onFinishRate = onFinish
    }

    private fun requestReview() {
        reviewInfoInstance?.let {
            val flow = reviewManagerInstance.launchReviewFlow(activity, it)
            flow.addOnCompleteListener { _ ->
                rateApp()
            }
        } ?: run {
            rateApp()
        }
    }

    fun setDismissRate(onDismiss: () -> Unit) = apply {
        this.onDismiss = onDismiss
    }

    override fun dismiss() {
        super.dismiss()
        onDismiss.invoke()
    }

    private fun composeEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@azuraglobal.app"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Feedback"))
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}


