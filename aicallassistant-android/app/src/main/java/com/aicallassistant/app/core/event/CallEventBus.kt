package com.aicallassistant.app.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AppCallEvent {
    data class CallCompleted(val callId: String) : AppCallEvent()
    data class CallDeleted(val callId: String) : AppCallEvent()
    data class ActionItemUpdated(val actionItemId: String) : AppCallEvent()
}

/**
 * Lightweight real-time event bus synchronizing call completions, deletions,
 * and task updates across ViewModels without tight coupling or polling.
 */
object CallEventBus {
    private val _events = MutableSharedFlow<AppCallEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppCallEvent> = _events.asSharedFlow()

    fun emit(event: AppCallEvent) {
        _events.tryEmit(event)
    }
}
