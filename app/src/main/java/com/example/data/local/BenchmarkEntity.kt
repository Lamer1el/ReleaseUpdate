package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmarks")
data class BenchmarkEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val category: String,
    val overallScore: Int,
    val singleCoreScore: Int,
    val multiCoreScore: Int,
    val durationMs: Long,
    val validationStatus: String,
    val syncStatus: String,
    val methodologyVersion: String,
    val deviceModel: String,
    val rawMetricsJson: String,
    val cloudDocId: String? = null
)
