package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemPdfThumbBinding
import com.azg.pdf8.model.PdfPage
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.widget.click
import com.dong.baselib.widget.transparent
import kotlinx.coroutines.launch

class PdfPreviewAdapter(val currentSelPage: (PdfPage) -> Unit = {}) :
    RecyclerView.Adapter<PdfPreviewAdapter.PdfPageViewHolder>() {
    private var pages = emptyList<PdfPage>()

    fun submitList(newPages: List<PdfPage>) {
        pages = newPages
        notifyDataSetChanged()
    }

    private var currentPage = MutableLiveData(0)
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
        fun bind(page: PdfPage) {
            lifecycle?.let { lf ->
                currentPage.observe(lf) {
                    binding.root.strokeColor(if (it == adapterPosition) mainColor else transparent)
                }
            }
            binding.apply {
                when {
                    page.isLoading -> {
                        imagePreview.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE
                        errorText.visibility = View.GONE
                        progressBar.isIndeterminate = true
                    }
                    page.error != null -> {
                        imagePreview.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        errorText.visibility = View.VISIBLE
                        errorText.text = "Error: ${page.error?.message}"
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
            binding.root.click {
                currentSelPage.invoke(page)
                currentPage.value = adapterPosition
            }
        }

        fun clear() {
            binding.imagePreview.setImageDrawable(null)
        }
    }
}