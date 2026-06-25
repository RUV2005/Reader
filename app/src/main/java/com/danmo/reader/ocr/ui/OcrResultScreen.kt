package com.danmo.reader.ocr.ui

import androidx.compose.foundation.background
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
    onBackClick: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isSpeaking by remember { mutableStateOf(false) }

    // TTS 回调逻辑
    val ttsCallbacks = remember(blocks) {
        object : TtsCallbacks {
            override fun onUtteranceDone(): Boolean = currentIndex < blocks.size - 1
            override fun getCurrentText(): String = blocks.getOrNull(currentIndex) ?: ""
            override fun getCurrentUtteranceId(): String = "ocr_block_$currentIndex"
            override fun moveToNext() { if (currentIndex < blocks.size - 1) currentIndex++ }
            override fun moveToPrevious() { if (currentIndex > 0) currentIndex-- }
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
                title = { Text("识别结果", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { ttsController.stop(); onBackClick() }) {
                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = "返回")
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
                currentIndex = currentIndex,
                totalCount = blocks.size,
                speechRate = ttsController.speechRate.collectAsState().value,
                accentColor = Color(0xFF4A6FA5),
                progressColor = Color(0xFF4A6FA5),
                previousLabel = "上一段",
                nextLabel = "下一段",
                positionText = "${currentIndex + 1}/${blocks.size}",
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
                .background(Color(0xFF1A1A1A))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(blocks.indices.toList()) { index ->
                val block = blocks[index]
                val isCurrent = index == currentIndex
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFF4A6FA5).copy(alpha = 0.3f) else Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        currentIndex = index
                        ttsController.speakCurrent()
                    }
                ) {
                    Text(
                        text = block,
                        color = if (isCurrent) Color(0xFFFFFF00) else Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
