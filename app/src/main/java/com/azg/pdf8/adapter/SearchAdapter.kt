package com.azg.pdf8.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemContentSearchBinding
import com.azg.pdf8.model.ContentWithPage

class SearchAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val listContentSearch = ArrayList<ContentWithPage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewBinding = ItemContentSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LinkViewHolder(viewBinding)
    }

    fun updateData(list: List<ContentWithPage>) {
        this.listContentSearch.clear()
        if (list.isNotEmpty()) {
            this.listContentSearch.addAll(list)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return listContentSearch.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LinkViewHolder -> {
                holder.bind(position)
            }
        }
    }

    inner class LinkViewHolder(itemView: ItemContentSearchBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        internal val binding = itemView
        fun bind(position: Int) {
            val searchModel = listContentSearch[position]
            val page = "${binding.root.context.getString(R.string.page)} \n ${searchModel.page}"
            binding.txtPage.text = page
            binding.txtContent.text = searchModel.content

            binding.btnExpand.setOnClickListener {
                searchModel.expand = !searchModel.expand
                if (searchModel.expand) {
                    binding.txtContent.maxLines = Integer.MAX_VALUE
                    binding.imgExpand.setImageResource(R.drawable.ic_drop_up)
                } else {
                    binding.txtContent.maxLines = 4
                    binding.imgExpand.setImageResource(R.drawable.ic_drop_down)
                }
            }
        }
    }

}

