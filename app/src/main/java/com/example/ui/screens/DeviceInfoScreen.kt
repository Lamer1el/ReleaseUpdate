package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BenchmarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    viewModel: BenchmarkViewModel,
    onBack: () -> Unit
) {
    val specs by viewModel.deviceSpecs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Информация об устройстве") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("device_info_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Section
            SpecsGroupCard(title = "Операционная система", icon = Icons.Default.PhoneAndroid) {
                SpecItem(label = "Модель", value = specs.model)
                SpecItem(label = "Производитель", value = specs.manufacturer)
                SpecItem(label = "Бренд", value = specs.brand)
                SpecItem(label = "Плата (Board)", value = specs.board)
                SpecItem(label = "Аппаратная платформа", value = specs.hardware)
                SpecItem(label = "Версия Android", value = specs.androidVersion)
                SpecItem(label = "API Level", value = "${specs.apiLevel}")
            }

            // CPU & Memory
            SpecsGroupCard(title = "Процессор и память", icon = Icons.Default.Memory) {
                SpecItem(label = "Количество ядер CPU", value = "${specs.cpuCores}")
                SpecItem(label = "Общая оперативная память", value = "${specs.totalRamMB} МБ")
                SpecItem(label = "Доступная память", value = "${specs.availableRamMB} МБ")
            }

            // Display
            SpecsGroupCard(title = "Дисплей", icon = Icons.Default.Tv) {
                SpecItem(label = "Разрешение", value = "${specs.displayWidth} x ${specs.displayHeight} px")
                SpecItem(label = "Текущая частота", value = "${specs.refreshRateHz} Гц")
                SpecItem(label = "Поддерживаемые режимы", value = specs.supportedRefreshRates.joinToString(", ") { "${it.toInt()} Гц" })
            }

            // Battery
            SpecsGroupCard(title = "Батарея", icon = Icons.Default.BatteryChargingFull) {
                SpecItem(label = "Уровень заряда", value = if (specs.batteryPercent >= 0) "${specs.batteryPercent}%" else "Недоступно")
                SpecItem(label = "Температура", value = "${specs.batteryTempCelsius} °C")
                SpecItem(label = "Статус зарядки", value = specs.batteryStatus)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpecsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
