package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemPhotoViewBinding
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.viewmodel.UiGalleryState
import com.bumptech.glide.Glide
import java.util.Calendar


object PhotoListItemDiff : DiffUtil.ItemCallback<DocumentModel>() {
    override fun areItemsTheSame(old: DocumentModel, new: DocumentModel): Boolean {
        return old.mediaId == new.mediaId
    }

    override fun areContentsTheSame(old: DocumentModel, new: DocumentModel): Boolean {
        return old == new
                && old.isSelected == new.isSelected
    }
}

interface ViewActionHandle {
    fun onView(documentModel: DocumentModel) {}
    fun onSelect(documentModel: DocumentModel) {}
    fun onUpdateState(documentModel: DocumentModel, position: Int) {}
}

class PhotoGalleryAdapter(
    private val viewActionHandle: ViewActionHandle
) : ListAdapter<DocumentModel, PhotoGalleryAdapter.ItemPhotoViewHolder>(PhotoListItemDiff) {

    private var currentState = UiGalleryState.Default

    fun setCurrentState(state: UiGalleryState) {
        this.currentState = state
        notifyDataSetChanged()
    }

    fun checkStateSelect(mergerListSelect: List<DocumentModel>) {
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

        fun bind(item: DocumentModel) {
            Glide.with(binding.root)
                .load(item.path)
                .into(binding.imageView)
            binding.imgSelect.setImageResource(
                if (item.isSelected)
                    R.drawable.ic_view_selectecd
                else
                    R.drawable.ic_language_unselected
            )
            binding.imgSelect.isVisible = currentState != UiGalleryState.Default
            binding.layoutOpacity.isVisible = item.isSelected
            binding.imgSelect.setOnClickListener {
                viewActionHandle.onSelect(item)
            }
            binding.root.setOnClickListener {
                if (currentState != UiGalleryState.Select) {
                    viewActionHandle.onView(item)
                }
            }
            binding.root.setOnLongClickListener {
                viewActionHandle.onUpdateState(item, adapterPosition)
                true
            }
        }
    }
}

