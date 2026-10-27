package com.servicerequest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        prepareJsonResponse(response);

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (email == null || email.isBlank() || email.length() > 100
                || password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Enter a valid email and password.\"}");
            return;
        }

        try {
            Optional<User> matchingUser = userDAO.findByEmail(email.trim());
            if (matchingUser.isEmpty() || !BCrypt.checkpw(password, matchingUser.get().getPassword())) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "{\"success\":false,\"message\":\"Invalid email or password.\"}");
                return;
            }

            User user = matchingUser.get();
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userRole", user.getRole());

            String json = "{\"success\":true,\"redirectUrl\":\"dashboard.html\",\"user\":{" 
                    + "\"id\":" + user.getUserId()
                    + ",\"name\":\"" + escapeJson(user.getName()) + "\""
                    + ",\"email\":\"" + escapeJson(user.getEmail()) + "\""
                    + ",\"role\":\"" + escapeJson(user.getRole()) + "\"}}";
            writeJson(response, HttpServletResponse.SC_OK, json);
        } catch (SQLException exception) {
            getServletContext().log("Unable to authenticate user.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Login is temporarily unavailable.\"}");
        } catch (IllegalArgumentException exception) {
            getServletContext().log("A stored password hash is invalid.", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Login is temporarily unavailable.\"}");
        }
    }

    private void prepareJsonResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
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