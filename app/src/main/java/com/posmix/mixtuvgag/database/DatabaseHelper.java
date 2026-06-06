package com.posmix.mixtuvgag.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.posmix.mixtuvgag.database.dao.EmployeeDao;
import com.posmix.mixtuvgag.database.dao.ExpenseCategoryDao;
import com.posmix.mixtuvgag.database.dao.PromotionDao;
import com.posmix.mixtuvgag.database.dao.ReturnInvoiceDao;
import com.posmix.mixtuvgag.database.dao.ShiftDao;
import com.posmix.mixtuvgag.models.Invoice;
import com.posmix.mixtuvgag.models.InvoiceItem;
import com.posmix.mixtuvgag.models.Product;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "micropos_db";
    private static final int DB_VERSION = 5;
    private static volatile DatabaseHelper INSTANCE;

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseHelper(context);
                }
            }
        }
        return INSTANCE;
    }

    // === New DAO Accessors (accept SQLiteDatabase directly) ===
    public EmployeeDao getEmployeeDao()            { return new EmployeeDao(getWritableDatabase()); }
    public ShiftDao getShiftDao()                  { return new ShiftDao(getWritableDatabase()); }
    public PromotionDao getPromotionDao()           { return new PromotionDao(getWritableDatabase()); }
    public ReturnInvoiceDao getReturnInvoiceDao()  { return new ReturnInvoiceDao(getWritableDatabase()); }
    public ExpenseCategoryDao getExpenseCategoryDao() { return new ExpenseCategoryDao(getWritableDatabase()); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;");
        createAllTables(db);
        insertDefaultData(db);
    }

    private void createAllTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT, barcode TEXT, category_id INTEGER DEFAULT 0, category_name TEXT," +
                "base_unit_id INTEGER DEFAULT 0, base_unit_name TEXT," +
                "buy_price REAL DEFAULT 0, sell_price REAL DEFAULT 0, wholesale_price REAL DEFAULT 0," +
                "stock_quantity INTEGER DEFAULT 0, min_stock_alert INTEGER DEFAULT 5," +
                "tax_percentage REAL DEFAULT 0, is_active INTEGER DEFAULT 1," +
                "image_path TEXT, notes TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, address TEXT," +
                "email TEXT, credit_limit REAL DEFAULT 10000, current_balance REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS suppliers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, address TEXT," +
                "current_balance REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS invoices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_number TEXT, type INTEGER," +
                "customer_id INTEGER, supplier_id INTEGER, date INTEGER," +
                "subtotal REAL DEFAULT 0, tax_amount REAL DEFAULT 0, discount REAL DEFAULT 0," +
                "total REAL DEFAULT 0, paid_amount REAL DEFAULT 0, remaining_amount REAL DEFAULT 0," +
                "payment_status INTEGER DEFAULT 1, printed INTEGER DEFAULT 0, notes TEXT," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id)," +
                "FOREIGN KEY(supplier_id) REFERENCES suppliers(id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS invoice_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER, product_id INTEGER," +
                "product_name TEXT, quantity REAL DEFAULT 0, unit_price REAL DEFAULT 0," +
                "tax_percentage REAL DEFAULT 0, tax_amount REAL DEFAULT 0, discount REAL DEFAULT 0," +
                "total REAL DEFAULT 0, notes TEXT," +
                "FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE," +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS cash_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER, amount REAL DEFAULT 0," +
                "reference_id INTEGER DEFAULT 0, reference_type TEXT, date INTEGER, description TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, category_id INTEGER DEFAULT 0," +
                "amount REAL DEFAULT 0, date INTEGER, notes TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS units (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, is_default INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS product_units (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER, unit_id INTEGER," +
                "unit_name TEXT, quantity INTEGER, barcode TEXT, sell_price REAL," +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE," +
                "FOREIGN KEY(unit_id) REFERENCES units(id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS employees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, username TEXT UNIQUE," +
                "password_hash TEXT, phone TEXT, email TEXT, role INTEGER DEFAULT 2," +
                "is_active INTEGER DEFAULT 1, salary REAL DEFAULT 0, notes TEXT, created_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS shifts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, employee_id INTEGER, status INTEGER DEFAULT 1," +
                "open_time INTEGER, close_time INTEGER DEFAULT 0, opening_cash REAL DEFAULT 0," +
                "closing_cash REAL DEFAULT 0, total_sales REAL DEFAULT 0, total_returns REAL DEFAULT 0," +
                "expected_cash REAL DEFAULT 0, cash_difference REAL DEFAULT 0, notes TEXT," +
                "FOREIGN KEY(employee_id) REFERENCES employees(id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS promotions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT," +
                "type INTEGER DEFAULT 1, product_id INTEGER DEFAULT 0, category_id INTEGER DEFAULT 0," +
                "discount_value REAL DEFAULT 0, min_amount REAL DEFAULT 0," +
                "buy_qty INTEGER DEFAULT 0, get_qty INTEGER DEFAULT 0," +
                "start_date INTEGER, end_date INTEGER, is_active INTEGER DEFAULT 1)");

        db.execSQL("CREATE TABLE IF NOT EXISTS return_invoices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER DEFAULT 1, return_number TEXT," +
                "original_invoice_id INTEGER, customer_id INTEGER, supplier_id INTEGER, date INTEGER," +
                "subtotal REAL DEFAULT 0, tax_amount REAL DEFAULT 0, discount REAL DEFAULT 0," +
                "total REAL DEFAULT 0, notes TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS return_invoice_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, return_invoice_id INTEGER, product_id INTEGER," +
                "original_item_id INTEGER DEFAULT 0, quantity INTEGER DEFAULT 1," +
                "unit_price REAL DEFAULT 0, tax_percentage REAL DEFAULT 0, tax_amount REAL DEFAULT 0," +
                "total REAL DEFAULT 0, reason TEXT," +
                "FOREIGN KEY(return_invoice_id) REFERENCES return_invoices(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, is_default INTEGER DEFAULT 0)");
    }

    private void insertDefaultData(SQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO units (id, name, is_default) VALUES (1,'حبة',1)");
        db.execSQL("INSERT OR IGNORE INTO units (id, name, is_default) VALUES (2,'كرتون',0)");
        db.execSQL("INSERT OR IGNORE INTO units (id, name, is_default) VALUES (3,'كيلو',0)");
        db.execSQL("INSERT OR IGNORE INTO units (id, name, is_default) VALUES (4,'لتر',0)");
        db.execSQL("INSERT OR IGNORE INTO units (id, name, is_default) VALUES (5,'علبة',0)");

        db.execSQL("INSERT OR IGNORE INTO categories (id, name, description) VALUES (1,'عام','منتجات عامة')");
        db.execSQL("INSERT OR IGNORE INTO categories (id, name, description) VALUES (2,'إلكترونيات','أجهزة إلكترونية')");
        db.execSQL("INSERT OR IGNORE INTO categories (id, name, description) VALUES (3,'ملابس','ملابس وأزياء')");
        db.execSQL("INSERT OR IGNORE INTO categories (id, name, description) VALUES (4,'مواد غذائية','أطعمة ومشروبات')");

        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('إيجار',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('رواتب',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('كهرباء وماء',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('هاتف وإنترنت',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('صيانة',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('مواصلات',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('مستلزمات مكتبية',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('تسويق وإعلان',1)");
        db.execSQL("INSERT OR IGNORE INTO expense_categories (name, is_default) VALUES ('أخرى',1)");

        long ts = System.currentTimeMillis();
        db.execSQL("INSERT OR IGNORE INTO employees (id,name,username,password_hash,role,is_active,created_at) " +
                "VALUES (1,'المدير العام','admin','a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',1,1," + ts + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS units (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, is_default INTEGER DEFAULT 0)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS product_units (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER, unit_id INTEGER, unit_name TEXT, quantity INTEGER, barcode TEXT, sell_price REAL, FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE, FOREIGN KEY(unit_id) REFERENCES units(id))"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN category_id INTEGER DEFAULT 0"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN category_name TEXT"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN base_unit_id INTEGER DEFAULT 0"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN base_unit_name TEXT"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN notes TEXT"); } catch (Exception e) {}
            insertDefaultData(db);
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE invoice_items ADD COLUMN notes TEXT"); } catch (Exception e) {}
        }
        if (oldVersion < 4) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS employees (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, username TEXT UNIQUE, password_hash TEXT, phone TEXT, email TEXT, role INTEGER DEFAULT 2, is_active INTEGER DEFAULT 1, salary REAL DEFAULT 0, notes TEXT, created_at INTEGER)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS shifts (id INTEGER PRIMARY KEY AUTOINCREMENT, employee_id INTEGER, status INTEGER DEFAULT 1, open_time INTEGER, close_time INTEGER DEFAULT 0, opening_cash REAL DEFAULT 0, closing_cash REAL DEFAULT 0, total_sales REAL DEFAULT 0, total_returns REAL DEFAULT 0, expected_cash REAL DEFAULT 0, cash_difference REAL DEFAULT 0, notes TEXT, FOREIGN KEY(employee_id) REFERENCES employees(id))"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS promotions (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT, type INTEGER DEFAULT 1, product_id INTEGER DEFAULT 0, category_id INTEGER DEFAULT 0, discount_value REAL DEFAULT 0, min_amount REAL DEFAULT 0, buy_qty INTEGER DEFAULT 0, get_qty INTEGER DEFAULT 0, start_date INTEGER, end_date INTEGER, is_active INTEGER DEFAULT 1)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS return_invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER DEFAULT 1, return_number TEXT, original_invoice_id INTEGER, customer_id INTEGER, supplier_id INTEGER, date INTEGER, subtotal REAL DEFAULT 0, tax_amount REAL DEFAULT 0, discount REAL DEFAULT 0, total REAL DEFAULT 0, notes TEXT)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS return_invoice_items (id INTEGER PRIMARY KEY AUTOINCREMENT, return_invoice_id INTEGER, product_id INTEGER, original_item_id INTEGER DEFAULT 0, quantity INTEGER DEFAULT 1, unit_price REAL DEFAULT 0, tax_percentage REAL DEFAULT 0, tax_amount REAL DEFAULT 0, total REAL DEFAULT 0, reason TEXT, FOREIGN KEY(return_invoice_id) REFERENCES return_invoices(id) ON DELETE CASCADE)"); } catch (Exception e) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, is_default INTEGER DEFAULT 0)"); } catch (Exception e) {}
            insertDefaultData(db);
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE products ADD COLUMN wholesale_price REAL DEFAULT 0"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN image_path TEXT"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE invoices ADD COLUMN notes TEXT"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE expenses ADD COLUMN category_id INTEGER DEFAULT 0"); } catch (Exception e) {}
            try { db.execSQL("ALTER TABLE invoice_items ADD COLUMN tax_amount REAL DEFAULT 0"); } catch (Exception e) {}
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

    // ========== Dashboard Helper Methods ==========

    public double getInvoiceTotalByDateRange(long from, long to, int type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(total),0) FROM invoices WHERE type=? AND date BETWEEN ? AND ?",
            new String[]{String.valueOf(type), String.valueOf(from), String.valueOf(to)});
        double v = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close(); return v;
    }

    public int getInvoiceCountByDateRange(long from, long to, int type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM invoices WHERE type=? AND date BETWEEN ? AND ?",
            new String[]{String.valueOf(type), String.valueOf(from), String.valueOf(to)});
        int v = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); return v;
    }

    public int getProductCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products WHERE is_active=1", null);
        int v = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); return v;
    }

    public int getLowStockCount() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM products WHERE is_active=1 AND stock_quantity <= min_stock_alert", null);
        int v = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); return v;
    }

    public int getCustomerCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers", null);
        int v = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); return v;
    }

    public double getTotalPendingDebts() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(remaining_amount),0) FROM invoices WHERE payment_status IN (2,3) AND remaining_amount>0", null);
        double v = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close(); return v;
    }

    public List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,stock_quantity,min_stock_alert FROM products " +
            "WHERE is_active=1 AND stock_quantity<=min_stock_alert LIMIT 10", null);
        while (c.moveToNext()) {
            Product p = new Product();
            p.setId(c.getInt(0)); p.setName(c.getString(1));
            p.setStockQuantity(c.getInt(2)); p.setMinStockAlert(c.getInt(3));
            list.add(p);
        }
        c.close(); return list;
    }

    public double getExpensesTotalByDateRange(long from, long to) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM expenses WHERE date BETWEEN ? AND ?",
            new String[]{String.valueOf(from), String.valueOf(to)});
        double v = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close(); return v;
    }

    public Invoice getInvoiceByNumber(String invoiceNumber) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM invoices WHERE invoice_number=?", new String[]{invoiceNumber});
        Invoice inv = null;
        if (c.moveToFirst()) {
            inv = new Invoice();
            inv.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            inv.setInvoiceNumber(c.getString(c.getColumnIndexOrThrow("invoice_number")));
            inv.setType(c.getInt(c.getColumnIndexOrThrow("type")));
            int ci = c.getColumnIndex("customer_id"); if (ci >= 0 && !c.isNull(ci)) inv.setCustomerId(c.getInt(ci));
            int si = c.getColumnIndex("supplier_id"); if (si >= 0 && !c.isNull(si)) inv.setSupplierId(c.getInt(si));
            inv.setDate(c.getLong(c.getColumnIndexOrThrow("date")));
            inv.setSubtotal(c.getDouble(c.getColumnIndexOrThrow("subtotal")));
            inv.setTaxAmount(c.getDouble(c.getColumnIndexOrThrow("tax_amount")));
            inv.setDiscount(c.getDouble(c.getColumnIndexOrThrow("discount")));
            inv.setTotal(c.getDouble(c.getColumnIndexOrThrow("total")));
            inv.setPaidAmount(c.getDouble(c.getColumnIndexOrThrow("paid_amount")));
            inv.setRemainingAmount(c.getDouble(c.getColumnIndexOrThrow("remaining_amount")));
            inv.setPaymentStatus(c.getInt(c.getColumnIndexOrThrow("payment_status")));
            inv.setItems(getInvoiceItems(db, inv.getId()));
        }
        c.close(); return inv;
    }

    private List<InvoiceItem> getInvoiceItems(SQLiteDatabase db, int invoiceId) {
        List<InvoiceItem> items = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT ii.*, p.name as prod_name FROM invoice_items ii " +
            "LEFT JOIN products p ON ii.product_id=p.id WHERE ii.invoice_id=?",
            new String[]{String.valueOf(invoiceId)});
        while (c.moveToNext()) {
            InvoiceItem item = new InvoiceItem();
            item.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            item.setInvoiceId(c.getInt(c.getColumnIndexOrThrow("invoice_id")));
            item.setProductId(c.getInt(c.getColumnIndexOrThrow("product_id")));
            item.setQuantity((int) c.getDouble(c.getColumnIndexOrThrow("quantity")));
            item.setUnitPrice(c.getDouble(c.getColumnIndexOrThrow("unit_price")));
            item.setTaxPercentage(c.getDouble(c.getColumnIndexOrThrow("tax_percentage")));
            item.setTotal(c.getDouble(c.getColumnIndexOrThrow("total")));
            int pn = c.getColumnIndex("prod_name");
            item.setProductName(pn >= 0 ? c.getString(pn) : c.getString(c.getColumnIndex("product_name")));
            items.add(item);
        }
        c.close(); return items;
    }

    public List<Invoice> getCreditInvoices(int filterType) {
        List<Invoice> list = new ArrayList<>();
        String extra = filterType == 1 ? " AND type=1" : filterType == 2 ? " AND type=2" : "";
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM invoices WHERE payment_status IN (2,3) AND remaining_amount>0" + extra + " ORDER BY date DESC", null);
        while (c.moveToNext()) {
            Invoice inv = new Invoice();
            inv.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            inv.setInvoiceNumber(c.getString(c.getColumnIndexOrThrow("invoice_number")));
            inv.setType(c.getInt(c.getColumnIndexOrThrow("type")));
            inv.setDate(c.getLong(c.getColumnIndexOrThrow("date")));
            inv.setTotal(c.getDouble(c.getColumnIndexOrThrow("total")));
            inv.setPaidAmount(c.getDouble(c.getColumnIndexOrThrow("paid_amount")));
            inv.setRemainingAmount(c.getDouble(c.getColumnIndexOrThrow("remaining_amount")));
            inv.setPaymentStatus(c.getInt(c.getColumnIndexOrThrow("payment_status")));
            list.add(inv);
        }
        c.close(); return list;
    }

    public void restoreStockForReturn(int productId, int qty) {
        getWritableDatabase().execSQL(
            "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?",
            new Object[]{qty, productId});
    }
}
