package com.yassernull.shappky.utils

import android.content.Context
import com.yassernull.shappky.R

object NotificationUtils {
  fun showTriggerFreedMemoryNotification(context: Context, triggerName: String, freedMemoryText: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "ShappkyTriggerChannel"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val channel = android.app.NotificationChannel(
        channelId,
        context.getString(R.string.trigger_channel_name),
        android.app.NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = context.getString(R.string.trigger_channel_desc)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val fullText = "$triggerName: $freedMemoryText"

    val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
      .setContentTitle(context.getString(R.string.trigger_channel_name))
      .setContentText(fullText)
      .setSmallIcon(R.drawable.ic_shappky)
      .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setAutoCancel(false)

    notificationManager.notify(2, builder.build())
  }
}
