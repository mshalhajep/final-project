package com.example.ui.theme

import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path

/**
 * Bespoke in-app icon set drawn with vector paths in the LocalConnect brand palette:
 * Deep Indigo (#6366F1), Cyber Cyan (#0EA5E9), Emerald (#10B981), Crimson Rose (#EF4444).
 */
object AppIcons {

    /** P2P radar emblem: glowing central node between telemetry arcs. */
    val P2PRadar: ImageVector by lazy {
        materialIcon(name = "P2PRadar") {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                // Central glowing node
                moveTo(12f, 10.2f)
                arcTo(1.8f, 1.8f, 0f, false, true, 12f, 13.8f)
                arcTo(1.8f, 1.8f, 0f, false, true, 12f, 10.2f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.95f,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                // Left telemetry arc
                moveTo(8.5f, 8.2f)
                curveTo(7.1f, 9.2f, 6.2f, 10.5f, 6.2f, 12f)
                curveTo(6.2f, 13.5f, 7.1f, 14.8f, 8.5f, 15.8f)
                // Right telemetry arc
                moveTo(15.5f, 8.2f)
                curveTo(16.9f, 9.2f, 17.8f, 10.5f, 17.8f, 12f)
                curveTo(17.8f, 13.5f, 16.9f, 14.8f, 15.5f, 15.8f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.55f,
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                fill = null
            ) {
                // Outer wave arcs
                moveTo(5.4f, 5.6f)
                curveTo(3.3f, 7.2f, 2f, 9.4f, 2f, 12f)
                curveTo(2f, 14.6f, 3.3f, 16.8f, 5.4f, 18.4f)
                moveTo(18.6f, 5.6f)
                curveTo(20.7f, 7.2f, 22f, 9.4f, 22f, 12f)
                curveTo(22f, 14.6f, 20.7f, 16.8f, 18.6f, 18.4f)
            }
        }
    }

    /** End-to-end encryption shield with keyhole cut-out. */
    val ShieldEncryption: ImageVector by lazy {
        materialIcon(name = "ShieldEncryption") {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Shield body
                moveTo(12f, 2f)
                lineTo(20f, 5.6f)
                lineTo(20f, 11.4f)
                curveTo(20f, 16.4f, 16.8f, 20.2f, 12f, 22f)
                curveTo(7.2f, 20.2f, 4f, 16.4f, 4f, 11.4f)
                lineTo(4f, 5.6f)
                close()
                // Keyhole cut-out (subpath, removed by EvenOdd)
                moveTo(12f, 8.2f)
                arcTo(2.1f, 2.1f, 0f, false, true, 12f, 12.4f)
                lineTo(12.9f, 16.4f)
                lineTo(11.1f, 16.4f)
                lineTo(12f, 12.4f)
                close()
            }
        }
    }

    /** Live voice waveform bars. */
    val VoiceWave: ImageVector by lazy {
        materialIcon(name = "VoiceWave") {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = null
            ) {
                moveTo(4f, 10f)
                lineTo(4f, 14f)
                moveTo(8f, 7f)
                lineTo(8f, 17f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(16f, 7f)
                lineTo(16f, 17f)
                moveTo(20f, 10f)
                lineTo(20f, 14f)
            }
        }
    }

    /** Screen broadcast: display with rising broadcast signal. */
    val ScreenBroadcast: ImageVector by lazy {
        materialIcon(name = "ScreenBroadcast") {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(3f, 4.5f)
                lineTo(21f, 4.5f)
                arcTo(1.2f, 1.2f, 0f, false, true, 22.2f, 5.7f)
                lineTo(22.2f, 15.3f)
                arcTo(1.2f, 1.2f, 0f, false, true, 21f, 16.5f)
                lineTo(14.4f, 16.5f)
                lineTo(15.2f, 19f)
                lineTo(17.2f, 19f)
                lineTo(17.2f, 20.6f)
                lineTo(6.8f, 20.6f)
                lineTo(6.8f, 19f)
                lineTo(8.8f, 19f)
                lineTo(9.6f, 16.5f)
                lineTo(3f, 16.5f)
                arcTo(1.2f, 1.2f, 0f, false, true, 1.8f, 15.3f)
                lineTo(1.8f, 5.7f)
                arcTo(1.2f, 1.2f, 0f, false, true, 3f, 4.5f)
                close()
                moveTo(3.6f, 6.3f)
                lineTo(3.6f, 14.7f)
                lineTo(20.4f, 14.7f)
                lineTo(20.4f, 6.3f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.9f
            ) {
                // Play/broadcast triangle inside the display
                moveTo(10.2f, 8f)
                lineTo(15.2f, 10.5f)
                lineTo(10.2f, 13f)
                close()
            }
        }
    }

    /** Group room: three connected members. */
    val GroupRoom: ImageVector by lazy {
        materialIcon(name = "GroupRoom") {
            path(
                fill = SolidColor(Color.White)
            ) {
                // Center member
                moveTo(12f, 7.2f)
                arcTo(2.2f, 2.2f, 0f, false, true, 12f, 11.6f)
                arcTo(2.2f, 2.2f, 0f, false, true, 12f, 7.2f)
                close()
                // Center body
                moveTo(8.2f, 17.6f)
                curveTo(8.2f, 14.9f, 9.9f, 13.1f, 12f, 13.1f)
                curveTo(14.1f, 13.1f, 15.8f, 14.9f, 15.8f, 17.6f)
                lineTo(15.8f, 18.4f)
                lineTo(8.2f, 18.4f)
                close()
                // Left member
                moveTo(4.9f, 9.1f)
                arcTo(1.8f, 1.8f, 0f, false, true, 4.9f, 12.7f)
                arcTo(1.8f, 1.8f, 0f, false, true, 4.9f, 9.1f)
                close()
                moveTo(1.8f, 17f)
                curveTo(1.8f, 14.9f, 3.2f, 13.5f, 4.9f, 13.5f)
                curveTo(5.4f, 13.5f, 5.9f, 13.6f, 6.3f, 13.9f)
                curveTo(5.1f, 14.9f, 4.4f, 16.3f, 4.2f, 18f)
                lineTo(1.8f, 18f)
                close()
                // Right member
                moveTo(19.1f, 9.1f)
                arcTo(1.8f, 1.8f, 0f, false, true, 19.1f, 12.7f)
                arcTo(1.8f, 1.8f, 0f, false, true, 19.1f, 9.1f)
                close()
                moveTo(22.2f, 17f)
                curveTo(22.2f, 14.9f, 20.8f, 13.5f, 19.1f, 13.5f)
                curveTo(18.6f, 13.5f, 18.1f, 13.6f, 17.7f, 13.9f)
                curveTo(18.9f, 14.9f, 19.6f, 16.3f, 19.8f, 18f)
                lineTo(22.2f, 18f)
                close()
            }
        }
    }

    /** Signal strength: four ascending quality bars. */
    val SignalStrength: ImageVector by lazy {
        materialIcon(name = "SignalStrength") {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(3f, 13.6f)
                lineTo(6.2f, 13.6f)
                lineTo(6.2f, 19.4f)
                lineTo(3f, 19.4f)
                close()
                moveTo(8.4f, 10.2f)
                lineTo(11.6f, 10.2f)
                lineTo(11.6f, 19.4f)
                lineTo(8.4f, 19.4f)
                close()
                moveTo(13.8f, 6.6f)
                lineTo(17f, 6.6f)
                lineTo(17f, 19.4f)
                lineTo(13.8f, 19.4f)
                close()
                moveTo(19.2f, 3f)
                lineTo(22.4f, 3f)
                lineTo(22.4f, 19.4f)
                lineTo(19.2f, 19.4f)
                close()
            }
        }
    }
}
