package com.azg.pdf8.ui.main.document.pdf

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.R
import com.azg.pdf8.adapter.SearchAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivitySearchResultBinding
import com.azg.pdf8.model.ContentWithPage
import com.azg.pdf8.utils.Constant.ARG_SEARCH_RESULT_PAGE
import com.azg.pdf8.utils.Constant.ARG_SEARCH_WITH_PAGE
import com.dong.baselib.api.parcelableList

class SearchResultActivity :
    BaseActivity<ActivitySearchResultBinding>(ActivitySearchResultBinding::inflate) {
    private val searchList by lazy {
        runCatching {
            intent.parcelableList<ContentWithPage>(ARG_SEARCH_WITH_PAGE)
        }
    }
    private val adapter by lazy {
        SearchAdapter { item ->
            setResult(Activity.RESULT_OK, Intent().putExtra(ARG_SEARCH_RESULT_PAGE, item.page))
            finish()
        }
    }

    private fun initRecyclerview(searchList: List<ContentWithPage>) {
        binding.rvLinksResult.layoutManager = LinearLayoutManager(this)
        binding.rvLinksResult.adapter = adapter
        binding.rvLinksResult.setHasFixedSize(true)
        adapter.updateData(searchList)
    }

    override fun backPressed() {
        finish()
    }

    override fun initialize() {

    }
    @SuppressLint("SetTextI18n")
    override fun ActivitySearchResultBinding.setData() {
        searchList.getOrNull()?.let {
            initRecyclerview(it)
            binding.txtLinkTitle.text = "${getString(R.string.search_result)} (${it.size})"
        }
    }

    override fun ActivitySearchResultBinding.onClick() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}