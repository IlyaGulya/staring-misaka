package me.gulya.misaka.views.login

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession
import com.wontlost.zxing.ZXingVaadinWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@Route("login")
class LoginView @Autowired constructor(
    private val viewModel: LoginViewModel
) : VerticalLayout(), BeforeEnterObserver {

    private val phoneField = TextField("Phone Number")
    private val codeField = TextField("Authentication Code")
    private val passwordField = PasswordField("Two-Step Verification Password")
    private val submitButton = Button("Submit")
    private val switchMethodButton = Button("Switch to QR Login")
    private val qrCodeImage = ZXingVaadinWriter()
    private val messageDiv = Div()

    init {
        add(H2("Telegram Login"))
        add(phoneField, codeField, passwordField, submitButton, switchMethodButton, qrCodeImage, messageDiv)

        submitButton.addClickListener { handleSubmit() }
        switchMethodButton.addClickListener { handleSwitchMethod() }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        val ui = attachEvent.ui
        ui.access {
            CoroutineScope(Dispatchers.Default).launch {
                viewModel.uiState.collect { state ->
                    ui.access {
                        updateView(state)
                        if (state.shouldRedirect) {
                            redirectToRoot()
                        }
                    }
                }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.checkAuthStatus()
        }
    }

    private fun updateView(state: TelegramLoginUiState) {
        phoneField.isVisible =
            state.loginState is TelegramLoginState.Initial || state.loginState is TelegramLoginState.PhoneEntered
        codeField.isVisible = state.loginState is TelegramLoginState.CodeRequested
        passwordField.isVisible = state.loginState is TelegramLoginState.PasswordRequired
        qrCodeImage.isVisible = state.loginState is TelegramLoginState.WaitingForQR

        submitButton.isEnabled = !state.isLoading
        switchMethodButton.isEnabled = !state.isLoading

        when (state.loginState) {
            is TelegramLoginState.Initial -> {
                submitButton.text = "Send Code"
                switchMethodButton.text = "Switch to QR Login"
            }

            is TelegramLoginState.WaitingForQR -> {
                submitButton.isVisible = false
                switchMethodButton.text = "Switch to Phone Login"
                state.qrCodeLink?.let { link ->
                    qrCodeImage.value = link
                }
            }

            is TelegramLoginState.PhoneEntered -> {
                submitButton.text = "Resend Code"
            }

            is TelegramLoginState.CodeRequested -> {
                submitButton.text = "Verify Code"
            }

            is TelegramLoginState.PasswordRequired -> {
                submitButton.text = "Submit Password"
            }

            is TelegramLoginState.Error -> {
                messageDiv.text = state.loginState.message
            }
        }
    }

    private fun getImageAsBase64(value: ByteArray?): String? {
        val mimeType = "image/png"
        val htmlValue: String = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(value)
        return htmlValue
    }

    private fun handleSubmit() {
        CoroutineScope(Dispatchers.Default).launch {
            when (val state = viewModel.uiState.value.loginState) {
                is TelegramLoginState.Initial, is TelegramLoginState.PhoneEntered ->
                    viewModel.sendAuthenticationCode(phoneField.value)

                is TelegramLoginState.CodeRequested ->
                    viewModel.verifyAuthenticationCode(codeField.value)

                is TelegramLoginState.PasswordRequired ->
                    viewModel.submitPassword(passwordField.value)

                else -> {}
            }
        }
    }

    private fun handleSwitchMethod() {
        CoroutineScope(Dispatchers.Default).launch {
            if (viewModel.uiState.value.loginState is TelegramLoginState.WaitingForQR) {
                viewModel.switchLoginMethod()
            } else {
                viewModel.generateQRCode()
            }
        }
    }

    private fun redirectToRoot() {
        val redirectTo = VaadinSession.getCurrent().session.getAttribute("REDIRECT_TO") as? String ?: ""
        UI.getCurrent().navigate(redirectTo)
    }
}