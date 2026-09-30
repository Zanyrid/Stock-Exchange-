package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class NotificationHelper(private val context: Context) {

  companion object {
    const val CHANNEL_ID = "absurd_price_alerts"
    const val CHANNEL_NAME = "Bursa Absurd - Target Harga"
    const val CHANNEL_DESC = "Peringatan real-time saat komoditas tak berwujud mencapai target"
  }

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 150, 100, 250)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun canPostNotification(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun sendPriceAlertNotification(
    alertId: String,
    symbol: String,
    name: String,
    targetPrice: Double,
    currentPrice: Double,
    isAbove: Boolean
  ) {
    if (!canPostNotification()) return

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("ALERT_TRIGGERED_SYMBOL", symbol)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      alertId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val conditionText = if (isAbove) "MELAMPAUI (>=)" else "JATUH DI BAWAH (<=)"
    val formattedCurrent = String.format(Locale.US, "%,.2f ABS", currentPrice)
    val formattedTarget = String.format(Locale.US, "%,.2f ABS", targetPrice)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(">>> [TARGET REACHED: $symbol] <<<")
      .setContentText("$name $conditionText target! Harga sekarang: $formattedCurrent (Target: $formattedTarget)")
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(
          "COMMODEX-84 PROTOCOL ALERT:\n" +
          "Komoditas: $symbol ($name)\n" +
          "Status: Target $conditionText tercapai!\n" +
          "Harga Terkini: $formattedCurrent\n" +
          "Target Anda: $formattedTarget\n" +
          "Segera periksa order book untuk mengambil aksi."
        )
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(alertId.hashCode(), notification)
  }
}
