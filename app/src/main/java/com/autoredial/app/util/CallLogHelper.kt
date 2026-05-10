package com.autoredial.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog
import android.telephony.PhoneNumberUtils

object CallLogHelper {
    
    @SuppressLint("Range")
    fun wasLastCallAnswered(context: Context, targetNumber: String): Boolean {
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE),
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                    val durationStr = it.getString(it.getColumnIndex(CallLog.Calls.DURATION))
                    val type = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))

                    // Check if the last call was an outgoing call to our target number
                    if (type == CallLog.Calls.OUTGOING_TYPE && (number == targetNumber || PhoneNumberUtils.compare(context, number, targetNumber))) {
                        val duration = durationStr.toIntOrNull() ?: 0
                        return duration > 0 // If duration > 0, it means it was answered
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
