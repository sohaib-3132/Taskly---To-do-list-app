package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Task

object ReminderScheduler {
    fun schedule(context: Context, task: Task) {
        val timestamp = task.dueTimestamp ?: return
        if (timestamp < System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_name", task.name)
            putExtra("task_desc", task.description)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            pendingIntentFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // Fallback to standard alarm if can't schedule exact alarms on S+
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        timestamp,
                        pendingIntent
                    )
                    Log.w("ReminderScheduler", "Exact alarms not allowed. Scheduled non-exact instead.")
                    return
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled exact reminder for task ${task.id} at $timestamp")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent
            )
            Log.e("ReminderScheduler", "SecurityException scheduling exact. Defaulted to standard non-exact.", e)
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent
            )
            Log.e("ReminderScheduler", "General Exception scheduling. Defaulted to standard non-exact.", e)
        }
    }

    fun cancel(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            pendingIntentFlags
        )

        alarmManager.cancel(pendingIntent)
        Log.d("ReminderScheduler", "Canceled reminder if any for task ${task.id}")
    }
}
