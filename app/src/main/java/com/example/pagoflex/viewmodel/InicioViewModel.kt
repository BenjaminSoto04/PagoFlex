package com.example.pagoflex.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pagoflex.model.Compromiso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InicioViewModel : ViewModel() {

    private val inicioCompromisos = Compromiso(
        folio = "12345"
    )
}