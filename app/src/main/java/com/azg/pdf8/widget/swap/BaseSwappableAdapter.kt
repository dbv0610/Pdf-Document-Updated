package com.azg.pdf8.widget.swap

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseSwappableAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback) {
    private var swapManager: RecyclerViewSwapManager<T>? = null
    private val _currentList = MutableStateFlow<List<T>>(emptyList())
    val currentAdapterList: StateFlow<List<T>> = _currentList.asStateFlow()


    fun setupSwapManager(recyclerView: RecyclerView): BaseSwappableAdapter<T, VH> {
        swapManager = RecyclerViewSwapManager(this, recyclerView)
        return this
    }

    fun enableDragAndDrop() {
        swapManager?.enableDragAndDrop()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentItems = currentList.toMutableList()
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                currentItems[i] = currentItems[i + 1].also { currentItems[i + 1] = currentItems[i] }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                currentItems[i] = currentItems[i - 1].also { currentItems[i - 1] = currentItems[i] }
            }
        }
        _currentList.value = currentItems
        submitList(currentItems)
        swapManager?.swapItems(fromPosition, toPosition)
    }

    override fun submitList(list: List<T>?) {
        super.submitList(list)
        _currentList.value = list ?: emptyList()
    }
}