package com.wxiwei.office.fc.hslf.blip

import com.wxiwei.office.fc.hslf.model.Picture
import com.wxiwei.office.fc.util.LittleEndian

class DIB : Bitmap() {
    override fun getType(): Int {
        return Picture.DIB.toInt()
    }

    override fun getSignature(): Int {
        return 0x7A80
    }

    override fun getData(): ByteArray {
        return addBMPHeader(super.getData())
    }

    override fun setData(data: ByteArray) {
        super.setData(data.copyOfRange(HEADER_SIZE, data.size))
    }

    companion object {
        const val HEADER_SIZE: Int = 14

        @JvmStatic
        fun addBMPHeader(data: ByteArray): ByteArray {
            val header = ByteArray(HEADER_SIZE)
            LittleEndian.putInt(header, 0, 0x4D42)
            val imageSize = LittleEndian.getInt(data, 0x22 - HEADER_SIZE)
            val fileSize = data.size + HEADER_SIZE
            val offset = fileSize - imageSize
            LittleEndian.putInt(header, 2, fileSize)
            LittleEndian.putInt(header, 6, 0)
            LittleEndian.putInt(header, 10, offset)
            return header + data
        }
    }
}
