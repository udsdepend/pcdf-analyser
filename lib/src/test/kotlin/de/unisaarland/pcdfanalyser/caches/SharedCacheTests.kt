package de.unisaarland.pcdfanalyser.caches

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedCacheTests {

    val donation1 = ClassLoader.getSystemClassLoader().getResource("donation1.ppcdf")

    @Test
    fun testDonation1() {
        assertNotNull(donation1, "Donation resource must be available.")
        val file = File(donation1.toURI())
        println(file.absolutePath)

        val cacheFile = File("/tmp/pcdf-analyser-test-shared-cache.db")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }

        val db = AnalysisCache.sharedDatabase(cacheFile)

        val vinCache = VINAnalysisCache(db)
        assertFalse("Analysis result must NOT be cached (2)") { vinCache.hasAnalysisResultForFile(file) }
        val vinResult = vinCache.analysisResultForFile(file, true)
        assertTrue("Analysis result MUST be cached") { vinCache.hasAnalysisResultForFile(file) }
        assertEquals(vinResult, vinCache.cachedAnalysisResultForFile(file), "Computed and cached value must be equal.")

        val pidCache = SupportedPIDsAnalysisCache(db)
        assertFalse("Analysis result must NOT be cached (2)") { pidCache.hasAnalysisResultForFile(file) }
        val pidResult = pidCache.analysisResultForFile(file, true)
        assertTrue("Analysis result MUST be cached") { pidCache.hasAnalysisResultForFile(file) }
        assertEquals(pidResult, pidCache.cachedAnalysisResultForFile(file), "Computed and cached value must be equal.")
    }

    @Test
    fun testReopenCache() {
        val file = File(donation1.toURI())
        val cacheFile = File("/tmp/pcdf-analyser-test-shared-cache.db")
        assertTrue("Cache file must already exist (execute all tests in this suite)") { cacheFile.exists() }

        val db = AnalysisCache.sharedDatabase(cacheFile)

        val vinCache = VINAnalysisCache(db)
        assertEquals("WVGZZZ5NZGWZZZZZZ", vinCache.cachedAnalysisResultForFile(file), "Previously computed and cached value must be equal.")

        val pidCache = SupportedPIDsAnalysisCache(db)
        assertTrue(pidCache.cachedAnalysisResultForFile(file)?.parameterRecords?.isNotEmpty() ?: false, "Cached value must be available.")
    }

}