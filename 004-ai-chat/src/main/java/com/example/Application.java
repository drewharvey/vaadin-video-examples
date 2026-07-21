package com.example;

import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

@SpringBootApplication
// loads vaadin's aura theme, forced into dark mode
@StyleSheet(Aura.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
@StyleSheet("styles.css") // your custom styles
// lets the server push ui updates to the browser without a page request;
// needed for streaming ai responses token by token
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
