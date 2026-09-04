package com.example.ui.components

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.network.VideoEngine

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect

@Composable
fun CameraPreviewSurface(
    videoEngine: VideoEngine,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFrontCamera by videoEngine.isFrontCamera.collectAsState()
    val isCameraOff by videoEngine.isCameraOff.collectAsState()
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(isFrontCamera, isCameraOff, lifecycleOwner, previewViewRef) {
        if (!isCameraOff) {
            previewViewRef?.let { pv ->
                if (androidx.core.content.ContextCompat.checkSelfPermission(pv.context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    videoEngine.startCameraStream(lifecycleOwner, pv.surfaceProvider)
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                previewViewRef = this
                if (!isCameraOff) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        videoEngine.startCameraStream(lifecycleOwner, surfaceProvider)
                    }
                }
            }
        },
        update = { previewView ->
            previewViewRef = previewView
        },
        modifier = modifier.graphicsLayer(scaleX = if (isFrontCamera) -1f else 1f)
    )
}

@Composable
fun RemotePeerVideoView(
    bitmap: Bitmap?,
    peerName: String,
    modifier: Modifier = Modifier,
    enableZoom: Boolean = true
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Video stream from $peerName",
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (enableZoom) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val maxOffsetX = (size.width * (scale - 1)) / 2f
                                            val maxOffsetY = (size.height * (scale - 1)) / 2f
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        } else Modifier
                    ),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Camera off",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Peer name tag
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = peerName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
