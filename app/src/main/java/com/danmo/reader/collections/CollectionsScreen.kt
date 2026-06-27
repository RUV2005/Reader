package com.danmo.reader.collections

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danmo.reader.R
import com.danmo.reader.data.local.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CollectionsScreen(
    viewModel: CollectionsViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showDetailDialog by remember { mutableStateOf<NoteEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 搜索栏
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text(stringResource(id = R.string.files_search_placeholder)) },
            leadingIcon = { Icon(painterResource(id = R.drawable.ic_search), contentDescription = null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (notes.isEmpty()) {
            EmptyNotesState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onClick = { showDetailDialog = note }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    showDetailDialog?.let { note ->
        NoteDetailDialog(
            note = note,
            onDismiss = { showDetailDialog = null },
            onDelete = {
                viewModel.deleteNote(note)
                showDetailDialog = null
            }
        )
    }
}

@Composable
private fun NoteItem(
    note: NoteEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(getCategoryColor(note.category))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = dateStr,
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun NoteDetailDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(note.content, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("${stringResource(id = R.string.note_category_label)}: ${note.category}", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_close)) }
        },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                Text(stringResource(id = R.string.note_delete))
            }
        }
    )
}

@Composable
private fun EmptyNotesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_file),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.note_empty_tip), color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

@Composable
private fun getCategoryColor(category: String): Color {
    val medicine = stringResource(id = R.string.cat_medicine)
    val life = stringResource(id = R.string.cat_life)
    val work = stringResource(id = R.string.cat_work)
    
    return when (category) {
        medicine -> Color(0xFFE91E63)
        life -> Color(0xFF4CAF50)
        work -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
}
