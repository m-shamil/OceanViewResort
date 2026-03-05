package com.hotel.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.hotel.db.DBConnection;
import com.hotel.models.User;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class AuthHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;

        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
            login(ex);
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    // ── AuthService.login(username, password) ──────────────────────────────
    private void login(HttpExchange ex) throws IOException {
        Map<String, String> body = JsonUtil.parse(readBody(ex));
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "").trim();

        if (username.isEmpty() || password.isEmpty()) {
            sendError(ex, 400, "Username and password are required."); return;
        }

        // UserDB.findUser(username)
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT id, username, password, role, full_name, " +
                         "email, status, CONVERT(VARCHAR, created_at, 120) AS created_at " +
                         "FROM Users WHERE username = ? AND status = 'active'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedPw = rs.getString("password");
                if (!storedPw.equals(password)) {
                    sendError(ex, 401, "Invalid credentials."); return;
                }
                User u = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                // result(success) → send user record (without password)
                sendOk(ex, "{\"success\":true,\"user\":" + u.toJson() + "}");
            } else {
                // result(fail) → showError("Invalid credentials")
                sendError(ex, 401, "Invalid credentials.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(ex, 500, "Database error: " + e.getMessage());
        }
    }
}
