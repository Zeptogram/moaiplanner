package com.example.moaiplanner.service

import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.moaiplanner.R


class MoaiRadioService : Service(), OnPreparedListener {

    private var startMode: Int = 0
    private var binder: IBinder? = null

    // Variabili per Media Player
    private val mediaPlayer = MediaPlayer()
    private val currentSongId : Int = 0
    private var mediaSession : MediaSessionCompat? = null
    private var mediaController : MediaControllerCompat? = null

    // Media Actions
    private val ACTION_PAUSE = "com.example.moaiplanner.action.PAUSE"
    private val ACTION_PLAY = "com.example.moaiplanner.action.PLAY"
    private val ACTION_PLAY_PAUSE = "com.example.moaiplanner.action.PLAY_PAUSE"
    private val ACTION_NEXT = "com.example.moaiplanner.action.NEXT"
    private val ACTION_PREV = "com.example.moaiplanner.action.PREV"

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(applicationContext, "MediaSessionDebug")
        mediaController = mediaSession!!.controller

        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession?.setMediaButtonReceiver(PendingIntent.getBroadcast(applicationContext, 0, Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(mediaButtonReceiver), FLAG_MUTABLE))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PLAYBACK", "START")

        when (intent?.action) {
            ACTION_PAUSE -> {
                // Handle pause action
                Log.d("PLAYBACK", "PAUSED")
                mediaPlayer.pause()
                updatePlaybackState(PlaybackState.STATE_PAUSED)
            }
            ACTION_PLAY_PAUSE -> {
                // Handle play action
                Log.d("PLAYBACK", "PLAY")
                mediaPlayer.start()
                updatePlaybackState(PlaybackState.STATE_PLAYING)
            }
            ACTION_NEXT -> {
                // Handle next action
                // ...
                Log.d("PLAYBACK", "NEXT")
            }
            ACTION_PREV -> {
                // Handle previous action
                // ...
                Log.d("PLAYBACK", "PREV")
            }
            else -> {
                // Handle other cases
                Log.d("PLAYBACK", "FUKCU")
            }
        }

        val notificationManager : NotificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("moairadio", "Moai Radio", IMPORTANCE_HIGH))

        val albumCover = BitmapFactory.decodeResource(
            applicationContext.resources,
            R.drawable.moai_radio_stylized
        )

        val metadataBuilder = MediaMetadataCompat.Builder().apply {
            // To provide most control over how an item is displayed set the
            // display fields in the metadata
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Prova")
            //putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, myData.displaySubtitle)
            //putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, myData.artUri)
            // And at minimum the title and artist for legacy support
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Prova")
            //putString(MediaMetadata.METADATA_KEY_ARTIST, myData.artist)
            // A small bitmap for the artwork is also recommended
            //putBitmap(MediaMetadata.METADATA_KEY_ART, myData.artBitmap)
            // Add any other fields you have for your data as well
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 197000)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
        mediaSession?.isActive = true

        val playPauseAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_pause_24, "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )?.let {
                PendingIntent.getBroadcast(applicationContext, 0, Intent(Intent.ACTION_MEDIA_BUTTON).setClass(applicationContext, MediaButtonReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            }
        ).build()

        val prevAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_skip_previous_24, "Previous",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )?.let {
                PendingIntent.getBroadcast(applicationContext, 0, Intent(Intent.ACTION_MEDIA_BUTTON).setClass(applicationContext, MediaButtonReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            }
        ).build()

        val nextAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_skip_next_24, "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )?.let {
                PendingIntent.getBroadcast(applicationContext, 0, Intent(Intent.ACTION_MEDIA_BUTTON).setClass(applicationContext, MediaButtonReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            }
        ).build()

        val notification = NotificationCompat.Builder(applicationContext, "moairadio")
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession!!.sessionToken))
            .setContentTitle("Moai Radio")
            .setContentText("Listen closely")
            .setSmallIcon(R.drawable.icon)
            .setLargeIcon(albumCover)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()

        // Set the playback state of the MediaSessionCompat
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(
                PlaybackStateCompat.STATE_PLAYING,
                mediaPlayer.currentPosition.toLong(),
                1.0f
            )
            .build()

        mediaSession!!.setPlaybackState(playbackState)
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val ONGOING_NOTIFICATION_ID = 1
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(currentSongId.toString())) // METTERE RANDOM
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.prepareAsync()

        return startMode
    }

    private fun updatePlaybackState(state: Int) {
        Log.d("PLAYBACK", "CIAO")
        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        stateBuilder.setState(state, mediaPlayer.currentPosition.toLong(), 1.0f)
        mediaSession?.setPlaybackState(stateBuilder.build())
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