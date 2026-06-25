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
    val title: String,            // 卡片标题 (如: 打开Word)
    val subtitle: String,         // 副标题 (如: 打开以查看文档)
    val iconRes: Int,             // 卡片显示的图标资源 ID
    val backgroundColor: Color    // 卡片的背景主题色
)

/**
 * 底部/侧边导航项的数据结构
 */
data class BottomNavItemData(
    val label: String,            // 导航项显示的文字
    val iconRes: Int              // 导航项显示的图标
)

// ==================== 静态配置数据 ====================

/**
 * 定义首页展示的四种核心文档类型入口
 */
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

/**
 * 导航栏项配置
 */
val bottomNavItems = listOf(
    BottomNavItemData("文件", R.drawable.ic_files),
    BottomNavItemData("首页", R.drawable.ic_home),
    BottomNavItemData("设置", R.drawable.ic_settings_nav)
)

// ==================== 首页主屏幕 ====================

/**
 * 首页 Composable，支持响应式布局（自动切换横竖屏结构）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShelf: () -> Unit = {},          // 切换到“文件”Tab的回调
    onNavigateToProfile: () -> Unit = {},        // 切换到“设置”Tab的回调
    onSettingsClick: () -> Unit = {},            // 点击右上角设置按钮的回调
    onScanClick: () -> Unit = {},                // 扫描按钮回调
    onFunctionCardClick: (String) -> Unit = {},  // 点击文档类型卡片的回调
    onRecentFileClick: (RecentFile) -> Unit = {}, // 点击最近打开文件的回调
    recentFiles: List<RecentFile> = emptyList(), // 最近文件历史数据列表
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedTab by remember { mutableIntStateOf(1) } // 首页默认索引为 1

    if (isLandscape) {
        // ── 横屏布局方案 ────────────────────────────────
        // 采用“左侧导航导轨 + 右侧双栏内容”的结构
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .systemBarsPadding() // 避免遮挡系统状态栏/手势条
        ) {
            // 1. 左侧导航导轨 (NavigationRail)
            NavigationRail(
                containerColor = Color.White,
                header = {
                    ScanFloatingButton {
                        onScanClick()
                    }
                },
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                bottomNavItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = index
                            when (index) {
                                0 -> onNavigateToShelf()
                                1 -> { /* 已经在首页 */ }
                                2 -> onNavigateToProfile()
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF4A6FA5),
                            selectedTextColor = Color(0xFF4A6FA5),
                            unselectedIconColor = Color(0xFF999999),
                            unselectedTextColor = Color(0xFF999999),
                            indicatorColor = Color(0xFF4A6FA5).copy(alpha = 0.1f)
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // 2. 右侧双栏内容区
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // 左栏：放置欢迎语和四个核心功能卡片
                LazyColumn(
                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        HeaderSection(
                            greeting = "下午好",
                            subtitle = "高效阅读每一天",
                            isLandscape = true,
                            onSettingsClick = onSettingsClick
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        // 横屏下卡片平铺排列，不使用竖屏下的重叠负位移
                        FunctionCardsGrid(
                            cards = functionCards,
                            useOffset = false,
                            isLandscape = true,
                            onCardClick = onFunctionCardClick
                        )
                    }
                }

                // 右栏：放置最近文件列表
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        RecentFilesSection(
                            files = recentFiles,
                            onFileClick = onRecentFileClick
                        )
                    }
                }
            }
        }
    } else {
        // ── 竖屏布局方案 ────────────────────────────────
        // 采用经典的“列表滚动 + 底部导航栏”结构
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        when (index) {
                            0 -> onNavigateToShelf()
                            1 -> { /* 已经在首页 */ }
                            2 -> onNavigateToProfile()
                        }
                    }
                )
            },
            floatingActionButton = {
                ScanFloatingButton {
                    onScanClick()
                }
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
                // 1. 顶部 Header 区
                item {
                    HeaderSection(
                        greeting = "下午好",
                        subtitle = "高效阅读每一天",
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
                        onFileClick = onRecentFileClick
                    )
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 70.dp else 180.dp) // 横屏下极大压缩高度以节省垂直空间
            .clip(RoundedCornerShape(if (isLandscape) 12.dp else 0.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A6FA5),
                        Color(0xFF6B8CBB)
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
                contentDescription = "设置",
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
            // 竖屏下通过负位移实现卡片与 Header 视觉上的“衔接”
            .then(if (useOffset) Modifier.offset(y = (-30).dp) else Modifier)
    ) {
        // 第一行卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.take(2).forEach { card ->
                FunctionCardItem(
                    card = card,
                    isLandscape = isLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(card.title) }
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
                FunctionCardItem(
                    card = card,
                    isLandscape = isLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = { onCardClick(card.title) }
                )
            }
        }
    }
}

/**
 * 单个功能卡片组件，支持响应式尺寸调整
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
            .height(if (isLandscape) 85.dp else 110.dp)
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
                .padding(if (isLandscape) 12.dp else 16.dp)
        ) {
            Text(
                text = card.title,
                fontSize = if (isLandscape) 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 卡片底部图标
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 28.dp else 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(card.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = card.iconRes),
                        contentDescription = card.title,
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                    )
                }

                if (!isLandscape) {
                    // 横屏下隐藏详细描述以精简界面
                    Text(
                        text = card.subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ==================== BottomNavigationBar (底部导航栏) ====================

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

// ==================== ScanFloatingButton (悬浮按钮) ====================

/**
 * 屏幕中心的扫描悬浮按钮，支持手势和屏幕阅读器
 */
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
            contentDescription = "扫描识字",
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
                    modifier = Modifier.clickable { /* TODO: 跳转至文件列表 Tab */ }
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

// ==================== 界面预览 ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
