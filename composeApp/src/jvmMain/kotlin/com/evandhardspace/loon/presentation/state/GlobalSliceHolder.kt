package com.evandhardspace.loon.presentation.state

import java.util.Collections
import kotlin.reflect.KClass

@PublishedApi
internal object GlobalSliceHolder {

    private val appStates = Collections.synchronizedMap(mutableMapOf<KClass<*>, Slice>())

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Slice> get(key: KClass<T>): T {
        return appStates[key] as T
    }

    @PublishedApi
    internal fun <T : Slice> set(key: KClass<T>, value: T) {
        appStates[key] = value
    }
}

inline fun <reified T : Slice> getSlice(): T = GlobalSliceHolder.get(T::class)

interface SliceBuilder

inline fun <reified T : Slice> SliceBuilder.register(initializer: () -> T) {
    GlobalSliceHolder.set(T::class, initializer())
}
fun registerSlice(
    block: SliceBuilder.() -> Unit,
) {
    (object : SliceBuilder {}).apply(block)
}