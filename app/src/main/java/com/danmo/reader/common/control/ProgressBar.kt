package com.danmo.reader.common.control

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 单进度条（Word / PPT / Excel 默认使用）
 */
@Composable
fun SingleProgressBar(
    currentIndex: Int,
    totalCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { (currentIndex + 1).toFloat() / totalCount.coerceAtLeast(1).toFloat() },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = color,
        trackColor = Color(0xFF444444),
    )
}