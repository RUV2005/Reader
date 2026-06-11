package com.danmo.reader

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.data.repository.RecentFile
import com.danmo.reader.data.repository.RecentFileRepository

// ==================== 数据模型 ====================

data class FunctionCardData(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val backgroundColor: Color
)

data class BottomNavItemData(
    val label: String,
    val iconRes: Int
)

// ==================== 模拟数据 ====================

val functionCards = listOf(
    FunctionCardData(
        title = "打开Word",
        subtitle = "打开以查看文档",
        iconRes = R.drawable.ic_word,
        backgroundColor = Color(0xFF2B579A)
    ),
    FunctionCardData(
        title = "打开Excel",
        subtitle = "打开以查看表格",
        iconRes = R.drawable.ic_excel,
        backgroundColor = Color(0xFF217346)
    ),
    FunctionCardData(
        title = "打开PPT",
        subtitle = "打开以查看演示文稿",
        iconRes = R.drawable.ic_ppt,
        backgroundColor = Color(0xFFD24726)
    ),
    FunctionCardData(
        title = "打开PDF",
        subtitle = "打开以查看PDF",
        iconRes = R.drawable.ic_pdf,
        backgroundColor = Color(0xFFB91C1C)
    )
)

val bottomNavItems = listOf(
    BottomNavItemData("文件", R.drawable.ic_files),
    BottomNavItemData("首页", R.drawable.ic_home),
    BottomNavItemData("设置", R.drawable.ic_settings_nav)
)

// ==================== 主页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShelf: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onFunctionCardClick: (String) -> Unit = {},
    onRecentFileClick: (RecentFile) -> Unit = {},
    recentFiles: List<RecentFile> = emptyList(),
) {
    var selectedTab by remember { mutableIntStateOf(1) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        0 -> onNavigateToShelf()
                        1 -> { /* 已在首页 */ }
                        2 -> onNavigateToProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            ScanFloatingButton(
                onScanClick = { /* TODO: 打开扫描功能 */ }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HeaderSection(
                    greeting = "下午好",
                    subtitle = "高效阅读每一天",
                    onSettingsClick = { /* TODO */ }
                )
            }

            item {
                FunctionCardsGrid(
                    cards = functionCards,
                    onCardClick = onFunctionCardClick
                )
            }

            item {
                RecentFilesSection(
                    files = recentFiles,
                    onFileClick = onRecentFileClick
                )
            }
        }
    }
}

// ==================== Header 区域 ====================

@Composable
fun HeaderSection(
    greeting: String,
    subtitle: String,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A6FA5),
                        Color(0xFF6B8CBB)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = greeting,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }

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
                contentDescription = "设置",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== 功能卡片网格 ====================

@Composable
fun FunctionCardsGrid(
    cards: List<FunctionCardData>,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-30).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.take(2).forEach { card ->
                FunctionCardItem(
                    card = card,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(card.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.drop(2).take(2).forEach { card ->
                FunctionCardItem(
                    card = card,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(card.title) }
                )
            }
        }
    }
}

@Composable
fun FunctionCardItem(
    card: FunctionCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = card.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = card.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(card.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = card.iconRes),
                        contentDescription = card.title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = card.subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ==================== 底部导航栏 ====================

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) Color(0xFF4A6FA5) else Color(0xFF999999)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF4A6FA5) else Color(0xFF999999)
                    )
                }
            }
        }
    }
}

// ==================== 悬浮扫描按钮 ====================

@Composable
fun ScanFloatingButton(
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4A6FA5),
                        Color(0xFF6B8CBB)
                    )
                )
            )
            .clickable(onClick = onScanClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_card_camera),
            contentDescription = "扫描",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ==================== 最近打开文件区域 ====================

@Composable
fun RecentFilesSection(
    files: List<RecentFile>,
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
                text = "最近打开",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            if (files.isNotEmpty()) {
                Text(
                    text = "查看全部",
                    fontSize = 14.sp,
                    color = Color(0xFF4A6FA5),
                    modifier = Modifier.clickable { /* TODO */ }
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
            text = "暂无最近打开的文件",
            fontSize = 14.sp,
            color = Color(0xFF999999),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "打开文档后将显示在这里",
            fontSize = 12.sp,
            color = Color(0xFFBBBBBB),
        )
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = Color(0xFF333333),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.openTimeDisplay,
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = "打开",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFCCCCCC)
            )
        }
    }
}

// ==================== 预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}