package com.azg.pdf8.ui.main.document;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.azg.pdf8.R;
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentDocumentBinding
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.paddingTop

class DocumentFragment :
    BaseFragment<FragmentDocumentBinding>(FragmentDocumentBinding::inflate, true) {
    override fun FragmentDocumentBinding.initView() {
        lnHeader.paddingTop(statusBarHeight + appActivity.dimenSdp(6))
    }

    override fun FragmentDocumentBinding.onClick() {
    }
}