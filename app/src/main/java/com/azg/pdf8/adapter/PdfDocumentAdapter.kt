package com.azg.pdf8.adapter
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfDocumentAdapter(
    private val file: File
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            val input = FileInputStream(file)
            val output = FileOutputStream(destination?.fileDescriptor)

            input.copyTo(output)

            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

            input.close()
            output.close()
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}