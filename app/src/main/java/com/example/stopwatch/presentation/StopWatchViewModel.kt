package com.example.stopwatch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopWatchViewModel: ViewModel() {

    private val _elapsedTime = MutableStateFlow(0L);
    private val _timerState = MutableStateFlow(TimerState.RESET);
    val timerState = _timerState.asStateFlow();

    // 데이터 변환 intermediate
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    val stopWatchText = _elapsedTime
        .map {millis ->
            LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter);
        }
        .stateIn( // Flow를 특 정한 상태로 수신하도록 하기 위한 함수
            viewModelScope, // ViewModel 범위 내에서 실행되는 코루틴
            // 해당 Flow가 구독되어 있는 동안에만 값을 공유하며, 구독이 종료 되어도  5초간의 딜레이
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        );

    // 초기화 블럭
    init {
        _timerState
            // _timerState가 변경될 때마다 실행되는 코드 블록 정의
            // flatMapLatest는 새로운 Flow를 생성하는 함수를 받아, 이 함수에서 반환한 Flow의 가장 최근 값을 가져와서 방출
            .flatMapLatest { timerState ->
                getTimerFlow(
                    isRunning = timerState == TimerState.RUNNING
                )
            }
            .onEach { timeDiff ->
                // 람다식 변수가 하나일 때 it으로 받아서 사용 가능
                _elapsedTime.update { it + timeDiff}
            }
            .launchIn(viewModelScope)
    }

    fun toggleIsRunning() {
        when(timerState.value){
            TimerState.RUNNING -> _timerState.update { TimerState.PAUSED }
            TimerState.PAUSED,
            TimerState.RESET -> _timerState.update { TimerState.RUNNING }
        }
    }

    fun resetTimer(){
        _timerState.update { TimerState.RESET }
        _elapsedTime.update { 0L }
    }

    // 데이터 생성 producer
    private fun getTimerFlow(isRunning: Boolean): Flow<Long>{
        return flow {
            var startMillis = System.currentTimeMillis();
            while(isRunning){
                val currentMillis = System.currentTimeMillis();
                val timeDiff = if(currentMillis > startMillis){
                    currentMillis - startMillis;
                } else 0L;
                emit(timeDiff)
                startMillis = System.currentTimeMillis();
                delay(10L);
            }
        }
    }
}