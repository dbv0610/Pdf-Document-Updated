package com.azg.pdf8.ui.main.document

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.R
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityRecentBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.dialog.DeleteDialog
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.dialog.SortFavoriteDialog
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.document.other.ReadDocumentActivity
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.base.PopupDataHelper
import com.dong.baselib.file.shareFileWithPath
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import org.koin.android.ext.android.inject
import kotlin.getValue

class RecentActivity : BaseActivity<ActivityRecentBinding>(ActivityRecentBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val documentViewModel: DocumentViewModel by inject()
    private var currentIndex = 0
    var popupHerper: PopupDataHelper<PopupMenuActionBinding, RecentDocument>? = null
    private val recentAdapter by lazy {
        RecentAdapter(onFavoriteClick = { doc, index ->
            currentIndex = 0
            documentViewModel.toggleFavoriteRecent(doc)
        }, onMenuClick = { view, doc, pos ->
            popupHerper?.show(view, doc)
        }) {
            if (it.type == DocumentType.Pdf) {
                launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            } else {
                val docKey = when (it.type) {
                    DocumentType.Doc -> Constant.Doc
                    DocumentType.Excel -> Constant.Xls
                    else -> Constant.Ppt
                }
                launchActivity<ReadDocumentActivity>(
                    hashMapOf(
                        Constant.DOCUMENT_TYPE to docKey,
                        Constant.ARG_MEDIA_MODEL to it
                    )
                )
            }

            documentViewModel.addToRecent(it.apply { it.lastTimeView = System.currentTimeMillis() })
        }.attachLifecycle(this@RecentActivity)
    }

    override fun initialize() {
        popupHerper?.onBindData { binding, popup, model ->
            binding.lnShare.click {
                shareFileWithPath(this@RecentActivity, model?.path ?: "")
                popup.dismiss()
            }
            binding.lnRename.click {
                popup.dismiss()
                RenameDialog(this@RecentActivity) { newN ->
                    model?.let {
                        documentViewModel.renameFile(model, newN)
                    }
                }.showRename(model?.path?.fileName() ?: "")
            }
            binding.lnDelete.click {
                popup.dismiss()
                DeleteDialog(this@RecentActivity) {
                    model?.let {
                        documentViewModel.removeRecent(model)
                    }
                }.show()
            }
        }
    }

    override fun ActivityRecentBinding.setData() {
        lifecycleLaunch {
            documentViewModel.listRecentSearch.collect {
                if (it.isEmpty()) {
                    rcvListData.gone()
                    lnNoData.visible()
                } else {
                    rcvListData.visible()
                    lnNoData.gone()
                    val lm = rcvListData.layoutManager as LinearLayoutManager
                    recentAdapter.submitList(it) {
                        lm.scrollToPositionWithOffset(0, 0)
                    }
                }
            }
        }
        rcvListData.adapter = recentAdapter

        edtSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    documentViewModel.setSearchCriteria(query, null)
                }
                v.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        edtSearch.afterTextChanged {
            val query = it.trim()
            documentViewModel.setSearchCriteria(query, null)
        }
    }

    override fun ActivityRecentBinding.onClick() {
        icSearch.click {
            val query = edtSearch.text.toString().trim()
            documentViewModel.setSearchCriteria(query, null)
        }
        icBack.click {
            backPressed()
        }
        lnSortData.click {
            SortFavoriteDialog(this@RecentActivity)
                .attachLifecycle(this@RecentActivity)
                .showFileType(documentViewModel.filterTypesFlow.value.toMutableList())
                .showSortDate(documentViewModel.sortByDateFlow.value)
                .showSortSize(documentViewModel.sortBySizeFlow.value)
                .onSearchEvent { byDate, bySize, types ->
                    documentViewModel.filterSortFavorite(byDate, bySize, types)
                }
                .show()
        }
    }
}