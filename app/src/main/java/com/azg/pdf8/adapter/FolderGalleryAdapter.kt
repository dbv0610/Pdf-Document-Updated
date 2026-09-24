package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.databinding.ItemViewFolderBinding
import com.azg.pdf8.model.FolderItem
import com.bumptech.glide.Glide

val folderItemDiff = object : DiffUtil.ItemCallback<FolderItem>() {
    override fun areItemsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        return oldItem.folderName == newItem.folderName
    }

    override fun areContentsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        return oldItem == newItem
    }
}

class FolderGalleryAdapter(
    private val callback: (FolderItem) -> Unit = {}
) : ListAdapter<FolderItem, FolderGalleryAdapter.FolderViewHolder>(folderItemDiff) {
    inner class FolderViewHolder(
        private val binding: ItemViewFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FolderItem) = with(binding) {
            txtName.text = item.folderName
            txtCount.text = item.itemCount.toString()

            Glide.with(root.context)
                .load(item.previewPath)
                .centerCrop()
                .into(imagePreview)

            root.setOnClickListener {
                callback.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding =
            ItemViewFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

