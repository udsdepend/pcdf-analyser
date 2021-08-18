package de.unisaarland.pcdfanalyser.caches

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VINAnalyserCacheTests {

    val donation1 = ClassLoader.getSystemClassLoader().getResource("donation1.ppcdf")

    @Test
    fun testDonation1() {
        assertNotNull(donation1, "Donation resource must be available.")
        val file = File(donation1.toURI())
        println(file.absolutePath)

        val cacheFile = File("/tmp/pcdf-analyser-test-vin-cache.db")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }

        val vinCache = VINAnalysisCache(AnalysisCache.sharedDatabase(cacheFile))
        assertFalse("Analysis result must NOT be cached") { vinCache.hasAnalysisResultForFile(file) }
        val result = vinCache.analysisResultForFile(file, false)
        assertFalse("Analysis result must NOT be cached (2)") { vinCache.hasAnalysisResultForFile(file) }
        vinCache.analysisResultForFile(file, true)
        assertTrue("Analysis result MUST be cached") { vinCache.hasAnalysisResultForFile(file) }
        assertEquals(result, vinCache.cachedAnalysisResultForFile(file), "Computed and cached value must be equal.")
    }

}