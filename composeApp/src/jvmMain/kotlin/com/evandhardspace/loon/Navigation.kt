package com.evandhardspace.loon

import kotlinx.serialization.Serializable

@Serializable
data object FileChooserRoute

@Serializable
data class EditorRoute(
    val selectedPath: String,
)