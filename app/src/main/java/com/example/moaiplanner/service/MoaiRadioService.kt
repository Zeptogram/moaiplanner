package com.example.moaiplanner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class MoaiRadioService : Service(), OnPreparedListener {

    private var startMode: Int = 0
    private var binder: IBinder? = null

    // Variabili per Media Player
    private val mediaPlayer = MediaPlayer()
    private var currentSongId : Int = 0
    private var mediaSession : MediaSessionCompat? = null
    private var mediaController : MediaControllerCompat? = null
    private var playlistRadio : IntArray? = null
    private var numSong = 2
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    data class Song(
        val artist: String,
        val duration: Long,
        val id: Int,
        val link: String,
        val name: String
    )

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        playlistRadio = IntArray(numSong, { it + 1 })
        playlistRadio?.shuffle()

        mediaSession = MediaSessionCompat(applicationContext, "MediaSessionDebug")
        mediaController = mediaSession!!.controller

        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession?.setMediaButtonReceiver(PendingIntent.getBroadcast(applicationContext, 0, Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(mediaButtonReceiver), FLAG_MUTABLE))
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

        updateMetadata()
        mediaSession!!.setPlaybackState(playbackState)
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val ONGOING_NOTIFICATION_ID = 1
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(playlistRadio?.get(currentSongId).toString()))

        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.prepareAsync()

        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    mediaPlayer.start()
                    updatePlaybackState(PlaybackState.STATE_PLAYING)
                }

                override fun onPause() {
                    super.onPause()
                    mediaPlayer.pause()
                    updatePlaybackState(PlaybackState.STATE_PAUSED)
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    skipSong()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    updatePlaybackState(PlaybackState.STATE_STOPPED)
                    if (currentSongId == 0) {
                        currentSongId = numSong - 1
                    } else {
                        currentSongId--
                    }
                    mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(playlistRadio?.get(currentSongId).toString())) // METTERE RANDOM
                    mediaPlayer.prepareAsync()
                    updateMetadata()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer.seekTo(pos.toInt())
                    updatePlaybackState(PlaybackState.STATE_PLAYING)
                }
            }
        )

        mediaSession?.isActive = true

        return startMode
    }

    private fun skipSong() {
        mediaPlayer.stop()
        mediaPlayer.reset()
        updatePlaybackState(PlaybackState.STATE_STOPPED)
        currentSongId = (currentSongId + 1) % numSong
        mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(playlistRadio?.get(currentSongId).toString()))
        mediaPlayer.prepareAsync()
        updateMetadata()
    }

    private fun updatePlaybackState(state: Int) {
        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
        )
        stateBuilder.setState(state, mediaPlayer.currentPosition.toLong(), 1.0f)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    fun updateMetadata() {
        var info : Song? = null
        var response : Response? = null
        scope.launch {
            val client = OkHttpClient()

            val url: HttpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("moai.eu.pythonanywhere.com")
                .addPathSegment("info")
                .addQueryParameter("id", playlistRadio?.get(currentSongId).toString())
                .build()

            val requesthttp: Request = Request.Builder()
                .addHeader("accept", "application/json")
                .url(url)
                .build()

            response = client.newCall(requesthttp).execute()

        }.invokeOnCompletion {
            info = Gson().fromJson(response?.body?.string(), Song::class.java)

            val metadataBuilder = MediaMetadataCompat.Builder().apply {
                info?.let { it ->
                    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, it.name)
                    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, it.artist)
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, it.name)
                    putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it.artist)
                }
                info?.duration?.let { it ->
                    putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        it
                    )
                }
            }
            mediaSession?.setMetadata(metadataBuilder.build())
        }
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer.start()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
    }
}