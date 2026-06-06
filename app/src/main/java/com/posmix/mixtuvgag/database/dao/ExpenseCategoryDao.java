package com.posmix.mixtuvgag.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.posmix.mixtuvgag.models.ExpenseCategory;
import java.util.ArrayList;
import java.util.List;

public class ExpenseCategoryDao {
    private final SQLiteDatabase db;
    public ExpenseCategoryDao(SQLiteDatabase db) { this.db = db; }

    public long insert(ExpenseCategory ec) {
        ContentValues cv = new ContentValues();
        cv.put("name", ec.getName());
        cv.put("is_default", ec.isDefault() ? 1 : 0);
        return db.insert("expense_categories", null, cv);
    }

    public int update(ExpenseCategory ec) {
        ContentValues cv = new ContentValues();
        cv.put("name", ec.getName());
        return db.update("expense_categories", cv, "id=?", new String[]{String.valueOf(ec.getId())});
    }

    public void delete(int id) { db.delete("expense_categories", "id=? AND is_default=0", new String[]{String.valueOf(id)}); }

    public List<ExpenseCategory> getAll() {
        List<ExpenseCategory> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM expense_categories ORDER BY name ASC", null);
        while (c.moveToNext()) {
            ExpenseCategory ec = new ExpenseCategory();
            ec.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            ec.setName(c.getString(c.getColumnIndexOrThrow("name")));
            ec.setDefault(c.getInt(c.getColumnIndexOrThrow("is_default")) == 1);
            list.add(ec);
        }
        c.close();
        return list;
    }

    public List<String> getAllNames() {
        List<String> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT name FROM expense_categories ORDER BY name ASC", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }
}
