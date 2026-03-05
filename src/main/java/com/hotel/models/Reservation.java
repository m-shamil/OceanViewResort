package com.hotel.models;

public class Reservation {
    public int    id;
    public String resNo;
    public String guestName;
    public String guestContact;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public String status;
    public int    createdBy;
    public String createdAt;
    public double ratePerNight;
    public int    nights;
    public double total;

    public Reservation() {}

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"resNo\":\"%s\",\"guestName\":\"%s\"," +
            "\"guestContact\":\"%s\",\"roomType\":\"%s\"," +
            "\"checkIn\":\"%s\",\"checkOut\":\"%s\"," +
            "\"status\":\"%s\",\"createdBy\":%d," +
            "\"ratePerNight\":%.2f,\"nights\":%d,\"total\":%.2f," +
            "\"createdAt\":\"%s\"}",
            id, esc(resNo), esc(guestName), esc(guestContact),
            esc(roomType), esc(checkIn), esc(checkOut),
            esc(status), createdBy,
            ratePerNight, nights, total, esc(createdAt)
        );
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
