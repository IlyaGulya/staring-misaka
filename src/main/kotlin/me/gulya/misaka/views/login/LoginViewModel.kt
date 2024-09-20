package me.gulya.misaka.views.login

import TelegramAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.springframework.stereotype.Service

@Service
class LoginViewModel(private val telegramAuthService: TelegramAuthService) {

    private val _uiState = MutableStateFlow(TelegramLoginUiState())
    val uiState: StateFlow<TelegramLoginUiState> = _uiState.asStateFlow()

    suspend fun sendAuthenticationCode(phoneNumber: String) {
        _uiState.update { it.copy(isLoading = true) }
        telegramAuthService.authWithPhoneNumber(phoneNumber)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.CodeRequested(phoneNumber),
                        isLoading = false
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.Error(e.message ?: "Failed to send authentication code"),
                        isLoading = false
                    )
                }
            }
    }

    suspend fun verifyAuthenticationCode(code: String) {
        _uiState.update { it.copy(isLoading = true) }
        telegramAuthService.sendAuthCode(code)
            .onSuccess {
                if (telegramAuthService.checkAuthStatus()) {
                    _uiState.update { it.copy(shouldRedirect = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            loginState = TelegramLoginState.PasswordRequired,
                            isLoading = false
                        )
                    }
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.Error(e.message ?: "Failed to verify authentication code"),
                        isLoading = false
                    )
                }
            }
    }

    suspend fun submitPassword(password: String) {
        _uiState.update { it.copy(isLoading = true) }
        telegramAuthService.sendPassword(password)
            .onSuccess {
                if (telegramAuthService.checkAuthStatus()) {
                    _uiState.update { it.copy(shouldRedirect = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            loginState = TelegramLoginState.Error("Authentication failed"),
                            isLoading = false
                        )
                    }
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.Error(e.message ?: "Failed to verify password"),
                        isLoading = false
                    )
                }
            }
    }

    suspend fun generateQRCode() {
        _uiState.update { it.copy(isLoading = true) }
        telegramAuthService.authWithQr()
            .onSuccess { qrCode ->
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.WaitingForQR,
                        qrCodeLink = qrCode.link,
                        isLoading = false
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        loginState = TelegramLoginState.Error(e.message ?: "Failed to generate QR code"),
                        isLoading = false
                    )
                }
            }
    }

    fun switchLoginMethod() {
        _uiState.update {
            it.copy(
                loginState = if (it.loginState is TelegramLoginState.WaitingForQR)
                    TelegramLoginState.Initial
                else
                    it.loginState
            )
        }
    }

    suspend fun checkAuthStatus() {
        if (telegramAuthService.checkAuthStatus()) {
            _uiState.update { it.copy(shouldRedirect = true) }
        }
    }
}

data class TelegramLoginUiState(
    val loginState: TelegramLoginState = TelegramLoginState.Initial,
    val isLoading: Boolean = false,
    val qrCodeLink: String? = null,
    val shouldRedirect: Boolean = false
)

sealed class TelegramLoginState {
    object Initial : TelegramLoginState()
    data class PhoneEntered(val phoneNumber: String) : TelegramLoginState()
    data class CodeRequested(val phoneNumber: String) : TelegramLoginState()
    object PasswordRequired : TelegramLoginState()
    object WaitingForQR : TelegramLoginState()
    data class Error(val message: String) : TelegramLoginState()
}