package com.evandhardspace.loon.presentation.state

import java.util.Collections
import kotlin.reflect.KClass

// TODO: manage slices lifecycle
@PublishedApi
internal object GlobalSliceHolder {

    private val appSlices = Collections.synchronizedMap(mutableMapOf<KClass<*>, Slice>())
    private val appSliceInitializers = Collections.synchronizedMap(mutableMapOf<KClass<*>, () -> Slice>())

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Slice> get(key: KClass<T>): T {
        val slice = appSlices[key] as? T
        if (slice == null) {
            val initializer = appSliceInitializers[key]
            val slice: T = initializer?.invoke() as? T ?: error("initializer for $key is not set")
            this[key] = slice
        }
        return appSlices[key] as? T ?: error("state for $key is not set or is wrong type")
    }

    private operator fun <T : Slice> set(key: KClass<T>, value: T) {
        appSlices[key] = value
    }

    @PublishedApi
    internal fun <T : Slice> setInitializer(key: KClass<T>, value: () -> T) {
        appSliceInitializers[key] = value
    }

    internal fun onClear() {
        appSlices.clear()
    }
}

inline fun <reified T : Slice> getSlice(): T = GlobalSliceHolder.get(T::class)

interface SliceBuilder

inline fun <reified T : Slice> SliceBuilder.register(noinline initializer: () -> T) {
    GlobalSliceHolder.setInitializer(T::class, initializer)
}

fun registerSlice(
    block: SliceBuilder.() -> Unit,
) {
    (object : SliceBuilder {}).apply(block)
}

fun clearSlices() {
    GlobalSliceHolder.onClear()
}