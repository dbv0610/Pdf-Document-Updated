package com.azg.pdf8.ui.main.favorite

import android.annotation.SuppressLint
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.adapter.FavoriteAdapter
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentFavoriteBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding.inflate
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DeleteDialog
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.dialog.SortFavoriteDialog
import com.azg.pdf8.model.FavoriteDocument
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.base.PopupDataHelper
import com.dong.baselib.file.shareFileWithPath
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.paddingTop
import com.dong.baselib.widget.visible
import org.koin.android.ext.android.inject
import kotlin.getValue
import kotlin.text.trim

class FavoriteFragment :
    BaseFragment<FragmentFavoriteBinding>(FragmentFavoriteBinding::inflate, true) {
    val documentViewModel: DocumentViewModel by inject()

    override fun backPress() {
        super.backPress()
        fragmentAttach?.fragmentOnBack()
    }

    private var currentIndex = 0
    var popupHerper: PopupDataHelper<PopupMenuActionBinding, FavoriteDocument>? = null
    private val favoriteAdapter by lazy {
        FavoriteAdapter(onFavoriteClick = { document, index ->
            documentViewModel.toggleFavorite(document)
        }, onMenuClick = { view, doc, pos ->
            popupHerper?.show(view, doc)
        }) {
            launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            documentViewModel.addToRecent(it)
        }.attachLifecycle(viewLifecycleOwner)
    }
    @SuppressLint("SetTextI18n")
    override fun FragmentFavoriteBinding.initView() {
        lnHeader.paddingTop(statusBarHeight + appActivity.dimenSdp(6))
        lifecycleLaunch {
            documentViewModel.listFavoriteSearch.collect {
                if (it.isEmpty()) {
                    rcvListData.gone()
                    lnNoData.visible()
                } else {
                    rcvListData.visible()
                    lnNoData.gone()
                    val lm = rcvListData.layoutManager as LinearLayoutManager
                    favoriteAdapter.submitList(it) {
                        lm.scrollToPositionWithOffset(0, 0)
                    }
                }
            }
        }
        rcvListData.adapter = favoriteAdapter

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

        popupHerper = PopupDataHelper.with(
            appActivity,
            PopupMenuActionBinding::inflate
        )
        popupHerper?.onBindData { binding, popup, model ->
            binding.lnShare.click {
                shareFileWithPath(appActivity, model?.path ?: "")
                popup.dismiss()
            }
            binding.lnRename.click {
                popup.dismiss()
                RenameDialog(appActivity) { newN ->
                    model?.let {
                        documentViewModel.renameFile(model, newN)
                    }
                }.showRename(model?.path?.fileName() ?: "")
            }
            binding.lnDelete.click {
                popup.dismiss()
                DeleteDialog(appActivity) {
                    model?.let {
                        documentViewModel.removeFavorite(model)
                    }
                }.show()
            }
        }
    }

    override fun FragmentFavoriteBinding.onClick() {
        icSearch.click {
            val query = edtSearch.text.toString().trim()
            documentViewModel.setSearchCriteria(query, null)
        }
        lnSortData.click {
            SortFavoriteDialog(appActivity)
                .attachLifecycle(viewLifecycleOwner)
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