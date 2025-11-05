package com.evandhardspace.loon.presentation.state

import java.util.Collections
import kotlin.reflect.KClass

@PublishedApi
internal object GlobalStateHolder {

    private val appStates = Collections.synchronizedMap(mutableMapOf<KClass<*>, State>())

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : State> get(key: KClass<T>): T {
        return appStates[key] as T
    }

    @PublishedApi
    internal fun <T : State> set(key: KClass<T>, value: T) {
        appStates[key] = value
    }
}

inline fun <reified T : State> getState(): T = GlobalStateHolder.get(T::class)

interface StateBuilder

inline fun <reified T : State> StateBuilder.register(initializer: () -> T) {
    GlobalStateHolder.set(T::class, initializer())
}
fun registerStates(
    block: StateBuilder.() -> Unit,
) {
    (object : StateBuilder {}).apply(block)
}