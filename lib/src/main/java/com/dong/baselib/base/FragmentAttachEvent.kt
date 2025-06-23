package com.dong.baselib.base

interface FragmentAttachEvent {
    fun fragmentOnBack(){}
    fun fragmentAction(data:(Any)){}
    fun <T>fragmentSendData(key:String="",data:(T)){}
}