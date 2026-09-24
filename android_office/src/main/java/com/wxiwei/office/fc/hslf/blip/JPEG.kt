package com.wxiwei.office.fc.hslf.blip

import com.wxiwei.office.fc.hslf.model.Picture

class JPEG : Bitmap() {
    override fun getType(): Int {
        return Picture.JPEG
    }

    override fun getSignature(): Int {
        return 0x46A0
    }
}
