import dev.voroby.springframework.telegram.client.TdApi
import dev.voroby.springframework.telegram.client.TelegramClient
import dev.voroby.springframework.telegram.client.updates.UpdateNotificationListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class TelegramAuthService(
    private val telegramClient: TelegramClient
) : UpdateNotificationListener<TdApi.UpdateAuthorizationState> {

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState

    private val authDeferred: CompletableDeferred<Boolean> = CompletableDeferred()

    data class QrCode(val link: String)

    override fun handleNotification(notification: TdApi.UpdateAuthorizationState) {
        _authState.value = notification.authorizationState
        when (val state = _authState.value) {
            is TdApi.AuthorizationStateReady -> authDeferred.complete(true)
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing,
            is TdApi.AuthorizationStateClosed -> authDeferred.complete(false)

            else -> {} // Other states are handled in respective methods
        }
    }

    override fun notificationType(): Class<TdApi.UpdateAuthorizationState> {
        return TdApi.UpdateAuthorizationState::class.java
    }

    suspend fun authWithPhoneNumber(phoneNumber: String): Result<Unit> = runCatching {
        withTimeout(30.seconds) {
            waitForState<TdApi.AuthorizationStateWaitPhoneNumber>()
            val setAuthenticationPhoneNumber = TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)
            telegramClient.sendAsync(setAuthenticationPhoneNumber).await()
        }
    }

    suspend fun authWithQr(): Result<QrCode> = runCatching {
        withTimeout(30.seconds) {
            val state = waitForState<TdApi.AuthorizationStateWaitOtherDeviceConfirmation>()
            QrCode(state.link)
        }
    }

    suspend fun sendAuthCode(code: String): Result<Unit> = runCatching {
        withTimeout(30.seconds) {
            waitForState<TdApi.AuthorizationStateWaitCode>()
            val checkAuthenticationCode = TdApi.CheckAuthenticationCode(code)
            telegramClient.sendAsync(checkAuthenticationCode).await()
        }
    }

    suspend fun sendPassword(password: String): Result<Unit> = runCatching {
        withTimeout(30.seconds) {
            waitForState<TdApi.AuthorizationStateWaitPassword>()
            val checkAuthenticationPassword = TdApi.CheckAuthenticationPassword(password)
            telegramClient.sendAsync(checkAuthenticationPassword).await()
        }
    }

    suspend fun awaitAuthResult(): Result<Boolean> = runCatching {
        withTimeout(120.seconds) {
            authDeferred.await()
        }
    }

    suspend fun checkAuthStatus(): Boolean {
        return when (_authState.value) {
            is TdApi.AuthorizationStateReady -> true
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing,
            is TdApi.AuthorizationStateClosed -> false

            else -> awaitAuthResult().getOrDefault(false)
        }
    }

    private suspend inline fun <reified T : TdApi.AuthorizationState> waitForState(): T {
        while (true) {
            val state = _authState.value
            if (state is T) {
                return state
            }
            kotlinx.coroutines.delay(100)
        }
    }
}