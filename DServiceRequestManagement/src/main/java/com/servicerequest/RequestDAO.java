package com.servicerequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RequestDAO {
    private static final Set<String> ALLOWED_STATUSES =
            Set.of("NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED");
    private static final String REQUEST_COLUMNS =
            "SELECT r.request_id, r.user_id, r.title, r.description, r.category, r.priority, "
                    + "r.assigned_to, e.name AS assigned_employee_name, r.status, r.resolution, "
                    + "r.created_at, r.resolved_at FROM requests r "
                    + "LEFT JOIN employees e ON e.employee_id = r.assigned_to ";

    public int create(ServiceRequest request) throws SQLException {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("A request title and description are required.");
        }

        String priority = request.getPriority() == null || request.getPriority().isBlank()
                ? "MEDIUM"
                : request.getPriority().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("LOW", "MEDIUM", "HIGH").contains(priority)) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, or HIGH.");
        }

        String sql = "INSERT INTO requests "
                + "(user_id, title, description, category, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, 'NEW')";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, request.getUserId());
            statement.setString(2, request.getTitle().trim());
            statement.setString(3, request.getDescription().trim());
            statement.setString(4, request.getCategory());
            statement.setString(5, priority);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating the request did not return a request ID.");
                }
                int requestId = keys.getInt(1);
                request.setRequestId(requestId);
                request.setStatus("NEW");
                return requestId;
            }
        }
    }

    public List<ServiceRequest> findAll() throws SQLException {
        return queryRequests(REQUEST_COLUMNS + "ORDER BY r.created_at DESC, r.request_id DESC", null, null);
    }

    public List<ServiceRequest> findByUserId(int userId) throws SQLException {
        String sql = REQUEST_COLUMNS + "WHERE r.user_id = ? ORDER BY r.created_at DESC, r.request_id DESC";
        return queryRequests(sql, userId, null);
    }

    public List<ServiceRequest> findRecent(int limit) throws SQLException {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Recent request limit must be between 1 and 100.");
        }
        String sql = REQUEST_COLUMNS + "ORDER BY r.created_at DESC, r.request_id DESC LIMIT ?";
        return queryRequests(sql, null, limit);
    }

    public List<ServiceRequest> findRecentByUserId(int userId, int limit) throws SQLException {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Recent request limit must be between 1 and 100.");
        }
        String sql = REQUEST_COLUMNS
                + "WHERE r.user_id = ? ORDER BY r.created_at DESC, r.request_id DESC LIMIT ?";
        return queryRequests(sql, userId, limit);
    }

    public Map<String, Integer> getStatusCounts() throws SQLException {
        return getStatusCounts(null);
    }

    public Map<String, Integer> getStatusCounts(int userId) throws SQLException {
        return getStatusCounts(Integer.valueOf(userId));
    }

    public int countAll() throws SQLException {
        return countRequests(null);
    }

    public int countByUserId(int userId) throws SQLException {
        return countRequests(Integer.valueOf(userId));
    }

    private Map<String, Integer> getStatusCounts(Integer userId) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : ALLOWED_STATUSES) {
            counts.put(status, 0);
        }

        String sql = "SELECT status, COUNT(*) AS request_count FROM requests"
                + (userId == null ? "" : " WHERE user_id = ?")
                + " GROUP BY status";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId != null) {
                statement.setInt(1, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getString("status"), resultSet.getInt("request_count"));
                }
            }
        }

        return counts;
    }

    private int countRequests(Integer userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM requests" + (userId == null ? "" : " WHERE user_id = ?");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId != null) {
                statement.setInt(1, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public boolean assignRequest(int requestId, int employeeId) throws SQLException {
        String sql = "UPDATE requests SET assigned_to = ?, "
                + "status = CASE WHEN status = 'NEW' THEN 'ASSIGNED' ELSE status END "
                + "WHERE request_id = ? AND status NOT IN ('RESOLVED', 'CLOSED')";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, requestId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int requestId, String status, String resolution) throws SQLException {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported request status.");
        }

        String sql = "UPDATE requests SET status = ?, resolution = ?, "
                + "resolved_at = CASE WHEN ? IN ('RESOLVED', 'CLOSED') "
                + "THEN COALESCE(resolved_at, CURRENT_TIMESTAMP) ELSE NULL END "
                + "WHERE request_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedStatus);
            statement.setString(2, resolution == null || resolution.isBlank() ? null : resolution.trim());
            statement.setString(3, normalizedStatus);
            statement.setInt(4, requestId);
            return statement.executeUpdate() > 0;
        }
    }

    private List<ServiceRequest> queryRequests(String sql, Integer userId, Integer limit)
            throws SQLException {
        List<ServiceRequest> requests = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            if (userId != null) {
                statement.setInt(parameterIndex++, userId);
            }
            if (limit != null) {
                statement.setInt(parameterIndex, limit);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRequest(resultSet));
                }
            }
        }
        return requests;
    }

    private ServiceRequest mapRequest(ResultSet resultSet) throws SQLException {
        int assignedEmployeeId = resultSet.getInt("assigned_to");
        Integer assignedTo = resultSet.wasNull() ? null : assignedEmployeeId;
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp resolvedAt = resultSet.getTimestamp("resolved_at");

        ServiceRequest request = new ServiceRequest(
                resultSet.getInt("request_id"),
                resultSet.getInt("user_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getString("priority"),
                assignedTo,
                resultSet.getString("status"),
                resultSet.getString("resolution"),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                resolvedAt == null ? null : resolvedAt.toLocalDateTime());
        request.setAssignedEmployeeName(resultSet.getString("assigned_employee_name"));
        return request;
    }
}