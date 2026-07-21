package com.example.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

@StyleSheet("view-title.css")
public class ViewTitle extends Composite<HorizontalLayout> {

    public ViewTitle(String title) {
        addClassName("view-title");
        // aura hides a non-"permanent" drawer toggle inside an .aura-view-header
        // while the drawer is open, and shows it when the drawer is closed
        addClassName("aura-view-header");

        var toggle = new DrawerToggle();
        toggle.getElement().getThemeList().add("tertiary");

        var h = new H1(title);
        getContent().add(toggle, h);
        getContent().setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        getContent().setWidthFull();
    }

    /**
     * Adds components after the title, e.g. view-specific action buttons.
     */
    public void add(Component... components) {
        getContent().add(components);
    }

}
