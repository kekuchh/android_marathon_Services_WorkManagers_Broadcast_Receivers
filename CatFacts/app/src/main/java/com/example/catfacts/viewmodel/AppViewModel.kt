package com.example.catfacts.viewmodel

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.catfacts.model.CatFact
import com.example.catfacts.service.CatFactsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val _catFacts = MutableLiveData<List<CatFact>>()
    val catFacts: LiveData<List<CatFact>> = _catFacts

    private val _timer = MutableStateFlow(0L)
    private var timerJob: Job? = null
    val timer = _timer.asStateFlow()

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for(i in 1..5) { // задержка в 5 секунд
                delay(1000)
                _timer.value++
            }
        }
    }

    fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    fun startService(context: Context) {
        startTimer()
        viewModelScope.launch {
            val intent = Intent(context, CatFactsService::class.java)
            context.startService(intent)
        }
    }

    fun startWorkManager(context: Context, request: OneTimeWorkRequest) {
        startTimer()
        viewModelScope.launch {
            WorkManager.getInstance(context).enqueueUniqueWork("worker",ExistingWorkPolicy.REPLACE,request)
        }
    }

    fun updateCatFacts(catFacts: List<CatFact>) {
        _catFacts.value = catFacts
    }
}