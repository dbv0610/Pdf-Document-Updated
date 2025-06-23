package com.ag.sampleadsfirstflow.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ag.sampleadsfirstflow.databinding.ItemLfoLanguageBinding
import com.ag.sampleadsfirstflow.model.LanguageItem

class LfoAdapter : ListAdapter<LanguageItem, LfoAdapter.LanguageViewHolder>(
    object : DiffUtil.ItemCallback<LanguageItem>() {
        override fun areItemsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
            return oldItem == newItem
        }
    }) {
    companion object {
        const val PAYLOAD_SELECTED = "PAYLOAD_SELECTED"
    }

    private var onItemSelected: (item: LanguageItem) -> Unit = {}
    private var isStartTutorial: Boolean = false

    fun setOnItemSelected(onItemSelected: (item: LanguageItem) -> Unit) = apply {
        this.onItemSelected = onItemSelected
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        return LanguageViewHolder(
            ItemLfoLanguageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            if (payloads.contains(PAYLOAD_SELECTED)) {
                holder.bindViewSelected(getItem(position).isChoose)
            }
        }
    }

    inner class LanguageViewHolder(private val binding: ItemLfoLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindView(item: LanguageItem) {
            binding.imgItemFlag.setImageResource(item.flagId)
            binding.tvItemName.text = item.name
            binding.imgItemSelect.isSelected = item.isChoose
            binding.llItemLanguage.setOnClickListener {
                if (item.isChoose) return@setOnClickListener
                onItemSelected(item)
            }
            updateTutorialLanguage(item)
        }

        fun bindViewSelected(selected: Boolean) {
            binding.imgItemSelect.isSelected = selected
        }

        private fun updateTutorialLanguage(item: LanguageItem) {
            if (!isStartTutorial) return
            if (item.isDefault) {
                binding.lavClick.isVisible = true
            } else {
                binding.lavClick.isVisible = false
            }
        }
    }

    fun selectedItem(item: LanguageItem) {
        val indexUnselected = currentList.indexOfFirst { it.isChoose }
        val indexSelected = currentList.indexOf(item)
        item.isChoose = true
        if (indexUnselected != -1) {
            currentList[indexUnselected].isChoose = false
            notifyItemChanged(indexUnselected, PAYLOAD_SELECTED)
        }
        notifyItemChanged(indexSelected, PAYLOAD_SELECTED)
    }

    fun getLanguageSelected(): LanguageItem? {
        return currentList.find { it.isChoose }
    }

    fun startTutorial() {
        isStartTutorial = true
        val index = currentList.indexOfFirst { it.isDefault }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun selectedLanguage(languageCode: String) {
        val indexUnselected = currentList.indexOfFirst { it.isChoose }
        if (indexUnselected != -1) {
            currentList[indexUnselected].isChoose = false
            notifyItemChanged(indexUnselected)
        }
        val indexSelected = currentList.indexOfFirst { it.code == languageCode }
        if (indexSelected != -1) {
            currentList[indexSelected].isChoose = true
            notifyItemChanged(indexSelected)
        }
    }
}
