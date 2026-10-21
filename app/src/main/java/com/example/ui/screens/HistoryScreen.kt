package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.benchmark.BenchmarkCategory
import com.example.benchmark.SyncStatus
import com.example.data.local.BenchmarkEntity
import com.example.ui.BenchmarkViewModel
import com.example.ui.components.IconAnimationStyle
import com.example.ui.components.SkitefiIcon
import com.example.ui.components.getBenchmarkCategoryIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: BenchmarkViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedDetailEntity by remember { mutableStateOf<BenchmarkEntity?>(null) }

    // Selection for comparison
    var selectedForCompare by remember { mutableStateOf<List<BenchmarkEntity>>(emptyList()) }
    var showCompareDialog by remember { mutableStateOf(false) }

    // STX Import modal state
    var showImportDialog by remember { mutableStateOf(false) }
    var importTextValue by remember { mutableStateOf("") }

    // SAF File Picker for .stx
    val stxFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    viewModel.importStxData(content) { success, msg ->
                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения STX файла: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val filteredHistory = history.filter { entity ->
        val matchesCategory = selectedCategoryFilter == "ALL" || entity.category == selectedCategoryFilter
        val matchesSearch = searchQuery.isBlank() ||
                entity.deviceModel.contains(searchQuery, ignoreCase = true) ||
                entity.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("history_screen")
    ) {
        // Top action bar: Search + Import STX button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                placeholder = { Text("Поиск тестов...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = { showImportDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("history_import_stx_button")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(".STX")
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("ALL", "OVERALL", "CPU", "AI_NEURAL", "CRYPTO", "RAYTRACING", "GPU", "RAM", "STORAGE", "STABILITY")
            items(categories) { cat ->
                val label = if (cat == "ALL") "Все" else cat
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(label) }
                )
            }
        }

        // Comparison selection banner
        if (selectedForCompare.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выбрано для сравнения: ${selectedForCompare.size}/2",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row {
                        if (selectedForCompare.size == 2) {
                            Button(
                                onClick = { showCompareDialog = true },
                                modifier = Modifier.testTag("compare_selected_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Сравнить")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { selectedForCompare = emptyList() }) {
                            Text("Сброс")
                        }
                    }
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "История тестов пуста",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Запустите тесты или импортируйте отчёт в формате .STX",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHistory, key = { it.id }) { item ->
                    val isSelected = selectedForCompare.any { it.id == item.id }

                    HistoryRunCard(
                        entity = item,
                        isSelected = isSelected,
                        onClick = { selectedDetailEntity = item },
                        onShare = {
                            val shareIntent = viewModel.createShareIntent(context, item)
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться тестом Skitefi"))
                        },
                        onToggleCompare = {
                            selectedForCompare = if (isSelected) {
                                selectedForCompare.filterNot { it.id == item.id }
                            } else {
                                if (selectedForCompare.size < 2) {
                                    selectedForCompare + item
                                } else {
                                    listOf(selectedForCompare.last(), item)
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Detail Dialog
    selectedDetailEntity?.let { entity ->
        RunDetailDialog(
            entity = entity,
            onDismiss = { selectedDetailEntity = null },
            onDelete = {
                viewModel.deleteBenchmark(entity.id)
                selectedDetailEntity = null
            },
            onShare = {
                val shareIntent = viewModel.createShareIntent(context, entity)
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться отчётом Skitefi"))
            },
            onExportStx = {
                val stxString = viewModel.exportToStxString(entity)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Skitefi STX Report", stxString))
                Toast.makeText(context, "STX отчёт скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
            },
            onExportJson = {
                val json = viewModel.exportToJson(entity)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Skitefi Benchmark JSON", json))
                Toast.makeText(context, "JSON скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Compare Dialog (Side-by-side)
    if (showCompareDialog && selectedForCompare.size == 2) {
        CompareDialog(
            first = selectedForCompare[0],
            second = selectedForCompare[1],
            onDismiss = { showCompareDialog = false }
        )
    }

    // Import STX Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Импорт отчёта (.STX)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Выберите файл отчёта .stx на устройстве или вставьте текстовый код:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            showImportDialog = false
                            stxFilePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выбрать файл .stx")
                    }

                    OutlinedTextField(
                        value = importTextValue,
                        onValueChange = { importTextValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("STX1_...") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importTextValue.isNotBlank()) {
                            viewModel.importStxData(importTextValue) { success, msg ->
                                Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                if (success) {
                                    showImportDialog = false
                                    importTextValue = ""
                                }
                            }
                        }
                    }
                ) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun HistoryRunCard(
    entity: BenchmarkEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onToggleCompare: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(entity.timestamp))
    val isStxImported = entity.syncStatus == SyncStatus.STX_IMPORTED.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${entity.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else if (isStxImported) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.category,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isStxImported) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "[${entity.validationStatus}]",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${entity.deviceModel} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entity.overallScore}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "баллов",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onToggleCompare) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Сравнить",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RunDetailDialog(
    entity: BenchmarkEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExportStx: () -> Unit,
    onExportJson: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${entity.category} — ${entity.overallScore} б.",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = entity.deviceModel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Методика", value = entity.methodologyVersion)
                DetailRow(label = "Длительность", value = "${entity.durationMs / 1000.0} с")
                DetailRow(label = "Статус проверки", value = entity.validationStatus)
                DetailRow(label = "Тип отчёта", value = if (entity.syncStatus == SyncStatus.STX_IMPORTED.name) "Импортирован (.STX)" else "Локальный тест")

                if (entity.singleCoreScore > 0) {
                    DetailRow(label = "CPU Single-core", value = "${entity.singleCoreScore}")
                }
                if (entity.multiCoreScore > 0) {
                    DetailRow(label = "CPU Multi-core", value = "${entity.multiCoreScore}")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поделиться отчётом")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportStx,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Копировать STX")
                    }
                    OutlinedButton(
                        onClick = onExportJson,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("JSON")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить")
            }
        }
    )
}

@Composable
private fun CompareDialog(
    first: BenchmarkEntity,
    second: BenchmarkEntity,
    onDismiss: () -> Unit
) {
    val diff = second.overallScore - first.overallScore
    val percentDiff = if (first.overallScore > 0) (diff.toDouble() / first.overallScore) * 100.0 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сравнение отчётов", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Устройство 1", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${first.overallScore} б.",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = first.deviceModel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2
                        )
                        Text(text = first.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = "Устройство 2", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${second.overallScore} б.",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = second.deviceModel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2
                        )
                        Text(text = second.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()

                // Comparative Delta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Разница производительности:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text(
                        text = "${if (diff >= 0) "+" else ""}$diff (${"%.1f".format(percentDiff)}%)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                val maxScore = maxOf(first.overallScore, second.overallScore, 1)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Устройство 1 (${first.overallScore})", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(
                        progress = { first.overallScore.toFloat() / maxScore },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Устройство 2 (${second.overallScore})", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(
                        progress = { second.overallScore.toFloat() / maxScore },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}
