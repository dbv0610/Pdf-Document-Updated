package com.azg.pdf8.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemViewLanguageBinding
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.widget.fromColor
import com.dong.baselib.widget.transparent

class LfoAdapter : ListAdapter<LanguageItem, LfoAdapter.LanguageViewHolder>(LanguageItemDiffCallback()) {

    private var onItemSelected: ((LanguageItem) -> Unit)? = null
    private var tutorialMode: Boolean = false

    fun setOnItemSelected(listener: (LanguageItem) -> Unit) {
        onItemSelected = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemViewLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding).apply {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemSelected?.invoke(getItem(position))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] == PAYLOAD_SELECTION_CHANGE) {
            holder.updateSelectionState(getItem(position).isChoose)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun selectItem(item: LanguageItem) {
        val current = currentList.toMutableList().map {
            it.copy(isChoose = it.code == item.code, isDefault = false)
        }
        submitList(current) {}
    }

    fun getSelectedLanguage(): LanguageItem? = currentList.firstOrNull { it.isChoose }

    fun enableTutorialMode() {
        tutorialMode = true
        notifyItemChanged(currentList.indexOfFirst { it.isDefault })
    }

    inner class LanguageViewHolder(
        private val binding: ItemViewLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageItem) {
            with(binding) {
                imgItemFlag.setImageResource(item.flagId)
                tvItemName.text = item.name
                updateSelectionState(item.isChoose)
                lavClick.isVisible = tutorialMode && item.isDefault
            }
        }

        fun updateSelectionState(isSelected: Boolean) {
            with(binding) {
                imgItemSelect.apply {
                    setImageResource(
                        if (isSelected) R.drawable.ic_language_selected
                        else R.drawable.ic_language_unselected
                    )
                }
                llFocusItem.strokeColor(
                    if (isSelected) mainColor else transparent
                )
            }
        }
    }

    private class LanguageItemDiffCallback : DiffUtil.ItemCallback<LanguageItem>() {
        override fun areItemsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: LanguageItem, newItem: LanguageItem): Any? {
            return if (oldItem.isChoose != newItem.isChoose) {
                PAYLOAD_SELECTION_CHANGE
            } else {
                null
            }
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGE = "payload_selection_change"
    }
}