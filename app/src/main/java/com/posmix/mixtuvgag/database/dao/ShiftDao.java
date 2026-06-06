package com.posmix.mixtuvgag.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.posmix.mixtuvgag.models.Shift;
import java.util.ArrayList;
import java.util.List;

public class ShiftDao {
    private final SQLiteDatabase db;

    public ShiftDao(SQLiteDatabase db) { this.db = db; }

    public long insert(Shift s) {
        return db.insert("shifts", null, toCV(s));
    }

    public int update(Shift s) {
        return db.update("shifts", toCV(s), "id=?", new String[]{String.valueOf(s.getId())});
    }

    public Shift getOpenShift() {
        Cursor c = db.rawQuery("SELECT s.*, e.name as emp_name FROM shifts s LEFT JOIN employees e ON s.employee_id=e.id WHERE s.status=1 ORDER BY s.open_time DESC LIMIT 1", null);
        Shift s = null;
        if (c.moveToFirst()) s = fromCursor(c);
        c.close();
        return s;
    }

    public List<Shift> getAll() {
        List<Shift> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT s.*, e.name as emp_name FROM shifts s LEFT JOIN employees e ON s.employee_id=e.id ORDER BY s.open_time DESC", null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public Shift getById(int id) {
        Cursor c = db.rawQuery("SELECT s.*, e.name as emp_name FROM shifts s LEFT JOIN employees e ON s.employee_id=e.id WHERE s.id=?", new String[]{String.valueOf(id)});
        Shift s = null;
        if (c.moveToFirst()) s = fromCursor(c);
        c.close();
        return s;
    }

    private ContentValues toCV(Shift s) {
        ContentValues cv = new ContentValues();
        cv.put("employee_id", s.getEmployeeId());
        cv.put("status", s.getStatus());
        cv.put("open_time", s.getOpenTime());
        cv.put("close_time", s.getCloseTime());
        cv.put("opening_cash", s.getOpeningCash());
        cv.put("closing_cash", s.getClosingCash());
        cv.put("total_sales", s.getTotalSales());
        cv.put("total_returns", s.getTotalReturns());
        cv.put("expected_cash", s.getExpectedCash());
        cv.put("cash_difference", s.getCashDifference());
        cv.put("notes", s.getNotes());
        return cv;
    }

    private Shift fromCursor(Cursor c) {
        Shift s = new Shift();
        s.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        s.setEmployeeId(c.getInt(c.getColumnIndexOrThrow("employee_id")));
        s.setStatus(c.getInt(c.getColumnIndexOrThrow("status")));
        s.setOpenTime(c.getLong(c.getColumnIndexOrThrow("open_time")));
        s.setCloseTime(c.getLong(c.getColumnIndexOrThrow("close_time")));
        s.setOpeningCash(c.getDouble(c.getColumnIndexOrThrow("opening_cash")));
        s.setClosingCash(c.getDouble(c.getColumnIndexOrThrow("closing_cash")));
        s.setTotalSales(c.getDouble(c.getColumnIndexOrThrow("total_sales")));
        s.setTotalReturns(c.getDouble(c.getColumnIndexOrThrow("total_returns")));
        s.setExpectedCash(c.getDouble(c.getColumnIndexOrThrow("expected_cash")));
        s.setCashDifference(c.getDouble(c.getColumnIndexOrThrow("cash_difference")));
        s.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        int empNameIdx = c.getColumnIndex("emp_name");
        if (empNameIdx >= 0) s.setEmployeeName(c.getString(empNameIdx));
        return s;
    }
}
