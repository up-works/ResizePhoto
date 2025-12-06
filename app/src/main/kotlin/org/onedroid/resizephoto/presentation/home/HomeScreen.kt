package org.onedroid.resizephoto.presentation.home

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

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

    var previewFile by remember { mutableStateOf<File?>(null) }
    var previewTitle by remember { mutableStateOf<String?>(null) }
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Original / Placeholder
            Box(
                Modifier
                    .weight(1f)
                    .height(240.dp)
                    .shadow(5.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = { pickImage.launch("image/*") },
                        onLongClick = {
//                            if (state.actualImage != null) {
//                                previewFile = state.actualImage
//                                previewTitle = "Original"
//                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val actual = state.actualImage
                if (actual != null) {
                    val bmp = remember(actual.absolutePath, actual.lastModified()) {
                        BitmapFactory.decodeFile(actual.absolutePath)
                    }
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Actual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Exif info button
                    if (state.originalExif != null) {
                        IconButton(
                            onClick = { showExifDialog = state.originalExif },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Exif Info",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pick Image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Compressed Image
            Box(
                Modifier
                    .weight(1f)
                    .height(240.dp)
                    .shadow(5.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val resizedImage = state.resizedImage
                if (resizedImage != null) {
                    val bmp = remember(resizedImage.absolutePath, resizedImage.lastModified()) {
                        BitmapFactory.decodeFile(resizedImage.absolutePath)
                    }
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Compressed",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                previewFile = resizedImage
                                previewTitle = "Resized Image"
                            }
                    )
                    // Exif info button
                    if (state.resizedExif != null) {
                        IconButton(
                            onClick = { showExifDialog = state.resizedExif },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Exif Info",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Compressed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Processing Time Info
        if (state.processingTime > 0) {
            Text(
                text = "Processing Time: ${state.processingTime} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Resolution Info
        if (state.originalResolution != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Original Size",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${state.originalResolution!!.first} x ${state.originalResolution!!.second}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (state.resizedResolution != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Resized Size",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${state.resizedResolution!!.first} x ${state.resizedResolution!!.second}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Resize Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Resize Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = ResizeMode.entries.toTypedArray()
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.resizeMode == mode,
                            onClick = { viewModel.setResizeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(
                                when(mode) {
                                    ResizeMode.PERCENTAGE -> "Percentage"
                                    ResizeMode.PIXELS -> "Pixels"
                                    ResizeMode.LONG_EDGE -> "Long Edge"
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (state.resizeMode) {
                    ResizeMode.PERCENTAGE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = state.resolution * 100,
                                onValueChange = { viewModel.setResolution(it / 100f) },
                                valueRange = 1f..100f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = "${(state.resolution * 100).toInt()}",
                                onValueChange = {
                                    val num = it.toFloatOrNull()
                                    if (num != null) viewModel.setResolution(num / 100f)
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                suffix = { Text("%") }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(25, 50, 75).forEach { percent ->
                                val isSelected = (state.resolution * 100).toInt() == percent
                                OutlinedButton(
                                    onClick = { viewModel.setResolution(percent / 100f) },
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("$percent%")
                                }
                            }
                        }
                    }
                    ResizeMode.PIXELS -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Keep Aspect Ratio",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { viewModel.setKeepAspectRatio(!state.keepAspectRatio) }
                            )
                        }
                    }
                    ResizeMode.LONG_EDGE -> {
                         OutlinedTextField(
                            value = state.targetLongEdge,
                            onValueChange = { viewModel.setTargetLongEdge(it) },
                            label = { Text("Long Edge Size") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.useLanczos,
                        onCheckedChange = { viewModel.setUseLanczos(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Use Lanczos Resampling (High Quality, Slower)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { viewModel.setUseLanczos(!state.useLanczos) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.keepExif,
                        onCheckedChange = { viewModel.setKeepExif(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Keep EXIF Data",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { viewModel.setKeepExif(!state.keepExif) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        viewModel.resizeImage() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.actualImage != null
                ) {
                    Text("Resize Image")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (previewFile != null) {
            Dialog(onDismissRequest = { previewFile = null }) {
                val bmp =
                    remember(previewFile!!.absolutePath) { BitmapFactory.decodeFile(previewFile!!.absolutePath) }
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 8f)
                    val scaleRatio = newScale / scale
                    scale = newScale
                    offset = Offset(
                        x = (offset.x + panChange.x * scaleRatio).coerceIn(-1000f, 1000f),
                        y = (offset.y + panChange.y * scaleRatio).coerceIn(-1000f, 1000f)
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(transformState)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = previewTitle ?: "Preview", modifier = Modifier.weight(1f))
                        Button(onClick = {
                            previewFile = null; previewTitle = null
                        }) { Text("Close") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}


@Composable
private fun ExifInfoDialog(exifData: ExifData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Exif Data",
                    style = MaterialTheme.typography.titleLarge,
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
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}