package com.azg.pdf8.notilock

enum class ReminderType(val value: Int) {
    LOCK_FIRST_TYPE(0),
    LOCK_SECOND_TYPE(1),
    LOCK_THIRD_TYPE(2),
    LOCK_FOURTH_TYPE(3),
    LOCK_FIFTH_TYPE(4);

    companion object {
        fun valueOfName(value: Int): ReminderType {
            return entries.find { it.value == value } ?: LOCK_FIRST_TYPE
        }
    }
}
