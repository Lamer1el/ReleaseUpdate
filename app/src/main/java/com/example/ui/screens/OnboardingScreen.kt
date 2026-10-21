package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SkitefiIcon

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val details: List<String>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                title = "Добро пожаловать в Skitefi",
                subtitle = "Профессиональный инструмент аппаратного тестирования устройства",
                iconRes = R.drawable.ic_benchmark_all,
                details = listOf(
                    "Абсолютно автономное измерение производительности без обязательного интернета",
                    "Раздельное сохранение сырых физических метрик и нормализованных баллов",
                    "Версионированная методика оценки оборудования v1.0"
                )
            ),
            OnboardingStep(
                title = "Выбери тест",
                subtitle = "Специализированные испытания ключевых компонентов системы",
                iconRes = R.drawable.ic_benchmark_cpu,
                details = listOf(
                    "CPU: Однопоточная и многопоточная нагрузка (целые числа и вещественная арифметика)",
                    "GPU: Рендеринг динамических сцен и анализ задержек кадров (FPS и Frame Time)",
                    "RAM & Storage: Пропускная способность памяти и IOPS дисковой подсистемы",
                    "Stability & Battery: Испытание на троттлинг и мониторинг энергопотребления"
                )
            ),
            OnboardingStep(
                title = "Изучай результаты",
                subtitle = "Локальная база данных истории с поддержкой глубокого анализа",
                iconRes = R.drawable.ic_benchmark_stability,
                details = listOf(
                    "Детальная карточка каждого запуска с сырыми показателями",
                    "Сравнение двух любых запусков с процентной разницей метрик",
                    "Экспорт отчетов в открытые форматы JSON и CSV",
                    "Полная конфиденциальность: данные хранятся локально в Room"
                )
            ),
            OnboardingStep(
                title = "Конфиденциальность и экспорт",
                subtitle = "100% автономная работа без регистрации и отправки данных",
                iconRes = R.drawable.ic_benchmark_ram,
                details = listOf(
                    "Никаких онлайн-аккаунтов, паролей и сбора персональных данных",
                    "Вся база измерений хранится строго локально на вашем устройстве",
                    "Удобный экспорт отчетов в открытые форматы JSON и CSV",
                    "Полный контроль над своими аппаратными замерами"
                )
            ),
            OnboardingStep(
                title = "Обновления и уведомления",
                subtitle = "Прямой встроенный Patcher через GitHub Releases",
                iconRes = R.drawable.ic_update_cloud,
                details = listOf(
                    "Проверка официальных релизов репозитория Lamer1el/ReleaseUpdate",
                    "Проверка сигнатур, package name и хеша SHA-256 перед установкой",
                    "Фоновые проверки WorkManager с учетом ограничений Doze и батареи",
                    "Настраиваемые уведомления для Android 13+ без спама"
                )
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]
    val totalSteps = steps.size
    val isLast = currentStepIndex == totalSteps - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back & Skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStepIndex > 0) {
                IconButton(
                    onClick = { currentStepIndex-- },
                    modifier = Modifier.testTag("onboarding_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = "${currentStepIndex + 1} из $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onFinish,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text("Пропустить")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentStepIndex + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Content Area
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "step_content"
        ) { currentStep ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    SkitefiIcon(
                        drawableId = currentStep.iconRes,
                        contentDescription = currentStep.title,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentStep.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentStep.details.forEach { detail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 12.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom action button
        if (isLast) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_finish_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Перейти в приложение",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        } else {
            Button(
                onClick = { currentStepIndex++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = "Далее",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
