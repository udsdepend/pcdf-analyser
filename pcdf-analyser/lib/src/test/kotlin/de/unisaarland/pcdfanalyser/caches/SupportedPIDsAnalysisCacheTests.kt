package de.unisaarland.pcdfanalyser.caches

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SupportedPIDsAnalysisCacheTests {

    val donation1 = ClassLoader.getSystemClassLoader().getResource("donation1.ppcdf")

    @Test
    fun testDonation1() {
        assertNotNull(donation1, "Donation resource must be available.")
        val file = File(donation1.toURI())
        println(file.absolutePath)

        val cacheFile = File("/tmp/pcdf-analyser-test-pid-cache.db")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }

        val pidCache = SupportedPIDsAnalysisCache(AnalysisCache.sharedDatabase(cacheFile))
        assertFalse("Analysis result must NOT be cached") { pidCache.hasAnalysisResultForFile(file) }
        val result = pidCache.analysisResultForFile(file, false)
        println(result)
        assertFalse("Analysis result must NOT be cached (2)") { pidCache.hasAnalysisResultForFile(file) }
        pidCache.analysisResultForFile(file, true)
        assertTrue("Analysis result MUST be cached") { pidCache.hasAnalysisResultForFile(file) }
        assertEquals(result, pidCache.cachedAnalysisResultForFile(file), "Computed and cached value must be equal.")
    }
}