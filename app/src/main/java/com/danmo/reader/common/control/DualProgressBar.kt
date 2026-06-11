package com.danmo.reader.common.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PDF 专用双进度条：页进度 + 段进度
 */
@Composable
fun DualProgressBar(
    currentPage: Int,
    totalPages: Int,
    currentParagraph: Int,
    totalParagraphs: Int,
    pageColor: Color,
    paragraphColor: Color = Color(0xFFFF6B6B),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProgressRow(
            label = "页",
            progress = (currentPage + 1).toFloat() / totalPages.coerceAtLeast(1).toFloat(),
            color = pageColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        ProgressRow(
            label = "段",
            progress = (currentParagraph + 1).toFloat() / totalParagraphs.coerceAtLeast(1).toFloat(),
            color = paragraphColor,
        )
    }
}

@Composable
private fun ProgressRow(
    label: String,
    progress: Float,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF888888),
            modifier = Modifier.width(20.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0xFF444444),
        )
    }
}