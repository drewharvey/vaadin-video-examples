package com.example.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("")
@PageTitle("Employees")
@Menu(title = "Employees", icon = "vaadin:users")
public class EmployeeView extends VerticalLayout {

    public EmployeeView(EmployeeService service) {
        setSizeFull();

        var grid = new EmployeeGrid();
        add(grid);

        var employees = service.getAllEmployees();
        grid.setItems(employees);
    }

    class EmployeeGrid extends Grid<Employee> {

        public EmployeeGrid() {
            setMultiSort(true);
            setSizeFull();

            addComponentColumn(EmployeeImage::new);

            addSortableColumn(Employee::getName, "Name");
            addSortableColumn(Employee::getDepartment, "Department");
            addSortableColumn(Employee::getEmail, "Email");

            getColumns().forEach(c -> c.setAutoWidth(true));
        }

        private void addSortableColumn(ValueProvider<Employee, String> columnContent, String header) {
            addColumn(columnContent)
                    .setHeader(header)
                    .setSortable(true);
        }
    }

    class EmployeeImage extends Image {

        public EmployeeImage(Employee employee) {
            super(employee.getPicture(), employee.getName());
            addClassNames(LumoUtility.BorderRadius.FULL);
            setHeight("48px");
            setWidth("48px");
        }
    }
}
