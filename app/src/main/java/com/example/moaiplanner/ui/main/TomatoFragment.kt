package com.example.moaiplanner.ui.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.databinding.TomatoFragmentBinding
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.model.TomatoViewModel
import com.example.moaiplanner.service.MediaPlayerService
import com.example.moaiplanner.service.MediaPlayerService.LocalBinder
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*


class TomatoFragment : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: TomatoFragmentBinding
    private lateinit var pomodoroViewModel: TomatoViewModel
    private lateinit var c: Context
    private var minutesSession: Long = 5
    private var minutesBreak: Long = 1
    private var roundsRemaining: Long = -1
    private var pomodoroDuration: Long = -1
    private var max: Long = -1
    private var running: Boolean = false
    var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("hh:mm:ss")
    private val mediaPlayer = MediaPlayer()

    private var player: MediaPlayerService? = null
    var serviceBound = false

    //Binding this Client to the AudioPlayer Service
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocalBinder
            player = binder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    private fun playAudio(media: String) {
        //Check is service is active
        if (!serviceBound) {
            val playerIntent = Intent(requireContext(), MediaPlayerService::class.java)
            playerIntent.putExtra("media", media)
            requireContext().startService(playerIntent)
            requireContext().bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            serviceBound = savedInstanceState.getBoolean("ServiceState")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            //service is active
            player!!.stopSelf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = TomatoFragmentBinding.inflate(inflater, container, false)
        // ViewModels
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(requireActivity())[TomatoViewModel::class.java]
        // Carico le impostazioni
        settingsViewModel.restoreSettings()
        // Inizializzo il pomodoroViewModel con il valore massimo
        if(pomodoroViewModel.maxRounds.value?.toInt() == -1)
            pomodoroViewModel.maxRounds.value = settingsViewModel.round.value?.toLong()
        else if(pomodoroViewModel.maxRounds.value?.toInt() != settingsViewModel.round.value?.toInt())
            reset()

        minutesSession = settingsViewModel.session.value?.toLong() ?: 5
        minutesBreak = settingsViewModel.pausa.value?.toLong() ?: 1
        roundsRemaining = pomodoroViewModel.rounds.value!!
        if(roundsRemaining.toInt() == -1) {
            roundsRemaining = settingsViewModel.round.value?.toLong() ?: 1
            pomodoroViewModel.rounds.value = settingsViewModel.round.value?.toLong() ?: 1
            binding.roundsRemaining.text =  roundsRemaining.toString() + "/" + settingsViewModel.round.value.toString()
        }

        // osservazione sui valori delle impostazioni
        settingsViewModel.session.observe(viewLifecycleOwner) {
            binding.sessione.text = settingsViewModel.session.value.toString() + " mins"
        }
        settingsViewModel.pausa.observe(viewLifecycleOwner) {
            binding.pausa.text = settingsViewModel.pausa.value.toString() + " mins"
        }

        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, true)

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    findNavController().navigate(R.id.optionsFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }
                    )
                }
            }
            true
        }

        updateRound()
        if(pomodoroViewModel.timeRemaining.value!! > 0) {
            if(pomodoroViewModel.paused.value == true) {
                binding.playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                pomodoroViewModel.timer.value?.cancel()
                running = false
                binding.timerLabel.text = pomodoroViewModel.timeLabel.value
                binding.timerBar.max = pomodoroViewModel.timeMax.value!!.toInt()
                binding.timerBar.progress = pomodoroViewModel.timeRemaining.value!!.toInt()

            }
            else {
                initTimer()
                pomodoroViewModel.timer.value?.start()
                running = true
                binding.playPause.setImageResource(R.drawable.ic_baseline_pause_24)

            }
        }
        else {
            initTimerLabel()
        }

        binding.playPause.setOnClickListener {
            if(!running) {
                start()
            } else {
                pause()
            }
        }

        binding.stop.setOnClickListener {
            stop(false)
        }

        binding.reset.setOnClickListener {
            reset()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(3).isChecked = true;
    }

    override fun onStart() {
        super.onStart()
        playAudio("https://moai.eu.pythonanywhere.com/2")

        /*
        var response: Response? = null
        Log.d("API", "CIAO")
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("API", "CIAO2")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://moai.eu.pythonanywhere.com/1")
                .get()
                .build()
            response = client.newCall(request).execute()
            Log.d("API", "CIAO3")
        }.invokeOnCompletion {
            val rawData = response!!.body?.bytes()
            if (rawData != null) {
                Log.d("API", rawData.size.toString())
            }
            if (rawData != null) {
                //playByteArray(rawData)
                val Mytemp = File.createTempFile("TCL", "mp3", activity?.cacheDir)
                Mytemp.deleteOnExit()
                val fos = FileOutputStream(Mytemp)
                fos.write(rawData)
                fos.close()
                val MyFile = FileInputStream(Mytemp)
                lifecycleScope.launch(Dispatchers.IO) {
                    mediaPlayer.setDataSource(Mytemp.path)
                    mediaPlayer.prepare()
                }.invokeOnCompletion {
                    mediaPlayer.start()
                }
            }
        }
         */
    }

    override fun onStop() {
        super.onStop()
        //mediaPlayer.stop()
        //mediaPlayer.release()
    }

    /*
    private fun playByteArray(mp3SoundByteArray: ByteArray) {
        try {
            val Mytemp = File.createTempFile("TCL", "mp3", activity?.cacheDir)
            Mytemp.deleteOnExit()
            val fos = FileOutputStream(Mytemp)
            fos.write(mp3SoundByteArray)
            fos.close()
            val mediaPlayer = MediaPlayer()
            val MyFile = FileInputStream(Mytemp)
            mediaPlayer.setDataSource(Mytemp.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (ex: IOException) {
            val s = ex.toString()
            ex.printStackTrace()
        }
    }
     */

    fun initTimer() {


        pomodoroViewModel.timer.value?.cancel()
        c = requireActivity()
        pomodoroDuration = pomodoroViewModel.timeRemaining.value!!
        pomodoroViewModel.updateTimer(pomodoroDuration)
        max = pomodoroViewModel.timeMax.value!!

        if(pomodoroViewModel.timeRemaining.value!! < 0) {
            if(pomodoroViewModel.pausa.value == false) {
                pomodoroDuration = minutesSession * 60 * 1000
                pomodoroViewModel.pausa.value = true
                binding.typeLabel.text = "Work"
            }
            else {
                pomodoroDuration = minutesBreak * 60 * 1000
                pomodoroViewModel.pausa.value = false
                binding.typeLabel.text = "Break"

            }
            binding.timerBar.max = pomodoroDuration.toInt()
            pomodoroViewModel.timeMax.value = pomodoroDuration
            max = pomodoroDuration
            if(pomodoroViewModel.paused.value == false)
                binding.timerBar.progress = binding.timerBar.max
        } else {
            binding.timerBar.max = pomodoroViewModel.timeMax.value!!.toInt()
            if(pomodoroViewModel.paused.value == false)
                binding.timerBar.progress = pomodoroViewModel.timeMax.value!!.toInt()
        }

        pomodoroViewModel.timer.value = object: CountDownTimer(pomodoroDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                pomodoroViewModel.updateTimer(millisUntilFinished)
                binding.timerBar.setProgress(millisUntilFinished.toInt(), false)

                val date = Date(millisUntilFinished)
                if(max >= 3600000) {
                    simpleDateFormat = SimpleDateFormat("hh:mm:ss")
                    binding.timerLabel.text = simpleDateFormat.format(date)
                    pomodoroViewModel.timeLabel.value = simpleDateFormat.format(date)
                }
                else if(max >= 60000) {
                    simpleDateFormat= SimpleDateFormat("mm:ss")
                    binding.timerLabel.text = simpleDateFormat.format(date)
                    pomodoroViewModel.timeLabel.value = simpleDateFormat.format(date)
                }
            }


            override fun onFinish() {

                stop(true)
                if(settingsViewModel.notifiche.value == true) {
                    val notificationManager =
                        c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notificationId = 1
                    val channelId = "pomodoro_channel"
                    val channelName = "Pomodoro Timer"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            channelName,
                            NotificationManager.IMPORTANCE_HIGH
                        )
                        notificationManager.createNotificationChannel(channel)
                    }
                    val notification: Notification
                    // Se devo fare la pausa
                    if(pomodoroViewModel.pausa.value == true) {
                        notification = NotificationCompat.Builder(c, channelId)
                            .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
                            .setContentTitle("Moai Planner")
                            .setContentText("Tempo di pausa! Inizia la pausa di ${minutesBreak} minuti!")
                            .setAutoCancel(true)
                            .build()
                    }
                    else{
                        roundsRemaining--
                        pomodoroViewModel.rounds.value = roundsRemaining
                        if(roundsRemaining <= 0){
                            notification = NotificationCompat.Builder(c, channelId)
                                .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
                                .setContentTitle("Moai Planner")
                                .setContentText("Ultimo round terminato! Ben fatto!")
                                .setAutoCancel(true)
                                .build()
                        }
                        else {
                            notification = NotificationCompat.Builder(c, channelId)
                                .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
                                .setContentTitle("Moai Planner")
                                .setContentText("Round terminato! Inizia la sessione di ${minutesSession} minuti! Round rimanenti: ${roundsRemaining}")
                                .setAutoCancel(true)
                                .build()
                        }
                    }

                    notificationManager.notify(notificationId, notification)
                }
                if(roundsRemaining <= 0) {
                    if(roundsRemaining < 0) {
                        pomodoroViewModel.rounds.value = settingsViewModel.round.value?.toLong()
                        roundsRemaining = settingsViewModel.round.value?.toLong()!!
                        binding.roundsRemaining.text = roundsRemaining.toString();
                    }
                }
                else
                    restart()

            }
        }
    }

    fun stop(forced: Boolean) {
        pomodoroViewModel.stopTimer()
        if(max != -1L)
            binding.timerBar.progress = max.toInt()
        else
            binding.timerBar.progress = 100
        initTimerLabel()
        binding.typeLabel.text = "Time to focus!"
        binding.timerBar.max = 100
        max = 100
        running = false
        binding.playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        if(roundsRemaining <= 0 && pomodoroViewModel.pausa.value == false && !forced){
            pomodoroViewModel.rounds.value = settingsViewModel.round.value?.toLong()
            roundsRemaining = settingsViewModel.round.value?.toLong()!!
            updateRound()
        }
        else if(pomodoroViewModel.pausa.value == false && !forced){
            roundsRemaining--
            pomodoroViewModel.rounds.value = roundsRemaining
            updateRound()
        }


    }

    fun start() {
        if(roundsRemaining <= 0)
            reset()
        initTimer()
        pomodoroViewModel.timer.value?.start()
        running = true
        pomodoroViewModel.paused.value = false
        binding.playPause.setImageResource(R.drawable.ic_baseline_pause_24)
    }

    fun pause(){
        binding.playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        pomodoroViewModel.timer.value?.cancel()
        running = false
        pomodoroViewModel.paused.value = true
    }

    fun restart() {
        start()
    }

    private fun reset(){
        stop(false)
        pomodoroViewModel.pausa.value = false
        pomodoroViewModel.rounds.value = settingsViewModel.round.value?.toLong()
        roundsRemaining = settingsViewModel.round.value?.toLong()!!
        pomodoroViewModel.maxRounds.value = settingsViewModel.round.value?.toLong()
        binding.typeLabel.text = "Time to focus!"
        updateRound()
    }

    private fun updateRound(){
        // Aggiorno i rounds rimanenti
        pomodoroViewModel.rounds.observe(viewLifecycleOwner) {
            // Reset Rounds
            if(roundsRemaining < 0)
                binding.roundsRemaining.text = settingsViewModel.round.value.toString() + "/" + settingsViewModel.round.value.toString()
            // Aggiorno
            else
                binding.roundsRemaining.text = pomodoroViewModel.rounds.value.toString() + "/" + settingsViewModel.round.value.toString()
        }
    }

    private fun initTimerLabel() {
        // Ottengo dal ViewModel i minuti
        val startLabel = (settingsViewModel.session.value?.toInt()?.times(60) ?: 1) * 1000
        // Formato il tempo
        if(startLabel >= 3600000) {
            simpleDateFormat= SimpleDateFormat("hh:mm:ss")
            binding.timerLabel.text = simpleDateFormat.format(startLabel)

        }
        else if(startLabel >= 60000) {
            simpleDateFormat= SimpleDateFormat("mm:ss")
            binding.timerLabel.text = simpleDateFormat.format(startLabel)
        }
    }
}


