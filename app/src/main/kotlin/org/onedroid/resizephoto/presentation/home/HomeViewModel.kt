package org.onedroid.resizephoto.presentation.home

import android.app.Application
import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.onedroid.resizephoto.core.util.FileUtil
import org.onedroid.resizephoto.domain.usecase.GetExifUseCase
import org.onedroid.resizephoto.domain.usecase.ResizeImageUseCase
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class HomeViewModel(
    private val application: Application,
    private val getExifUseCase: GetExifUseCase,
    private val resizeImageUseCase: ResizeImageUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun setResolution(resolution: Float) {
        _state.update {
            it.copy(
                resolution = resolution.coerceIn(0.01f, 1.0f)
            )
        }
    }

    fun setResizeMode(mode: ResizeMode) {
        _state.update { it.copy(resizeMode = mode) }
    }

    fun setTargetWidth(width: String) {
        _state.update {
            val newState = it.copy(targetWidth = width)
            if (it.keepAspectRatio && it.originalResolution != null && width.isNotEmpty()) {
                val w = width.toIntOrNull()
                if (w != null) {
                     val aspectRatio = it.originalResolution.second.toFloat() / it.originalResolution.first.toFloat()
                     val h = (w * aspectRatio).toInt()
                     return@update newState.copy(targetHeight = h.toString())
                }
            }
            newState
        }
    }

    fun setTargetHeight(height: String) {
        _state.update {
            val newState = it.copy(targetHeight = height)
             if (it.keepAspectRatio && it.originalResolution != null && height.isNotEmpty()) {
                val h = height.toIntOrNull()
                if (h != null) {
                     val aspectRatio = it.originalResolution.first.toFloat() / it.originalResolution.second.toFloat()
                     val w = (h * aspectRatio).toInt()
                     return@update newState.copy(targetWidth = w.toString())
                }
            }
            newState
        }
    }
    
    fun setTargetLongEdge(size: String) {
        _state.update {
            it.copy(targetLongEdge = size)
        }
    }

    fun setKeepAspectRatio(keep: Boolean) {
        _state.update {
             it.copy(keepAspectRatio = keep)
        }
        // Recalculate height based on width if enabled
        if (keep && _state.value.targetWidth.isNotEmpty()) {
            setTargetWidth(_state.value.targetWidth)
        }
    }

    fun setUseLanczos(use: Boolean) {
        _state.update {
            it.copy(algorithm = if (use) ResizeAlgorithm.LANCZOS else ResizeAlgorithm.BITMAP_SCALING)
        }
    }

    fun setAlgorithm(algorithm: ResizeAlgorithm) {
        _state.update { it.copy(algorithm = algorithm) }
    }

    fun clearAll() {
        _state.update { HomeUiState() }
    }

    fun resizeImage() {
        val currentImage = _state.value.actualImage ?: return
        val originalResolution = _state.value.originalResolution ?: return
        val useLanczos = _state.value.algorithm == ResizeAlgorithm.LANCZOS

        _state.update { it.copy(isResizing = true, message = null, processingTime = 0L) }

        when (_state.value.resizeMode) {

            ResizeMode.PERCENTAGE -> {
                resizeImage(currentImage, (_state.value.resolution * 100).toInt(), _state.value.algorithm)
            }

            ResizeMode.PIXELS -> {
                val w = _state.value.targetWidth.toIntOrNull() ?: 0
                val h = _state.value.targetHeight.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    resizeImage(currentImage, w, h, _state.value.algorithm)
                } else {
                    _state.update { it.copy(message = "Invalid dimensions") }
                }
            }

            ResizeMode.LONG_EDGE -> {
                val longEdge = _state.value.targetLongEdge.toIntOrNull() ?: 0
                if (longEdge > 0) {
                    val (origW, origH) = originalResolution
                    val w: Int
                    val h: Int
                    val aspectRatio = origW.toFloat() / origH.toFloat()
                    
                    if (origW >= origH) {
                        // Width is the long edge
                        w = longEdge
                        h = (longEdge / aspectRatio).roundToInt()
                    } else {
                        // Height is the long edge
                        h = longEdge
                        w = (longEdge * aspectRatio).roundToInt()
                    }
                    resizeImage(currentImage, w, h, _state.value.algorithm)
                } else {
                    _state.update { it.copy(message = "Invalid long edge size") }
                }
            }
        }
    }

    fun setKeepExif(keep: Boolean) {
        _state.update {
            it.copy(
                keepExif = keep
            )
        }
    }


    fun setActualImage(uri: Uri) {
        val file = FileUtil.from(application, uri)
        if (file == null) {
            _state.update {
                it.copy(
                    message = "Failed to load image"
                )
            }
            return
        }
        viewModelScope.launch {
            val exif = getExifUseCase(file)
            val resolution = FileUtil.getImageResolution(file)
            val size = file.length()
            val maxEdge = maxOf(resolution.first, resolution.second)
            
            _state.update { 
                it.copy(
                    actualImage = file,
                    message = null,
                    originalExif = exif,
                    originalResolution = resolution,
                    originalSize = size,
                    // Clear previous resized image when new image is loaded
                    resizedImage = null,
                    resizedExif = null,
                    resizedResolution = null,
                    resizedSize = null,
                    // Reset inputs
                    targetWidth = resolution.first.toString(),
                    targetHeight = resolution.second.toString(),
                    targetLongEdge = maxEdge.toString(),
                    processingTime = 0L,
                    isResizing = false
                )
            }
        }
    }

    private fun resizeImage(file: File, percentage: Int, algorithm: ResizeAlgorithm) {
         viewModelScope.launch {
             try {
                 var resized: File? = null
                 val time = measureTimeMillis {
                     resized = resizeImageUseCase(file, percentage, algorithm)
                 }
                 if (resized != null) {
                    handleResizedImage(file, resized, time)
                 }
                 _state.update { it.copy(isResizing = false) }
             } catch (e: Exception) {
                 _state.update {
                     it.copy(message = "Error resizing image: ${e.message}", isResizing = false)
                 }
             }
         }
    }

    private fun resizeImage(file: File, width: Int, height: Int, algorithm: ResizeAlgorithm) {
         viewModelScope.launch {
             try {
                 var resized: File? = null
                 val time = measureTimeMillis {
                    resized = resizeImageUseCase(file, width, height, algorithm)
                 }
                 if (resized != null) {
                     handleResizedImage(file, resized!!, time)
                 }
                 _state.update { it.copy(isResizing = false) }
             } catch (e: Exception) {
                 _state.update {
                     it.copy(message = "Error resizing image: ${e.message}", isResizing = false)
                 }
             }
         }
    }

    private suspend fun handleResizedImage(original: File, resized: File, processingTime: Long) {
         val resizedExif = getExifUseCase(resized)
         if (_state.value.keepExif) {
             getExifUseCase.copy(original, resized)
         }

         // Reload exif after potential copy
         val finalResizedExif = if (_state.value.keepExif) getExifUseCase(resized) else resizedExif
         
         val resizedResolution = FileUtil.getImageResolution(resized)
         val resizedSize = resized.length()

         _state.update {
             it.copy(
                 resizedImage = resized,
                 resizedExif = finalResizedExif,
                 resizedResolution = resizedResolution,
                 resizedSize = resizedSize,
                 processingTime = processingTime
             )
         }
    }
}
