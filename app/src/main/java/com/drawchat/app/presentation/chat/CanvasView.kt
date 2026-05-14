package com.drawchat.app.presentation.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.drawchat.app.data.model.CanvasPoint

@Composable
fun CanvasView(
    selectedColor: Color,
    strokeWidth: Float,
    onDrawEvent: (CanvasPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<PathData>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Canvas(
        modifier = modifier
            .background(Color.White)
            .pointerInput(selectedColor, strokeWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newPath = Path().apply {
                            moveTo(offset.x, offset.y)
                        }
                        currentPath = newPath
                        onDrawEvent(
                            CanvasPoint(
                                x = offset.x,
                                y = offset.y,
                                color = colorToHex(selectedColor),
                                strokeWidth = strokeWidth
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPath?.lineTo(
                            change.position.x,
                            change.position.y
                        )
                        onDrawEvent(
                            CanvasPoint(
                                x = change.position.x,
                                y = change.position.y,
                                color = colorToHex(selectedColor),
                                strokeWidth = strokeWidth
                            )
                        )
                    },
                    onDragEnd = {
                        currentPath?.let { path ->
                            paths.add(
                                PathData(
                                    path = path,
                                    color = selectedColor,
                                    strokeWidth = strokeWidth
                                )
                            )
                        }
                        currentPath = null
                    }
                )
            }
    ) {
        paths.forEach { pathData ->
            drawPath(
                path = pathData.path,
                color = pathData.color,
                style = Stroke(
                    width = pathData.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        currentPath?.let { path ->
            drawPath(
                path = path,
                color = selectedColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

data class PathData(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}