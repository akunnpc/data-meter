package com.mamang.datameter.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.mamang.datameter.MainActivity
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.PermissionHelper

object NotificationHelper {

    const val CHANNEL_ID = "datameter_quota_alerts"
    private const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val descriptionText = context.getString(R.string.notif_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showQuotaAlert(
        context: Context,
        threshold: Int,
        usedBytes: Long,
        limitBytes: Long
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            threshold,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val usedFormatted = DataSizeFormatter.formatBytes(usedBytes)
        val limitFormatted = DataSizeFormatter.formatBytes(limitBytes)
        val body = context.getString(R.string.notif_alert_body, threshold, usedFormatted, limitFormatted)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.datameter_icon_1791012467194)
            .setContentTitle(context.getString(R.string.notif_alert_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + threshold, builder.build())
        } catch (_: SecurityException) {
            // Permission might have been revoked
        }
    }
}
