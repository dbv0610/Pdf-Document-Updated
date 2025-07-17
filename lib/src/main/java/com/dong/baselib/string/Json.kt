package com.dong.baselib.string

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.Serializable

fun <A> String.fromJson(type: Class<A>): A? {
    return try {
        Gson().fromJson(this, type)
    } catch (e: JsonSyntaxException) {
        Log.e("JSON_PARSE_ERROR", "Failed to parse JSON: $this", e)
        null
    }
}
fun <A> A.toJson(): String {
    return Gson().toJson(this)
}
