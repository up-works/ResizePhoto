package org.onedroid.resizephoto.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.size.Size
import kotlin.ranges.coerceIn

@Composable
fun CompareImage(
    modifier: Modifier = Modifier,
    beforeImage: Any,
    afterImage: Any,
    imageLoader: ImageLoader? = null,
    dividerColor: Color = Color.White,
    dividerWidth: Dp = 2.dp,
    contentScale: ContentScale = ContentScale.Fit
) {
    var dragPercentage by remember { mutableFloatStateOf(0.5f) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    // Zoom/Pan State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var beforeLoading by remember { mutableStateOf(true) }
    var afterLoading by remember { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds() // Ensure zoomed content doesn't spill out
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset zoom on double tap
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onTap = { tapOffset ->
                        // Only move slider if not zoomed in
                        if (scale <= 1.01f) {
                            val width = size.width.toFloat()
                            if (width > 0) {
                                dragPercentage = (tapOffset.x / width).coerceIn(0f, 1f)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // Combine Zoom/Pan and Slider Drag
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // If we are dragging the handle, do not pan/zoom
                    if (isDraggingHandle) return@detectTransformGestures

                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    val isZooming = newScale > 1.01f || oldScale > 1.01f

                    if (!isZooming) {
                        // Slider Drag Mode via screen drag (Scale is ~1f)
                        val width = size.width.toFloat()
                        if (width > 0) {
                            val dragDelta = pan.x / width
                            dragPercentage = (dragPercentage + dragDelta).coerceIn(0f, 1f)
                        }
                        // Ensure state is clean
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        // Zoom/Pan Image Mode

                        // Calculate offset correction to keep centroid stationary during zoom
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val zoomRatio = if (oldScale != 0f) newScale / oldScale else 1f
                        val newOffsetBase = offset + (centroid - center - offset) * (1 - zoomRatio)

                        val proposedOffset = newOffsetBase + pan

                        scale = newScale

                        // Constrain pan so we don't fly off too far
                        val maxPanX = (size.width * newScale - size.width) / 2
                        val maxPanY = (size.height * newScale - size.height) / 2

                        offset = Offset(
                            x = proposedOffset.x.coerceIn(-maxPanX, maxPanX),
                            y = proposedOffset.y.coerceIn(-maxPanY, maxPanY)
                        )
                    }
                }
            }
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val offsetPx = maxWidthPx * dragPercentage

        // Common modifier for both images to apply Zoom/Pan
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }

        val context = LocalContext.current
        val afterModel: Any = if (afterImage is java.io.File) {
            ImageRequest.Builder(context)
                .data(afterImage)
                .size(coil3.size.Size.ORIGINAL)
                .memoryCacheKey("after-${afterImage.absolutePath}-${afterImage.length()}-${afterImage.lastModified()}")
                .build()
        } else afterImage

        if (imageLoader != null) {
            AsyncImage(
                model = afterModel,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = contentScale,
                modifier = imageModifier,
                onLoading = { afterLoading = true },
                onSuccess = { afterLoading = false },
                onError = { afterLoading = false }
            )
        } else {
            AsyncImage(
                model = afterModel,
                contentDescription = null,
                contentScale = contentScale,
                modifier = imageModifier,
                onLoading = { afterLoading = true },
                onSuccess = { afterLoading = false },
                onError = { afterLoading = false }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * dragPercentage) {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            val beforeModel: Any = if (beforeImage is java.io.File) {
                ImageRequest.Builder(context)
                    .data(beforeImage)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCacheKey("before-${beforeImage.absolutePath}-${beforeImage.length()}-${beforeImage.lastModified()}")
                    .build()
            } else beforeImage

            if (imageLoader != null) {
                AsyncImage(
                    model = beforeModel,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = imageModifier,
                    onLoading = { beforeLoading = true },
                    onSuccess = { beforeLoading = false },
                    onError = { beforeLoading = false }
                )
            } else {
                AsyncImage(
                    model = beforeModel,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = imageModifier,
                    onLoading = { beforeLoading = true },
                    onSuccess = { beforeLoading = false },
                    onError = { beforeLoading = false }
                )
            }
        }

        val handleSize = 32.dp
        val handleSizePx = with(LocalDensity.current) { handleSize.toPx() }

        // Specific drag detector for the handle to allow interaction even when zoomed
        val handleDragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { isDraggingHandle = true },
                onDragEnd = { isDraggingHandle = false },
                onDragCancel = { isDraggingHandle = false }
            ) { change, dragAmount ->
                change.consume()
                if (maxWidthPx > 0) {
                    dragPercentage = (dragPercentage + dragAmount.x / maxWidthPx).coerceIn(0f, 1f)
                }
            }
        }

        // DIVIDER LINE (Fixed to viewport)
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetPx.toInt() - (dividerWidth.toPx() / 2).toInt(), 0) }
                .fillMaxHeight()
                .width(dividerWidth)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(dividerColor)
            )
            Spacer(modifier = Modifier.height(handleSize))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(dividerColor)
            )
        }

        // HANDLE (Fixed to viewport)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (offsetPx - handleSizePx / 2).toInt(),
                        y = (maxHeightPx / 2 - handleSizePx / 2).toInt()
                    )
                }
                .size(handleSize)
                // Add the drag modifier here so it takes precedence
                .then(handleDragModifier)
                .border(width = dividerWidth, color = dividerColor, shape = CircleShape)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                contentDescription = "Slider Handle",
                tint = dividerColor,
                modifier = Modifier.size(24.dp)
            )
        }

        if (beforeLoading || afterLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}