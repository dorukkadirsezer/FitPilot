package com.doruk.fitprogress.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class EncodingValidationTest {

    @Test
    fun testNoMojibakeInStringsXml() {
        val mojibakeSequences = listOf("Ãƒ", "Ã„", "Ã…", "Ã‚")
        
        val filesToCheck = listOf(
            "src/main/res/values-tr/strings.xml",
            "src/main/res/values/strings.xml"
        )
        
        for (resourcesPath in filesToCheck) {
            val file = File(resourcesPath)
            if (file.exists()) {
                val content = file.readText(Charsets.UTF_8)
                for (sequence in mojibakeSequences) {
                    assertFalse("Found mojibake sequence '\$sequence' in \$resourcesPath", content.contains(sequence))
                }
            }
        }
    }

    @Test
    fun testTurkishStringsAreCorrectlyEncoded() {
        val resourcesPath = "src/main/res/values-tr/strings.xml"
        val file = File(resourcesPath)
        if (file.exists()) {
            val content = file.readText(Charsets.UTF_8)
            val positiveCases = listOf(
                "Akıllı Ayarlar",
                "Türkçe",
                "İngilizce",
                "Antrenmanı Başlat",
                "Antrenman Önizlemesi",
                "Uygulama Teması",
                "Açık",
                "Koyu" // Dark theme
            )
            for (case in positiveCases) {
                assertTrue("Missing positive string '\$case' in \$resourcesPath", content.contains(case))
            }
        }
    }

    @Test
    fun testResourceKeyParity() {
        val trPath = "src/main/res/values-tr/strings.xml"
        val enPath = "src/main/res/values/strings.xml"
        
        val trFile = File(trPath)
        val enFile = File(enPath)
        
        if (trFile.exists() && enFile.exists()) {
            val trKeys = extractKeys(trFile)
            val enKeys = extractKeys(enFile)
            
            val missingKeys = enKeys.subtract(trKeys)
            assertTrue("Missing keys in Turkish strings: $missingKeys", missingKeys.isEmpty())
        }
    }

    private fun extractKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("string")
        for (temp in 0 until nList.length) {
            val nNode = nList.item(temp)
            if (nNode.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val eElement = nNode as org.w3c.dom.Element
                keys.add(eElement.getAttribute("name"))
            }
        }
        return keys
    }
}

