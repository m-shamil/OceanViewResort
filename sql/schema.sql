-- ============================================
--  Hotel Reservation System - MSSQL Schema
--  Server: DESKTOP-2PLMT2N\SQLEXPRESS
--  Auth  : Windows Authentication
-- ============================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'HotelDB')
    CREATE DATABASE HotelDB;
GO

USE HotelDB;
GO

-- ── Users (Staff / Admin) ──────────────────
IF OBJECT_ID('Users','U') IS NULL
CREATE TABLE Users (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   NVARCHAR(50)  UNIQUE NOT NULL,
    password   NVARCHAR(255) NOT NULL,
    role       NVARCHAR(10)  NOT NULL CHECK (role IN ('admin','staff')),
    full_name  NVARCHAR(100) NOT NULL,
    email      NVARCHAR(100),
    status     NVARCHAR(10)  NOT NULL DEFAULT 'active'
                             CHECK (status IN ('active','inactive')),
    created_at DATETIME      DEFAULT GETDATE()
);
GO

-- ── Room Rates ─────────────────────────────
IF OBJECT_ID('RoomRates','U') IS NULL
CREATE TABLE RoomRates (
    room_type      NVARCHAR(50)   PRIMARY KEY,
    rate_per_night DECIMAL(10,2)  NOT NULL
);
GO

-- ── Reservations ───────────────────────────
IF OBJECT_ID('Reservations','U') IS NULL
CREATE TABLE Reservations (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    res_no        NVARCHAR(20)  UNIQUE NOT NULL,
    guest_name    NVARCHAR(100) NOT NULL,
    guest_contact NVARCHAR(50),
    room_type     NVARCHAR(50)  NOT NULL REFERENCES RoomRates(room_type),
    check_in      DATE          NOT NULL,
    check_out     DATE          NOT NULL,
    status        NVARCHAR(20)  NOT NULL DEFAULT 'confirmed'
                                CHECK (status IN ('confirmed','checked_in','checked_out','cancelled')),
    created_by    INT           REFERENCES Users(id),
    created_at    DATETIME      DEFAULT GETDATE(),
    CONSTRAINT chk_dates CHECK (check_out > check_in)
);
GO

-- ── Seed Data ──────────────────────────────
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'admin')
BEGIN
    INSERT INTO Users (username, password, role, full_name, email)
    VALUES ('admin', 'admin123', 'admin', 'System Administrator', 'admin@hotel.com');
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'staff1')
BEGIN
    INSERT INTO Users (username, password, role, full_name, email)
    VALUES ('staff1', 'staff123', 'staff', 'Maria Santos', 'maria@hotel.com');
END

IF NOT EXISTS (SELECT 1 FROM RoomRates WHERE room_type = 'Standard')
BEGIN
    INSERT INTO RoomRates VALUES
        ('Standard', 1500.00),
        ('Deluxe',   2500.00),
        ('Suite',    5000.00),
        ('Family',   3500.00);
END
GO

PRINT 'HotelDB schema created successfully.';
GO
