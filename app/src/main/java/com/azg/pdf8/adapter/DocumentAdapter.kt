package com.azg.pdf8.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemDocumentViewBinding
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.model.DocumentType
import com.dong.baselib.base.DiffAdapter
import com.dong.baselib.base.ModelDiffCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface MenuOption {
}

class DocumentAdapter(
    private val onFavoriteClick: (DocumentModel, Int) -> Unit = { _, _ -> },
    private val onMenuClick: (DocumentModel, Int) -> Unit = { _, _ -> }
) : DiffAdapter<DocumentModel, ItemDocumentViewBinding>(
    ModelDiffCallback(
        areItemsTheSameCallback = { old, new -> old.mediaId == new.mediaId },
        areContentsTheSameCallback = { old, new -> old == new },
        payloadProvider = { old, new ->
            val payloads = mutableListOf<String>()
            if (old.isSelected != new.isSelected) payloads.add("payload_selected")
            if (payloads.isNotEmpty()) payloads else null
        }
    )
) {
    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemDocumentViewBinding {
        return ItemDocumentViewBinding.inflate(inflater, parent, false)
    }

    override fun ItemDocumentViewBinding.bind(item: DocumentModel, position: Int) {
        val file = File(item.path)
        tvFileName.text = file.name
        tvFileInfo.text = buildInfoText(item)

        imgSource.setImageResource(getIconByType(item.type))
        imgFavorite.setOnClickListener {
            onFavoriteClick(item, position)
        }

        imgTools.setOnClickListener {
            onMenuClick(item, position)
        }

        root.alpha = if (item.isSelected) 0.6f else 1f
    }

    override fun ItemDocumentViewBinding.bind(
        item: DocumentModel,
        position: Int,
        payloads: List<Any>
    ) {
        if ("payload_selected" in payloads) {
            root.alpha = if (item.isSelected) 0.6f else 1f
        } else {
            bind(item, position)
        }
    }

    private fun getIconByType(type: DocumentType): Int {
        return when (type) {
            DocumentType.Pdf -> R.drawable.ic_app_pdf
            DocumentType.Doc -> R.drawable.ic_app_docx
            DocumentType.Excel -> R.drawable.ic_app_xls
            DocumentType.Ppt -> R.drawable.ic_app_ppt
        }
    }

    private fun buildInfoText(item: DocumentModel): String {
        val size = Formatter.formatFileSize(context, item.size)
        val date =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.lastModified))
        return "$size · $date"
    }
}
