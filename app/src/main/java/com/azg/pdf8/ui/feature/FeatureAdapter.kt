package com.azg.pdf8.ui.feature

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.databinding.ItemFeatureViewBinding
import com.azg.pdf8.widget.color_C9CDD4
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.widget.black
import com.dong.baselib.widget.white

class FeatureAdapter(
    private val items: MutableList<FeatureModel>,
    private val onSelectedItem: (FeatureModel) -> Unit,
) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FeatureViewHolder(
            ItemFeatureViewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class FeatureViewHolder(
        private val binding: ItemFeatureViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: FeatureModel) {
            binding.tvContent.apply {
                text = binding.root.context.getString(model.name)
                strokeColor(
                    if (model.isSelected) mainColor else color_C9CDD4
                )
                backgroundAll(if (model.isSelected) mainColor else white)
                setTextColor(if (model.isSelected) white else black)
            }
            binding.root.setOnClickListener {
                model.isSelected = !model.isSelected
                notifyItemChanged(adapterPosition)
                onSelectedItem(model)
            }
        }
    }
}