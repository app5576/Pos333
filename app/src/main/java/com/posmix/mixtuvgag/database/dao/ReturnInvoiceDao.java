package com.posmix.mixtuvgag.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.posmix.mixtuvgag.models.ReturnInvoice;
import com.posmix.mixtuvgag.models.ReturnInvoiceItem;
import java.util.ArrayList;
import java.util.List;

public class ReturnInvoiceDao {
    private final SQLiteDatabase db;

    public ReturnInvoiceDao(SQLiteDatabase db) { this.db = db; }

    public long insertReturnInvoice(ReturnInvoice r) {
        ContentValues cv = new ContentValues();
        cv.put("type", r.getType());
        cv.put("return_number", r.getReturnNumber());
        cv.put("original_invoice_id", r.getOriginalInvoiceId());
        cv.put("customer_id", r.getCustomerId());
        cv.put("supplier_id", r.getSupplierId());
        cv.put("date", r.getDate());
        cv.put("subtotal", r.getSubtotal());
        cv.put("tax_amount", r.getTaxAmount());
        cv.put("discount", r.getDiscount());
        cv.put("total", r.getTotal());
        cv.put("notes", r.getNotes());
        return db.insert("return_invoices", null, cv);
    }

    public long insertItem(ReturnInvoiceItem item) {
        ContentValues cv = new ContentValues();
        cv.put("return_invoice_id", item.getReturnInvoiceId());
        cv.put("product_id", item.getProductId());
        cv.put("original_item_id", item.getOriginalItemId());
        cv.put("quantity", item.getQuantity());
        cv.put("unit_price", item.getUnitPrice());
        cv.put("tax_percentage", item.getTaxPercentage());
        cv.put("tax_amount", item.getTaxAmount());
        cv.put("total", item.getTotal());
        cv.put("reason", item.getReason());
        return db.insert("return_invoice_items", null, cv);
    }

    public List<ReturnInvoice> getAll() {
        List<ReturnInvoice> list = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT r.*, i.invoice_number as orig_num, c.name as cust_name, s.name as supp_name " +
            "FROM return_invoices r " +
            "LEFT JOIN invoices i ON r.original_invoice_id=i.id " +
            "LEFT JOIN customers c ON r.customer_id=c.id " +
            "LEFT JOIN suppliers s ON r.supplier_id=s.id " +
            "ORDER BY r.date DESC", null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public List<ReturnInvoice> getByDateRange(long from, long to) {
        List<ReturnInvoice> list = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT r.*, i.invoice_number as orig_num, c.name as cust_name, s.name as supp_name " +
            "FROM return_invoices r " +
            "LEFT JOIN invoices i ON r.original_invoice_id=i.id " +
            "LEFT JOIN customers c ON r.customer_id=c.id " +
            "LEFT JOIN suppliers s ON r.supplier_id=s.id " +
            "WHERE r.date BETWEEN ? AND ? ORDER BY r.date DESC",
            new String[]{String.valueOf(from), String.valueOf(to)});
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public List<ReturnInvoiceItem> getItemsByReturnId(int returnId) {
        List<ReturnInvoiceItem> list = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT ri.*, p.name as prod_name FROM return_invoice_items ri " +
            "LEFT JOIN products p ON ri.product_id=p.id WHERE ri.return_invoice_id=?",
            new String[]{String.valueOf(returnId)});
        while (c.moveToNext()) {
            ReturnInvoiceItem item = new ReturnInvoiceItem();
            item.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            item.setReturnInvoiceId(c.getInt(c.getColumnIndexOrThrow("return_invoice_id")));
            item.setProductId(c.getInt(c.getColumnIndexOrThrow("product_id")));
            item.setOriginalItemId(c.getInt(c.getColumnIndexOrThrow("original_item_id")));
            item.setQuantity(c.getInt(c.getColumnIndexOrThrow("quantity")));
            item.setUnitPrice(c.getDouble(c.getColumnIndexOrThrow("unit_price")));
            item.setTaxPercentage(c.getDouble(c.getColumnIndexOrThrow("tax_percentage")));
            item.setTaxAmount(c.getDouble(c.getColumnIndexOrThrow("tax_amount")));
            item.setTotal(c.getDouble(c.getColumnIndexOrThrow("total")));
            item.setReason(c.getString(c.getColumnIndexOrThrow("reason")));
            int pIdx = c.getColumnIndex("prod_name");
            if (pIdx >= 0) item.setProductName(c.getString(pIdx));
            list.add(item);
        }
        c.close();
        return list;
    }

    public double getTotalReturnsByDateRange(long from, long to) {
        Cursor c = db.rawQuery("SELECT SUM(total) FROM return_invoices WHERE date BETWEEN ? AND ?",
            new String[]{String.valueOf(from), String.valueOf(to)});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        return total;
    }

    private ReturnInvoice fromCursor(Cursor c) {
        ReturnInvoice r = new ReturnInvoice();
        r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        r.setType(c.getInt(c.getColumnIndexOrThrow("type")));
        r.setReturnNumber(c.getString(c.getColumnIndexOrThrow("return_number")));
        r.setOriginalInvoiceId(c.getInt(c.getColumnIndexOrThrow("original_invoice_id")));
        r.setDate(c.getLong(c.getColumnIndexOrThrow("date")));
        r.setSubtotal(c.getDouble(c.getColumnIndexOrThrow("subtotal")));
        r.setTaxAmount(c.getDouble(c.getColumnIndexOrThrow("tax_amount")));
        r.setDiscount(c.getDouble(c.getColumnIndexOrThrow("discount")));
        r.setTotal(c.getDouble(c.getColumnIndexOrThrow("total")));
        r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        int custIdx = c.getColumnIndex("cust_name");
        if (custIdx >= 0) r.setCustomerName(c.getString(custIdx));
        int suppIdx = c.getColumnIndex("supp_name");
        if (suppIdx >= 0) r.setSupplierName(c.getString(suppIdx));
        int origIdx = c.getColumnIndex("orig_num");
        if (origIdx >= 0) r.setOriginalInvoiceNumber(c.getString(origIdx));
        return r;
    }
}
