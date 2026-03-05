package com.hotel.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.hotel.db.DBConnection;
import com.hotel.models.Reservation;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

/**
 * GET  /api/billing/{resNo}       → calculateBill(resNo) — staff use
 * GET  /api/ebill/{resNo}         → viewEBill(resNo)     — guest (public)
 * POST /api/billing/print/{resNo} → printBill(resNo)
 */
public class BillingHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;

        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();

        try {
            if (path.contains("/billing/print/")) {
                // POST /api/billing/print/{resNo}
                printBill(ex, lastSegment(ex)); return;
            }
            if (path.contains("/billing/")) {
                // GET /api/billing/{resNo}
                calculateBill(ex, lastSegment(ex)); return;
            }
            if (path.contains("/ebill/")) {
                // GET /api/ebill/{resNo}  — guest access
                viewEBill(ex, lastSegment(ex)); return;
            }
            sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── BillingService.calculateBill(resNo) ───────────────────────────────
    private void calculateBill(HttpExchange ex, String resNo) throws IOException, SQLException {
        Reservation r = findReservation(resNo);
        if (r == null) {
            // [not found] showNotFound()
            sendError(ex, 404, "Reservation not found."); return;
        }
        // computeNights() → getRate() → computeTotal() → showBill(total)
        sendOk(ex, buildBillJson(r, true));
    }

    // ── BillingService.viewEBill(resNo) — guest ───────────────────────────
    private void viewEBill(HttpExchange ex, String resNo) throws IOException, SQLException {
        Reservation r = findReservation(resNo);
        if (r == null) {
            // [invalid/not found] showError("Invalid reservation number")
            sendError(ex, 404, "Invalid reservation number."); return;
        }
        // computeTotal() → displayEBill(total)
        sendOk(ex, buildBillJson(r, false));
    }

    // ── BillingService.printBill(resNo) ───────────────────────────────────
    private void printBill(HttpExchange ex, String resNo) throws IOException, SQLException {
        Reservation r = findReservation(resNo);
        if (r == null) { sendError(ex, 404, "Reservation not found."); return; }
        // In a real app this could trigger a PDF/printer job.
        // Here we return the bill data — the frontend calls window.print().
        // printSuccess()
        sendOk(ex, "{\"success\":true,\"message\":\"Bill ready to print.\",\"bill\":" +
                   buildBillJson(r, true) + "}");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Reservation findReservation(String resNo) throws SQLException {
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
            if (!rs.next()) return null;

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
            // computeNights()
            LocalDate in  = LocalDate.parse(r.checkIn);
            LocalDate out = LocalDate.parse(r.checkOut);
            r.nights = (int)(out.toEpochDay() - in.toEpochDay());
            // computeTotal()
            r.total  = r.nights * r.ratePerNight;
            return r;
        }
    }

    private String buildBillJson(Reservation r, boolean includeContact) {
        double tax   = r.total * 0.12;
        double grand = r.total + tax;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true");
        sb.append(",\"resNo\":\"").append(esc(r.resNo)).append("\"");
        sb.append(",\"guestName\":\"").append(esc(r.guestName)).append("\"");
        if (includeContact)
            sb.append(",\"guestContact\":\"")
              .append(esc(r.guestContact != null ? r.guestContact : "")).append("\"");
        sb.append(",\"roomType\":\"").append(esc(r.roomType)).append("\"");
        sb.append(",\"checkIn\":\"").append(esc(r.checkIn)).append("\"");
        sb.append(",\"checkOut\":\"").append(esc(r.checkOut)).append("\"");
        sb.append(",\"nights\":").append(r.nights);
        sb.append(String.format(",\"ratePerNight\":%.2f", r.ratePerNight));
        sb.append(String.format(",\"subtotal\":%.2f", r.total));
        sb.append(String.format(",\"tax\":%.2f", tax));
        sb.append(String.format(",\"total\":%.2f", grand));
        sb.append(",\"status\":\"").append(esc(r.status)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
