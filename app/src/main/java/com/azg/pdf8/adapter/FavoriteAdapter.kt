package com.azg.pdf8.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ItemDocumentViewBinding
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.FavoriteDocument
import com.azg.pdf8.model.FavoriteUi
import com.dong.baselib.base.DiffAdapter
import com.dong.baselib.base.ModelDiffCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteAdapter(
    private val onFavoriteClick: (FavoriteDocument, Int) -> Unit = { _, _ -> },
    private val onMenuClick: (View, FavoriteDocument, Int) -> Unit = { _, _, _ -> },
    private val onViewFilePdf: (FavoriteDocument) -> Unit = { _ -> }
) : DiffAdapter<FavoriteUi, ItemDocumentViewBinding>(
    ModelDiffCallback(
        areItemsTheSameCallback = { old, new -> old.document.mediaId == new.document.mediaId },
        areContentsTheSameCallback = { old, new -> old == new },
        payloadProvider = { old, new ->
            if (old.document.isSelected != new.document.isSelected) {
                listOf("payload_selected")
            } else null
        }
    )
) {
    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ) = ItemDocumentViewBinding.inflate(inflater, parent, false)

    override fun ItemDocumentViewBinding.bind(item: FavoriteUi, position: Int) {
        val doc = item.document
        val file = File(doc.path)

        tvFileName.text = file.name
        tvFileInfo.text = buildInfoText(doc)
        imgSource.setImageResource(getIconByType(doc.type))
        imgFavorite.setImageResource(
            if (item.isFavorite) R.drawable.ic_favorite_select
            else R.drawable.ic_favorite_no
        )
        imgFavorite.setOnClickListener {
            onFavoriteClick(doc, position)
        }

        imgTools.setOnClickListener {
            onMenuClick(imgTools, doc, position)
        }

        root.setOnClickListener {
            onViewFilePdf(doc)
        }
        root.alpha = if (doc.isSelected) 0.6f else 1f
    }
    override fun ItemDocumentViewBinding.bind(
        item: FavoriteUi,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.contains("payload_selected")) {
            root.alpha = if (item.document.isSelected) 0.6f else 1f
        } else {
            bind(item, position)
        }
    }

    private fun getIconByType(type: DocumentType) = when (type) {
        DocumentType.Pdf   -> R.drawable.ic_app_pdf
        DocumentType.Doc   -> R.drawable.ic_app_docx
        DocumentType.Excel -> R.drawable.ic_app_xls
        DocumentType.Ppt   -> R.drawable.ic_app_ppt
        DocumentType.Txt   -> R.drawable.ic_app_txt
        else               -> R.drawable.ic_app_pdf
    }

    private fun buildInfoText(doc: FavoriteDocument): String {
        val size = Formatter.formatFileSize(context, doc.size)
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(doc.lastModified))
        return "$size · $date"
    }
}
