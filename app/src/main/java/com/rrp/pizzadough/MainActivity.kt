package com.rrp.pizzadough

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrp.pizzadough.ui.DoughScreen
import com.rrp.pizzadough.ui.theme.PizzaDoughTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = RecipeStore(applicationContext)
        setContent {
            PizzaDoughTheme {
                val vm: DoughViewModel = viewModel(factory = DoughViewModel.factory(store))
                val state by vm.uiState.collectAsStateWithLifecycle()
                DoughScreen(state = state, onUpdate = vm::update, onReset = vm::resetDefaults)
            }
        }
    }
}
