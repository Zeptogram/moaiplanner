package com.example.moaiplanner.ui.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
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


class TomatoFragment : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: TomatoFragmentBinding
    private lateinit var pomodoroViewModel: TomatoViewModel
    private lateinit var c: Context
    private var minutesSession: Long = 5
    private var minutesBreak: Long = 1
    private var pomodoroDuration: Long = -1
    private var max: Long = -1
    private var running: Boolean = false




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = TomatoFragmentBinding.inflate(inflater, container, false)
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(requireActivity())[TomatoViewModel::class.java]
        settingsViewModel.restoreSettings()

        minutesSession = settingsViewModel.session.value?.toLong() ?: 5
        minutesBreak = settingsViewModel.pausa.value?.toLong() ?: 1


        // osservazione sui valori delle impostazioni
        settingsViewModel.session.observe(viewLifecycleOwner) {
            binding.sessione?.text = settingsViewModel.session.value.toString()
        }
        settingsViewModel.pausa.observe(viewLifecycleOwner) {
            binding.pausa?.text = settingsViewModel.pausa.value.toString()
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





        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.playPause.setOnClickListener {
            if(!running) {
                start()
            } else {
               pause()
            }

        }

        binding.stop.setOnClickListener {
            stop()
        }

    }

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
            }
            else {
                pomodoroDuration = minutesBreak * 60 * 1000
                pomodoroViewModel.pausa.value = false
            }
            binding.timerBar.max = pomodoroDuration.toInt()
            pomodoroViewModel.timeMax.value = pomodoroDuration
            max = pomodoroDuration
        } else
            binding.timerBar.max = max.toInt()
        pomodoroViewModel.timer.value = object: CountDownTimer(pomodoroDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                pomodoroViewModel.updateTimer(millisUntilFinished)
                binding.timerBar.progress = millisUntilFinished.toInt()
                val secondsRemaining = millisUntilFinished / 1000
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                val hours = minutes / 60
                if(max >= 3600000) {
                    binding.timerLabel.text = "${hours}:${minutes}:${seconds}"
                    pomodoroViewModel.timeLabel.value = "${hours}:${minutes}:${seconds}"
                }
                else if(max >= 60000) {
                    binding.timerLabel.text = "${minutes}:${seconds}"
                    pomodoroViewModel.timeLabel.value = "${minutes}:${seconds}"
                }
            }


            override fun onFinish() {

                stop()
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
                    if(pomodoroViewModel.pausa.value == true) {
                        notification = NotificationCompat.Builder(c, channelId)
                            .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
                            .setContentTitle("Moai Planner")
                            .setContentText("Tempo di pausa! Inizia la pausa di ${minutesBreak} minuti/o!")
                            .setAutoCancel(true)
                            .build()
                    }
                    else{
                        notification = NotificationCompat.Builder(c, channelId)
                            .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
                            .setContentTitle("Moai Planner")
                            .setContentText("Round terminato! Inizia la sessione di ${minutesSession} minuti/o!")
                            .setAutoCancel(true)
                            .build()
                    }

                    notificationManager.notify(notificationId, notification)
                }
                restart()

            }
        }
    }

    fun stop() {
        pomodoroViewModel.stopTimer()
        binding.timerBar.progress = 100
        binding.timerLabel.text = pomodoroViewModel.timeLabel.value
        max = -1
        running = false
        binding.playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)

    }

    fun start() {
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




}


