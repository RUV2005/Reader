package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.danmo.reader.common.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// ==================== 数据模型 ====================

data class ExcelCell(
    val value: String,
    val rowIndex: Int,
    val colIndex: Int,
    val cellType: String
)

data class ExcelRow(
    val cells: List<String>,
    val rowIndex: Int,
    val isHeader: Boolean = false,
    val isTotalRow: Boolean = false
)

data class ExcelSheet(
    val name: String,
    val index: Int,
    val headers: List<String>,
    val rows: List<ExcelRow>,
    val totalRows: Int,
    val totalCols: Int,
    val imagePaths: List<String> = emptyList()
)

data class ExcelParseResult(
    val fileName: String,
    val sheets: List<ExcelSheet>,
    val currentSheetIndex: Int = 0,
    val totalSheets: Int = 0
)

// ==================== Excel 解析器 ====================

class ExcelParser : DocumentParser<ExcelParseResult> {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val numberFormat = DecimalFormat("0.##")

    override suspend fun parse(context: Context, uri: Uri): ParseResult<ExcelParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"
                val docHash = FileUtils.getUriHash(uri)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(context, inputStream, fileName, docHash)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 Excel 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<ExcelParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                parseInternal(null, inputStream, fileName, "temp_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                ParseResult.Error("解析 Excel 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(context: Context?, inputStream: InputStream, fileName: String, docHash: String): ParseResult<ExcelParseResult> {
        return try {
            WorkbookFactory.create(inputStream).use { workbook ->
                val sheets = mutableListOf<ExcelSheet>()
                for (i in 0 until workbook.numberOfSheets) {
                    sheets.add(parseSheet(context, workbook.getSheetAt(i), i, docHash))
                }

                ParseResult.Success(
                    ExcelParseResult(
                        fileName = fileName,
                        sheets = sheets,
                        currentSheetIndex = 0,
                        totalSheets = sheets.size
                    )
                )
            }
        } catch (e: Exception) {
            ParseResult.Error("解析失败: ${e.message}", e)
        }
    }

    private fun parseSheet(context: Context?, sheet: Sheet, sheetIndex: Int, docHash: String): ExcelSheet {
        val rows = mutableListOf<ExcelRow>()
        val headers = mutableListOf<String>()
        val imagePaths = mutableListOf<String>()

        // 1. 提取图片
        try {
            val docCacheDir = if (context != null) FileUtils.getDocCacheDir(context, docHash) else null
            if (docCacheDir != null) {
                val drawing = sheet.drawingPatriarch
                val shapes = drawing?.iterator()?.asSequence()?.toList() ?: emptyList()

                for (shape in shapes) {
                    if (shape is Picture) {
                        val picData = shape.pictureData.data
                        val ext = shape.pictureData.suggestFileExtension()
                        val fileName = "excel_pic_${sheetIndex}_${imagePaths.size}.$ext"
                        val picFile = java.io.File(docCacheDir, fileName)
                        
                        if (!picFile.exists()) {
                            java.io.FileOutputStream(picFile).use { fos ->
                                fos.write(picData)
                            }
                        }
                        imagePaths.add(picFile.absolutePath)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. 提取数据
        var maxCols = 0
        for (row in sheet) {
            val cellValues = mutableListOf<String>()
            for (i in 0 until row.lastCellNum) {
                cellValues.add(getCellValue(row.getCell(i)))
            }
            if (cellValues.size > maxCols) maxCols = cellValues.size

            val isHeader = row.rowNum == 0 && cellValues.all { it.isNotEmpty() && !it.matches(Regex("^\\d+\\.?\\d*$")) }
            val isTotalRow = cellValues.any { cell ->
                cell.contains("合计") || cell.contains("总计") || cell.contains("SUM") ||
                        cell.contains("平均") || cell.contains("Average", ignoreCase = true)
            }

            rows.add(
                ExcelRow(
                    cells = cellValues,
                    rowIndex = row.rowNum,
                    isHeader = isHeader,
                    isTotalRow = isTotalRow
                )
            )

            if (isHeader) {
                headers.addAll(cellValues)
            }
        }

        // 如果没有显式表头，使用默认表头
        if (headers.isEmpty()) {
            headers.addAll((0 until maxCols).map { "第${it + 1}列" })
        }

        // 补齐单元格
        val paddedRows = rows.map { row ->
            if (row.cells.size < maxCols) {
                val paddedCells = row.cells.toMutableList()
                while (paddedCells.size < maxCols) paddedCells.add("")
                row.copy(cells = paddedCells)
            } else row
        }

        return ExcelSheet(
            name = sheet.sheetName,
            index = sheetIndex,
            headers = headers,
            rows = paddedRows,
            totalRows = paddedRows.size,
            totalCols = maxCols,
            imagePaths = imagePaths
        )
    }

    private fun getCellValue(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    dateFormat.format(cell.dateCellValue)
                } else {
                    numberFormat.format(cell.numericCellValue)
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue ?: ""
                } catch (e: Exception) {
                    try {
                        numberFormat.format(cell.numericCellValue)
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }

    fun getColumnData(excelSheet: ExcelSheet, colIndex: Int): List<String> {
        val columnData = mutableListOf<String>()
        for (row in excelSheet.rows) {
            if (colIndex < row.cells.size) {
                columnData.add(row.cells[colIndex])
            }
        }
        return columnData
    }
}
