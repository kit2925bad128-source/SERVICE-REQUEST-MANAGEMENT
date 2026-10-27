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
import java.util.Locale;
import java.util.Set;

@WebServlet("/update-request")
public class UpdateRequestServlet extends HttpServlet {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final Set<String> ALLOWED_STATUSES =
            Set.of("NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED");
    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJsonResponse(response);
        if (!isSignedIn(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in to track request resolution.\"}");
            return;
        }
        if (!isAdministrator(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"Administrator access is required.\"}");
            return;
        }

        try {
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"requests\":[" + serializeRequests(requestDAO.findAll()) + "]}");
        } catch (SQLException exception) {
            getServletContext().log("Unable to load request resolution data.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Resolution data is temporarily unavailable.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        prepareJsonResponse(response);
        if (!isSignedIn(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in to update request resolution.\"}");
            return;
        }
        if (!isAdministrator(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"Administrator access is required.\"}");
            return;
        }

        int requestId = parsePositiveInteger(request.getParameter("requestId"));
        String status = request.getParameter("status");
        String resolution = trimToNull(request.getParameter("resolution"));
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (requestId < 1 || !ALLOWED_STATUSES.contains(normalizedStatus)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Select a valid request and status.\"}");
            return;
        }
        if (("RESOLVED".equals(normalizedStatus) || "CLOSED".equals(normalizedStatus))
                && resolution == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Enter a resolution before marking the request resolved or closed.\"}");
            return;
        }
        if (resolution != null && resolution.length() > 60000) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Resolution must be 60,000 characters or fewer.\"}");
            return;
        }

        try {
            if (!requestDAO.updateStatus(requestId, normalizedStatus, resolution)) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                        "{\"success\":false,\"message\":\"Request not found. Refresh and try again.\"}");
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"message\":\"Request status updated successfully.\","
                            + "\"requestId\":" + requestId + ",\"status\":\"" + normalizedStatus + "\"}");
        } catch (IllegalArgumentException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"The request status is invalid.\"}");
        } catch (SQLException exception) {
            getServletContext().log("Unable to update request resolution.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Resolution update is temporarily unavailable.\"}");
        }
    }

    private boolean isSignedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("userId") instanceof Integer;
    }

    private boolean isAdministrator(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute("userRole"));
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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