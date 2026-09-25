package com.wxiwei.office.thirdpart.emf

import com.wxiwei.office.thirdpart.emf.data.EMFRectangle
import com.wxiwei.office.thirdpart.emf.data.EOF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads a hand-built EMF through EMFInputStream, i.e. the converted thirdpart/emf/io streams. */
class EmfStreamTest {
    private fun emf(): ByteArray {
        val description = "Test\u0000".toByteArray(Charsets.UTF_16LE)
        val headerSize = 108 + description.size
        val buf = ByteBuffer.allocate(headerSize + 24 + 20).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(1).putInt(headerSize)                       // EMR_HEADER, record size
        buf.putInt(10).putInt(20).putInt(310).putInt(420)      // bounds (device units)
        buf.putInt(0).putInt(0).putInt(8000).putInt(10000)     // frame (0.01 mm)
        buf.putInt(0x464D4520)                                 // " EMF"
        buf.putInt(0x10000).putInt(headerSize + 24 + 20).putInt(3)  // version, bytes, records
        buf.putShort(1).putShort(0)                            // handles, reserved
        buf.putInt(description.size / 2).putInt(108)           // nDescription (WCHARs), offDescription
        buf.putInt(0)                                          // nPalEntries
        buf.putInt(1080).putInt(2340)                          // device pixels
        buf.putInt(68).putInt(147)                             // millimeters
        buf.putInt(0).putInt(0).putInt(0)                      // pixel format, openGL
        buf.putInt(68000).putInt(147000)                       // micrometers
        buf.put(description)
        buf.putInt(43).putInt(24).putInt(-5).putInt(7).putInt(100).putInt(200) // EMR_RECTANGLE
        buf.putInt(14).putInt(20).putInt(0).putInt(16).putInt(20)              // EMR_EOF
        return buf.array()
    }

    @Test fun readsHeaderAndRecords() {
        val input = EMFInputStream(ByteArrayInputStream(emf()))
        val header = input.readHeader()
        assertEquals(" EMF", header.signature)
        assertEquals("Test", header.description.trimEnd('\u0000'))
        with(header.bounds) { assertEquals(listOf(10, 20, 300, 400), listOf(x, y, width.toInt(), height.toInt())) }
        with(header.device) { assertEquals(listOf(1080, 2340), listOf(width.toInt(), height.toInt())) }
        with(header.millimeters) { assertEquals(listOf(68, 147), listOf(width.toInt(), height.toInt())) }

        val rect = input.readTag()
        assertTrue(rect.toString(), rect is EMFRectangle)
        assertTrue(rect.toString(), rect.toString().contains("x=-5,y=7,width=105,height=193") || rect.toString().contains("-5") && rect.toString().contains("193"))
        assertTrue(input.readTag() is EOF)
        assertNull(input.readTag()) // end of stream
    }

    /** Every record of a generated EMF (pens, brushes, shapes, bezier, pie, font, text) parses. */
    @Test fun readsAllRecordTypesOfGeneratedEmf() {
        val input = EMFInputStream(javaClass.getResourceAsStream("/emf/shapes.emf")!!)
        input.readHeader()
        val tags = generateSequence { input.readTag() }.toList()
        val names = tags.map { it.javaClass.simpleName }
        assertEquals(
            listOf("CreatePen", "SelectObject", "CreateBrushIndirect", "SelectObject", "EMFRectangle", "Ellipse",
                "CreateBrushIndirect", "SelectObject", "Polygon16", "Polyline16", "PolyBezier16", "Pie",
                "ExtCreateFontIndirectW", "SelectObject", "SetTextColor", "ExtTextOutW", "EOF"),
            names
        )
        val text = (tags.first { it is com.wxiwei.office.thirdpart.emf.data.ExtTextOutW } as com.wxiwei.office.thirdpart.emf.data.ExtTextOutW).getText()!!
        assertEquals("EMF OK Việt", text.string)
        // Points are android.graphics.Point (stubbed on the JVM: always 0,0); bounds use our own Rectangle
        val poly = tags.first { it is com.wxiwei.office.thirdpart.emf.data.Polygon16 }.toString()
        assertTrue(poly, poly.contains("x=30,y=150,width=130,height=110") && poly.contains("#points: 4"))
        val font = tags.first { it is com.wxiwei.office.thirdpart.emf.data.ExtCreateFontIndirectW }.toString()
        assertTrue(font, font.contains("Arial"))
    }
}
