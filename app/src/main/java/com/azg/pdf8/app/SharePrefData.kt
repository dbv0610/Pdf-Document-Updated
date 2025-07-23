package com.azg.pdf8.app



var isFinishFirstFlow by sharedPreference.boolean("isFinishFirstFlow", false)
var firstOpenApp by sharedPreference.int("firstOpenApp", 0)
fun isUfo(): Boolean {
    return firstOpenApp == 1
}
