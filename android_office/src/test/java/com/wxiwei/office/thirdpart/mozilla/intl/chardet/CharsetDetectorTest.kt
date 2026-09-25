package com.wxiwei.office.thirdpart.mozilla.intl.chardet

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedInputStream
import java.nio.charset.Charset

class CharsetDetectorTest {
    private fun detect(bytes: ByteArray) = CharsetDetector.detect(BufferedInputStream(bytes.inputStream()))

    private fun text(sample: String, charset: String) = sample.repeat(40).toByteArray(Charset.forName(charset))

    // Expected values are what the original Java detector (HEAD before the Kotlin conversion)
    // returns for the same bytes, including its misses (null) on BOM-less UTF-8 and this Big5 text.
    @Test fun matchesOriginalJavaDetector() {
        assertEquals("ASCII", detect("plain ascii text only\n".repeat(20).toByteArray()))
        assertEquals(null, detect(text("Tiếng Việt có dấu: ăâđêôơư ", "UTF-8")))
        assertEquals("UTF-8", detect(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "bom".toByteArray()))
        assertEquals("GB2312", detect(text("中华人民共和国成立于一九四九年，首都是北京。", "GB2312")))
        assertEquals(null, detect(text("中華民國臺灣的首都是臺北市，人口眾多。", "Big5")))
        assertEquals("Shift_JIS", detect(text("日本語のテキストです。東京は日本の首都です。", "Shift_JIS")))
        assertEquals("EUC-KR", detect(text("대한민국의 수도는 서울입니다. 한국어 문장입니다.", "EUC-KR")))
    }
}
