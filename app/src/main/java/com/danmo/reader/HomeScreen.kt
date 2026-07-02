package com.danmo.reader

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.data.repository.RecentFile
import com.danmo.reader.data.repository.RecentFileRepository

// ==================== 数据模型 ====================

/**
 * 首页功能卡片的数据结构
 */
data class FunctionCardData(
    val titleRes: Int,            // 卡片标题资源 ID
    val subtitleRes: Int,         // 副标题资源 ID
    val iconRes: Int,             // 卡片显示的图标资源 ID
    val backgroundColor: Color    // 卡片的背景主题色
)

// ==================== 静态配置数据 ====================

/**
 * 定义首页展示的四种核心文档类型入口
 */
val functionCards = listOf(
    FunctionCardData(
        titleRes = R.string.action_open_word,
        subtitleRes = R.string.desc_open_word,
        iconRes = R.drawable.ic_word,
        backgroundColor = Color(0xFF2B579A)
    ),
    FunctionCardData(
        titleRes = R.string.action_open_excel,
        subtitleRes = R.string.desc_open_excel,
        iconRes = R.drawable.ic_excel,
        backgroundColor = Color(0xFF217346)
    ),
    FunctionCardData(
        titleRes = R.string.action_open_ppt,
        subtitleRes = R.string.desc_open_ppt,
        iconRes = R.drawable.ic_ppt,
        backgroundColor = Color(0xFFD24726)
    ),
    FunctionCardData(
        titleRes = R.string.action_open_pdf,
        subtitleRes = R.string.desc_open_pdf,
        iconRes = R.drawable.ic_pdf,
        backgroundColor = Color(0xFFB91C1C)
    )
)

// ==================== 首页主屏幕 ====================

/**
 * 首页 Composable，支持响应式布局
 */
@Composable
fun HomeScreen(
    onNavigateToShelf: () -> Unit = {},          // 切换到“文件”Tab的回调
    onNavigateToProfile: () -> Unit = {},        // 切换到“设置”Tab的回调
    onSettingsClick: () -> Unit = {},            // 点击右上角设置按钮的回调
    onViewAllClick: () -> Unit = {},             // 点击“查看全部”的回调
    onScanClick: () -> Unit = {},                // 扫描按钮回调
    onFunctionCardClick: (String) -> Unit = {},  // 点击文档类型卡片的回调
    onRecentFileClick: (RecentFile) -> Unit = {}, // 点击最近打开文件的回调
    recentFiles: List<RecentFile> = emptyList(), // 最近文件历史数据列表
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. 顶部 Header 区
        item {
            HeaderSection(
                greeting = stringResource(id = R.string.greeting_afternoon),
                subtitle = stringResource(id = R.string.home_subtitle),
                onSettingsClick = onSettingsClick
            )
        }

        // 2. 功能入口卡片网格
        item {
            FunctionCardsGrid(
                cards = functionCards,
                onCardClick = onFunctionCardClick
            )
        }

        // 3. 最近文件历史记录
        item {
            RecentFilesSection(
                files = recentFiles,
                onViewAllClick = onViewAllClick,
                onFileClick = onRecentFileClick
            )
        }
    }
}

// ==================== HeaderSection (顶部欢迎区) ====================

@Composable
fun HeaderSection(
    greeting: String,
    subtitle: String,
    isLandscape: Boolean = false,
    onSettingsClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 70.dp else 180.dp) // 横屏下极大压缩高度以节省垂直空间
            .clip(RoundedCornerShape(if (isLandscape) 12.dp else 0.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 8.dp else 24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = greeting,
                color = Color.White,
                fontSize = if (isLandscape) 20.sp else 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (!isLandscape) {
                // 竖屏下显示鼓励语，横屏下为节省空间而隐藏
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // 右上角快速设置按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = stringResource(id = R.string.tab_settings),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== FunctionCards (功能卡片网格) ====================

/**
 * 将四个功能入口按 2x2 网格排列
 */
@Composable
fun FunctionCardsGrid(
    cards: List<FunctionCardData>,
    useOffset: Boolean = true,
    isLandscape: Boolean = false,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (useOffset) 16.dp else 0.dp)
            // 降低负位移量，适应更紧凑的横向卡片
            .then(if (useOffset) Modifier.offset(y = (-20).dp) else Modifier)
    ) {
        // 第一行卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.take(2).forEach { card ->
                val title = stringResource(id = card.titleRes)
                FunctionCardItem(
                    card = card,
                    isLandscape = isLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 第二行卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.asSequence().drop(2).take(2).forEach { card ->
                val title = stringResource(id = card.titleRes)
                FunctionCardItem(
                    card = card,
                    isLandscape = isLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(title) }
                )
            }
        }
    }
}

/**
 * 单个功能卡片组件，支持响应式尺寸调整
 * 改版布局：左侧图标，右侧文字
 */
@Composable
fun FunctionCardItem(
    card: FunctionCardData,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(if (isLandscape) 70.dp else 80.dp) // 压缩高度使布局更精致
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = card.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 1. 左侧图标区
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 32.dp else 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = card.iconRes),
                    contentDescription = stringResource(id = card.titleRes),
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. 右侧文字区
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = card.titleRes),
                    fontSize = if (isLandscape) 15.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (!isLandscape) {
                    Text(
                        text = stringResource(id = card.subtitleRes),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==================== ScanFloatingButton (悬浮按钮) ====================

/**
 * 屏幕中心的扫描悬浮按钮，支持手势和屏幕阅读器
 */
@Composable
fun ScanFloatingButton(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .clickable(onClick = onScanClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_card_camera),
            contentDescription = stringResource(id = R.string.ocr_title),
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ==================== RecentFilesSection (最近历史) ====================

/**
 * 展示最近打开过的文档列表
 */
@Composable
fun RecentFilesSection(
    files: List<RecentFile>,
    onViewAllClick: () -> Unit = {},
    onFileClick: (RecentFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.home_recent_files),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (files.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.home_view_all),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (files.isEmpty()) {
            EmptyRecentFiles()
        } else {
            files.forEach { file ->
                RecentFileItem(
                    file = file,
                    onClick = { onFileClick(file) }
                )
                if (file != files.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 列表为空时的占位 UI
 */
@Composable
private fun EmptyRecentFiles() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_file),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFCCCCCC),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.home_no_recent_files),
            fontSize = 14.sp,
            color = Color(0xFF999999),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.home_recent_files_tip),
            fontSize = 12.sp,
            color = Color(0xFFBBBBBB),
        )
    }
}

/**
 * 单条历史文件记录项
 */
@Composable
fun RecentFileItem(
    file: RecentFile,
    onClick: () -> Unit
) {
    val (iconRes, iconColor, bgColor) = when (file.type) {
        "word" -> Triple(R.drawable.ic_word, Color(0xFF2B579A), Color(0xFF2B579A).copy(alpha = 0.08f))
        "excel" -> Triple(R.drawable.ic_excel, Color(0xFF217346), Color(0xFF217346).copy(alpha = 0.08f))
        "ppt" -> Triple(R.drawable.ic_ppt, Color(0xFFD24726), Color(0xFFD24726).copy(alpha = 0.08f))
        "pdf" -> Triple(R.drawable.ic_pdf, Color(0xFFB91C1C), Color(0xFFB91C1C).copy(alpha = 0.08f))
        else -> Triple(R.drawable.ic_file, Color(0xFF666666), Color(0xFF666666).copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = file.type,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.openTimeDisplay,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = stringResource(id = R.string.reader_play).replace("开始朗读", "打开"),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

// ==================== 界面预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
