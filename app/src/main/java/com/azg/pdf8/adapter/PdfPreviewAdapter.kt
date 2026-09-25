package com.azg.pdf8.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemPdfThumbBinding
import com.azg.pdf8.model.DocumentPage
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.widget.click
import com.dong.baselib.widget.transparent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PdfPreviewAdapter(
    private val thumbnailScope: CoroutineScope? = null,
    private val loadThumbnail: (suspend (Int) -> Bitmap?)? = null,
    val currentSelPage: (DocumentPage) -> Unit = {}
) :
    RecyclerView.Adapter<PdfPreviewAdapter.PdfPageViewHolder>() {
    private var pages = emptyList<DocumentPage>()

    fun submitList(newPages: List<DocumentPage>) {
        pages = newPages
        notifyDataSetChanged()
    }

    private var currentPage = MutableLiveData(0)
    fun setCurrentPage(page:Int){
        currentPage.value = page
    }
    private var lifecycle: LifecycleOwner? = null
    fun attachLifecycle(lifecycle: LifecycleOwner):PdfPreviewAdapter {
        this@PdfPreviewAdapter.lifecycle = lifecycle
        return  this@PdfPreviewAdapter
    }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val binding = ItemPdfThumbBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun onViewRecycled(holder: PdfPageViewHolder) {
        holder.clear()
    }

    inner class PdfPageViewHolder(private val binding: ItemPdfThumbBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null
        private var boundIndex: Int? = null

        fun bind(page: DocumentPage) {
            thumbnailJob?.cancel()
            boundIndex = page.index
            binding.imagePreview.setImageDrawable(null)
            lifecycle?.let { lf ->
                currentPage.observe(lf) {
                    binding.root.strokeColor(if (it == adapterPosition) mainColor else transparent)
                }
            }
            binding.apply {
                when {
                    loadThumbnail != null || page.isLoading -> {
                        imagePreview.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE
                        errorText.visibility = View.GONE
                        progressBar.isIndeterminate = true
                    }
                    page.error != null -> {
                        imagePreview.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        errorText.visibility = View.VISIBLE
                        errorText.text = "Error: ${page.error.message}"
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                        errorText.visibility = View.GONE
                        imagePreview.visibility = View.VISIBLE
                        page.bitmap?.let { bitmap ->
                            imagePreview.setImageBitmap(bitmap)
                        } ?: run {
                            imagePreview.setImageResource(R.drawable.img_no_permission)
                        }
                    }
                }
            }
            val loader = loadThumbnail
            if (loader != null) {
                thumbnailJob = thumbnailScope?.launch {
                    val bitmap = loader(page.index)
                    if (boundIndex == page.index) {
                        binding.progressBar.visibility = View.GONE
                        binding.errorText.visibility = View.GONE
                        binding.imagePreview.visibility = View.VISIBLE
                        if (bitmap != null) {
                            binding.imagePreview.setImageBitmap(bitmap)
                        } else {
                            binding.imagePreview.setImageResource(R.drawable.img_no_permission)
                        }
                    }
                }
            }
            binding.root.click {
                currentSelPage.invoke(page)
            }
        }

        fun clear() {
            thumbnailJob?.cancel()
            thumbnailJob = null
            boundIndex = null
            binding.imagePreview.setImageDrawable(null)
        }
    }
}