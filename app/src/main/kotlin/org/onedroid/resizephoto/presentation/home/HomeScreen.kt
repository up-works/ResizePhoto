package org.onedroid.resizephoto.presentation.home

import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import org.onedroid.resizephoto.domain.model.ResizeAlgorithm
import org.onedroid.resizephoto.presentation.home.component.CompareImage
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.setActualImage(uri)
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    var showExifDialog by remember { mutableStateOf<ExifData?>(null) }

    if (showExifDialog != null) {
        ExifInfoDialog(exifData = showExifDialog!!) {
            showExifDialog = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            var originalLoading by remember { mutableStateOf(true) }
            if (state.actualImage != null) {
                if (state.resizedImage != null) {
                    CompareImage(
                        beforeImage = state.actualImage!!,
                        afterImage = state.resizedImage!!,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (state.originalExif != null) {
                        ExifInfoButton(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            contentDescription = "Original Exif Info",
                            onClick = { showExifDialog = state.originalExif }
                        )
                    }

                    if (state.resizedExif != null) {
                        ExifInfoButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            contentDescription = "Resized Exif Info",
                            onClick = { showExifDialog = state.resizedExif }
                        )
                    }

                    if (state.isResizing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    AsyncImage(
                        model = state.actualImage!!,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onLoading = { originalLoading = true },
                        onSuccess = { originalLoading = false },
                        onError = { originalLoading = false }
                    )

                    if (state.originalExif != null) {
                        ExifInfoButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            contentDescription = "Exif Info",
                            onClick = { showExifDialog = state.originalExif }
                        )
                    }

                    if (state.isResizing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { pickImage.launch("image/*") },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Add Image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tap to Select Image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Processing Stats ---
        if (state.processingTime > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${state.processingTime} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // --- Info Card (Resolution & Size) ---
        AnimatedVisibility(
            visible = state.originalResolution != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Image Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth()) {
                        // Original Stats
                        Column(Modifier.weight(1f)) {
                            StatItem(
                                icon = Icons.Rounded.AspectRatio,
                                label = "Dimensions",
                                value = "${state.originalResolution?.first} x ${state.originalResolution?.second}"
                            )
                            Spacer(Modifier.height(8.dp))
                            StatItem(
                                icon = Icons.Rounded.PhotoSizeSelectLarge,
                                label = "Size",
                                value = formatFileSize(context, state.originalSize ?: 0)
                            )
                            if (state.actualImage != null) {
                                Spacer(Modifier.height(8.dp))
                                StatItem(
                                    icon = Icons.Rounded.Image,
                                    label = "Format",
                                    value = state.actualImage!!.extension.uppercase(Locale.getDefault())
                                )
                            }
                        }

                        // Arrow
                        if (state.resizedResolution != null) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.CompareArrows,
                                    contentDescription = "to",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Resized Stats
                        if (state.resizedResolution != null) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                StatItem(
                                    icon = Icons.Rounded.AspectRatio,
                                    label = "Dimensions",
                                    value = "${state.resizedResolution?.first} x ${state.resizedResolution?.second}",
                                    alignment = Alignment.End
                                )
                                Spacer(Modifier.height(8.dp))
                                StatItem(
                                    icon = Icons.Rounded.Compress,
                                    label = "Size",
                                    value = formatFileSize(context, state.resizedSize ?: 0),
                                    alignment = Alignment.End,
                                    valueColor = MaterialTheme.colorScheme.primary
                                )

                                if (state.resizedImage != null) {
                                    Spacer(Modifier.height(8.dp))
                                    StatItem(
                                        icon = Icons.Rounded.Image,
                                        label = "Format",
                                        value = state.resizedImage!!.extension.uppercase(Locale.getDefault()),
                                        alignment = Alignment.End
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))
        // --- Resize Settings ---
        AnimatedVisibility(visible = state.actualImage != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Resize Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val modes = ResizeMode.entries.toTypedArray()
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.resizeMode == mode,
                                onClick = { viewModel.setResizeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size
                                )
                            ) {
                                Text(
                                    when (mode) {
                                        ResizeMode.PERCENTAGE -> "Percentage"
                                        ResizeMode.PIXELS -> "Pixels"
                                        ResizeMode.LONG_EDGE -> "Long Edge"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    AnimatedVisibility(visible = state.resizeMode == ResizeMode.PERCENTAGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = state.resolution * 100,
                                onValueChange = { viewModel.setResolution(it / 100f) },
                                valueRange = 1f..100f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${(state.resolution * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.resizeMode == ResizeMode.PIXELS) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.targetWidth,
                                    onValueChange = { viewModel.setTargetWidth(it) },
                                    label = { Text("Width") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.targetHeight,
                                    onValueChange = { viewModel.setTargetHeight(it) },
                                    label = { Text("Height") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.keepAspectRatio,
                                    onCheckedChange = { viewModel.setKeepAspectRatio(it) }
                                )
                                Text(
                                    text = "Keep Aspect Ratio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { viewModel.setKeepAspectRatio(!state.keepAspectRatio) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = state.resizeMode == ResizeMode.LONG_EDGE) {
                        Column {
                            OutlinedTextField(
                                value = state.targetLongEdge,
                                onValueChange = { viewModel.setTargetLongEdge(it) },
                                label = { Text("Long Edge (px)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                supportingText = { Text("The longer side will be set to this value") }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Advanced Options
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.algorithm == ResizeAlgorithm.BITMAP_SCALING,
                                onCheckedChange = { viewModel.setAlgorithm(ResizeAlgorithm.BITMAP_SCALING) }
                            )
                            Column(Modifier.clickable { viewModel.setAlgorithm(ResizeAlgorithm.BITMAP_SCALING) }) {
                                Text(
                                    text = "Bitmap Scaling",
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.algorithm == ResizeAlgorithm.LANCZOS,
                                onCheckedChange = { viewModel.setAlgorithm(ResizeAlgorithm.LANCZOS) }
                            )
                            Column(Modifier.clickable { viewModel.setAlgorithm(ResizeAlgorithm.LANCZOS) }) {
                                Text(
                                    text = "Lanczos algorithm",
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.keepExif,
                                onCheckedChange = { viewModel.setKeepExif(it) }
                            )
                            Column(Modifier.clickable { viewModel.setKeepExif(!state.keepExif) }) {
                                Text(
                                    text = "Keep EXIF Data",
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.resizeImage()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = state.actualImage != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Compress, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resize Image")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignment == Alignment.End) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
private fun ExifInfoButton(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                CircleShape
            )
            .size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ExifInfoDialog(exifData: ExifData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Exif Data",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val exifEntries = exifData.values.entries.filter { it.value != null }.toList()

                if (exifEntries.isEmpty()) {
                    Text("No Exif data found.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(
                        modifier = Modifier
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        exifEntries.forEach { (tag, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.3f
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

private fun formatFileSize(context: Context, size: Long): String {
    return Formatter.formatFileSize(context, size)
}
