package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemPhotoViewBinding
import com.azg.pdf8.model.RecentDocument
import com.bumptech.glide.Glide
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible

object PhotoListItemDiff : DiffUtil.ItemCallback<RecentDocument>() {
    override fun areItemsTheSame(old: RecentDocument, new: RecentDocument): Boolean {
        return old.mediaId == new.mediaId
    }

    override fun areContentsTheSame(old: RecentDocument, new: RecentDocument): Boolean {
        return old == new
                && old.isSelected == new.isSelected
    }
}

interface ViewActionHandle {
    fun onSelect(recentDocument: RecentDocument) {}
}

class PhotoGalleryAdapter(
    private val viewActionHandle: ViewActionHandle
) : ListAdapter<RecentDocument, PhotoGalleryAdapter.ItemPhotoViewHolder>(PhotoListItemDiff) {
    fun checkStateSelect(mergerListSelect: List<RecentDocument>) {
        val selectedIds = mergerListSelect.map { it.mediaId }.toSet()
        val updated = currentList.map { doc ->
            doc.clone().apply {
                isSelected = mediaId in selectedIds
            }
        }
        submitList(updated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPhotoViewBinding.inflate(inflater, parent, false)
        return ItemPhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemPhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemPhotoViewHolder(
        private val binding: ItemPhotoViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecentDocument) {
            Glide.with(binding.root)
                .load(item.path)
                .into(binding.imageView)
            if (item.isSelected) {
                binding.imgSelect.visible()
            } else {
                binding.imgSelect.gone()
            }
            binding.layoutOpacity.isVisible = item.isSelected
            binding.imgSelect.setOnClickListener {
                viewActionHandle.onSelect(item)
            }
            binding.root.setOnClickListener {
                viewActionHandle.onSelect(item)
            }
        }
    }
}

