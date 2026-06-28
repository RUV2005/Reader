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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R

// ==================== 数据模型 ====================

enum class FileType {
    WORD, EXCEL, PPT, PDF
}

data class DocumentFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String,
    val modifiedTime: String,
    val path: String
)

// ==================== 文件列表屏幕 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    files: List<DocumentFile>,
    onFileClick: (DocumentFile) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<FileType?>(null) }

    val filteredFiles = remember(files, searchQuery, selectedFilter) {
        files.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || file.type == selectedFilter
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. 搜索栏
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // 2. 过滤器
        FilterChips(
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it }
        )

        // 3. 文件列表
        if (filteredFiles.isEmpty()) {
            EmptyFileState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFiles) { file ->
                    FileItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ==================== 搜索栏 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(stringResource(id = R.string.files_search_placeholder)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.files_search_placeholder),
                tint = Color.Gray
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.dialog_close),
                        tint = Color.Gray
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true
    )
}

// ==================== 过滤器 ====================

@Composable
private fun FilterChips(
    selectedFilter: FileType?,
    onFilterChange: (FileType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipItem(
            label = stringResource(id = R.string.files_filter_all),
            isSelected = selectedFilter == null,
            onClick = { onFilterChange(null) }
        )
        FilterChipItem(
            label = stringResource(id = R.string.files_filter_word),
            isSelected = selectedFilter == FileType.WORD,
            onClick = { onFilterChange(FileType.WORD) }
        )
        FilterChipItem(
            label = stringResource(id = R.string.files_filter_excel),
            isSelected = selectedFilter == FileType.EXCEL,
            onClick = { onFilterChange(FileType.EXCEL) }
        )
        FilterChipItem(
            label = stringResource(id = R.string.files_filter_ppt),
            isSelected = selectedFilter == FileType.PPT,
            onClick = { onFilterChange(FileType.PPT) }
        )
        FilterChipItem(
            label = stringResource(id = R.string.files_filter_pdf),
            isSelected = selectedFilter == FileType.PDF,
            onClick = { onFilterChange(FileType.PDF) }
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== 文件项 ====================

@Composable
private fun FileItem(
    file: DocumentFile,
    onClick: () -> Unit
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
                    contentDescription = file.type.name,
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
                    text = stringResource(id = R.string.last_opened, file.modifiedTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = stringResource(id = R.string.file_detail),
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFCCCCCC)
            )
        }
    }
}

// ==================== 空状态 ====================

@Composable
private fun EmptyFileState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_file),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFCCCCCC)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.files_empty_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF999999)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.files_empty_tip),
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
