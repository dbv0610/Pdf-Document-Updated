package com.azg.pdf8.widget.swap

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class RecyclerViewSwapManager<T>(
    private val adapter: ListAdapter<T, *>,
    private val recyclerView: RecyclerView
) {
    private val itemTouchHelper by lazy {
        createItemTouchHelper(
            getRecyclerViewOrientation(
                recyclerView
            )
        )
    }

    fun enableDragAndDrop() {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun getRecyclerViewOrientation(recyclerView: RecyclerView): Int? {
        return when (val layoutManager = recyclerView.layoutManager) {
            is LinearLayoutManager -> layoutManager.orientation
            is GridLayoutManager -> layoutManager.orientation
            else -> null
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentList = adapter.currentList.toMutableList()
        if (fromPosition in 0 until currentList.size && toPosition in 0 until currentList.size) {
            Collections.swap(currentList, fromPosition, toPosition)
            adapter.submitList(currentList)
        }
    }

    private fun createItemTouchHelper(recyclerViewOrientation: Int?): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                recyclerViewOrientation?.let {
                    return makeMovementFlags(
                        if (it == RecyclerView.HORIZONTAL) ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                        else ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                        0
                    )
                } ?: run {
                    return makeMovementFlags(
                        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                        0
                    )
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                swapItems(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
        })
    }
}