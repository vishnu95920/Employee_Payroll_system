import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        PayrollSystem payrollSystem = new PayrollSystem();
        FullTimeEmployee emp1 = new FullTimeEmployee("Vishnu", 2232915, 70000);
        PartTimeEmployee emp2 = new PartTimeEmployee("Anshita", 2232849, 5, 300);
        payrollSystem.addEmployee(emp1);
        payrollSystem.addEmployee(emp2);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Single context for /api/employees handling GET, POST, and OPTIONS
        server.createContext("/api/employees", exchange -> {
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:3000");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("GET".equals(method)) {
                // Fetch all employees
                StringBuilder response = new StringBuilder("[");
                List<Employee> employees = payrollSystem.getEmployeeList();
                for (int i = 0; i < employees.size(); i++) {
                    Employee emp = employees.get(i);
                    response.append(String.format(
                        "{\"name\":\"%s\",\"id\":%d,\"salary\":%.2f,\"type\":\"%s\"}",
                        emp.getName().replace("\"", "\\\""), // Escape quotes
                        emp.getId(), emp.calculateSalary(),
                        emp instanceof FullTimeEmployee ? "fulltime" : "parttime"
                    ));
                    if (i < employees.size() - 1) response.append(",");
                }
                response.append("]");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.toString().getBytes(StandardCharsets.UTF_8));
                }
            } else if ("POST".equals(method)) {
                // Add an employee
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
                String json = requestBody.toString();
                try {
                    String name = json.split("\"name\":\"")[1].split("\"")[0];
                    int id = Integer.parseInt(json.split("\"id\":")[1].split(",")[0]);
                    String type = json.split("\"type\":\"")[1].split("\"")[0];
                    double salary = Double.parseDouble(json.split("\"salary\":")[1].split("}")[0]);

                    Employee employee;
                    if ("fulltime".equals(type)) {
                        employee = new FullTimeEmployee(name, id, salary);
                    } else {
                        employee = new PartTimeEmployee(name, id, (int) (salary / 300), 300);
                    }
                    payrollSystem.addEmployee(employee);
                    exchange.sendResponseHeaders(201, -1); // Created
                } catch (Exception e) {
                    String error = "{\"error\":\"Invalid input\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, error.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else if ("OPTIONS".equals(method)) {
                // Handle CORS preflight
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        });

        // DELETE /api/employees/{id}: Remove an employee
        server.createContext("/api/employees/", exchange -> {
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:3000");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("DELETE".equals(method)) {
                String path = exchange.getRequestURI().getPath();
                String idStr = path.substring("/api/employees/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    payrollSystem.removeEmployee(id);
                    exchange.sendResponseHeaders(200, -1); // OK
                } catch (NumberFormatException e) {
                    String error = "{\"error\":\"Invalid ID\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, error.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:8080");
    }
}

abstract class Employee {
    private String name;
    private int id;

    Employee(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public abstract double calculateSalary();

    @Override
    public String toString() {
        return "Employee [name=" + name + ", id=" + id + ", salary=" + calculateSalary() + "]";
    }
}

class FullTimeEmployee extends Employee {
    private double monthlySalary;

    FullTimeEmployee(String name, int id, double monthlySalary) {
        super(name, id);
        this.monthlySalary = monthlySalary;
    }

    @Override
    public double calculateSalary() {
        return monthlySalary;
    }
}

class PartTimeEmployee extends Employee {
    private int hours;
    private double hourlyRate;

    public PartTimeEmployee(String name, int id, int hours, double hourlyRate) {
        super(name, id);
        this.hourlyRate = hourlyRate;
        this.hours = hours;
    }

    @Override
    public double calculateSalary() {
        return hours * hourlyRate;
    }
}

class PayrollSystem {
    private ArrayList<Employee> employeeList;

    public PayrollSystem() {
        employeeList = new ArrayList<>();
    }

    public void addEmployee(Employee employee) {
        employeeList.add(employee);
    }

    public void removeEmployee(int id) {
        Employee employeeToRemove = null;
        for (Employee employee : employeeList) {
            if (employee.getId() == id) {
                employeeToRemove = employee;
                break;
            }
        }
        if (employeeToRemove != null) {
            employeeList.remove(employeeToRemove);
        }
    }

    public void displayEmployee() {
        for (Employee employee : employeeList) {
            System.out.println(employee);
        }
    }

    public List<Employee> getEmployeeList() {
        return new ArrayList<>(employeeList);
    }
}