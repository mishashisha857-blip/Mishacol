package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLevel
import com.example.data.model.Gender
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.FatColor
import com.example.ui.theme.ProteinColor
import com.example.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()

    var currentWeight by remember(profile) { mutableStateOf(profile.currentWeightKg.toString()) }
    var targetWeight by remember(profile) { mutableStateOf(profile.targetWeightKg.toString()) }
    var height by remember(profile) { mutableStateOf(profile.heightCm.toInt().toString()) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var selectedGender by remember(profile) {
        mutableStateOf(try { Gender.valueOf(profile.gender) } catch (e: Exception) { Gender.MALE })
    }
    var selectedActivity by remember(profile) {
        mutableStateOf(try { ActivityLevel.valueOf(profile.activityLevel) } catch (e: Exception) { ActivityLevel.LIGHT })
    }
    var deficitTarget by remember(profile) { mutableIntStateOf(profile.deficitTargetKcal) }
    var stepGoal by remember(profile) { mutableIntStateOf(profile.dailyStepGoal) }

    var showSavedSnackbar by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Параметры и Цели Похудения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Расчет метаболизма, суточной нормы калорий и дефицита для безопасного сброса веса",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Calculated Metabolism Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ваш индивидуальный расчет нормы:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${profile.targetDailyCalories} ккал",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Целевая норма в день",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${profile.bmr.toInt()} ккал (BMR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Базовый расход (TDEE: ${profile.tdee.toInt()} ккал)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Целевой баланс БЖУ в день:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Белки: ${"%.0f".format(profile.targetProteinGrams)}г",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProteinColor
                        )
                        Text(
                            text = "Жиры: ${"%.0f".format(profile.targetFatGrams)}г",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FatColor
                        )
                        Text(
                            text = "Углеводы: ${"%.0f".format(profile.targetCarbsGrams)}г",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarbsColor
                        )
                    }
                }
            }
        }

        // Editable Parameters Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Физические данные",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Weights Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentWeight,
                            onValueChange = { currentWeight = it },
                            label = { Text("Текущий вес (кг)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_current_weight_input")
                        )
                        OutlinedTextField(
                            value = targetWeight,
                            onValueChange = { targetWeight = it },
                            label = { Text("Целевой вес (кг)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_target_weight_input")
                        )
                    }

                    // Height and Age Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Рост (см)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Возраст (лет)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Gender Selection
                    Text(
                        text = "Пол:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Gender.values().forEach { gender ->
                            FilterChip(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                label = { Text(gender.titleRu) }
                            )
                        }
                    }

                    // Activity Level Selection
                    Text(
                        text = "Уровень физической активности:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActivityLevel.values().forEach { level ->
                            FilterChip(
                                selected = selectedActivity == level,
                                onClick = { selectedActivity = level },
                                label = { Text(level.titleRu, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Deficit Target Selection
                    Text(
                        text = "Желаемый дефицит калорий в день:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(300, 400, 500, 600).forEach { def ->
                            FilterChip(
                                selected = deficitTarget == def,
                                onClick = { deficitTarget = def },
                                label = { Text("-$def", fontSize = 12.sp) }
                            )
                        }
                    }

                    // Daily Step Goal
                    Text(
                        text = "Цель шагов в день:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(6000, 8000, 10000, 12000).forEach { steps ->
                            FilterChip(
                                selected = stepGoal == steps,
                                onClick = { stepGoal = steps },
                                label = { Text("${steps / 1000}k", fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val cWeight = currentWeight.toFloatOrNull() ?: 75f
                            val tWeight = targetWeight.toFloatOrNull() ?: 70f
                            val hCm = height.toFloatOrNull() ?: 175f
                            val aInt = age.toIntOrNull() ?: 28
                            viewModel.updateProfile(
                                currentWeightKg = cWeight,
                                targetWeightKg = tWeight,
                                heightCm = hCm,
                                age = aInt,
                                gender = selectedGender,
                                activityLevel = selectedActivity,
                                deficitTargetKcal = deficitTarget,
                                dailyStepGoal = stepGoal
                            )
                            showSavedSnackbar = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить параметры", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
