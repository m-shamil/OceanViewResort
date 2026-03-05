package com.hotel.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.hotel.db.DBConnection;
import com.hotel.models.User;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

/**
 * Admin CRUD for Staff Accounts
 *
 * GET    /api/staff          → list all users
 * GET    /api/staff/{id}     → find(staffId)
 * POST   /api/staff          → save(staffData)
 * PUT    /api/staff/{id}     → update(staffId, newData)
 * DELETE /api/staff/{id}     → delete(staffId)
 */
public class StaffHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;

        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();
        // /api/staff or /api/staff/{id}
        boolean hasId = path.matches(".*/staff/\\d+$");
        String id     = hasId ? lastSegment(ex) : null;

        try {
            switch (method) {
                case "GET"    -> { if (hasId) findStaff(ex, id); else listStaff(ex); }
                case "POST"   -> createStaff(ex);
                case "PUT"    -> updateStaff(ex, id);
                case "DELETE" -> deleteStaff(ex, id);
                default       -> sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── [View Staff] StaffDB.find(staffId) ────────────────────────────────
    private void listStaff(HttpExchange ex) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT id, username, role, full_name, email, status, " +
                         "CONVERT(VARCHAR, created_at, 120) AS created_at FROM Users ORDER BY id";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append(mapUser(rs).toJson());
                first = false;
            }
            sb.append("]");
            sendOk(ex, JsonUtil.wrap("staff", sb.toString()));
        }
    }

    private void findStaff(HttpExchange ex, String id) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, username, role, full_name, email, status, " +
                "CONVERT(VARCHAR, created_at, 120) AS created_at FROM Users WHERE id = ?");
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // [found] display(record)
                sendOk(ex, JsonUtil.wrap("staff", mapUser(rs).toJson()));
            } else {
                // [not found] showNotFound()
                sendError(ex, 404, "Staff member not found.");
            }
        }
    }

    // ── [Create Staff] StaffDB.save(staffData) ────────────────────────────
    private void createStaff(HttpExchange ex) throws IOException, SQLException {
        Map<String, String> b = JsonUtil.parse(readBody(ex));
        String username = b.getOrDefault("username", "").trim();
        String password = b.getOrDefault("password", "").trim();
        String role     = b.getOrDefault("role", "staff").trim();
        String fullName = b.getOrDefault("fullName", "").trim();
        String email    = b.getOrDefault("email", "").trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            sendError(ex, 400, "username, password, and fullName are required."); return;
        }
        if (!role.equals("admin") && !role.equals("staff")) {
            sendError(ex, 400, "role must be 'admin' or 'staff'."); return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Users (username, password, role, full_name, email) " +
                "VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.setString(5, email.isEmpty() ? null : email);
            ps.executeUpdate();
            // saved → showSuccess()
            sendOk(ex, JsonUtil.success("Staff account created successfully."));
        } catch (SQLIntegrityConstraintViolationException e) {
            sendError(ex, 409, "Username already exists.");
        }
    }

    // ── [Update Staff] StaffDB.update(staffId, newData) ───────────────────
    private void updateStaff(HttpExchange ex, String id) throws IOException, SQLException {
        if (id == null) { sendError(ex, 400, "Staff ID required."); return; }
        Map<String, String> b = JsonUtil.parse(readBody(ex));

        StringBuilder set = new StringBuilder();
        if (b.containsKey("fullName"))  appendSet(set, "full_name = ?");
        if (b.containsKey("email"))     appendSet(set, "email = ?");
        if (b.containsKey("role"))      appendSet(set, "role = ?");
        if (b.containsKey("status"))    appendSet(set, "status = ?");
        if (b.containsKey("password") && !b.get("password").isBlank())
                                        appendSet(set, "password = ?");

        if (set.isEmpty()) { sendError(ex, 400, "No fields to update."); return; }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Users SET " + set + " WHERE id = ?");
            int i = 1;
            if (b.containsKey("fullName")) ps.setString(i++, b.get("fullName"));
            if (b.containsKey("email"))    ps.setString(i++, b.get("email"));
            if (b.containsKey("role"))     ps.setString(i++, b.get("role"));
            if (b.containsKey("status"))   ps.setString(i++, b.get("status"));
            if (b.containsKey("password") && !b.get("password").isBlank())
                                           ps.setString(i++, b.get("password"));
            ps.setInt(i, Integer.parseInt(id));
            int rows = ps.executeUpdate();
            if (rows == 0) { sendError(ex, 404, "Staff not found."); return; }
            // updated → showSuccess()
            sendOk(ex, JsonUtil.success("Staff updated successfully."));
        }
    }

    // ── [Delete Staff] StaffDB.delete(staffId) ────────────────────────────
    private void deleteStaff(HttpExchange ex, String id) throws IOException, SQLException {
        if (id == null) { sendError(ex, 400, "Staff ID required."); return; }
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM Users WHERE id = ?");
            ps.setInt(1, Integer.parseInt(id));
            int rows = ps.executeUpdate();
            if (rows == 0) { sendError(ex, 404, "Staff not found."); return; }
            // deleted → showDeleted()
            sendOk(ex, JsonUtil.success("Staff deleted."));
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("role"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("status"),
            rs.getString("created_at")
        );
    }

    private void appendSet(StringBuilder sb, String clause) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(clause);
    }
}
