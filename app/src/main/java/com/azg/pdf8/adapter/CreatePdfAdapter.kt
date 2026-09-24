package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.databinding.ItemCreatePdfBinding
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.widget.swap.BaseSwappableAdapter
import com.bumptech.glide.Glide
import com.dong.baselib.widget.click
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreatePdfAdapter(
    val onRemove: (CreatePdf, MutableList<CreatePdf>) -> Unit = {_, _ ->},
    val onEdit: (CreatePdf) -> Unit = {},
) :
    BaseSwappableAdapter<CreatePdf, CreatePdfAdapter.ViewHolder>(MyItemDiffCallback()) {
    private var onListChanged: ((List<CreatePdf>) -> Unit)? = null
    private var lifecycleOwner: LifecycleOwner? = null

    fun setOnListChangedListener(listener: (List<CreatePdf>) -> Unit) {
        onListChanged = listener
    }

    var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun attachToLifecycle(owner: LifecycleOwner): CreatePdfAdapter {
        lifecycleOwner = owner
        owner.lifecycleScope.launch {
            currentAdapterList.collectLatest { newList ->
                onListChanged?.invoke(newList)
            }
        }
        return this
    }



    inner class ViewHolder(val binding: ItemCreatePdfBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindItem(model: CreatePdf) {
            Glide.with(binding.root.context)
                .load(model.picture).into(binding.viewData)
            binding.icCrop.click {
                onEdit.invoke(model)
            }
            binding.icDelete.click {
                onRemove.invoke(model,currentList.toMutableList())
            }
            binding.icMove.setOnTouchListener { v, ev ->
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    dragStartListener?.invoke(this)
                }
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCreatePdfBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindItem(item)
    }

    class MyItemDiffCallback : DiffUtil.ItemCallback<CreatePdf>() {
        override fun areItemsTheSame(oldItem: CreatePdf, newItem: CreatePdf): Boolean {
            return oldItem.defId == newItem.defId
        }

        override fun areContentsTheSame(oldItem: CreatePdf, newItem: CreatePdf): Boolean {
            return oldItem == newItem
        }
    }
}