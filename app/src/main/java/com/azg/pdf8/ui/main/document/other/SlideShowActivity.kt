package com.azg.pdf8.ui.main.document.other

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.azg.pdf8.R
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivitySlideShowBinding
import com.azg.pdf8.model.DocumentPage
import com.dong.baselib.api.isApi33orHigher
import com.dong.baselib.widget.delay
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideShowActivity :
    BaseActivity<ActivitySlideShowBinding>(ActivitySlideShowBinding::inflate, true) {
    companion object {
        private const val EXTRA_BITMAP_LIST = "extra_bitmap_list"
    }

    private lateinit var adapter: SlideAdapter
    private val uiVisibleDuration = 2_000L
    private lateinit var gestureDetector: GestureDetectorCompat
    override fun backPressed() {
        finish()
    }

    override fun initialize() {
        lifecycleScope.launch {
            ReadDocumentActivity.listDataSlideShow.collectLatest {
                withContext(Dispatchers.Main) {
                    adapter = SlideAdapter(it)
                    binding.viewPager2.adapter = adapter
                    binding.viewPager2.registerOnPageChangeCallback(
                        object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                super.onPageSelected(position)
                                updatePageIndicator(position)
                            }
                        }
                    )

                    updatePageIndicator(0)
                }
            }
        }
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                showUiControls()
                return true
            }
        })
        val pagerChild = binding.viewPager2.getChildAt(0)
        pagerChild.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun showUiControls() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.lnUiState.visible()
            hideUiJob?.cancel()
            hideUiJob = lifecycleScope.launch {
                delay(uiVisibleDuration)
                withContext(Dispatchers.Main) {
                    binding.lnUiState.gone()
                }
            }
        }
    }

    private var hideUiJob: Job? = null
    override fun ActivitySlideShowBinding.setData() {
        showUiControls()
        root.setOnLongClickListener {
            showUiControls()
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideUiJob?.cancel()
        hideUiJob = null
    }

    override fun ActivitySlideShowBinding.onClick() {
        icBack.setOnClickListener { backPressed() }
        icNexPage.setOnClickListener {
            val next = binding.viewPager2.currentItem + 1
            if (next < adapter.itemCount) {
                binding.viewPager2.currentItem = next
            }
        }
        icPrevPage.setOnClickListener {
            val prev = binding.viewPager2.currentItem - 1
            if (prev >= 0) {
                binding.viewPager2.currentItem = prev
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        binding.tvPageCurrent.text = "${position + 1}/${adapter.itemCount}"
    }

    private class SlideAdapter(
        private val items: List<DocumentPage>
    ) : RecyclerView.Adapter<SlideAdapter.SlideVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideVH {
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.WHITE)
            }
            return SlideVH(imageView)
        }

        override fun onBindViewHolder(holder: SlideVH, position: Int) {
            holder.imageView.setImageBitmap(items[position].bitmap)
        }

        override fun getItemCount(): Int = items.size

        class SlideVH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}
