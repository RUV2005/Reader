package com.danmo.reader.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class DocumentFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String,
    val modifiedTime: String,
    val path: String,
)

enum class FileType {
    WORD, EXCEL, PPT, PDF
}

// 模拟数据
val sampleFiles = listOf(
    DocumentFile("1", "项目计划书.docx", FileType.WORD, "256 KB", "2024-06-10 14:30", "/storage/documents/"),
    DocumentFile("2", "销售数据报表.xlsx", FileType.EXCEL, "1.2 MB", "2024-06-09 09:15", "/storage/documents/"),
    DocumentFile("3", "产品发布会.pptx", FileType.PPT, "5.8 MB", "2024-06-08 16:45", "/storage/documents/"),
    DocumentFile("4", "合同协议.pdf", FileType.PDF, "320 KB", "2024-06-07 11:20", "/storage/documents/"),
    DocumentFile("5", "年度总结.docx", FileType.WORD, "180 KB", "2024-06-06 17:00", "/storage/documents/"),
    DocumentFile("6", "财务报表.xlsx", FileType.EXCEL, "890 KB", "2024-06-05 08:30", "/storage/documents/"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    files: List<DocumentFile> = sampleFiles,
    onFileClick: (DocumentFile) -> Unit = {},
    onBackClick: () -> Unit = {},
    onPickFile: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<FileType?>(null) }

    val filteredFiles = remember(searchQuery, selectedFilter, files) {
        files.filter { file ->
            val matchesSearch = searchQuery.isBlank() ||
                    file.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || file.type == selectedFilter
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件管理", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp)
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPickFile,
                containerColor = Color(0xFF4A6FA5),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                text = { Text("打开文件", color = Color.White) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(16.dp),
            )

            // 筛选标签
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 文件列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredFiles, key = { it.id }) { file ->
                    FileListItem(
                        file = file,
                        onClick = { onFileClick(file) },
                    )
                }

                if (filteredFiles.isEmpty()) {
                    item {
                        EmptyFileList()
                    }
                }
            }
        }
    }
}

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

@Composable
private fun FilterChips(
    selectedFilter: FileType?,
    onFilterSelected: (FileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = "全部",
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
        )
        FilterChip(
            label = "Word",
            selected = selectedFilter == FileType.WORD,
            onClick = { onFilterSelected(FileType.WORD) },
            color = Color(0xFF2B579A),
        )
        FilterChip(
            label = "Excel",
            selected = selectedFilter == FileType.EXCEL,
            onClick = { onFilterSelected(FileType.EXCEL) },
            color = Color(0xFF217346),
        )
        FilterChip(
            label = "PPT",
            selected = selectedFilter == FileType.PPT,
            onClick = { onFilterSelected(FileType.PPT) },
            color = Color(0xFFD24726),
        )
        FilterChip(
            label = "PDF",
            selected = selectedFilter == FileType.PDF,
            onClick = { onFilterSelected(FileType.PDF) },
            color = Color(0xFFB91C1C),
        )
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
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
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
            // 文件图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = file.type.name,
                    modifier = Modifier.size(26.dp),
                    tint = iconColor,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.size,
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = Color(0xFFCCCCCC),
                    )
                    Text(
                        text = file.modifiedTime,
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                    )
                }
            }

            // 右箭头
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = "打开",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFCCCCCC),
            )
        }
    }
}

@Composable
private fun EmptyFileList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_file),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFCCCCCC),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无文件",
            fontSize = 16.sp,
            color = Color(0xFF999999),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角按钮打开文件",
            fontSize = 13.sp,
            color = Color(0xFFBBBBBB),
        )
    }
}