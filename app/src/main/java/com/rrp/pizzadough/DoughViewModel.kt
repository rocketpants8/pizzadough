package com.rrp.pizzadough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrp.pizzadough.calculator.Defaults
import com.rrp.pizzadough.calculator.DoughCalculator
import com.rrp.pizzadough.calculator.DoughResult
import com.rrp.pizzadough.calculator.FieldError
import com.rrp.pizzadough.calculator.Recipe
import com.rrp.pizzadough.calculator.RecipeSummary
import com.rrp.pizzadough.calculator.RecipeValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DoughUiState(
    val recipe: Recipe,
    val result: DoughResult,
    val errors: List<FieldError>,
    val summary: String,
)

class DoughViewModel(private val store: RecipeStore) : ViewModel() {
    private val _uiState = MutableStateFlow(Defaults.recipe.toState())
    val uiState: StateFlow<DoughUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = store.recipes.first().toState()
        }
    }

    fun update(transform: (Recipe) -> Recipe) {
        val next = transform(_uiState.value.recipe)
        val errors = RecipeValidator.validate(next)
        if (errors.isEmpty()) {
            _uiState.value = next.toState()
            viewModelScope.launch { store.save(next) }
        } else {
            _uiState.value = _uiState.value.copy(recipe = next, errors = errors)
        }
    }

    fun resetDefaults() {
        update { Defaults.recipe }
    }

    private fun Recipe.toState() = DoughUiState(
        recipe = this,
        result = DoughCalculator.calculate(this),
        errors = emptyList(),
        summary = RecipeSummary.format(this),
    )

    companion object {
        fun factory(store: RecipeStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DoughViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return DoughViewModel(store) as T
                }
            }
    }
}
