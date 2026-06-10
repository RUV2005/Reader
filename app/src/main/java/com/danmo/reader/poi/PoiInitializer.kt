package com.danmo.reader.poi

import android.util.Log
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.opc.PackageAccess
import java.io.InputStream

object PoiInitializer {
    private const val TAG = "PoiInitializer"

    init {
        // 禁用 POI 的 AWT 依赖（Android 不支持）
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.stax.EventFactoryImpl")
    }

    fun init() {
        Log.d(TAG, "POI initialized for Android")
    }
}