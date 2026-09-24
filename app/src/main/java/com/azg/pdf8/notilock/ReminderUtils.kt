package com.azg.pdf8.notilock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.azg.pdf8.app.isScreenLockType
import com.azg.pdf8.app.remoteConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderUtils {

    companion object {
        const val REMINDER_ID = 129
        var latTimeTestShow = 0L;
    }

    var isShow = false


    fun createScheduleLockScreenReminder(
        context: Context?,
        broadcastReceiver: Class<*> = NotifyLockScreenReceiver::class.java
    ) {
        val intent = Intent(context, broadcastReceiver)

        val calendar = Calendar.getInstance()
        val listTime = remoteConfig.timeNotiLockReminder
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        var hourAlarm = listTime[0]
        when {
            currentHour < listTime[0] -> {
                isScreenLockType =
                    ReminderType.LOCK_FIRST_TYPE.value
                hourAlarm = listTime[0]
            }

            currentHour < listTime[1] -> {
                isScreenLockType =
                    ReminderType.LOCK_SECOND_TYPE.value
                hourAlarm = listTime[1]
            }

            currentHour < listTime[2] -> {
                isScreenLockType =
                    ReminderType.LOCK_THIRD_TYPE.value
                hourAlarm = listTime[2]
            }

            currentHour < listTime[3] -> {
                isScreenLockType =
                    ReminderType.LOCK_FOURTH_TYPE.value
                hourAlarm = listTime[3]
            }

            currentHour < listTime[4] -> {
                isScreenLockType =
                    ReminderType.LOCK_FIFTH_TYPE.value
                hourAlarm = listTime[4]
            }

            else -> {
                isScreenLockType =
                    ReminderType.LOCK_FIRST_TYPE.value
                listTime[0]
            }
        }

        if (currentHour >= listTime[4]) {
            calendar.add(Calendar.DATE, 1)
        }

        calendar[Calendar.HOUR_OF_DAY] = hourAlarm
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0

        val time = calendar.timeInMillis
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExactAlarms =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        Log.e("TimeNotilock", "Next time show NotiLock: $hourAlarm")
        if (canScheduleExactAlarms) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
            val date = Date(time)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date)
            Log.d("TimeNotilock", "Date  reminder at: ${formattedDate}")
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
            val date = Date(time)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date)
            Log.d("TimeNotilock", "Date reminder at: ${formattedDate}")
        }

        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = time
    }
}
