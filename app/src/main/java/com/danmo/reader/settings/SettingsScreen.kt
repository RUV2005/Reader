package com.danmo.reader.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconRes: Int,
    val iconColor: Color,
    val type: SettingType = SettingType.NAVIGATE,
)

enum class SettingType {
    NAVIGATE,      // 点击进入子页面
    TOGGLE,        // 开关
    SELECT,        // 选择项
    VALUE,         // 数值调节
}

// 设置分组
data class SettingGroup(
    val title: String,
    val items: List<SettingItem>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAccessibility: () -> Unit = {},
) {
    var ttsEnabled by remember { mutableStateOf(true) }
    var autoScroll by remember { mutableStateOf(true) }
    var highContrast by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(18) }
    var speechRate by remember { mutableStateOf(1.0f) }

    val settingGroups = remember {
        listOf(
            SettingGroup(
                title = "阅读设置",
                items = listOf(
                    SettingItem(
                        id = "tts",
                        title = "语音朗读",
                        subtitle = "开启后自动朗读文档内容",
                        iconRes = R.drawable.ic_tts,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.TOGGLE,
                    ),
                    SettingItem(
                        id = "speech_rate",
                        title = "默认语速",
                        subtitle = "当前: 1.0x",
                        iconRes = R.drawable.ic_speed,
                        iconColor = Color(0xFF6B8CBB),
                        type = SettingType.VALUE,
                    ),
                    SettingItem(
                        id = "font_size",
                        title = "字体大小",
                        subtitle = "当前: 18sp",
                        iconRes = R.drawable.ic_text_size,
                        iconColor = Color(0xFF8B9DC3),
                        type = SettingType.VALUE,
                    ),
                    SettingItem(
                        id = "auto_scroll",
                        title = "自动滚动",
                        subtitle = "朗读时自动滚动到当前位置",
                        iconRes = R.drawable.ic_scroll,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.TOGGLE,
                    ),
                ),
            ),
            SettingGroup(
                title = "无障碍",
                items = listOf(
                    SettingItem(
                        id = "high_contrast",
                        title = "高对比度模式",
                        subtitle = "增强文字与背景对比度",
                        iconRes = R.drawable.ic_contrast,
                        iconColor = Color(0xFF217346),
                        type = SettingType.TOGGLE,
                    ),
                    SettingItem(
                        id = "gesture",
                        title = "手势控制",
                        subtitle = "配置滑动和点击手势",
                        iconRes = R.drawable.ic_gesture,
                        iconColor = Color(0xFFD24726),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "accessibility",
                        title = "TalkBack 优化",
                        subtitle = "优化屏幕阅读器体验",
                        iconRes = R.drawable.ic_accessibility,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.NAVIGATE,
                    ),
                ),
            ),
            SettingGroup(
                title = "通用",
                items = listOf(
                    SettingItem(
                        id = "storage",
                        title = "存储管理",
                        subtitle = "清理缓存、管理下载文件",
                        iconRes = R.drawable.ic_storage,
                        iconColor = Color(0xFF6B8CBB),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "language",
                        title = "语言",
                        subtitle = "简体中文",
                        iconRes = R.drawable.ic_language,
                        iconColor = Color(0xFF8B9DC3),
                        type = SettingType.SELECT,
                    ),
                    SettingItem(
                        id = "theme",
                        title = "主题",
                        subtitle = "跟随系统",
                        iconRes = R.drawable.ic_theme,
                        iconColor = Color(0xFF4A6FA5),
                        type = SettingType.SELECT,
                    ),
                ),
            ),
            SettingGroup(
                title = "关于",
                items = listOf(
                    SettingItem(
                        id = "about",
                        title = "关于应用",
                        subtitle = "版本 1.0.0",
                        iconRes = R.drawable.ic_info,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "feedback",
                        title = "反馈建议",
                        subtitle = "帮助我们改进产品",
                        iconRes = R.drawable.ic_feedback,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                    SettingItem(
                        id = "privacy",
                        title = "隐私政策",
                        iconRes = R.drawable.ic_privacy,
                        iconColor = Color(0xFF999999),
                        type = SettingType.NAVIGATE,
                    ),
                ),
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A6FA5),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState()),
        ) {

            // 设置分组
            settingGroups.forEach { group ->
                SettingGroupSection(
                    group = group,
                    onItemClick = { item ->
                        when (item.id) {
                            "tts" -> ttsEnabled = !ttsEnabled
                            "auto_scroll" -> autoScroll = !autoScroll
                            "high_contrast" -> highContrast = !highContrast
                            "about" -> onNavigateToAbout()
                            "accessibility" -> onNavigateToAccessibility()
                            "speech_rate" -> { /* 打开语速调节对话框 */ }
                            "font_size" -> { /* 打开字体大小选择 */ }
                        }
                    },
                    toggleStates = mapOf(
                        "tts" to ttsEnabled,
                        "auto_scroll" to autoScroll,
                        "high_contrast" to highContrast,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 版本信息
            Text(
                text = "版本 1.0.0 (Build 20240611)",
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingGroupSection(
    group: SettingGroup,
    onItemClick: (SettingItem) -> Unit,
    toggleStates: Map<String, Boolean>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 分组标题
        Text(
            text = group.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                group.items.forEachIndexed { index, item ->
                    SettingItemRow(
                        item = item,
                        isChecked = toggleStates[item.id] ?: false,
                        onClick = { onItemClick(item) },
                        isLast = index == group.items.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingItemRow(
    item: SettingItem,
    isChecked: Boolean,
    onClick: () -> Unit,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = item.iconColor,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 标题和副标题
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
            )
            item.subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                )
            }
        }

        // 右侧控件
        when (item.type) {
            SettingType.TOGGLE -> {
                Switch(
                    checked = isChecked,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = item.iconColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC),
                    ),
                )
            }

            SettingType.NAVIGATE, SettingType.SELECT, SettingType.VALUE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.type == SettingType.VALUE) {
                        Text(
                            text = when (item.id) {
                                "speech_rate" -> "1.0x"
                                "font_size" -> "18sp"
                                else -> ""
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "进入",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFCCCCCC),
                    )
                }
            }
        }
    }

    if (!isLast) {
        Divider(
            modifier = Modifier.padding(start = 66.dp),
            color = Color(0xFFEEEEEE),
            thickness = 0.5.dp,
        )
    }
}