package com.example.vaadin;

import static org.apache.commons.lang3.StringUtils.contains;
import org.apache.commons.lang3.StringUtils;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * View where we display a list of all customers in a {@link Grid} component.
 */
@Route("")
@PageTitle("Customers")
@Menu(title = "Customers", icon = "VAADIN_USER")
public class CustomerView extends VerticalLayout {

  public CustomerView(CustomerRepository repository) {
    // minimalExample(repository);
    bestPracticeExample(repository);
  }

  private void minimalExample(CustomerRepository repository) {
    setSizeFull();

    var grid = new Grid<Customer>();
    grid.addColumn(Customer::getName);
    grid.addColumn(Customer::getEmail);
    grid.setSizeFull();

    var dataView = grid.setItems();

    var searchField = new TextField();
    searchField.addValueChangeListener(e -> dataView.refreshAll());

    dataView.addFilter(customer -> {
      var searchStr = searchField.getValue();
      var name = customer.getName();
      var email = customer.getEmail();
      if (searchField.isEmpty()
          || contains(name, searchStr)
          || contains(email, searchStr)) {
        return true;
      }
      return false;
    });

    add(searchField, grid);

    var customers = repository.findAll();
    dataView.setItems(customers);
  }
  
  /**
   * Below is a more in-depth explenation of adding filtering to a {@link Grid} component.
   * The code in this function represents a best-practice approach and is the prefered approach
   * for production quality code.
   * @param repository
   */
  private void bestPracticeExample(CustomerRepository repository) {

    // tell the view to take up all of the remaining screen space
    setSizeFull();

    // create the grid component and add to the UI
    var grid = new Grid<Customer>();
    grid.setSizeFull();

    // add columns for each field in the User class
    grid.addColumn(Customer::getId);
    grid.addColumn(Customer::getName);
    grid.addColumn(Customer::getEmail);

    // automatically determine column widths based on content
    grid.getColumns().forEach(c -> c.setAutoWidth(true));

    // instantiate grid's data view by setting an empty list of items
    var dataView = grid.setItems();

    // create the search field component
    var searchField = new TextField();
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    searchField.addValueChangeListener(e -> dataView.refreshAll());
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    searchField.setWidthFull();

    add(searchField, grid);

    // tell grid to always filter its items based on our search field
    dataView.addFilter(customer -> {
      var searchStr = searchField.getValue();
      if (searchField.isEmpty()
          || StringUtils.containsIgnoreCase(customer.getName(), searchStr)
          || StringUtils.containsIgnoreCase(customer.getEmail(), searchStr)) {
        return true;
      }
      return false; // this customer did not pass the filter requirements - don't show in UI
    });

    // fetch all of the users and add them to our grid
    // note: we must use dataView.setItems, using grid.setItems 
    // will create a new data view and thus remove our filter function
    var customers = repository.findAll();
    dataView.setItems(customers);
  }

}


/*
public CustomerView(CustomerRepository repository) {

var grid = new Grid<Customer>();
grid.addColumn(Customer::getName);
grid.addColumn(Customer::getEmail);

var dataView = grid.setItems();

var searchField = new TextField();
searchField.addValueChangeListener(
    e -> dataView.refreshAll()
);

dataView.addFilter(customer -> {
  var searchStr = searchField.getValue();
  var name = customer.getName();
  var email = customer.getEmail();
  if (searchStr.isEmpty() 
      || contains(name, searchStr) 
      || contains(email, searchStr)) {
    return true;
  }
  return false;
});

add(searchField, grid);

var customers = repository.findAll();
dataView.setItems(customers);
} */
