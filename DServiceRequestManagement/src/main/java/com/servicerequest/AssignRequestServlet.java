package com.servicerequest;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/assign-request")
public class AssignRequestServlet extends HttpServlet {
    private final RequestDAO requestDAO = new RequestDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJsonResponse(response);
        if (!isSignedIn(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in to manage assignments.\"}");
            return;
        }
        if (!isAdministrator(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"Administrator access is required.\"}");
            return;
        }

        try {
            List<Employee> employees = employeeDAO.findAll();
            List<ServiceRequest> requests = requestDAO.findAll().stream()
                    .filter(item -> !"RESOLVED".equals(item.getStatus()) && !"CLOSED".equals(item.getStatus()))
                    .toList();
            String json = "{\"success\":true,\"employees\":[" + serializeEmployees(employees)
                    + "],\"requests\":[" + serializeRequests(requests) + "]}";
            writeJson(response, HttpServletResponse.SC_OK, json);
        } catch (SQLException exception) {
            getServletContext().log("Unable to load employee assignment data.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Assignment data is temporarily unavailable.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        prepareJsonResponse(response);
        if (!isSignedIn(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"success\":false,\"message\":\"Please sign in to manage assignments.\"}");
            return;
        }
        if (!isAdministrator(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"Administrator access is required.\"}");
            return;
        }

        int requestId = parsePositiveInteger(request.getParameter("requestId"));
        int employeeId = parsePositiveInteger(request.getParameter("employeeId"));
        if (requestId < 1 || employeeId < 1) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Select a valid request and employee.\"}");
            return;
        }

        try {
            if (employeeDAO.findById(employeeId).isEmpty()) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"message\":\"The selected employee does not exist.\"}");
                return;
            }
            if (!requestDAO.assignRequest(requestId, employeeId)) {
                writeJson(response, HttpServletResponse.SC_CONFLICT,
                        "{\"success\":false,\"message\":\"The request no longer accepts assignments. Refresh and try again.\"}");
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"message\":\"Request assigned successfully.\",\"requestId\":"
                            + requestId + "}");
        } catch (SQLException exception) {
            getServletContext().log("Unable to assign service request.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Assignment is temporarily unavailable.\"}");
        }
    }

    private boolean isAdministrator(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute("userRole"));
    }

    private boolean isSignedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("userId") instanceof Integer;
    }

    private int parsePositiveInteger(String value) {
        if (value == null) {
            return -1;
        }
        try {
            int parsedValue = Integer.parseInt(value);
            return parsedValue > 0 ? parsedValue : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String serializeEmployees(List<Employee> employees) {
        StringBuilder json = new StringBuilder();
        for (Employee employee : employees) {
            if (!json.isEmpty()) {
                json.append(',');
            }
            json.append("{\"id\":").append(employee.getEmployeeId())
                    .append(",\"name\":\"").append(escapeJson(employee.getName())).append('"')
                    .append(",\"email\":\"").append(escapeJson(employee.getEmail())).append('"')
                    .append(",\"department\":\"").append(escapeJson(employee.getDepartment())).append("\"}");
        }
        return json.toString();
    }

    private String serializeRequests(List<ServiceRequest> requests) {
        StringBuilder json = new StringBuilder();
        for (ServiceRequest serviceRequest : requests) {
            if (!json.isEmpty()) {
                json.append(',');
            }
            json.append("{\"id\":").append(serviceRequest.getRequestId())
                    .append(",\"title\":\"").append(escapeJson(serviceRequest.getTitle())).append('"')
                    .append(",\"priority\":\"").append(escapeJson(serviceRequest.getPriority())).append('"')
                    .append(",\"status\":\"").append(escapeJson(serviceRequest.getStatus())).append('"')
                    .append(",\"assignedTo\":")
                    .append(serviceRequest.getAssignedTo() == null ? "null" : serviceRequest.getAssignedTo())
                    .append(",\"assignedEmployee\":\"")
                    .append(escapeJson(serviceRequest.getAssignedEmployeeName())).append("\"}");
        }
        return json.toString();
    }

    private void prepareJsonResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.getWriter().write(json);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}