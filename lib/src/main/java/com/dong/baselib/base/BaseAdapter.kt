package com.dong.baselib.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.dong.baselib.lifecycle.change
import java.util.Collections

abstract class BaseAdapter<T, VB : ViewBinding> :
    RecyclerView.Adapter<BaseAdapter<T, VB>.ViewHolder>() {
    lateinit var context: Context
    private val listItem = mutableListOf<T>()
    var binding: VB? = null
    var currentPosition = MutableLiveData<Int>(RecyclerView.NO_POSITION)

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): VB
    protected var lifecycle: LifecycleOwner? = null

    fun attachLifecycle(lifecycle: LifecycleOwner): BaseAdapter<T,VB> {
        this@BaseAdapter.lifecycle = lifecycle
        return this@BaseAdapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = createBinding(inflater, parent, viewType)
        context = parent.context
        return ViewHolder(binding!!)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItem[position]
        holder.binding.bind(item, position)
    }

    override fun getItemCount(): Int = listItem.size

    abstract fun VB.bind(item: T, position: Int)

    var lastIndex: Int = 0

    fun submitList(items: List<T>) {
        this.listItem.clear()
        this.listItem.addAll(items)
        notifyDataSetChanged()
    }

    fun submitList(items: MutableLiveData<MutableList<T>>) {
        items.change {
            this.listItem.clear()
            this.listItem.addAll(it)
            notifyDataSetChanged()
        }
    }

    fun removeItem(position: Int) {
        if (position >= 0) {
            listItem.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addItems(item: MutableList<T>) {
        this.listItem.addAll(item)
        notifyDataSetChanged()
    }

    fun onMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(listItem, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(listItem, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun swipe(position: Int) {
        listItem.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addItem(item: T, index: Int) {
        this.listItem.add(index, item)
        notifyItemInserted(index)
    }

    fun removeItem(item: T) {
        val index = listItem.indexOf(item)
        listItem.remove(item)
        notifyItemRemoved(index)
    }

    fun changeItemWithPos(index: Int, newItem: T) {
        if (index < 0 || index >= listItem.size) return
        listItem[index] = newItem;
        notifyItemChanged(index)
    }

    fun getListItem(): MutableList<T> {
        return listItem as MutableList<T>
    }

    fun setCurrentPos(position: Int) {
        notifyItemChanged(currentPosition.value ?: RecyclerView.NO_POSITION)
        currentPosition.value = position
        notifyItemChanged(position)
    }

    inner class ViewHolder(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}
