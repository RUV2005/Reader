package com.danmo.reader.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR 识别结果数据类
 */
data class OcrResult(
    val text: String,
    val blocks: List<String> = emptyList()
)

/**
 * ML Kit 文字识别管理类
 * 支持中文和拉丁文字
 */
class OcrManager(private val context: Context) {

    // 初始化中文识别器
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /**
     * 从图片 URI 识别文本
     */
    suspend fun recognizeText(uri: Uri): OcrResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            
            val fullText = visionText.text
            val blocks = visionText.textBlocks.map { it.text }
            
            OcrResult(text = fullText, blocks = blocks)
        } catch (e: Exception) {
            OcrResult(text = "识别出错: ${e.message}")
        }
    }
}
