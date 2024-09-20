package me.gulya.misaka.views

import TelegramAuthService
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.server.VaadinSession
import kotlinx.coroutines.runBlocking
import me.gulya.misaka.views.login.LoginView
import org.springframework.beans.factory.annotation.Autowired

@Route("")
class RootView @Autowired constructor(
    private val telegramAuthService: TelegramAuthService
) : VerticalLayout() {

    init {
        runBlocking {
            if (!telegramAuthService.checkAuthStatus()) {
                RouteConfiguration.forSessionScope().setRoute("", LoginView::class.java)
                VaadinSession.getCurrent().session.setAttribute("REDIRECT_TO", "")
                ui.ifPresent { it.navigate("login") }
            } else {
                add(H1("Misaka is already serving!"))
            }
        }
    }
}