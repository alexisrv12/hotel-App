package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.HotelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el estado de entrada, validaciones y proceso de autenticación en LoginScreen.
 */
class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _emailError.value = null
        _loginError.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _passwordError.value = null
        _loginError.value = null
    }

    /**
     * Valida formato de correo y complejidad mínima de contraseña antes de invocar la autenticación.
     */
    fun validateInputs(): Boolean {
        var isValid = true
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        if (currentEmail.isBlank()) {
            _emailError.value = "El correo electrónico no puede estar vacío."
            isValid = false
        } else if (!currentEmail.contains("@") && !currentEmail.contains("gerente") && !currentEmail.contains("recepcion") && !currentEmail.contains("admin")) {
            _emailError.value = "Ingrese un formato de correo electrónico válido (ej. usuario@hotel.com)."
            isValid = false
        } else {
            _emailError.value = null
        }

        if (currentPassword.isBlank()) {
            _passwordError.value = "La contraseña no puede estar vacía."
            isValid = false
        } else if (currentPassword.length < 4) {
            _passwordError.value = "La contraseña debe tener al menos 4 caracteres."
            isValid = false
        } else {
            _passwordError.value = null
        }

        return isValid
    }

    /**
     * Ejecuta la lógica de autenticación e identifica el rol (Gerente o Recepción)
     */
    fun performLogin(hotelViewModel: HotelViewModel, onLoginSuccess: () -> Unit = {}) {
        if (!validateInputs()) return

        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            delay(500) // Circular progress indicator visual feedback delay

            val success = hotelViewModel.loginUser(_email.value, _password.value)
            _isLoading.value = false

            if (success) {
                onLoginSuccess()
            } else {
                _loginError.value = "Correo electrónico o contraseña incorrectos."
            }
        }
    }
}
