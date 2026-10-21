package com.example.benchmark

enum class BenchmarkCategory(val displayName: String, val description: String) {
    OVERALL("Комплексный", "Полное тестирование системы: CPU, GPU, RAM, Storage, AI, Crypto"),
    CPU("Процессор (CPU)", "Целочисленные и вещественные вычисления, Single/Multi-core"),
    GPU("Графика (GPU)", "Canvas/Render шейдерные стресс-сцены, 2D/3D матрицы, FPS"),
    RAM("Память (RAM)", "Пропускная способность прямого буфера и задержки доступа"),
    STORAGE("Накопитель", "Последовательное и случайное чтение/запись 4KB блоками"),
    AI_NEURAL("ИИ и Нейросети", "Тензорные матрицы GEMM, NPU симуляция, GFLOPS и задержка"),
    CRYPTO("Криптография", "Аппаратное хеширование SHA-256 и блочное шифрование AES"),
    RAYTRACING("Ray Tracing", "Трассировка лучей, векторные отражения и физика освещения"),
    STABILITY("Стабильность", "Длительный стресс-тест на устойчивость и троттлинг"),
    BATTERY("Батарея", "Мониторинг разряда, температуры и тока под нагрузкой"),
    DISPLAY("Дисплей", "Частота обновления, цветопередача, плотность и режимы")
}

enum class ValidationStatus(val title: String) {
    VALID("Проверен"),
    ANOMALOUS("Аномальные данные"),
    INCOMPLETE("Прерван")
}

enum class SyncStatus(val title: String) {
    LOCAL("Локально"),
    STX_IMPORTED("Импорт STX")
}

