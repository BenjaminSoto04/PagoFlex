package com.example.pagoflex.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.pagoflex.viewmodel.InicioViewModel

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    val InicioViewModel: InicioViewModel = viewModel()

}