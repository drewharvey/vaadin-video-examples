package com.example.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Collection;

/**
 * Changes since release of 'Add filtering to Grid' video:
 *
 * The video demonstrates a more advanced filtering approach using a
 * {@link com.vaadin.flow.data.provider.ListDataView}.
 * This approach is more efficient for situations where fetching from the
 * backend is expensive. However, to keep the example simple and focused
 * on high-level concepts, we use a straightforward method that fetches
 * filtered data directly from the service in this code and sets the list
 * to the grid via {@link Grid#setItems(Collection)}.
 *
 * We strongly recommend you use the main code example below, but if you
 * want to look at the example demonstrated in the video, see {@link #exampleFromVideo(CustomerService)}.
 */
@Route("")
@PageTitle("Customers")
@Menu(title = "Customers", icon = "vaadin:user")
public class CustomerView extends VerticalLayout {

    public CustomerView(CustomerService service) {
        // create the grid and search field components
        var grid = new CustomerGrid();
        var searchField = new SearchField();

        // add components and make grid consume all the space
        add(searchField);
        addAndExpand(grid);

        // refresh grid when search field value changes
        searchField.addValueChangeListener(e -> {
            var searchValue = e.getValue();
            var filteredCustomers = service.filterCustomers(searchValue);
            grid.setItems(filteredCustomers);
        });

        grid.setItems(service.findAll());
    }

    class CustomerGrid extends Grid<Customer> {

        public CustomerGrid() {
            addColumn(Customer::getId).setHeader("Id");
            addColumn(Customer::getName).setHeader("Name");
            addColumn(Customer::getEmail).setHeader("Email");

            // automatically determine column widths based on content
            getColumns().forEach(c -> c.setAutoWidth(true));
            setSizeFull();
        }
    }

    class SearchField extends TextField {

        public SearchField() {
            // With lazy value change mode, event is fired after user has a short break,
            // no need to hit enter
            setValueChangeMode(ValueChangeMode.LAZY);
            setPlaceholder("Search");
            setPrefixComponent(new Icon(VaadinIcon.SEARCH));
            setWidthFull();
            setClearButtonVisible(true);
        }
    }

    /**
     * This is a minimal example used for focusing on the high-level concepts. Use this to
     * get an overview of the feature, but refer to the code above for the full example.
     */
    private void minimalExample(CustomerService service) {
        var grid = new Grid<>(Customer.class);

        var searchField = new TextField();
        searchField.addValueChangeListener(e ->
                grid.setItems(service.filterCustomers(searchField.getValue())));

        add(searchField);
        addAndExpand(grid);

        grid.setItems(service.findAll());
    }

    private void exampleFromVideo(CustomerService service) {
        var grid = new Grid<>(Customer.class);
        var dataView = grid.setItems();

        var searchField = new TextField();
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> dataView.refreshAll());

        add(searchField);
        addAndExpand(grid);

        dataView.addFilter(customer -> {
            var searchValue = searchField.getValue();
            return StringUtils.isEmpty(searchValue)
                    || Strings.CI.contains(customer.getName(), searchValue)
                    || Strings.CI.contains(customer.getEmail(), searchValue);
        });

        dataView.setItems(service.findAll());
    }

}
