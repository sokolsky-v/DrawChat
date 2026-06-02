package com.drawchat.app.presentation.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

@Composable
fun CanvasView(
    selectedColor: Color,
    strokeWidth: Float,
    isEraser: Boolean = false,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf(viewModel.currentPathData) }

    Canvas(
        modifier = modifier
            .background(Color.White)
            .pointerInput(selectedColor, strokeWidth, isEraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val pd = PathData(
                            color = selectedColor,
                            strokeWidth = strokeWidth,
                            points = listOf(Offset(offset.x, offset.y)),
                            isEraser = isEraser
                        )
                        viewModel.currentPathData = pd
                        currentPath = pd
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        viewModel.currentPathData?.let { c ->
                            val updated = c.copy(points = c.points + Offset(change.position.x, change.position.y))
                            viewModel.currentPathData = updated
                            currentPath = updated
                        }
                    },
                    onDragEnd = {
                        viewModel.currentPathData?.let { viewModel.canvasPaths.add(it) }
                        viewModel.currentPathData = null
                        currentPath = null
                        viewModel.autoSaveCanvas()
                    }
                )
            }
    ) {
        // Загруженные линии
        viewModel.loadedPaths.forEach { pd -> drawPathData(pd) }
        // Новые линии
        viewModel.canvasPaths.forEach { pd -> drawPathData(pd) }
        // Текущая линия
        currentPath?.let { drawPathData(it) }
    }
}

private fun DrawScope.drawPathData(pd: PathData) {
    if (pd.points.size < 2) return
    val path = Path().apply {
        moveTo(pd.points[0].x, pd.points[0].y)
        for (i in 1 until pd.points.size) {
            lineTo(pd.points[i].x, pd.points[i].y)
        }
    }
    if (pd.isEraser) {
        drawPath(path, Color.White, style = Stroke(width = pd.strokeWidth * 3, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        drawPath(path, pd.color, style = Stroke(width = pd.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}