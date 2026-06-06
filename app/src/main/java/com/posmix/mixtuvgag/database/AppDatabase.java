package com.posmix.mixtuvgag.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.posmix.mixtuvgag.database.dao.CashTransactionDao;
import com.posmix.mixtuvgag.database.dao.CategoryDao;
import com.posmix.mixtuvgag.database.dao.CustomerDao;
import com.posmix.mixtuvgag.database.dao.EmployeeDao;
import com.posmix.mixtuvgag.database.dao.ExpenseCategoryDao;
import com.posmix.mixtuvgag.database.dao.ExpenseDao;
import com.posmix.mixtuvgag.database.dao.InvoiceDao;
import com.posmix.mixtuvgag.database.dao.ProductDao;
import com.posmix.mixtuvgag.database.dao.ProductUnitDao;
import com.posmix.mixtuvgag.database.dao.PromotionDao;
import com.posmix.mixtuvgag.database.dao.ReturnInvoiceDao;
import com.posmix.mixtuvgag.database.dao.ShiftDao;
import com.posmix.mixtuvgag.database.dao.SupplierDao;
import com.posmix.mixtuvgag.database.dao.UnitDao;

public class AppDatabase {

    private static volatile AppDatabase INSTANCE;
    private final DatabaseHelper helper;

    private AppDatabase(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppDatabase(context);
                }
            }
        }
        return INSTANCE;
    }

    public static void resetInstance() {
        INSTANCE = null;
    }

    public SQLiteDatabase getWritableDatabase() { return helper.getWritableDatabase(); }
    public SQLiteDatabase getReadableDatabase()  { return helper.getReadableDatabase(); }

    // ===== Existing DAOs (accept AppDatabase) =====
    public ProductDao      productDao()      { return new ProductDao(this); }
    public CustomerDao     customerDao()     { return new CustomerDao(this); }
    public SupplierDao     supplierDao()     { return new SupplierDao(this); }
    public InvoiceDao      invoiceDao()      { return new InvoiceDao(this); }
    public CashTransactionDao cashTransactionDao() { return new CashTransactionDao(this); }
    public ExpenseDao      expenseDao()      { return new ExpenseDao(this); }
    public CategoryDao     categoryDao()     { return new CategoryDao(this); }
    public UnitDao         unitDao()         { return new UnitDao(this); }
    public ProductUnitDao  productUnitDao()  { return new ProductUnitDao(this); }

    // ===== New DAOs (accept SQLiteDatabase directly) =====
    public EmployeeDao        employeeDao()        { return new EmployeeDao(getWritableDatabase()); }
    public ShiftDao           shiftDao()           { return new ShiftDao(getWritableDatabase()); }
    public PromotionDao       promotionDao()       { return new PromotionDao(getWritableDatabase()); }
    public ReturnInvoiceDao   returnInvoiceDao()   { return new ReturnInvoiceDao(getWritableDatabase()); }
    public ExpenseCategoryDao expenseCategoryDao() { return new ExpenseCategoryDao(getWritableDatabase()); }
}
