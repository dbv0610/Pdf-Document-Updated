package com.wxiwei.office.fc.hslf.blip

import com.wxiwei.office.fc.hslf.usermodel.PictureData

abstract class Bitmap : PictureData() {
    override fun getData(): ByteArray {
        val rawdata = getRawData()
        return rawdata.copyOfRange(17, rawdata.size)
    }

    override fun setData(data: ByteArray) {
    }
}
