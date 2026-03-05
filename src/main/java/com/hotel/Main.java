package com.hotel;

import com.hotel.handlers.AuthHandler;
import com.hotel.handlers.StaffHandler;
import com.sun.net.httpserver.HttpServer;
import com.hotel.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Hotel Reservation System — Java Backend (no frameworks)
 *
 * Dependencies (place JARs in lib/):
 *   mssql-jdbc-12.x.x.jre11.jar   — from https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
 *
 * Compile:
 *   javac -cp "lib/*" -sourcepath src -d out src/Main.java
 *
 * Run:
 *   java -cp "out;lib/*" Main
 *   (Windows Auth requires sqljdbc_auth.dll on system PATH or java.library.path)
 *
 * Server runs on http://localhost:8080
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ── Route registration ────────────────────────────────────────────
        // 1) Auth
        server.createContext("/api/auth/",    new AuthHandler());

        // 2) Staff CRUD  (Admin only — role check done on frontend; extend here for JWT)
        server.createContext("/api/staff/",   new StaffHandler());
        server.createContext("/api/staff",    new StaffHandler());

        // 3) Reservations
        server.createContext("/api/reservations/", new ReservationHandler());
        server.createContext("/api/reservations",  new ReservationHandler());
        server.createContext("/api/rates",          new ReservationHandler());

        // 4) Billing (staff)
        server.createContext("/api/billing/", new BillingHandler());

        // 5) E-Bill (guest — public)
        server.createContext("/api/ebill/",   new BillingHandler());

        // Thread pool
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  Hotel Reservation System — Backend    ║");
        System.out.println("║  Listening on http://localhost:" + PORT + "    ║");
        System.out.println("║  DB : DESKTOP-2PLMT2N\\SQLEXPRESS       ║");
        System.out.println("║  Auth: Windows Authentication          ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("Press Ctrl+C to stop.");
    }
}
