package com.posmix.mixtuvgag.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.posmix.mixtuvgag.models.Promotion;
import java.util.ArrayList;
import java.util.List;

public class PromotionDao {
    private final SQLiteDatabase db;

    public PromotionDao(SQLiteDatabase db) { this.db = db; }

    public long insert(Promotion p) { return db.insert("promotions", null, toCV(p)); }
    public int update(Promotion p) { return db.update("promotions", toCV(p), "id=?", new String[]{String.valueOf(p.getId())}); }
    public void delete(int id) { db.delete("promotions", "id=?", new String[]{String.valueOf(id)}); }

    public List<Promotion> getAll() {
        List<Promotion> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT p.*, pr.name as prod_name FROM promotions p LEFT JOIN products pr ON p.product_id=pr.id ORDER BY p.name ASC", null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public List<Promotion> getActive(long now) {
        List<Promotion> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT p.*, pr.name as prod_name FROM promotions p LEFT JOIN products pr ON p.product_id=pr.id WHERE p.is_active=1 AND p.start_date<=? AND p.end_date>=?", new String[]{String.valueOf(now), String.valueOf(now)});
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public Promotion getByCode(String code) {
        Cursor c = db.rawQuery("SELECT * FROM promotions WHERE code=? AND is_active=1", new String[]{code});
        Promotion p = null;
        if (c.moveToFirst()) p = fromCursor(c);
        c.close();
        return p;
    }

    private ContentValues toCV(Promotion p) {
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("code", p.getCode());
        cv.put("type", p.getType());
        cv.put("product_id", p.getProductId());
        cv.put("category_id", p.getCategoryId());
        cv.put("discount_value", p.getDiscountValue());
        cv.put("min_amount", p.getMinAmount());
        cv.put("buy_qty", p.getBuyQty());
        cv.put("get_qty", p.getGetQty());
        cv.put("start_date", p.getStartDate());
        cv.put("end_date", p.getEndDate());
        cv.put("is_active", p.isActive() ? 1 : 0);
        return cv;
    }

    private Promotion fromCursor(Cursor c) {
        Promotion p = new Promotion();
        p.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        p.setName(c.getString(c.getColumnIndexOrThrow("name")));
        p.setCode(c.getString(c.getColumnIndexOrThrow("code")));
        p.setType(c.getInt(c.getColumnIndexOrThrow("type")));
        p.setProductId(c.getInt(c.getColumnIndexOrThrow("product_id")));
        p.setCategoryId(c.getInt(c.getColumnIndexOrThrow("category_id")));
        p.setDiscountValue(c.getDouble(c.getColumnIndexOrThrow("discount_value")));
        p.setMinAmount(c.getDouble(c.getColumnIndexOrThrow("min_amount")));
        p.setBuyQty(c.getInt(c.getColumnIndexOrThrow("buy_qty")));
        p.setGetQty(c.getInt(c.getColumnIndexOrThrow("get_qty")));
        p.setStartDate(c.getLong(c.getColumnIndexOrThrow("start_date")));
        p.setEndDate(c.getLong(c.getColumnIndexOrThrow("end_date")));
        p.setActive(c.getInt(c.getColumnIndexOrThrow("is_active")) == 1);
        int pIdx = c.getColumnIndex("prod_name");
        if (pIdx >= 0) p.setProductName(c.getString(pIdx));
        return p;
    }
}
