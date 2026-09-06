package com.rrp.pizzadough.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrp.pizzadough.DoughUiState
import com.rrp.pizzadough.calculator.FieldError
import com.rrp.pizzadough.calculator.Grams
import com.rrp.pizzadough.calculator.IngredientLine
import com.rrp.pizzadough.calculator.Recipe
import com.rrp.pizzadough.calculator.YeastType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoughScreen(
    state: DoughUiState,
    onUpdate: ((Recipe) -> Recipe) -> Unit,
    onReset: () -> Unit,
) {
    val recipe = state.recipe
    val errors = state.errors
    val ballCountError = errors.message("ballCount")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dough") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(ballCount = (it.ballCount - 1).coerceAtLeast(1)) } },
                ) { Text("−") }
                Text(
                    text = recipe.ballCount.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(ballCount = it.ballCount + 1) } },
                ) { Text("+") }
            }
            if (ballCountError != null) {
                Text(
                    text = ballCountError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DecimalField(
                value = recipe.ballWeightGrams,
                onValueChange = { value -> onUpdate { it.copy(ballWeightGrams = value) } },
                label = "Ball weight (g)",
                field = "ballWeightGrams",
                errors = errors,
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                onUpdate { it.copy(recipeExpanded = !it.recipeExpanded) }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Recipe",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(if (recipe.recipeExpanded) "▲" else "▼")
                    }
                    if (recipe.recipeExpanded) {
                        DecimalField(
                            value = recipe.hydrationPercent,
                            onValueChange = { value -> onUpdate { it.copy(hydrationPercent = value) } },
                            label = "Hydration (%)",
                            field = "hydrationPercent",
                            errors = errors,
                        )
                        DecimalField(
                            value = recipe.saltPercent,
                            onValueChange = { value -> onUpdate { it.copy(saltPercent = value) } },
                            label = "Salt (%)",
                            field = "saltPercent",
                            errors = errors,
                        )
                        Text("Yeast type", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            YeastType.entries.forEachIndexed { index, type ->
                                SegmentedButton(
                                    selected = recipe.yeastType == type,
                                    onClick = { onUpdate { it.copy(yeastType = type) } },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index,
                                        YeastType.entries.size,
                                    ),
                                    label = { Text(type.label) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Preferment",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(
                                checked = recipe.usePreferment,
                                onCheckedChange = { checked ->
                                    onUpdate { it.copy(usePreferment = checked) }
                                },
                            )
                        }
                        if (recipe.usePreferment) {
                            DecimalField(
                                value = recipe.prefermentFlourPercent,
                                onValueChange = { value ->
                                    onUpdate { it.copy(prefermentFlourPercent = value) }
                                },
                                label = "Biga flour (%)",
                                field = "prefermentFlourPercent",
                                errors = errors,
                            )
                            DecimalField(
                                value = recipe.prefermentHydrationPercent,
                                onValueChange = { value ->
                                    onUpdate { it.copy(prefermentHydrationPercent = value) }
                                },
                                label = "Biga hydration (%)",
                                field = "prefermentHydrationPercent",
                                errors = errors,
                            )
                            DecimalField(
                                value = recipe.prefermentHours,
                                onValueChange = { value ->
                                    onUpdate { it.copy(prefermentHours = value) }
                                },
                                label = "Biga hours",
                                field = "prefermentHours",
                                errors = errors,
                            )
                            DecimalField(
                                value = recipe.prefermentTempC,
                                onValueChange = { value ->
                                    onUpdate { it.copy(prefermentTempC = value) }
                                },
                                label = "Biga (°C)",
                                field = "prefermentTempC",
                                errors = errors,
                            )
                        }
                        DecimalField(
                            value = recipe.finalHours,
                            onValueChange = { value -> onUpdate { it.copy(finalHours = value) } },
                            label = "Final hours",
                            field = "finalHours",
                            errors = errors,
                        )
                        DecimalField(
                            value = recipe.finalTempC,
                            onValueChange = { value -> onUpdate { it.copy(finalTempC = value) } },
                            label = "Final (°C)",
                            field = "finalTempC",
                            errors = errors,
                        )
                        if (recipe.usePreferment) {
                            YeastPercentField(
                                value = recipe.prefermentYeastOverridePercent,
                                autoPercent = state.result.autoPrefermentYeastPercent,
                                onValueChange = { value ->
                                    onUpdate { it.copy(prefermentYeastOverridePercent = value) }
                                },
                                label = "Biga yeast (%)",
                                field = "prefermentYeastOverridePercent",
                                errors = errors,
                            )
                        }
                        YeastPercentField(
                            value = recipe.yeastOverridePercent,
                            autoPercent = state.result.autoYeastPercent,
                            onValueChange = { value ->
                                onUpdate { it.copy(yeastOverridePercent = value) }
                            },
                            label = "Yeast (%)",
                            field = "yeastOverridePercent",
                            errors = errors,
                        )
                        TextButton(onClick = onReset) { Text("Reset defaults") }
                    } else {
                        Text(
                            text = state.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (recipe.usePreferment) {
                IngredientSection(
                    title = "Biga",
                    lines = state.result.preferment,
                    quiet = false,
                    dashYeast = false,
                )
            }
            IngredientSection(
                title = "Final dough",
                lines = state.result.finalDough,
                quiet = false,
                dashYeast = recipe.usePreferment && state.result.extraYeastGrams == 0.0,
            )
            IngredientSection(
                title = "Totals",
                lines = state.result.totals,
                quiet = true,
                dashYeast = false,
            )
        }
    }
}

@Composable
private fun IngredientSection(
    title: String,
    lines: List<IngredientLine>,
    quiet: Boolean,
    dashYeast: Boolean,
) {
    val titleStyle = if (quiet) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.titleMedium
    }
    val lineStyle = if (quiet) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val color = if (quiet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = titleStyle, color = color)
        lines.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = line.name,
                    modifier = Modifier.weight(1f),
                    style = lineStyle,
                    color = color,
                )
                Text(
                    text = formatLineGrams(line, dashYeast),
                    style = lineStyle,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun DecimalField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    field: String,
    errors: List<FieldError>,
    modifier: Modifier = Modifier,
) {
    val error = errors.message(field)
    var text by remember { mutableStateOf(formatNumber(value)) }
    LaunchedEffect(value) {
        if (!textRepresents(text, value)) {
            text = formatNumber(value)
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            onValueChange(new.toDoubleOrNull() ?: 0.0)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun YeastPercentField(
    value: Double?,
    autoPercent: Double,
    onValueChange: (Double?) -> Unit,
    label: String,
    field: String,
    errors: List<FieldError>,
) {
    val error = errors.message(field)
    var text by remember { mutableStateOf(value?.let { formatNumber(it) } ?: "") }
    LaunchedEffect(value) {
        if (!textRepresentsOverride(text, value)) {
            text = value?.let { formatNumber(it) } ?: ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            onValueChange(
                if (new.isBlank()) null else new.toDoubleOrNull() ?: 0.0,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(formatNumber(autoPercent)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

private val YeastType.label: String
    get() = when (this) {
        YeastType.IDY -> "IDY"
        YeastType.ADY -> "ADY"
        YeastType.FRESH -> "Fresh"
    }

private fun List<FieldError>.message(field: String): String? =
    firstOrNull { it.field == field }?.message

private fun formatNumber(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}

private fun textRepresents(text: String, value: Double): Boolean {
    val parsed = text.toDoubleOrNull()
    return (parsed != null && parsed == value) || (parsed == null && value == 0.0)
}

private fun textRepresentsOverride(text: String, value: Double?): Boolean =
    if (value == null) text.isBlank() else textRepresents(text, value)

private fun formatLineGrams(line: IngredientLine, dashYeast: Boolean): String {
    if (dashYeast && line.isYeast) return "—"
    return if (line.isYeast) {
        String.format(Locale.US, "%.2f g", Grams.yeast(line.grams))
    } else {
        String.format(Locale.US, "%.0f g", Grams.mass(line.grams))
    }
}
