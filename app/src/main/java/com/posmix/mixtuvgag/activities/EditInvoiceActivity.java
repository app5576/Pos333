package com.posmix.mixtuvgag.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.adapters.InvoiceItemsAdapter;
import com.posmix.mixtuvgag.database.AppDatabase;
import com.posmix.mixtuvgag.models.CashTransaction;
import com.posmix.mixtuvgag.models.Customer;
import com.posmix.mixtuvgag.models.Invoice;
import com.posmix.mixtuvgag.models.InvoiceItem;
import com.posmix.mixtuvgag.utils.CurrencyHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EditInvoiceActivity extends AppCompatActivity {

    private static final String TAG = "EditInvoiceActivity";
    
    // UI Elements
    private Toolbar toolbar;
    private TextView tvInvoiceNumber, tvInvoiceDate, tvSubtotal, tvTotal;
    private EditText etDiscount, etPaidAmount;
    private TextView tvRemaining;
    private RecyclerView rvItems;
    private Button btnSave, btnDelete, btnAddItem;
    
    // Data
    private AppDatabase db;
    private Invoice currentInvoice;
    private List<InvoiceItem> itemsList = new ArrayList<>();
    private InvoiceItemsAdapter itemsAdapter;
    private boolean isModified = false;
    private Customer customer;
    
    // Invoice data from intent
    private int invoiceId;
    private String invoiceNumber;
    private long invoiceDate;
    private double invoiceSubtotal, invoiceDiscount, invoiceTotal, invoicePaid, invoiceRemaining;
    private int invoicePaymentStatus;
    private String invoiceNotes;
    private Integer customerId;
    private String customerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_invoice);
        
        db = AppDatabase.getInstance(this);
        
        // Receive all data from intent
        receiveIntentData();
        
        initViews();
        setupListeners();
        loadInvoiceData();
    }
    
    /**
     * Receive all data from Intent
     */
    private void receiveIntentData() {
        invoiceId = getIntent().getIntExtra("invoice_id", -1);
        invoiceNumber = getIntent().getStringExtra("invoice_number");
        invoiceDate = getIntent().getLongExtra("invoice_date", System.currentTimeMillis());
        invoiceSubtotal = getIntent().getDoubleExtra("invoice_subtotal", 0);
        invoiceDiscount = getIntent().getDoubleExtra("invoice_discount", 0);
        invoiceTotal = getIntent().getDoubleExtra("invoice_total", 0);
        invoicePaid = getIntent().getDoubleExtra("invoice_paid", 0);
        invoiceRemaining = getIntent().getDoubleExtra("invoice_remaining", 0);
        invoicePaymentStatus = getIntent().getIntExtra("invoice_payment_status", Invoice.STATUS_CREDIT);
        invoiceNotes = getIntent().getStringExtra("invoice_notes");
        customerId = getIntent().getIntExtra("customer_id", -1);
        customerName = getIntent().getStringExtra("customer_name");
        
        if (invoiceId == -1) {
            Toast.makeText(this, "خطأ: لم يتم العثور على الفاتورة", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تعديل الفاتورة");
        }
        
        tvInvoiceNumber = findViewById(R.id.tv_edit_invoice_number);
        tvInvoiceDate = findViewById(R.id.tv_edit_invoice_date);
        tvSubtotal = findViewById(R.id.tv_edit_subtotal);
        tvTotal = findViewById(R.id.tv_edit_total);
        etDiscount = findViewById(R.id.et_edit_discount);
        etPaidAmount = findViewById(R.id.et_edit_paid_amount);
        tvRemaining = findViewById(R.id.tv_edit_remaining);
        rvItems = findViewById(R.id.rv_edit_invoice_items);
        btnSave = findViewById(R.id.btn_save_invoice);
        btnDelete = findViewById(R.id.btn_delete_invoice);
        btnAddItem = findViewById(R.id.btn_add_item);
        
        // Setup RecyclerView
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        itemsAdapter = new InvoiceItemsAdapter(item -> showEditItemDialog(item));
        rvItems.setAdapter(itemsAdapter);
        
        // Display basic invoice info
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        tvInvoiceNumber.setText("رقم الفاتورة: " + (invoiceNumber != null ? invoiceNumber : ""));
        tvInvoiceDate.setText("تاريخ الفاتورة: " + sdf.format(new Date(invoiceDate)));
        
        // Set values
        etDiscount.setText(String.format(Locale.US, "%.2f", invoiceDiscount));
        etPaidAmount.setText(String.format(Locale.US, "%.2f", invoicePaid));
        
        updateCalculations();
    }
    
    private void setupListeners() {
        // Text watcher for discount and paid amount
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isModified = true;
                updateCalculations();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
        
        etDiscount.addTextChangedListener(watcher);
        etPaidAmount.addTextChangedListener(watcher);
        
        // Save button
        btnSave.setOnClickListener(v -> saveInvoiceChanges());
        
        // Delete button
        btnDelete.setOnClickListener(v -> confirmDeleteInvoice());
        
        // Add item button
        btnAddItem.setOnClickListener(v -> showAddItemDialog());
    }
    
    private void updateCalculations() {
        double discount = 0;
        double paid = 0;
        
        try {
            discount = Double.parseDouble(etDiscount.getText().toString());
        } catch (NumberFormatException e) {
            discount = 0;
        }
        
        try {
            paid = Double.parseDouble(etPaidAmount.getText().toString());
        } catch (NumberFormatException e) {
            paid = 0;
        }
        
        double newTotal = invoiceSubtotal - discount;
        double newRemaining = newTotal - paid;
        
        tvSubtotal.setText(String.format(Locale.US, "المجموع الفرعي: %.2f %s", invoiceSubtotal, CurrencyHelper.getSymbol()));
        tvTotal.setText(String.format(Locale.US, "الإجمالي: %.2f %s", newTotal, CurrencyHelper.getSymbol()));
        tvRemaining.setText(String.format(Locale.US, "المتبقي: %.2f %s", Math.max(0, newRemaining), CurrencyHelper.getSymbol()));
        
        if (newRemaining < 0) {
            tvRemaining.setTextColor(getColor(android.R.color.holo_red_dark));
        } else if (newRemaining == 0) {
            tvRemaining.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvRemaining.setTextColor(getColor(android.R.color.black));
        }
    }
    
    private void loadInvoiceData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                currentInvoice = db.invoiceDao().getInvoiceByIdSync(invoiceId);
                itemsList = db.invoiceDao().getItemsForInvoiceSync(invoiceId);
                
                if (customerId != null && customerId > 0) {
                    customer = db.customerDao().getByIdSync(customerId);
                }
                
                runOnUiThread(() -> {
                    if (currentInvoice != null) {
                        itemsAdapter.submitList(new ArrayList<>(itemsList));
                    } else {
                        Toast.makeText(EditInvoiceActivity.this, "لم يتم العثور على الفاتورة", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(EditInvoiceActivity.this, "خطأ في تحميل بيانات الفاتورة", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    private void saveInvoiceChanges() {
        double discount;
        double paid;
        
        try {
            discount = Double.parseDouble(etDiscount.getText().toString());
        } catch (NumberFormatException e) {
            discount = 0;
        }
        
        try {
            paid = Double.parseDouble(etPaidAmount.getText().toString());
        } catch (NumberFormatException e) {
            paid = 0;
        }
        
        double newTotal = invoiceSubtotal - discount;
        double newRemaining = newTotal - paid;
        
        if (newRemaining < 0) {
            Toast.makeText(this, "⚠️ المبلغ المدفوع لا يمكن أن يزيد عن إجمالي الفاتورة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determine new payment status
        int newPaymentStatus;
        if (newRemaining <= 0) {
            newPaymentStatus = Invoice.STATUS_CASH;
        } else if (paid > 0) {
            newPaymentStatus = Invoice.STATUS_PARTIAL;
        } else {
            newPaymentStatus = Invoice.STATUS_CREDIT;
        }
        
        final double finalDiscount = discount;
        final double finalPaid = paid;
        final double finalTotal = newTotal;
        final double finalRemaining = newRemaining;
        final int finalPaymentStatus = newPaymentStatus;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.getWritableDatabase().beginTransaction();
                
                // Update customer balance if payment status changed
                if (customer != null && currentInvoice != null) {
                    double oldRemaining = currentInvoice.getRemainingAmount();
                    double balanceChange = oldRemaining - finalRemaining;
                    
                    if (balanceChange != 0) {
                        double newBalance = customer.getCurrentBalance() - balanceChange;
                        customer.setCurrentBalance(Math.max(0, newBalance));
                        db.customerDao().update(customer);
                        
                        // Record cash transaction if payment was made
                        if (finalPaid > currentInvoice.getPaidAmount()) {
                            double additionalPaid = finalPaid - currentInvoice.getPaidAmount();
                            CashTransaction transaction = new CashTransaction();
                            transaction.setType(CashTransaction.TYPE_IN);
                            transaction.setAmount(additionalPaid);
                            transaction.setDate(System.currentTimeMillis());
                            transaction.setDescription("دفعة على الفاتورة - " + invoiceNumber);
                            transaction.setReferenceId(invoiceId);
                            transaction.setReferenceType("INVOICE_PAYMENT");
                            db.cashTransactionDao().insert(transaction);
                        }
                    }
                }
                
                // Update invoice
                if (currentInvoice != null) {
                    currentInvoice.setDiscount(finalDiscount);
                    currentInvoice.setTotal(finalTotal);
                    currentInvoice.setPaidAmount(finalPaid);
                    currentInvoice.setRemainingAmount(finalRemaining);
                    currentInvoice.setPaymentStatus(finalPaymentStatus);
                    db.invoiceDao().update(currentInvoice);
                }
                
                db.getWritableDatabase().setTransactionSuccessful();
                
                runOnUiThread(() -> {
                    Toast.makeText(EditInvoiceActivity.this, "✅ تم تحديث الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditInvoiceActivity.this, "❌ خطأ في حفظ التغييرات: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                db.getWritableDatabase().endTransaction();
            }
        });
    }
    
    private void confirmDeleteInvoice() {
        new AlertDialog.Builder(this)
            .setTitle("حذف الفاتورة")
            .setMessage("هل أنت متأكد من حذف هذه الفاتورة؟ لا يمكن التراجع عن هذا الإجراء.")
            .setPositiveButton("حذف", (dialog, which) -> deleteInvoice())
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    private void deleteInvoice() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.getWritableDatabase().beginTransaction();
                
                // Restore stock for invoice items
                if (itemsList != null) {
                    for (InvoiceItem item : itemsList) {
                        db.productDao().increaseStock(item.getProductId(), (int) item.getQuantity());
                    }
                }
                
                // Update customer balance if credit invoice
                if (customer != null && currentInvoice != null && 
                    currentInvoice.getPaymentStatus() == Invoice.STATUS_CREDIT) {
                    double newBalance = customer.getCurrentBalance() - currentInvoice.getRemainingAmount();
                    customer.setCurrentBalance(Math.max(0, newBalance));
                    db.customerDao().update(customer);
                }
                
                // Delete invoice items and invoice
                db.getWritableDatabase().delete("invoice_items", "invoice_id=?", new String[]{String.valueOf(invoiceId)});
                db.invoiceDao().delete(currentInvoice);
                
                db.getWritableDatabase().setTransactionSuccessful();
                
                runOnUiThread(() -> {
                    Toast.makeText(EditInvoiceActivity.this, "✅ تم حذف الفاتورة", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditInvoiceActivity.this, "❌ خطأ في حذف الفاتورة", Toast.LENGTH_SHORT).show());
            } finally {
                db.getWritableDatabase().endTransaction();
            }
        });
    }
    
    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        
        EditText etProductName = view.findViewById(R.id.et_new_item_name);
        EditText etQuantity = view.findViewById(R.id.et_new_item_qty);
        EditText etPrice = view.findViewById(R.id.et_new_item_price);
        
        // Clear fields
        etProductName.setText("");
        etQuantity.setText("1");
        etPrice.setText("");
        
        builder.setTitle("إضافة منتج")
            .setView(view)
            .setPositiveButton("إضافة", (dialog, which) -> {
                String name = etProductName.getText().toString().trim();
                String qtyStr = etQuantity.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "الرجاء إدخال اسم المنتج", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (qtyStr.isEmpty()) {
                    Toast.makeText(this, "الرجاء إدخال الكمية", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (priceStr.isEmpty()) {
                    Toast.makeText(this, "الرجاء إدخال السعر", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                double qty = 1;
                double price = 0;
                try {
                    qty = Double.parseDouble(qtyStr);
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "الرجاء إدخال قيم صحيحة", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (qty <= 0 || price <= 0) {
                    Toast.makeText(this, "الكمية والسعر يجب أن يكونا أكبر من صفر", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Add new item
                InvoiceItem newItem = new InvoiceItem();
                newItem.setInvoiceId(invoiceId);
                newItem.setProductName(name);
                newItem.setQuantity(qty);
                newItem.setUnitPrice(price);
                newItem.setTotalPrice(qty * price);
                newItem.setProductId(0); // Temporary ID for new item
                
                itemsList.add(newItem);
                itemsAdapter.submitList(new ArrayList<>(itemsList));
                isModified = true;
                
                // Update subtotal
                invoiceSubtotal += (qty * price);
                updateCalculations();
                
                Toast.makeText(this, "تم إضافة المنتج", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    private void showEditItemDialog(InvoiceItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        
        EditText etProductName = view.findViewById(R.id.et_new_item_name);
        EditText etQuantity = view.findViewById(R.id.et_new_item_qty);
        EditText etPrice = view.findViewById(R.id.et_new_item_price);
        
        // Set values for editing
        etProductName.setText(item.getProductName());
        etProductName.setEnabled(false); // Product name cannot be changed
        etQuantity.setText(String.valueOf((int) item.getQuantity()));
        etPrice.setText(String.format(Locale.US, "%.2f", item.getUnitPrice()));
        
        builder.setTitle("تعديل المنتج")
            .setView(view)
            .setPositiveButton("تحديث", (d, w) -> {
                try {
                    double oldTotal = item.getTotalPrice();
                    double newQty = Double.parseDouble(etQuantity.getText().toString());
                    double newPrice = Double.parseDouble(etPrice.getText().toString());
                    double newTotal = newQty * newPrice;
                    
                    // Update item
                    item.setQuantity(newQty);
                    item.setUnitPrice(newPrice);
                    item.setTotalPrice(newTotal);
                    
                    // Update subtotal
                    invoiceSubtotal = invoiceSubtotal - oldTotal + newTotal;
                    
                    itemsAdapter.submitList(new ArrayList<>(itemsList));
                    updateCalculations();
                    isModified = true;
                    
                    Toast.makeText(this, "تم تحديث المنتج", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "الرجاء إدخال قيم صحيحة", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .setNeutralButton("حذف", (d, w) -> {
                // Remove item
                double itemTotal = item.getTotalPrice();
                invoiceSubtotal -= itemTotal;
                itemsList.remove(item);
                itemsAdapter.submitList(new ArrayList<>(itemsList));
                updateCalculations();
                isModified = true;
                Toast.makeText(this, "تم حذف المنتج", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        if (isModified) {
            new AlertDialog.Builder(this)
                .setTitle("تغييرات غير محفوظة")
                .setMessage("هل تريد الخروج دون حفظ التغييرات؟")
                .setPositiveButton("نعم", (dialog, which) -> finish())
                .setNegativeButton("لا", null)
                .show();
            return true;
        }
        finish();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (isModified) {
            new AlertDialog.Builder(this)
                .setTitle("تغييرات غير محفوظة")
                .setMessage("هل تريد الخروج دون حفظ التغييرات؟")
                .setPositiveButton("نعم", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("لا", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
}