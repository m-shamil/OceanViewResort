package com.hotel.models;

public class User {
    public int    id;
    public String username;
    public String password;
    public String role;       // "admin" | "staff"
    public String fullName;
    public String email;
    public String status;     // "active" | "inactive"
    public String createdAt;

    public User() {}

    public User(int id, String username, String role,
                String fullName, String email, String status, String createdAt) {
        this.id        = id;
        this.username  = username;
        this.role      = role;
        this.fullName  = fullName;
        this.email     = email;
        this.status    = status;
        this.createdAt = createdAt;
    }

    /** Safe JSON — never exposes password */
    public String toJson() {
        return String.format(
            "{\"id\":%d,\"username\":\"%s\",\"role\":\"%s\"," +
            "\"fullName\":\"%s\",\"email\":\"%s\"," +
            "\"status\":\"%s\",\"createdAt\":\"%s\"}",
            id,
            esc(username), esc(role), esc(fullName),
            esc(email), esc(status), esc(createdAt)
        );
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
