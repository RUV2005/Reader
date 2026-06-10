package com.danmo.reader.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Excel 单元格数据
 */
data class ExcelCell(
    val value: String,
    val rowIndex: Int,
    val colIndex: Int,
    val cellType: String
)

/**
 * Excel 行数据
 */
data class ExcelRow(
    val cells: List<String>,
    val rowIndex: Int,
    val isHeader: Boolean = false,
    val isTotalRow: Boolean = false
)

/**
 * Excel 工作表
 */
data class ExcelSheet(
    val name: String,
    val index: Int,
    val headers: List<String>,
    val rows: List<ExcelRow>,
    val totalRows: Int = 0,
    val totalCols: Int = 0
)

/**
 * Excel 解析结果
 */
data class ExcelParseResult(
    val fileName: String,
    val sheets: List<ExcelSheet>,
    val currentSheetIndex: Int = 0,
    val totalSheets: Int = 0
)

/**
 * Excel 文档解析器
 * 支持 .xls (HSSF) 和 .xlsx (XSSF) 格式
 */
class ExcelParser : DocumentParser<ExcelParseResult> {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val numberFormat = DecimalFormat("#.##")

    override suspend fun parse(context: Context, uri: Uri): ParseResult<ExcelParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "未知文件"

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseInternal(inputStream, fileName)
                } ?: ParseResult.Error("无法打开文件输入流")
            } catch (e: Exception) {
                ParseResult.Error("解析 Excel 文档失败: ${e.message}", e)
            }
        }
    }

    override suspend fun parse(inputStream: InputStream, fileName: String): ParseResult<ExcelParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                parseInternal(inputStream, fileName)
            } catch (e: Exception) {
                ParseResult.Error("解析 Excel 文档失败: ${e.message}", e)
            }
        }
    }

    private fun parseInternal(inputStream: InputStream, fileName: String): ParseResult<ExcelParseResult> {
        return try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheets = mutableListOf<ExcelSheet>()

            val sheetIterator = workbook.sheetIterator()
            var index = 0
            while (sheetIterator.hasNext()) {
                val sheet = sheetIterator.next()
                val sheetData = parseSheet(sheet, index)
                sheets.add(sheetData)
                index++
            }

            workbook.close()

            ParseResult.Success(
                ExcelParseResult(
                    fileName = fileName,
                    sheets = sheets,
                    currentSheetIndex = 0,
                    totalSheets = sheets.size
                )
            )
        } catch (e: Exception) {
            ParseResult.Error("解析失败: ${e.message}", e)
        }
    }

    private fun parseSheet(sheet: Sheet, sheetIndex: Int): ExcelSheet {
        val rows = mutableListOf<ExcelRow>()
        var maxCols = 0
        var headers = listOf<String>()

        // 遍历所有行
        val rowIterator = sheet.rowIterator()
        var rowIndex = 0
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            val cells = mutableListOf<String>()
            var cellCount = 0

            // 遍历行中所有单元格
            val cellIterator = row.cellIterator()
            while (cellIterator.hasNext()) {
                val cell = cellIterator.next()
                val cellValue = getCellValue(cell)
                cells.add(cellValue)
                cellCount++
            }

            if (cellCount > maxCols) {
                maxCols = cellCount
            }

            // 启发式判断表头（第一行且包含文本）
            val isHeader = rowIndex == 0 && cells.any { it.isNotEmpty() } &&
                    cells.all { it.isEmpty() || !it.matches(Regex("^\\d+\\.?\\d*$")) }

            if (isHeader) {
                headers = cells.toList()
            }

            // 判断是否为合计行
            val isTotalRow = cells.any { cell ->
                cell.contains("合计") || cell.contains("总计") || cell.contains("SUM") ||
                        cell.contains("平均") || cell.contains("Average", ignoreCase = true)
            }

            rows.add(
                ExcelRow(
                    cells = cells,
                    rowIndex = rowIndex,
                    isHeader = isHeader,
                    isTotalRow = isTotalRow
                )
            )
            rowIndex++
        }

        // 如果没有检测到表头，生成默认列名
        if (headers.isEmpty() && maxCols > 0) {
            headers = (0 until maxCols).map { "第${it + 1}列" }
        }

        // 统一每行的列数
        val normalizedRows = rows.map { row ->
            val paddedCells = row.cells.toMutableList()
            while (paddedCells.size < maxCols) {
                paddedCells.add("")
            }
            row.copy(cells = paddedCells.take(maxCols))
        }

        return ExcelSheet(
            name = sheet.sheetName,
            index = sheetIndex,
            headers = headers,
            rows = normalizedRows,
            totalRows = normalizedRows.size,
            totalCols = maxCols
        )
    }

    /**
     * 获取单元格的字符串值
     */
    private fun getCellValue(cell: Cell): String {
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
                    // 尝试获取公式计算结果
                    when (cell.cachedFormulaResultType) {
                        CellType.NUMERIC -> numberFormat.format(cell.numericCellValue)
                        CellType.STRING -> cell.stringCellValue ?: ""
                        else -> cell.cellFormula ?: ""
                    }
                } catch (e: Exception) {
                    cell.cellFormula ?: ""
                }
            }
            CellType.BLANK -> ""
            else -> ""
        }
    }

    /**
     * 按列朗读时获取列数据
     */
    fun getColumnData(sheet: ExcelSheet, colIndex: Int): List<String> {
        return sheet.rows.map { row ->
            row.cells.getOrNull(colIndex) ?: ""
        }.filter { it.isNotEmpty() }
    }
}