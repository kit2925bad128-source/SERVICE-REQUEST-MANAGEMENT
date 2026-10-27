package com.servicerequest;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

@WebServlet("/create-request")
public class CreateRequestServlet extends HttpServlet {
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        prepareJsonResponse(response);

        HttpSession session = request.getSession(false);
        Object userIdValue = session == null ? null : session.getAttribute("userId");
        if (!(userIdValue instanceof Integer userId)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in before creating a request.\"}");
            return;
        }

        String title = trimToNull(request.getParameter("title"));
        String description = trimToNull(request.getParameter("description"));
        String category = trimToNull(request.getParameter("category"));
        String priorityValue = trimToNull(request.getParameter("priority"));
        String priority = priorityValue == null ? "MEDIUM" : priorityValue.toUpperCase(Locale.ROOT);

        if (title == null || title.length() > 200) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Enter a request title of 1 to 200 characters.\"}");
            return;
        }
        if (description == null || description.length() > 60000) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Enter a description of 1 to 60,000 characters.\"}");
            return;
        }
        if (category != null && category.length() > 100) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Category must be 100 characters or fewer.\"}");
            return;
        }
        if (!ALLOWED_PRIORITIES.contains(priority)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Choose Low, Medium, or High priority.\"}");
            return;
        }

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setUserId(userId);
        serviceRequest.setTitle(title);
        serviceRequest.setDescription(description);
        serviceRequest.setCategory(category);
        serviceRequest.setPriority(priority);

        try {
            int requestId = requestDAO.create(serviceRequest);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"success\":true,\"message\":\"Request created successfully.\","
                    + "\"requestId\":" + requestId + ",\"status\":\"NEW\","
                    + "\"redirectUrl\":\"requests.html\"}");
        } catch (IllegalArgumentException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"The request details are invalid.\"}");
        } catch (SQLException exception) {
            getServletContext().log("Unable to create service request.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Request creation is temporarily unavailable.\"}");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
}