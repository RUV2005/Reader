package com.danmo.reader.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R

// ==================== 通用控制按钮 ====================

@Composable
fun ReaderControlButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    buttonDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color(0xFFCCCCCC),
    labelColor: Color = Color(0xFFCCCCCC),
    iconSize: Dp = 24.dp,
    labelFontSize: TextUnit = 11.sp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = buttonDescription }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = labelFontSize,
            color = labelColor,
        )
    }
}

// ==================== 播放/暂停按钮 ====================

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    backgroundColor: Color,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isPlaying) "暂停朗读" else "开始朗读"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            ),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ==================== 语速选择器 ====================

private val SPEECH_RATES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f)

@Composable
private fun SpeedSelector(
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

// ==================== 单进度条 ====================

@Composable
private fun SingleProgressBar(
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

// ==================== 竖屏底部控制栏 ====================

@Composable
private fun PortraitControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    accentColor: Color,
    previousLabel: String,
    nextLabel: String,
    positionText: String,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    progressColor: Color,
    playButtonSize: Dp,
    progressBar: @Composable ColumnScope.() -> Unit,
    extraContent: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2B2B2B),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            progressBar()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeedSelector(currentRate = speechRate, accentColor = accentColor, onRateChange = onRateChange)
                extraContent()
                ReaderControlButton(
                    iconRes = R.drawable.ic_previous,
                    label = previousLabel,
                    onClick = onPrevious,
                    buttonDescription = "朗读$previousLabel",
                )
                PlayPauseButton(
                    isPlaying = isSpeaking,
                    onClick = onPlayPause,
                    size = playButtonSize,
                    backgroundColor = accentColor,
                )
                ReaderControlButton(
                    iconRes = R.drawable.ic_next,
                    label = nextLabel,
                    onClick = onNext,
                    buttonDescription = "朗读$nextLabel",
                )
                ReaderControlButton(
                    iconRes = R.drawable.ic_chapters,
                    label = positionText,
                    onClick = { },
                    buttonDescription = "当前第${currentIndex + 1}项，共$totalCount 项，点击跳转",
                )
            }
        }
    }
}

// ==================== 横屏侧边控制栏 ====================

@Composable
private fun LandscapeControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    accentColor: Color,
    previousLabel: String,
    nextLabel: String,
    positionText: String,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    progressColor: Color,
    playButtonSize: Dp,
    progressBar: @Composable ColumnScope.() -> Unit,
    extraContent: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(80.dp),
        color = Color(0xFF2B2B2B),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            progressBar()
            Spacer(modifier = Modifier.height(8.dp))
            SpeedSelector(currentRate = speechRate, accentColor = accentColor, onRateChange = onRateChange)
            extraContent()
            ReaderControlButton(
                iconRes = R.drawable.ic_previous,
                label = previousLabel,
                onClick = onPrevious,
                buttonDescription = "朗读$previousLabel",
            )
            PlayPauseButton(
                isPlaying = isSpeaking,
                onClick = onPlayPause,
                size = 48.dp,
                backgroundColor = accentColor,
            )
            ReaderControlButton(
                iconRes = R.drawable.ic_next,
                label = nextLabel,
                onClick = onNext,
                buttonDescription = "朗读$nextLabel",
            )
            ReaderControlButton(
                iconRes = R.drawable.ic_chapters,
                label = positionText,
                onClick = { },
                buttonDescription = "当前第${currentIndex + 1}项，共$totalCount 项，点击跳转",
            )
        }
    }
}

// ==================== 通用阅读控制栏（自动适配横竖屏） ====================

@Composable
fun ReaderControlBar(
    isSpeaking: Boolean,
    currentIndex: Int,
    totalCount: Int,
    speechRate: Float,
    accentColor: Color,
    previousLabel: String,
    nextLabel: String,
    positionText: String,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRateChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    progressColor: Color = accentColor,
    playButtonSize: Dp = 56.dp,
    progressBar: @Composable ColumnScope.() -> Unit = {
        SingleProgressBar(
            currentIndex = currentIndex,
            totalCount = totalCount,
            color = progressColor,
        )
    },
    leftExtra: @Composable () -> Unit = {},
    rightExtra: @Composable () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val extraContent: @Composable () -> Unit = {
        leftExtra()
        rightExtra()
    }

    if (isLandscape) {
        LandscapeControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentIndex,
            totalCount = totalCount,
            speechRate = speechRate,
            accentColor = accentColor,
            previousLabel = previousLabel,
            nextLabel = nextLabel,
            positionText = positionText,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onRateChange = onRateChange,
            progressColor = progressColor,
            playButtonSize = playButtonSize,
            progressBar = progressBar,
            extraContent = extraContent,
        )
    } else {
        PortraitControlBar(
            isSpeaking = isSpeaking,
            currentIndex = currentIndex,
            totalCount = totalCount,
            speechRate = speechRate,
            accentColor = accentColor,
            previousLabel = previousLabel,
            nextLabel = nextLabel,
            positionText = positionText,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onRateChange = onRateChange,
            progressColor = progressColor,
            playButtonSize = playButtonSize,
            progressBar = progressBar,
            extraContent = extraContent,
        )
    }
}