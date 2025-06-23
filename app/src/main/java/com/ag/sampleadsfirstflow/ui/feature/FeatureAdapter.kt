package com.ag.sampleadsfirstflow.ui.feature

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ag.sampleadsfirstflow.databinding.ItemFeatureBinding
import com.ag.sampleadsfirstflow.model.FeatureModel

class FeatureAdapter(
    private val onSelectedItem: () -> Unit,
) : ListAdapter<FeatureModel, FeatureAdapter.FeatureViewHolder>(
    object : DiffUtil.ItemCallback<FeatureModel>() {
        override fun areItemsTheSame(oldItem: FeatureModel, newItem: FeatureModel): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: FeatureModel, newItem: FeatureModel): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        return FeatureViewHolder(
            ItemFeatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    inner class FeatureViewHolder(private val binding: ItemFeatureBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindView(item: FeatureModel) {
            binding.imgIcon.setImageResource(item.icon)
            binding.tvTitle.text = item.name
            binding.ctlContainer.isSelected = item.isSelected
            itemView.setOnClickListener {
                item.isSelected = !item.isSelected
                binding.ctlContainer.isSelected = item.isSelected
                onSelectedItem()
            }
        }
    }
}
