package com.servicerequest;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/requests")
public class RequestListServlet extends HttpServlet {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJsonResponse(response);
        HttpSession session = request.getSession(false);
        Object userIdValue = session == null ? null : session.getAttribute("userId");
        Object roleValue = session == null ? null : session.getAttribute("userRole");
        if (!(userIdValue instanceof Integer userId) || !(roleValue instanceof String role)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in to view requests.\"}");
            return;
        }

        try {
            List<ServiceRequest> requests = "ADMIN".equals(role)
                    ? requestDAO.findAll()
                    : requestDAO.findByUserId(userId);
            String json = "{\"success\":true,\"role\":\"" + escapeJson(role)
                    + "\",\"requests\":[" + serializeRequests(requests) + "]}";
            writeJson(response, HttpServletResponse.SC_OK, json);
        } catch (SQLException exception) {
            getServletContext().log("Unable to load service requests.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Requests are temporarily unavailable.\"}");
        }
    }

    private String serializeRequests(List<ServiceRequest> requests) {
        StringBuilder json = new StringBuilder();
        for (ServiceRequest serviceRequest : requests) {
            if (!json.isEmpty()) {
                json.append(',');
            }
            json.append("{\"id\":").append(serviceRequest.getRequestId())
                    .append(",\"title\":\"").append(escapeJson(serviceRequest.getTitle())).append('"')
                    .append(",\"description\":\"").append(escapeJson(serviceRequest.getDescription())).append('"')
                    .append(",\"category\":\"").append(escapeJson(serviceRequest.getCategory())).append('"')
                    .append(",\"priority\":\"").append(escapeJson(serviceRequest.getPriority())).append('"')
                    .append(",\"assignedEmployee\":\"")
                    .append(escapeJson(serviceRequest.getAssignedEmployeeName())).append('"')
                    .append(",\"status\":\"").append(escapeJson(serviceRequest.getStatus())).append('"')
                    .append(",\"createdAt\":\"")
                    .append(serviceRequest.getCreatedAt() == null
                            ? ""
                            : escapeJson(DATE_FORMAT.format(serviceRequest.getCreatedAt())))
                    .append("\",\"resolution\":\"").append(escapeJson(serviceRequest.getResolution())).append('"')
                    .append(",\"resolvedAt\":\"")
                    .append(serviceRequest.getResolvedAt() == null
                            ? ""
                            : escapeJson(DATE_FORMAT.format(serviceRequest.getResolvedAt())))
                    .append("\"}");
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