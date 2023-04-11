package com.moai.planner.ui.main

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.moai.planner.R
import com.moai.planner.data.repository.settings.SettingsRepository
import com.moai.planner.databinding.TomatoFragmentBinding
import com.moai.planner.model.SettingsViewModel
import com.moai.planner.model.SettingsViewModelFactory
import com.moai.planner.model.TomatoViewModel
import com.moai.planner.service.MoaiRadioService
import com.moai.planner.util.NavigationHelper
import com.moai.planner.util.NetworkUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION")
class TomatoFragment : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: TomatoFragmentBinding
    private lateinit var pomodoroViewModel: TomatoViewModel
    private lateinit var contesto: Context
    private var minutesSession: Long = 5
    private var minutesBreak: Long = 1
    private var roundsRemaining: Long = -1
    private var pomodoroDuration: Long = -1
    private var max: Long = -1
    private var running: Boolean = false
    var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())

    private var radioPlaying: Boolean = false

    override fun onResume() {
        super.onResume()
        radioPlaying = isServiceRunning(getString(R.string.service_name))
        if (radioPlaying) {
            binding.musicPlay.setImageResource(R.drawable.ic_baseline_stop_24)
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
            binding.roundsRemaining.text =  getString(R.string.round_div,  roundsRemaining.toString(), settingsViewModel.round.value.toString())
        }

        // osservazione sui valori delle impostazioni
        settingsViewModel.session.observe(viewLifecycleOwner) {
            binding.sessione.text = getString(R.string.mins, settingsViewModel.session.value.toString())
        }
        settingsViewModel.pausa.observe(viewLifecycleOwner) {
            binding.pausa.text = getString(R.string.mins, settingsViewModel.pausa.value.toString())
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

    private fun isServiceRunning(serviceName: String): Boolean {
        var serviceRunning = false
        val am = context?.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val l = am.getRunningServices(50)
        val i: Iterator<ActivityManager.RunningServiceInfo> = l.iterator()
        while (i.hasNext()) {
            val runningServiceInfo = i
                .next()
            if (runningServiceInfo.service.className == serviceName) {
                serviceRunning = true
            }
        }
        return serviceRunning
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, true)
        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    NavigationHelper.navigateTo(view, R.id.optionsFragment)
                }
            }
            true
        }

        binding.musicPlay.setOnClickListener {
            NetworkUtils.notifyMissingNetwork(requireContext(), requireView(), requireActivity())
            if(NetworkUtils.isNetworkAvailable(requireContext())) {
                if (!radioPlaying) {
                    requireContext().startForegroundService(
                        Intent(
                            requireContext(),
                            MoaiRadioService::class.java
                        )
                    )
                    radioPlaying = true
                    binding.musicPlay.setImageResource(R.drawable.ic_baseline_stop_24)
                } else {
                    requireContext().stopService(
                        Intent(
                            requireContext(),
                            MoaiRadioService::class.java
                        )
                    )
                    radioPlaying = false
                    binding.musicPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
            }
        }
        // Mette la home come main
        bottomNav.menu.getItem(3).isChecked = true
    }

    private fun initTimer() {

        pomodoroViewModel.timer.value?.cancel()
        contesto = requireActivity()
        pomodoroDuration = pomodoroViewModel.timeRemaining.value!!
        pomodoroViewModel.updateTimer(pomodoroDuration)
        max = pomodoroViewModel.timeMax.value!!

        if(pomodoroViewModel.timeRemaining.value!! < 0) {
            if(pomodoroViewModel.pausa.value == false) {
                pomodoroDuration = minutesSession * 60 * 1000
                pomodoroViewModel.pausa.value = true
                binding.typeLabel.text = getString(R.string.work)
            }
            else {
                pomodoroDuration = minutesBreak * 60 * 1000
                pomodoroViewModel.pausa.value = false
                binding.typeLabel.text = getString(R.string.pausa)

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
                    simpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
                    binding.timerLabel.text = simpleDateFormat.format(date)
                    pomodoroViewModel.timeLabel.value = simpleDateFormat.format(date)
                }
                else if(max >= 60000) {
                    simpleDateFormat= SimpleDateFormat("mm:ss", Locale.getDefault())
                    binding.timerLabel.text = simpleDateFormat.format(date)
                    pomodoroViewModel.timeLabel.value = simpleDateFormat.format(date)
                }
            }


            override fun onFinish() {
                stop(true)
                if(settingsViewModel.notifiche.value == true) {
                    val notificationManager = contesto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notificationId = 1
                    val channelId = "pomodoro_channel"
                    val channelName = "Pomodoro Timer"

                    val channel = NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(channel)
                    val notification: Notification
                    // Se devo fare la pausa
                    if(pomodoroViewModel.pausa.value == true) {
                        notification = NotificationCompat.Builder(contesto, channelId)
                            .setSmallIcon(R.drawable.baseline_timer_24)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.time_break, minutesBreak.toString()))
                            .setAutoCancel(true)
                            .build()
                    }
                    else{
                        roundsRemaining--
                        pomodoroViewModel.rounds.value = roundsRemaining
                        if(roundsRemaining <= 0){
                            notification = NotificationCompat.Builder(contesto, channelId)
                                .setSmallIcon(R.drawable.baseline_timer_24)
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.last_round_notification))
                                .setAutoCancel(true)
                                .build()
                        }
                        else {
                            notification = NotificationCompat.Builder(contesto, channelId)
                                .setSmallIcon(R.drawable.baseline_timer_24)
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.starting_session, minutesSession.toString(), roundsRemaining.toString()))
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
                        binding.roundsRemaining.text = roundsRemaining.toString()
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
        binding.typeLabel.text = getString(R.string.time_to_focus)
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

    private fun start() {
        if(roundsRemaining <= 0)
            reset()
        initTimer()
        pomodoroViewModel.timer.value?.start()
        running = true
        pomodoroViewModel.paused.value = false
        binding.playPause.setImageResource(R.drawable.ic_baseline_pause_24)
    }

    private fun pause(){
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
        binding.typeLabel.text = getString(R.string.time_to_focus)
        updateRound()
    }

    private fun updateRound(){
        // Aggiorno i rounds rimanenti
        pomodoroViewModel.rounds.observe(viewLifecycleOwner) {
            // Reset Rounds
            if(roundsRemaining < 0)
                binding.roundsRemaining.text = getString(R.string.round_div, settingsViewModel.round.value.toString(), settingsViewModel.round.value.toString())
            // Aggiorno
            else
                binding.roundsRemaining.text = getString(R.string.round_div, pomodoroViewModel.rounds.value.toString(), settingsViewModel.round.value.toString())
        }
    }

    private fun initTimerLabel() {
        // Ottengo dal ViewModel i minuti
        val startLabel = (settingsViewModel.session.value?.toInt()?.times(60) ?: 1) * 1000
        // Formato il tempo
        if(startLabel >= 3600000) {
            simpleDateFormat= SimpleDateFormat("hh:mm:ss", Locale.getDefault())
            binding.timerLabel.text = simpleDateFormat.format(startLabel)

        }
        else if(startLabel >= 60000) {
            simpleDateFormat= SimpleDateFormat("mm:ss", Locale.getDefault())
            binding.timerLabel.text = simpleDateFormat.format(startLabel)
        }
    }
}


