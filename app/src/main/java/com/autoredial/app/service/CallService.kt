package com.autoredial.app.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import android.widget.Toast
import com.autoredial.app.R
import com.autoredial.app.notification.NotificationHelper
import com.autoredial.app.util.CallLogHelper

class CallService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CALL_ENDED = "ACTION_CALL_ENDED"

        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val EXTRA_MAX_ATTEMPTS = "EXTRA_MAX_ATTEMPTS"
        const val EXTRA_WAIT_TIME = "EXTRA_WAIT_TIME"
        const val EXTRA_BRUTE_FORCE = "EXTRA_BRUTE_FORCE"
    }

    private var phoneNumber: String = ""
    private var maxAttempts: Int = 0
    private var waitTimeMs: Long = 5000L
    private var bruteForceMode: Boolean = false
    private var attemptsCount: Int = 0
    private var isRunning: Boolean = false
    private var lastCallTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private var makeCallRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                maxAttempts = intent.getIntExtra(EXTRA_MAX_ATTEMPTS, 0)
                waitTimeMs = intent.getLongExtra(EXTRA_WAIT_TIME, 5000L)
                bruteForceMode = intent.getBooleanExtra(EXTRA_BRUTE_FORCE, false)
                
                if (phoneNumber.isNotEmpty() && !isRunning) {
                    isRunning = true
                    attemptsCount = 0
                    startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildNotification(this, attemptsCount, getString(R.string.status_calling)))
                    makeCall()
                }
            }
            ACTION_STOP -> {
                stopAutoRedial()
            }
            ACTION_CALL_ENDED -> {
                if (isRunning) {
                    checkCallLogAndDecide()
                }
            }
        }
        return START_STICKY
    }

    private fun makeCall() {
        if (!isRunning) return

        if (maxAttempts > 0 && attemptsCount >= maxAttempts) {
            Toast.makeText(this, "وصلت للحد الأقصى من المحاولات", Toast.LENGTH_SHORT).show()
            stopAutoRedial()
            return
        }

        attemptsCount++
        lastCallTime = System.currentTimeMillis()
        updateNotification(getString(R.string.status_calling))

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            stopAutoRedial()
        }
    }

    private fun checkCallLogAndDecide() {
        // Wait a little bit for CallLog to be updated by the system
        handler.postDelayed({
            if (!isRunning) return@postDelayed
            
            val wasAnswered = CallLogHelper.wasLastCallAnswered(this, phoneNumber, lastCallTime)
            if (wasAnswered && !bruteForceMode) {
                Toast.makeText(this, getString(R.string.status_answered), Toast.LENGTH_LONG).show()
                updateNotification(getString(R.string.status_answered))
                stopAutoRedial()
            } else {
                updateNotification(getString(R.string.status_waiting))
                // Wait and call again
                makeCallRunnable = Runnable { makeCall() }
                handler.postDelayed(makeCallRunnable!!, waitTimeMs)
            }
        }, 2000) // 2 seconds delay to allow DB update
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildNotification(this, attemptsCount, status))
    }

    private fun stopAutoRedial() {
        isRunning = false
        makeCallRunnable?.let { handler.removeCallbacks(it) }
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRedial()
    }
}
