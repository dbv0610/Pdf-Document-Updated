package com.dong.baselib.listener

interface FileListener {
    fun onFileSelected(filePath: String){}
    fun onFileCancelled(){}
    fun onFileError(errorMessage: String){}
    fun onSaveSuccess(filePath: String){}
    fun onChooseSingleFile(filePath: String){}
    fun onSaveError(errorMessage: String){}
    fun onChooseMultipleFiles(filePaths: List<String>){}
}