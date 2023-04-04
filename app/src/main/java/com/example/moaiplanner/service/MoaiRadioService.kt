package com.example.moaiplanner.service

import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.session.PlaybackState
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
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
    private var playlistRadio : IntArray? = null
    private var numSong = 2
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var notificationManager : NotificationManager? = null
    private var notification : Notification? = null
    private var info : Song? = null
    private val notificationId = 1

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

        playlistRadio = IntArray(numSong) { it + 1 }
        playlistRadio?.shuffle()
        mediaSession = MediaSessionCompat(applicationContext, "MediaSessionDebug")
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val client = OkHttpClient()

            val url: HttpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("moai.eu.pythonanywhere.com")
                .addPathSegment("totalSongs")
                .build()

            val requesthttp: Request = Request.Builder()
                .addHeader("accept", "application/json")
                .url(url)
                .build()

            val response = client.newCall(requesthttp).execute()
            numSong = response.body?.string()?.trim()?.toInt()!!
        }

        notificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager!!.createNotificationChannel(NotificationChannel("moairadio", "Moai Radio", IMPORTANCE_LOW))

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

        notification = NotificationCompat.Builder(applicationContext, "moairadio")
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession!!.sessionToken))
            .setContentTitle(getString(R.string.moai_radio))
            .setContentText(getString(R.string.listen_closely))
            .setSmallIcon(R.drawable.icon)
            .setLargeIcon(albumCover)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()



        startForeground(notificationId, notification)

        mediaPlayer.setDataSource("https://moai.eu.pythonanywhere.com/".plus(playlistRadio?.get(currentSongId).toString()))
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener {
            if (mediaPlayer.currentPosition != 0 && mediaPlayer.duration != 0) {
                if (mediaPlayer.currentPosition + 300 >= mediaPlayer.duration) {
                    skipSong()
                }
            }
        }
        mediaPlayer.prepareAsync()

        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    mediaPlayer.start()
                    updatePlaybackState(PlaybackState.STATE_PLAYING)
                }

                override fun onPause() {
                    super.onPause()
                    if(mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
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

    private fun updateMetadata() {
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

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, info!!.name)
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, info!!.artist)
                .putText(MediaMetadataCompat.METADATA_KEY_TITLE, info!!.name)
                .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, info!!.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info!!.duration)
                .build()

            mediaSession?.setMetadata(metadataBuilder)
            info?.name?.let { it1 -> Log.d("METADATA", it1) }

            notificationManager!!.notify(notificationId, notification)
        }
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }


    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer.start()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        updateMetadata()
    }
}