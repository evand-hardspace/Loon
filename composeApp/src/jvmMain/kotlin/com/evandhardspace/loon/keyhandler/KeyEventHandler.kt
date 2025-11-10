package com.evandhardspace.loon.keyhandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import kotlin.reflect.KClass

val LocalKeyEventHandler: ProvidableCompositionLocal<KeyEventHandler> = compositionLocalOf { error("Not provided") }

@Composable
inline fun <reified T : AppKeyEvent> handleKeyEvent(
    name: String, // todo: thing about better key
    noinline muteKeyEvents: (MutedEventsBuilder.() -> Unit)? = null,
    noinline action: (T) -> Boolean,
) {
    val keyEventHandler = LocalKeyEventHandler.current
    val mutedEventsBuilder = remember(keyEventHandler) {
        DefaultMutedEventsBuilder(keyEventHandler)
    }
    DisposableEffect(action) {
        muteKeyEvents?.let { mutedEventsBuilder.apply(it) }
        keyEventHandler.subscribe<T>(name, action)
        onDispose {
            keyEventHandler.unsubscribe<T>(name)
            mutedEventsBuilder.unmuteAll()
        }
    }
}

// todo: not to use outside dialogs unless unmute logic is added
@Composable
inline fun <reified T : AppKeyEvent> forceHandleKeyEvent(
    name: String, // todo: thing about better key
    noinline action: (T) -> Boolean,
) {
    val keyEventHandler = LocalKeyEventHandler.current

    DisposableEffect(action) {
        keyEventHandler.forceSubscribe<T>(name, action)
        onDispose {
            keyEventHandler.forceUnsubscribe<T>(name)
        }
    }
}

interface MutedEventsBuilder {
    fun <T: AppKeyEvent> mute(event: KClass<T>)
}

inline fun <reified T: AppKeyEvent> MutedEventsBuilder.mute(): Unit = mute(T::class)

@PublishedApi
internal class DefaultMutedEventsBuilder(
    val keyEventHandler: KeyEventHandler,
): MutedEventsBuilder {
    val mutedEvents = mutableSetOf<KClass<out AppKeyEvent>>()

    override fun <T : AppKeyEvent> mute(event: KClass<T>) {
        mutedEvents.add(event)
        keyEventHandler.muteEvent(event)
    }

    fun unmuteAll() {
        mutedEvents.forEach {
            keyEventHandler.unmuteEvent(it)
        }
        mutedEvents.clear()
    }
}

class KeyEventHandler {
    private val listeners = mutableMapOf<KClass<out AppKeyEvent>, MutableMap<String, (AppKeyEvent) -> Boolean>>()
    private val forceListeners = mutableMapOf<KClass<out AppKeyEvent>, MutableMap<String, (AppKeyEvent) -> Boolean>>()
    private val mutedEventsStack = mutableMapOf<KClass<out AppKeyEvent>, Int>()

    @Suppress("UNCHECKED_CAST")
    fun <T : AppKeyEvent> subscribe(eventType: KClass<T>, key: String, listener: (T) -> Boolean) {
        val typedListener = listener as (AppKeyEvent) -> Boolean
        listeners.getOrPut(eventType) { mutableMapOf() }[key] = typedListener
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : AppKeyEvent> forceSubscribe(eventType: KClass<T>, key: String, listener: (T) -> Boolean) {
        val typedListener = listener as (AppKeyEvent) -> Boolean
        forceListeners.getOrPut(eventType) { mutableMapOf() }[key] = typedListener
    }

    inline fun <reified T : AppKeyEvent> forceSubscribe(key: String, noinline listener: (T) -> Boolean) {
        forceSubscribe(T::class, key, listener)
    }

    inline fun <reified T : AppKeyEvent> subscribe(key: String, noinline listener: (T) -> Boolean) {
        subscribe(T::class, key, listener)
    }

    fun <T : AppKeyEvent> unsubscribe(eventType: KClass<T>, key: String) {
        listeners[eventType]?.remove(key)
    }

    inline fun <reified T : AppKeyEvent> unsubscribe(key: String) {
        unsubscribe(T::class, key)
    }

    fun <T : AppKeyEvent> forceUnsubscribe(eventType: KClass<T>, key: String) {
        forceListeners[eventType]?.remove(key)
    }

    inline fun <reified T : AppKeyEvent> forceUnsubscribe(key: String) {
        forceUnsubscribe(T::class, key)
    }

    fun <T : AppKeyEvent> muteEvent(event: KClass<T>) {
        if(event in mutedEventsStack) {
            mutedEventsStack[event] = mutedEventsStack[event]!! + 1
            return
        }
        mutedEventsStack[event] = 1
    }

    fun <T : AppKeyEvent> unmuteEvent(event: KClass<T>) {
        if(event in mutedEventsStack) {
            if(mutedEventsStack[event]!! > 1) {
                mutedEventsStack[event] = mutedEventsStack[event]!! - 1
                return
            }
        }
        mutedEventsStack.remove(event)
    }

    private fun emit(event: AppKeyEvent): Boolean {
        if(forceListeners[event::class]?.values?.lastOrNull()?.invoke(event) == true) return true
        if (event::class in mutedEventsStack) return false
        return listeners[event::class]?.values?.lastOrNull()?.invoke(event) ?: false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        println("event: $event")
        return when (event.type) {
            KeyEventType.KeyDown if event.isMetaPressed && event.key == Key.N -> {
                emit(AppKeyEvent.New(event.isShiftPressed))
            }

            KeyEventType.KeyDown if event.isMetaPressed && event.key == Key.W -> {
                emit(AppKeyEvent.Close)
            }

            KeyEventType.KeyDown if event.isMetaPressed && event.key == Key.Backspace -> {
                emit(AppKeyEvent.Remove)
            }

            KeyEventType.KeyDown if event.key == Key.Enter -> {
                emit(AppKeyEvent.Enter)
            }

            KeyEventType.KeyDown if (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.S -> {
                emit(AppKeyEvent.Save)
            }

            KeyEventType.KeyDown if (event.isCtrlPressed) && event.key == Key.Tab -> {
                emit(AppKeyEvent.TabMenu(event.isShiftPressed))
            }

            KeyEventType.KeyUp if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight) -> {
                emit(AppKeyEvent.ReleaseCtrl)
            }

            else -> false
        }
    }
}

sealed interface AppKeyEvent {
    data class New(val isShiftPressed: Boolean) : AppKeyEvent
    data object Remove : AppKeyEvent
    data object Close : AppKeyEvent
    data object Enter : AppKeyEvent
    data object Save : AppKeyEvent
    data object ReleaseCtrl: AppKeyEvent
    data class TabMenu(val isShiftPressed: Boolean): AppKeyEvent
}