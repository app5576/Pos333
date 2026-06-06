package com.posmix.mixtuvgag.models;

public class Employee {
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_CASHIER = 2;
    public static final int ROLE_ACCOUNTANT = 3;
    public static final int ROLE_MANAGER = 4;

    private int id;
    private String name, username, passwordHash, phone, email;
    private int role;
    private boolean isActive = true;
    private long createdAt;
    private double salary;
    private String notes;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { name = n; }
    public String getUsername() { return username; } public void setUsername(String u) { username = u; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String p) { passwordHash = p; }
    public String getPhone() { return phone; } public void setPhone(String p) { phone = p; }
    public String getEmail() { return email; } public void setEmail(String e) { email = e; }
    public int getRole() { return role; } public void setRole(int r) { role = r; }
    public boolean isActive() { return isActive; } public void setActive(boolean a) { isActive = a; }
    public long getCreatedAt() { return createdAt; } public void setCreatedAt(long c) { createdAt = c; }
    public double getSalary() { return salary; } public void setSalary(double s) { salary = s; }
    public String getNotes() { return notes; } public void setNotes(String n) { notes = n; }

    public String getRoleName() {
        switch (role) {
            case ROLE_ADMIN: return "مدير عام";
            case ROLE_CASHIER: return "كاشير";
            case ROLE_ACCOUNTANT: return "محاسب";
            case ROLE_MANAGER: return "مشرف";
            default: return "غير محدد";
        }
    }
}
