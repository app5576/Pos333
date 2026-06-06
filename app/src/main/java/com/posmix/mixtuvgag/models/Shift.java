package com.posmix.mixtuvgag.models;

public class Shift {
    public static final int STATUS_OPEN = 1;
    public static final int STATUS_CLOSED = 2;

    private int id, employeeId, status;
    private String employeeName, notes;
    private long openTime, closeTime;
    private double openingCash, closingCash, totalSales, totalReturns, expectedCash;
    private double cashDifference;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; } public void setEmployeeId(int e) { employeeId = e; }
    public String getEmployeeName() { return employeeName; } public void setEmployeeName(String n) { employeeName = n; }
    public int getStatus() { return status; } public void setStatus(int s) { status = s; }
    public long getOpenTime() { return openTime; } public void setOpenTime(long t) { openTime = t; }
    public long getCloseTime() { return closeTime; } public void setCloseTime(long t) { closeTime = t; }
    public double getOpeningCash() { return openingCash; } public void setOpeningCash(double c) { openingCash = c; }
    public double getClosingCash() { return closingCash; } public void setClosingCash(double c) { closingCash = c; }
    public double getTotalSales() { return totalSales; } public void setTotalSales(double t) { totalSales = t; }
    public double getTotalReturns() { return totalReturns; } public void setTotalReturns(double t) { totalReturns = t; }
    public double getExpectedCash() { return expectedCash; } public void setExpectedCash(double e) { expectedCash = e; }
    public double getCashDifference() { return cashDifference; } public void setCashDifference(double d) { cashDifference = d; }
    public String getNotes() { return notes; } public void setNotes(String n) { notes = n; }
    public boolean isOpen() { return status == STATUS_OPEN; }
}
