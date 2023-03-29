package com.example.moaiplanner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.example.moaiplanner.R


class MoaiRadioService : Service(), OnPreparedListener {

    private var startMode: Int = 0
    private var binder: IBinder? = null

    // Variabili per Media Player
    private val mediaPlayer = MediaPlayer()
    private val currentSongId : Int = 0
    private var mediaSession : MediaSession? = null
    private var mediaController : MediaController? = null

    // Media Actions
    private val ACTION_PAUSE: String? = "com.example.moaiplanner.pause"
    private val ACTION_PLAY = "com.example.moaiplanner.play"
    private val ACTION_NEXT = "com.example.moaiplanner.next"
    private val ACTION_PREV = "com.example.moaiplanner.prev"

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession(applicationContext, "MediaSessionDebug")
        mediaController = mediaSession!!.controller
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager : NotificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("moairadio", "Moai Radio", IMPORTANCE_HIGH))

        val albumCover = BitmapFactory.decodeResource(
            applicationContext.resources,
            R.drawable.moai_radio_stylized
        )

        val notification: Notification = Notification.Builder(applicationContext, "moairadio")
            .setContentTitle("Moai Radio")
            .setContentText("Listen closely")
            .setSmallIcon(R.drawable.icon)
            .setLargeIcon(albumCover)
            .setStyle(Notification.MediaStyle()
                .setMediaSession(mediaSession!!.sessionToken))
            .addAction(
                Notification.Action(
                    R.drawable.ic_baseline_skip_previous_24, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
            .addAction(
                Notification.Action(
                    R.drawable.ic_baseline_pause_24, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_PAUSE)))
            .addAction(
                Notification.Action(
                    R.drawable.ic_baseline_skip_next_24, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
            .build()

        val ONGOING_NOTIFICATION_ID = 1
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(currentSongId.toString())) // METTERE RANDOM
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.prepareAsync()

        return startMode
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        startRadio()
    }

    private fun startRadio() {
        mediaPlayer.start()
    }
}