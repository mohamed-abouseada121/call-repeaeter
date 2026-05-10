package com.autoredial.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.autoredial.app.service.CallService

class PhoneStateReceiver : BroadcastReceiver() {
    private var lastState = TelephonyManager.EXTRA_STATE_IDLE

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_IDLE && lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // Call just ended
                val serviceIntent = Intent(context, CallService::class.java).apply {
                    action = CallService.ACTION_CALL_ENDED
                }
                context.startService(serviceIntent)
            }
            
            if (state != null) {
                lastState = state
            }
        }
    }
}
