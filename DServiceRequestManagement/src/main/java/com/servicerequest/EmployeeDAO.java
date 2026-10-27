package com.servicerequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDAO {
    private static final String FIND_ALL_SQL =
            "SELECT employee_id, name, email, department FROM employees ORDER BY name";
    private static final String FIND_BY_ID_SQL =
            "SELECT employee_id, name, email, department FROM employees WHERE employee_id = ?";

    public List<Employee> findAll() throws SQLException {
        List<Employee> employees = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                employees.add(mapEmployee(resultSet));
            }
        }

        return employees;
    }

    public Optional<Employee> findById(int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapEmployee(resultSet)) : Optional.empty();
            }
        }
    }

    private Employee mapEmployee(ResultSet resultSet) throws SQLException {
        return new Employee(
                resultSet.getInt("employee_id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("department"));
    }
}