package com.danmo.reader.common.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danmo.reader.R
import com.danmo.reader.common.ReaderControlButton

private val SPEECH_RATES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/**
 * 语速选择器（按钮 + DropdownMenu）
 */
@Composable
fun SpeedSelector(
    currentRate: Float,
    accentColor: Color,
    onRateChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ReaderControlButton(
            iconRes = R.drawable.ic_speed,
            label = "${(currentRate * 100).toInt()}%",
            onClick = { expanded = !expanded },
            buttonDescription = "当前语速${(currentRate * 100).toInt()}%，点击调节",
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF333333)),
        ) {
            SPEECH_RATES.forEach { rate ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(rate * 100).toInt()}%",
                            color = if (rate == currentRate) accentColor else Color.White,
                            fontWeight = if (rate == currentRate) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onRateChange(rate)
                        expanded = false
                    },
                )
            }
        }
    }
}