package com.hotel.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.hotel.db.DBConnection;
import com.hotel.models.Reservation;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * GET    /api/reservations         → list all reservations
 * GET    /api/reservations/{resNo} → find by resNo
 * POST   /api/reservations         → addReservation(details) with conflict check
 * PUT    /api/reservations/{resNo} → update status
 * DELETE /api/reservations/{resNo} → cancel
 * GET    /api/rates                → list room rates
 */
public class ReservationHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;

        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();

        try {
            if (path.endsWith("/rates")) {
                getRates(ex); return;
            }
            boolean hasSeg = path.matches(".*/reservations/[^/]+$");
            String  seg    = hasSeg ? lastSegment(ex) : null;

            switch (method) {
                case "GET"    -> { if (hasSeg) findReservation(ex, seg); else listReservations(ex); }
                case "POST"   -> addReservation(ex);
                case "PUT"    -> updateReservationStatus(ex, seg);
                case "DELETE" -> cancelReservation(ex, seg);
                default       -> sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── List ──────────────────────────────────────────────────────────────
    private void listReservations(HttpExchange ex) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql =
                "SELECT r.id, r.res_no, r.guest_name, r.guest_contact, r.room_type, " +
                "CONVERT(VARCHAR,r.check_in,23) AS check_in, " +
                "CONVERT(VARCHAR,r.check_out,23) AS check_out, " +
                "r.status, r.created_by, " +
                "CONVERT(VARCHAR,r.created_at,120) AS created_at, " +
                "rr.rate_per_night " +
                "FROM Reservations r " +
                "JOIN RoomRates rr ON r.room_type = rr.room_type " +
                "ORDER BY r.id DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append(mapRes(rs).toJson());
                first = false;
            }
            sb.append("]");
            sendOk(ex, JsonUtil.wrap("reservations", sb.toString()));
        }
    }

    private void findReservation(HttpExchange ex, String resNo) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT r.id, r.res_no, r.guest_name, r.guest_contact, r.room_type, " +
                "CONVERT(VARCHAR,r.check_in,23) AS check_in, " +
                "CONVERT(VARCHAR,r.check_out,23) AS check_out, " +
                "r.status, r.created_by, " +
                "CONVERT(VARCHAR,r.created_at,120) AS created_at, " +
                "rr.rate_per_night " +
                "FROM Reservations r " +
                "JOIN RoomRates rr ON r.room_type = rr.room_type " +
                "WHERE r.res_no = ?");
            ps.setString(1, resNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sendOk(ex, JsonUtil.wrap("reservation", mapRes(rs).toJson()));
            } else {
                sendError(ex, 404, "Reservation not found.");
            }
        }
    }

    // ── ReservationService.addReservation(details) ────────────────────────
    private void addReservation(HttpExchange ex) throws IOException, SQLException {
        Map<String, String> b = JsonUtil.parse(readBody(ex));
        String guestName    = b.getOrDefault("guestName",    "").trim();
        String guestContact = b.getOrDefault("guestContact", "").trim();
        String roomType     = b.getOrDefault("roomType",     "").trim();
        String checkIn      = b.getOrDefault("checkIn",      "").trim();
        String checkOut     = b.getOrDefault("checkOut",     "").trim();
        String createdByStr = b.getOrDefault("createdBy",    "0").trim();

        if (guestName.isEmpty() || roomType.isEmpty() || checkIn.isEmpty() || checkOut.isEmpty()) {
            sendError(ex, 400, "guestName, roomType, checkIn, checkOut are required."); return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // ReservationDB.checkConflict(roomType, dates)
            PreparedStatement conflict = conn.prepareStatement(
                "SELECT COUNT(*) FROM Reservations " +
                "WHERE room_type = ? AND status NOT IN ('cancelled','checked_out') " +
                "AND check_in < ? AND check_out > ?");
            conflict.setString(1, roomType);
            conflict.setString(2, checkOut);
            conflict.setString(3, checkIn);
            ResultSet cr = conflict.executeQuery();
            cr.next();
            if (cr.getInt(1) > 0) {
                // conflictYes → showConflict()
                sendError(ex, 409, "Room conflict: the selected room type is not available for the chosen dates.");
                return;
            }

            // [no conflict] save(details)
            String resNo = generateResNo(conn);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Reservations " +
                "(res_no, guest_name, guest_contact, room_type, check_in, check_out, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, resNo);
            ps.setString(2, guestName);
            ps.setString(3, guestContact.isEmpty() ? null : guestContact);
            ps.setString(4, roomType);
            ps.setString(5, checkIn);
            ps.setString(6, checkOut);
            ps.setInt   (7, Integer.parseInt(createdByStr));
            ps.executeUpdate();

            // saved(resNo) → showConfirmation(resNo)
            sendOk(ex, "{\"success\":true,\"message\":\"Reservation confirmed.\",\"resNo\":\"" + resNo + "\"}");
        }
    }

    // ── Update status ─────────────────────────────────────────────────────
    private void updateReservationStatus(HttpExchange ex, String resNo)
            throws IOException, SQLException {
        if (resNo == null) { sendError(ex, 400, "Reservation number required."); return; }
        Map<String, String> b = JsonUtil.parse(readBody(ex));
        String status = b.getOrDefault("status", "").trim();
        if (status.isEmpty()) { sendError(ex, 400, "Status required."); return; }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Reservations SET status = ? WHERE res_no = ?");
            ps.setString(1, status);
            ps.setString(2, resNo);
            int rows = ps.executeUpdate();
            if (rows == 0) { sendError(ex, 404, "Reservation not found."); return; }
            sendOk(ex, JsonUtil.success("Status updated to " + status));
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────
    private void cancelReservation(HttpExchange ex, String resNo)
            throws IOException, SQLException {
        if (resNo == null) { sendError(ex, 400, "Reservation number required."); return; }
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Reservations SET status = 'cancelled' WHERE res_no = ?");
            ps.setString(1, resNo);
            int rows = ps.executeUpdate();
            if (rows == 0) { sendError(ex, 404, "Reservation not found."); return; }
            sendOk(ex, JsonUtil.success("Reservation cancelled."));
        }
    }

    // ── Room rates ────────────────────────────────────────────────────────
    private void getRates(HttpExchange ex) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT room_type, rate_per_night FROM RoomRates ORDER BY rate_per_night");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"roomType\":\"").append(rs.getString("room_type"))
                  .append("\",\"rate\":").append(rs.getDouble("rate_per_night")).append("}");
                first = false;
            }
            sb.append("]");
            sendOk(ex, JsonUtil.wrap("rates", sb.toString()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Reservation mapRes(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.id           = rs.getInt("id");
        r.resNo        = rs.getString("res_no");
        r.guestName    = rs.getString("guest_name");
        r.guestContact = rs.getString("guest_contact");
        r.roomType     = rs.getString("room_type");
        r.checkIn      = rs.getString("check_in");
        r.checkOut     = rs.getString("check_out");
        r.status       = rs.getString("status");
        r.createdBy    = rs.getInt("created_by");
        r.createdAt    = rs.getString("created_at");
        r.ratePerNight = rs.getDouble("rate_per_night");
        // compute nights & total
        LocalDate in  = LocalDate.parse(r.checkIn);
        LocalDate out = LocalDate.parse(r.checkOut);
        r.nights = (int)(out.toEpochDay() - in.toEpochDay());
        r.total  = r.nights * r.ratePerNight;
        return r;
    }

    private String generateResNo(Connection conn) throws SQLException {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        ResultSet rs = conn.createStatement()
            .executeQuery("SELECT COUNT(*)+1 AS seq FROM Reservations " +
                          "WHERE res_no LIKE 'RES-" + date + "-%'");
        rs.next();
        return String.format("RES-%s-%03d", date, rs.getInt("seq"));
    }
}
