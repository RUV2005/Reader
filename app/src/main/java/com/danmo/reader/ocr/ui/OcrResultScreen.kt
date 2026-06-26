package com.danmo.reader.ocr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.reader.R
import com.danmo.reader.common.ReaderControlBar
import com.danmo.reader.tts.TtsCallbacks
import com.danmo.reader.tts.TtsState
import com.danmo.reader.tts.rememberTtsController
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultScreen(
    text: String,
    blocks: List<String>,
    onBackClick: () -> Unit = {}
) {
    var currentBlockIndex by remember { mutableIntStateOf(0) }
    var isSpeaking by remember { mutableStateOf(false) }

    val ttsCallbacks = remember(blocks, currentBlockIndex) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean {
                return currentBlockIndex < blocks.size - 1
            }

            override fun getCurrentText(): String {
                return blocks.getOrNull(currentBlockIndex) ?: ""
            }

            override fun getCurrentUtteranceId(): String {
                return "ocr_block_$currentBlockIndex"
            }

            override fun moveToNext() {
                if (currentBlockIndex < blocks.size - 1) {
                    currentBlockIndex++
                }
            }

            override fun moveToPrevious() {
                if (currentBlockIndex > 0) {
                    currentBlockIndex--
                }
            }
        }
    }

    val ttsController = rememberTtsController(callbacks = ttsCallbacks)

    LaunchedEffect(ttsController) {
        ttsController.state.collectLatest { state ->
            isSpeaking = state is TtsState.Speaking
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ocr_result_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = stringResource(id = R.string.dialog_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A6FA5),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ReaderControlBar(
                isSpeaking = isSpeaking,
                currentIndex = currentBlockIndex,
                totalCount = blocks.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = Color(0xFF4A6FA5),
                progressColor = Color(0xFF4A6FA5),
                previousLabel = stringResource(id = R.string.reader_prev_para),
                nextLabel = stringResource(id = R.string.reader_next_para),
                positionText = stringResource(id = R.string.reader_pos_format, currentBlockIndex + 1, blocks.size),
                onPrevious = { ttsController.speakPrevious() },
                onPlayPause = { ttsController.togglePlayPause() },
                onNext = { ttsController.speakNext() },
                onRateChange = { ttsController.setSpeechRate(it) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
                val isCurrent = index == currentBlockIndex
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFF4A6FA5).copy(alpha = 0.1f) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
                ) {
                    Text(
                        text = block,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp,
                        color = if (isCurrent) Color(0xFF4A6FA5) else Color(0xFF333333),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}
