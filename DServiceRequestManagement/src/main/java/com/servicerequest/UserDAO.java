package com.servicerequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO {
    private static final String FIND_BY_EMAIL_SQL =
            "SELECT user_id, name, email, password, role FROM users WHERE email = ?";

    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL_SQL)) {
            statement.setString(1, email.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                User user = new User(
                        resultSet.getInt("user_id"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("password"),
                        resultSet.getString("role"));
                return Optional.of(user);
            }
        }
    }
}