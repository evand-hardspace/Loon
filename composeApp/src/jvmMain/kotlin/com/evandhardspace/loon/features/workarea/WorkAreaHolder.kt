package com.evandhardspace.loon.features.workarea

import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class WorkAreaHolder {
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    open fun init() {}

    @CallSuper
    open fun dispose() {
        coroutineScope.cancel()
    }
}

class UnsupportedAreaHolder(val unsupportedExtension: String): WorkAreaHolder()
