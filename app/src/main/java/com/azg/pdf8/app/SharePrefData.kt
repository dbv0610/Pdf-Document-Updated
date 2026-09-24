package com.azg.pdf8.app

import com.azg.pdf8.notilock.ReminderType

var isFinishFirstFlow by sharedPreference.boolean("isFinishFirstFlow", false)
var firstOpenApp by sharedPreference.int("firstOpenApp", 0)
fun isUfo(): Boolean {
    return firstOpenApp == 1
}
var isScreenLockType by sharedPreference.int("isScreenLockType", ReminderType.LOCK_FIRST_TYPE.value)
