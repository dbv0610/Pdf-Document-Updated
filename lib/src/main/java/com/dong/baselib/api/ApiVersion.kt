package com.dong.baselib.api

import android.os.Build


val isApi30orHigher: Boolean get() =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
val isApi30to33: Boolean
    get() = Build.VERSION.SDK_INT in Build.VERSION_CODES.R..Build.VERSION_CODES.TIRAMISU

val isApiFrom23to29: Boolean
    get() = Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.Q

val isApiFromHigher26: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val isApi23orLower: Boolean
    get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M

val isApi33orHigher: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
