package org.onedroid.resizephoto.presentation.home

import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import java.io.File

data class HomeUiState(
    val actualImage: File? = null,
    val resizedImage: File? = null,
    val resolution: Float = 1.0f,
    val message: String? = null,
    val originalExif: ExifData? = null,
    val resizedExif: ExifData? = null,
    val keepExif: Boolean = false,
    val originalResolution: Pair<Int, Int>? = null,
    val resizedResolution: Pair<Int, Int>? = null,
    val originalSize: Long? = null,
    val resizedSize: Long? = null,
    val targetWidth: String = "",
    val targetHeight: String = "",
    val keepAspectRatio: Boolean = true,
    val resizeMode: ResizeMode = ResizeMode.PERCENTAGE,
    val targetLongEdge: String = "",
    val algorithm: ResizeAlgorithm = ResizeAlgorithm.BITMAP_SCALING,
    val processingTime: Long = 0L,
    val isResizing: Boolean = false
)

enum class ResizeMode {
    PERCENTAGE, PIXELS, LONG_EDGE
}

data class ExifData(
    val values: Map<String, String?>
)
