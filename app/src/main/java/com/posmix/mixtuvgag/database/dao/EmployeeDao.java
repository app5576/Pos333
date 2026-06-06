package com.posmix.mixtuvgag.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.posmix.mixtuvgag.models.Employee;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDao {
    private final SQLiteDatabase db;
    public EmployeeDao(SQLiteDatabase db) { this.db = db; }

    public long insert(Employee e)  { return db.insert("employees", null, toCV(e)); }
    public int  update(Employee e)  { return db.update("employees", toCV(e), "id=?", new String[]{String.valueOf(e.getId())}); }
    public void delete(int id)      { db.delete("employees", "id=?", new String[]{String.valueOf(id)}); }

    public List<Employee> getAll() {
        List<Employee> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM employees ORDER BY name ASC", null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close(); return list;
    }

    public List<Employee> getActive() {
        List<Employee> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM employees WHERE is_active=1 ORDER BY name ASC", null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close(); return list;
    }

    public Employee getById(int id) {
        Cursor c = db.rawQuery("SELECT * FROM employees WHERE id=?", new String[]{String.valueOf(id)});
        Employee e = null; if (c.moveToFirst()) e = fromCursor(c); c.close(); return e;
    }

    public Employee getByUsername(String username) {
        Cursor c = db.rawQuery("SELECT * FROM employees WHERE username=?", new String[]{username});
        Employee e = null; if (c.moveToFirst()) e = fromCursor(c); c.close(); return e;
    }

    public Employee getByUsernameAndPassword(String username, String passwordHash) {
        Cursor c = db.rawQuery(
            "SELECT * FROM employees WHERE username=? AND password_hash=? AND is_active=1",
            new String[]{username, passwordHash});
        Employee e = null; if (c.moveToFirst()) e = fromCursor(c); c.close(); return e;
    }

    private ContentValues toCV(Employee e) {
        ContentValues cv = new ContentValues();
        cv.put("name", e.getName()); cv.put("username", e.getUsername());
        cv.put("password_hash", e.getPasswordHash()); cv.put("phone", e.getPhone());
        cv.put("email", e.getEmail()); cv.put("role", e.getRole());
        cv.put("is_active", e.isActive() ? 1 : 0); cv.put("salary", e.getSalary());
        cv.put("notes", e.getNotes()); cv.put("created_at", e.getCreatedAt());
        return cv;
    }

    private Employee fromCursor(Cursor c) {
        Employee e = new Employee();
        e.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        e.setName(c.getString(c.getColumnIndexOrThrow("name")));
        e.setUsername(c.getString(c.getColumnIndexOrThrow("username")));
        e.setPasswordHash(c.getString(c.getColumnIndexOrThrow("password_hash")));
        e.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
        e.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        e.setRole(c.getInt(c.getColumnIndexOrThrow("role")));
        e.setActive(c.getInt(c.getColumnIndexOrThrow("is_active")) == 1);
        e.setSalary(c.getDouble(c.getColumnIndexOrThrow("salary")));
        e.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        e.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return e;
    }
}
