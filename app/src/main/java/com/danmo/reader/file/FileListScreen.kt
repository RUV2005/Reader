package com.danmo.reader.file

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * 文件基本元数据模型
 */
data class DocumentFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String,
    val modifiedTime: String,
    val path: String,
)

/**
 * 支持的文件类型枚举
 */
enum class FileType {
    WORD, EXCEL, PPT, PDF
}

/**
 * 文件管理页面组件
 * 展示所有已打开文件的历史记录，支持搜索、筛选和快速打开新文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    files: List<DocumentFile>,             // 文件列表数据
    onFileClick: (DocumentFile) -> Unit = {}, // 点击文件条目的回调
) {
    var searchQuery by remember { mutableStateOf("") }       // 搜索关键词
    var selectedFilter by remember { mutableStateOf<FileType?>(null) } // 当前选中的文件类型筛选器

    // 根据搜索和筛选条件过滤后的列表
    val filteredFiles = remember(searchQuery, selectedFilter, files) {
        files.filter { file ->
            val matchesSearch = searchQuery.isBlank() ||
                    file.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = (selectedFilter == null) || (file.type == selectedFilter)
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        // 1. 顶部搜索栏
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // 2. 类型筛选标签组
        FilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 文件列表区域
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filteredFiles, key = { it.id }) { file ->
                FileListItem(
                    file = file,
                ) { onFileClick(file) }
            }

            // 如果列表为空，展示占位提示
            if (filteredFiles.isEmpty()) {
                item {
                    EmptyFileList()
                }
            }
            
            // 底部留白，避免内容被悬浮按钮遮挡
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * 自定义搜索输入框
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("搜索文件名...", color = Color(0xFF999999)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "搜索",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF999999),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "清除",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF999999),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF4A6FA5),
            unfocusedBorderColor = Color(0xFFDDDDDD),
        ),
    )
}

/**
 * 文件类型筛选标签组
 */
@Composable
private fun FilterChips(
    selectedFilter: FileType?,
    onFilterSelected: (FileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                label = "全部",
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
            )
        }
        item {
            FilterChip(
                label = "Word",
                selected = selectedFilter == FileType.WORD,
                onClick = { onFilterSelected(FileType.WORD) },
                color = Color(0xFF2B579A),
            )
        }
        item {
            FilterChip(
                label = "Excel",
                selected = selectedFilter == FileType.EXCEL,
                onClick = { onFilterSelected(FileType.EXCEL) },
                color = Color(0xFF217346),
            )
        }
        item {
            FilterChip(
                label = "PPT",
                selected = selectedFilter == FileType.PPT,
                onClick = { onFilterSelected(FileType.PPT) },
                color = Color(0xFFD24726),
            )
        }
        item {
            FilterChip(
                label = "PDF",
                selected = selectedFilter == FileType.PDF,
                onClick = { onFilterSelected(FileType.PDF) },
                color = Color(0xFFB91C1C),
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = Color(0xFF4A6FA5),
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) color.copy(alpha = 0.15f)
                else Color(0xFFEEEEEE)
            )
            .border(
                width = 1.dp,
                color = if (selected) color.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) color else Color(0xFF666666),
        )
    }
}

/**
 * 单个文件列表项
 */
@Composable
private fun FileListItem(
    file: DocumentFile,
    onClick: () -> Unit,
) {
    val (iconRes, iconColor, bgColor) = when (file.type) {
        FileType.WORD -> Triple(R.drawable.ic_word, Color(0xFF2B579A), Color(0xFF2B579A).copy(alpha = 0.08f))
        FileType.EXCEL -> Triple(R.drawable.ic_excel, Color(0xFF217346), Color(0xFF217346).copy(alpha = 0.08f))
        FileType.PPT -> Triple(R.drawable.ic_ppt, Color(0xFFD24726), Color(0xFFD24726).copy(alpha = 0.08f))
        FileType.PDF -> Triple(R.drawable.ic_pdf, Color(0xFFB91C1C), Color(0xFFB91C1C).copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = iconColor,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 文件详细信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "上次打开：${file.modifiedTime}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                )
            }

            // 右侧装饰图标
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = "查看详情",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFCCCCCC),
            )
        }
    }
}

/**
 * 列表为空时的 UI
 */
@Composable
private fun EmptyFileList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_files),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFDDDDDD),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无文件记录",
            fontSize = 16.sp,
            color = Color(0xFF999999),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "从首页打开文档后将显示在此处",
            fontSize = 13.sp,
            color = Color(0xFFBBBBBB),
        )
    }
}
