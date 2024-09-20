package me.gulya.misaka

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.Theme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
//@EnableJpaRepositories("me.gulya.misaka.*")
//@EntityScan("me.gulya.misaka.*")
@Theme(value = "staring-misaka")
open class Application : SpringBootServletInitializer(), AppShellConfigurator

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
