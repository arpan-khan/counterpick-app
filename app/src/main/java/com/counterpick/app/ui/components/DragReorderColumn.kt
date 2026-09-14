package com.counterpick.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun <T> DragReorderColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit,
    itemHeight: Dp = 56.dp,
    itemContent: @Composable (T, isDragging: Boolean) -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0f) }

    Column {
        items.forEachIndexed { index, item ->
            val isDragging = draggingIndex == index
            Surface(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(0, if (isDragging) dragOffsetPx.roundToInt() else 0)
                    }
                    .onGloballyPositioned { coords ->
                        if (itemHeightPx == 0f) itemHeightPx = coords.size.height.toFloat()
                    }
                    .pointerInput(item) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetPx = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx += dragAmount.y
                                val from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val heightPx = if (itemHeightPx > 0f) itemHeightPx else 1f
                                val shift = (dragOffsetPx / heightPx).roundToInt()
                                if (shift != 0) {
                                    val to = (from + shift).coerceIn(0, items.lastIndex)
                                    if (to != from) {
                                        onReorder(from, to)
                                        draggingIndex = to
                                        dragOffsetPx -= shift * heightPx
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffsetPx = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffsetPx = 0f
                            }
                        )
                    }
            ) {
                itemContent(item, isDragging)
            }
        }
    }
}
