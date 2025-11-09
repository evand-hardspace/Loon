package com.evandhardspace.loon.features.imagearea

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO

@Composable
fun ImageViewScreen(
    modifier: Modifier = Modifier,
    imageFile: File?,
) {
    if(imageFile == null) {
        Text("Not an image")
        return
    }
    val bitmap = remember(imageFile) {
        ImageIO.read(imageFile)?.toComposeImageBitmap()
    }

    bitmap?.let {
        Image(
            modifier = modifier,
            painter = BitmapPainter(it),
            contentDescription = "Image for ${imageFile.path}"
        )
    }
}