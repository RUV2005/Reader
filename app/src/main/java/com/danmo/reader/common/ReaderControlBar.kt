package com.danmo.reader.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R

/**
 * 通用阅读器底栏组件
 * 集成了播放/暂停、上一个/下一个切换、进度显示和语速调节
 */
@Composable
fun ReaderControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    previousLabel: String = "上一个",
    nextLabel: String = "下一个",
    positionText: String = "",
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    leftExtra: @Composable () -> Unit = {},
    progressBar: @Composable () -> Unit = {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. 进度条
            progressBar()

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧辅助功能（如朗读模式切换）
                Box(modifier = Modifier.width(48.dp)) {
                    leftExtra()
                }

                // 核心控制区
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ReaderControlButton(
                        iconRes = R.drawable.ic_previous,
                        label = previousLabel,
                        onClick = onPrevious
                    )

                    PlayPauseButton(
                        isPlaying = isSpeaking,
                        onClick = onPlayPause,
                        accentColor = accentColor
                    )

                    ReaderControlButton(
                        iconRes = R.drawable.ic_next,
                        label = nextLabel,
                        onClick = onNext
                    )
                }

                // 语速切换
                SpeedRateButton(
                    currentRate = speechRate,
                    onRateChange = onRateChange,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isPlaying) "暂停朗读" else "开始朗读"
            },
        color = accentColor,
        tonalElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun ReaderControlButton(
    iconRes: Int,
    label: String,
    buttonDescription: String? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .semantics {
                contentDescription = buttonDescription ?: label
            }
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpeedRateButton(
    currentRate: Float,
    onRateChange: (Float) -> Unit,
    accentColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }
    val rates = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showMenu = true }
                .padding(4.dp)
                .semantics {
                    contentDescription = "语速：${(currentRate * 100).toInt()}%，点击切换"
                }
        ) {
            Text(
                text = "${(currentRate * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = "语速",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(rate * 100).toInt()}%",
                            color = if (rate == currentRate) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onRateChange(rate)
                        showMenu = false
                    }
                )
            }
        }
    }
}
