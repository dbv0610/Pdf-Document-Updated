package com.azg.pdf8.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.model.FolderItem
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.ScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UiGalleryState {
    Default, Select
}

enum class ScreenType {
    Folder, Detail
}

class ScanImageViewModel(val repository: AppDataRepo) : ViewModel() {
    private val _headerUiState = MutableStateFlow(UiGalleryState.Default)
    val headerUiState = _headerUiState.asStateFlow()
    fun setHeaderState(state: UiGalleryState) {
        _headerUiState.value = state
    }

    private val _listSelectPhotoFolder = MutableStateFlow<List<DocumentModel>>(emptyList())
    val listSelectPhotoFolder = _listSelectPhotoFolder.asStateFlow()
    private val _listPhotoFolder = MutableStateFlow<List<FolderItem>>(emptyList())
    val listPhotoFolder = _listPhotoFolder.asStateFlow()
    private val _scType = MutableStateFlow(ScreenType.Detail)
    val screenType = _scType.asStateFlow()
    fun setScType(sc: ScreenType) {
        _scType.value = sc
    }
    private val _listSelectedItem = MutableStateFlow<List<DocumentModel>>(emptyList())
    val listSelectedItem = _listSelectedItem.asStateFlow()

    fun modifyItemSelect(model: DocumentModel) {
        val current = _listSelectedItem.value
        val exists = current.any { it.path == model.path }
        _listSelectedItem.value = if (exists) {
            current.filter { it.path != model.path }
        } else {
            current + model
        }
    }

    fun clearSelected(){
        _listSelectedItem.value = mutableListOf<DocumentModel>()
    }

    fun groupFolder(mutableList: MutableList<DocumentModel>) {
        _listPhotoFolder.value = repository.groupToFolderList(mutableList)
        _listSelectPhotoFolder.value = mutableList
    }

    fun setCurrentPhotoModel(folderItem: FolderItem) {
        _scType.value = ScreenType.Detail
        _listSelectPhotoFolder.value = folderItem.listData
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Start)
    val scanState: StateFlow<ScanState> = _scanState

    fun startScan(context: Context) {
        viewModelScope.launch {
            AppUtils.scanImagesFlow(context).collect {
                _scanState.value = it
            }
        }
    }

    fun removeList(i: Int) {}

}
