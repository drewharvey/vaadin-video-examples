package com.example.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * View where we display a list of all customers in a {@link Grid} component.
 */
@Route("")
@PageTitle("Customers")
@Menu(title = "Customers", icon = "vaadin:users")
public class CustomerView extends VerticalLayout {

  public CustomerView(CustomerRepository repository) {
      // create the Customer grid
      var grid = new CustomerGrid();

      // add the grid to our view and make it consume all the space
      addAndExpand(grid);

      // fetch all of the customers and add them to our grid
      var customers = repository.findAll();
      grid.setItems(customers);
  }

  class CustomerGrid extends Grid<Customer> {

      public CustomerGrid() {
          // Instead of reflection you can manually define which columns to show and
          // what they should contain
          addColumn(Customer::getId).setHeader("ID");
          addColumn(Customer::getName).setHeader("Name");
          addColumn(Customer::getEmail).setHeader("Email");

          // automatically adjust column widths based on their content
          getColumns().forEach(c -> c.setAutoWidth(true));
          setSizeFull();
      }
  }

  /**
   * This is a minimal example used for focusing on the high-level concepts. Use this to 
   * get an overview of the feature, but refer to the code above for the full example.
   */
  private void minimalExample(CustomerRepository repository) {

    // grid automatically generates columns when the entity class is provided
    var grid = new Grid<Customer>(Customer.class);
    add(grid);

    var customers = repository.findAll();
    grid.setItems(customers);
  }

}
