package com.wxiwei.office.fc.ppt

import android.graphics.Bitmap
import com.wxiwei.office.constant.MainConstant

class PPTReaderThumbnail private constructor() {
    fun getThumbnail(filePath: String): Bitmap? {
        return try {
            val fileName = filePath.lowercase()
            if (fileName.endsWith(MainConstant.FILE_TYPE_PPT) || fileName.endsWith(MainConstant.FILE_TYPE_POT)) {
                getThumbnailForPPT(filePath)
            } else if (fileName.endsWith(MainConstant.FILE_TYPE_PPTX) ||
                fileName.endsWith(MainConstant.FILE_TYPE_PPTM) ||
                fileName.endsWith(MainConstant.FILE_TYPE_POTX) ||
                fileName.endsWith(MainConstant.FILE_TYPE_POTM)
            ) {
                getThumbnailForPPT(filePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getThumbnailForPPT(filePath: String): Bitmap? {
        return null
    }

    private fun getThumbnailForPPTX(filePath: String): Bitmap? {
        return null
    }

    companion object {
        private val kit = PPTReaderThumbnail()

        @JvmStatic
        fun instance(): PPTReaderThumbnail {
            return kit
        }
    }
}
