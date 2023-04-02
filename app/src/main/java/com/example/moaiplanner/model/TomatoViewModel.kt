package com.example.moaiplanner.model

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TomatoViewModel : ViewModel() {
    val timeRemaining = MutableLiveData<Long>(-1)
    val rounds = MutableLiveData<Long>(-1)
    val maxRounds = MutableLiveData<Long>(-1)
    val timeMax = MutableLiveData<Long>(-1)
    var paused = MutableLiveData<Boolean>(false)
    var timeLabel = MutableLiveData<String>("")
    var timer = MutableLiveData<CountDownTimer>(null)
    var pausa = MutableLiveData<Boolean>(false)

    fun updateTimer(duration: Long) {
        timeRemaining.value = duration
    }

    fun stopTimer() {
        timeRemaining.value = -1
        timeMax.value = -1
        maxRounds.value = -1
        paused.value = false
        timer.value?.cancel()
    }
}
