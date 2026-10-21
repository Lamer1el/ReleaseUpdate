package com.example

import com.example.benchmark.BenchmarkCategory
import com.example.benchmark.BenchmarkResultData
import com.example.benchmark.CpuMetrics
import com.example.benchmark.ScoringEngine
import com.example.benchmark.StxFormatManager
import com.example.benchmark.ValidationStatus
import com.example.data.local.BenchmarkEntity
import com.example.updater.VersionTriple
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkitefiUnitTest {

    @Test
    fun testVersionTripleParsingAndComparison() {
        val v1 = VersionTriple.parse("v1.0.0")
        val v2 = VersionTriple.parse("1.2.0")
        val v3 = VersionTriple.parse("v1.2.1")
        val v4 = VersionTriple.parse("2.0.0")

        assertNotNull(v1)
        assertNotNull(v2)
        assertNotNull(v3)
        assertNotNull(v4)

        assertTrue(v2!! > v1!!)
        assertTrue(v3!! > v2)
        assertTrue(v4!! > v3)
        assertEquals(0, VersionTriple.parse("v1.5.2")!!.compareTo(VersionTriple.parse("1.5.2")!!))

        assertNull(VersionTriple.parse("invalid_tag"))
    }

    @Test
    fun testCpuScoringBaseline() {
        // 2,500,000 ops/s baseline should yield ~1000 points
        val (single, multi) = ScoringEngine.calculateCpuScore(2_500_000.0, 12_000_000.0)
        assertEquals(1000, single)
        assertEquals(1000, multi)
    }

    @Test
    fun testGpuScoringPenalty() {
        val scoreClean = ScoringEngine.calculateGpuScore(60.0, 0.0)
        val scoreSlow = ScoringEngine.calculateGpuScore(60.0, 20.0)

        assertEquals(1000, scoreClean)
        assertEquals(800, scoreSlow)
    }

    @Test
    fun testRamScoring() {
        val ramScore = ScoringEngine.calculateRamScore(5000.0, 5000.0, 5000.0)
        assertEquals(1000, ramScore)
    }

    @Test
    fun testAiScoring() {
        val aiScore = ScoringEngine.calculateAiScore(4.0, 25.0)
        assertEquals(1000, aiScore)
    }

    @Test
    fun testCryptoScoring() {
        val cryptoScore = ScoringEngine.calculateCryptoScore(180.0, 180.0)
        assertEquals(1000, cryptoScore)
    }

    @Test
    fun testOverallCompositeScore() {
        val overall = ScoringEngine.calculateOverall(1000, 1000, 1000, 1000, 1000, 1000, 1000)
        assertEquals(1000, overall)
    }

    @Test
    fun testStxExportAndImportIntegrity() {
        val entity = BenchmarkEntity(
            id = "test-uuid-12345",
            timestamp = 1700000000000L,
            category = "AI_NEURAL",
            overallScore = 2450,
            singleCoreScore = 0,
            multiCoreScore = 0,
            durationMs = 2800L,
            validationStatus = ValidationStatus.VALID.name,
            syncStatus = "LOCAL",
            methodologyVersion = ScoringEngine.METHODOLOGY_VERSION,
            deviceModel = "TestPhone Pro",
            rawMetricsJson = "{}"
        )

        val stxToken = StxFormatManager.exportToStxString(entity, "lamer42")
        assertTrue(stxToken.startsWith("STX1_"))

        val importResult = StxFormatManager.importFromStxString(stxToken)
        assertTrue(importResult.isSuccess)

        val imported = importResult.getOrThrow()
        assertEquals(entity.id, imported.id)
        assertEquals(entity.overallScore, imported.overallScore)
        assertEquals("AI_NEURAL", imported.category)
        assertEquals(ValidationStatus.VALID.name, imported.validationStatus)
        assertTrue(imported.deviceModel.contains("lamer42"))
    }

    @Test
    fun testResultSerialization() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(BenchmarkResultData::class.java)

        val sample = BenchmarkResultData(
            category = BenchmarkCategory.CPU,
            overallScore = 1250,
            durationMs = 4500L,
            cpuMetrics = CpuMetrics(
                singleCoreScore = 1200,
                multiCoreScore = 1300,
                singleCoreOpsPerSec = 3000000.0,
                multiCoreOpsPerSec = 15000000.0,
                integerOpsPerSec = 3200000.0,
                floatingPointOpsPerSec = 2800000.0,
                threadsUsed = 8,
                durationMs = 4500L
            ),
            validationStatus = ValidationStatus.VALID,
            deviceModel = "TestModel",
            deviceManufacturer = "TestManufacturer",
            androidVersion = "14"
        )

        val json = adapter.toJson(sample)
        assertTrue(json.contains("\"overallScore\":1250"))
        assertTrue(json.contains("\"CPU\""))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(1250, deserialized!!.overallScore)
        assertEquals(8, deserialized.cpuMetrics?.threadsUsed)
    }
}
