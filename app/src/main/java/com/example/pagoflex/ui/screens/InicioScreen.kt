package com.example.pagoflex.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.viewmodel.InicioViewModel

@Composable
fun InicioScreen(
    viewModel: InicioViewModel = viewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}