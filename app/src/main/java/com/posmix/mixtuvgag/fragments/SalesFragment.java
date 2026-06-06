package com.posmix.mixtuvgag.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.adapters.CartHorizontalAdapter;
import com.posmix.mixtuvgag.adapters.SearchResultAdapter;
import com.posmix.mixtuvgag.models.CartItem;
import com.posmix.mixtuvgag.models.CashTransaction;
import com.posmix.mixtuvgag.models.Customer;
import com.posmix.mixtuvgag.models.Invoice;
import com.posmix.mixtuvgag.models.InvoiceItem;
import com.posmix.mixtuvgag.models.Product;
import com.posmix.mixtuvgag.viewmodels.SalesViewModel;
import com.posmix.mixtuvgag.database.AppDatabase;
import com.posmix.mixtuvgag.utils.InvoicePdfHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import androidx.recyclerview.widget.GridLayoutManager;

public class SalesFragment extends Fragment {
    private SalesViewModel viewModel;
    private CartHorizontalAdapter cartAdapter;
    private SearchResultAdapter searchAdapter;
    private List<CartItem> cart = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private List<Customer> allCustomers = new ArrayList<>();
    private EditText etSearch;
    private TextView tvTotal, tvEmptyCart;
    private Button btnCheckout;
    private String currencySymbol = "ر.س";
    private static final String TAG = "SalesFragment";
    private DatabaseReference firebaseRef;
    private View cardSearchResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null");
            return;
        }
        
        loadCurrencySettings();
        
        try {
            firebaseRef = FirebaseDatabase.getInstance().getReference();
        } catch (Exception e) {
            Log.e(TAG, "Firebase init error", e);
        }
        
        etSearch = view.findViewById(R.id.etSearch);
        tvTotal = view.findViewById(R.id.tvCartTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);
        cardSearchResults = view.findViewById(R.id.cardSearchResults);
        
        if (cardSearchResults != null) {
            cardSearchResults.setVisibility(View.GONE);
        }
        
        setupRecyclerViews(view);
        setupObservers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                filterSearch(s.toString()); 
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        searchAdapter.setOnProductClickListener(p -> {
            addItem(p);
            etSearch.setText("");
            if (cardSearchResults != null) {
                cardSearchResults.setVisibility(View.GONE);
            }
            hideKeyboard();
        });

        cartAdapter.setOnCartItemClickListener(item -> showEditCartItemDialog(item));
        btnCheckout.setOnClickListener(v -> showCheckoutDialog());
        
        updateUI();
    }
    
    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    private void setupRecyclerViews(View view) {
        try {
            // 1. Horizontal Cart RecyclerView
            RecyclerView rvCart = view.findViewById(R.id.rvCartHorizontal);
            if (rvCart != null) {
                cartAdapter = new CartHorizontalAdapter();
                rvCart.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvCart.setAdapter(cartAdapter);
            }

            // 2. Products RecyclerView (VERTICAL GRID)
            RecyclerView rvResults = view.findViewById(R.id.rvSearchResults);
            if (rvResults != null) {
                searchAdapter = new SearchResultAdapter();
                // استخدام GridLayoutManager بدلاً من LinearLayoutManager
                // الرقم 2 = عرض منتجين في كل صف (يمكنك تغييره إلى 1 أو 3)
                rvResults.setLayoutManager(new GridLayoutManager(getContext(), 2));
                rvResults.setAdapter(searchAdapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "RecyclerView error", e);
        }
    }

    private void setupObservers() {
        try {
            viewModel = new ViewModelProvider(this).get(SalesViewModel.class);
            viewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
                if (products != null) this.allProducts = products;
            });
            viewModel.getCustomers().observe(getViewLifecycleOwner(), customers -> {
                if (customers != null) {
                    this.allCustomers.clear();
                    this.allCustomers.addAll(customers);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Observer error", e);
        }
    }

    private void loadCurrencySettings() {
        if (getContext() != null) {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                currencySymbol = prefs.getString("currency", "ر.س");
            } catch (Exception e) {
                currencySymbol = "ر.س";
            }
        }
    }

    /**
     * توليد رقم الفاتورة محلياً فوراً (Offline-first).
     * Firebase يُحدَّث لاحقاً في الخلفية فقط للمزامنة.
     */
    private String generateLocalInvoiceNumber() {
        int lastLocal = getLastLocalInvoiceNumber();
        int fromPrefs = 0;
        if (getContext() != null) {
            try {
                fromPrefs = getContext()
                    .getSharedPreferences("invoice_counter", android.content.Context.MODE_PRIVATE)
                    .getInt("last_invoice_number", 0);
            } catch (Exception ignored) {}
        }
        int next = Math.max(Math.max(lastLocal, fromPrefs) + 1, 10);
        updateLocalCounter(next);
        return String.valueOf(next);
    }

    /** مزامنة العداد مع Firebase في الخلفية بعد الحفظ */
    private void syncCounterToFirebase(int number) {
        if (firebaseRef == null) return;
        try {
            DatabaseReference counterRef = firebaseRef.child("app_data").child("invoice_counter");
            counterRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(
                        @NonNull com.google.firebase.database.MutableData mutableData) {
                    Integer current = mutableData.getValue(Integer.class);
                    if (current == null || current < number) {
                        mutableData.setValue(number);
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }
                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed,
                        @Nullable DataSnapshot snapshot) {
                    if (error != null) Log.w(TAG, "Firebase counter sync: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "syncCounterToFirebase error", e);
        }
    }

    // DEPRECATED: استُبدلت بـ generateLocalInvoiceNumber - تُبقى للتوافقية
    private void generateNextInvoiceNumber(InvoiceNumberCallback callback) {
        if (firebaseRef == null) {
            // fallback to local if firebase is not available
            int lastLocal = getLastLocalInvoiceNumber();
            int next = Math.max(lastLocal + 1, 10);
            updateLocalCounter(next);
            callback.onNumberGenerated(String.valueOf(next));
            return;
        }

        DatabaseReference counterRef = firebaseRef.child("app_data").child("invoice_counter");
        counterRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {
                Integer current = mutableData.getValue(Integer.class);
                if (current == null || current < 10) {
                    // Start from 10 if not exists or current is less than 10
                    mutableData.setValue(10);
                } else {
                    mutableData.setValue(current + 1);
                }
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed && snapshot != null) {
                    Integer nextNumber = snapshot.getValue(Integer.class);
                    if (nextNumber != null) {
                        updateLocalCounter(nextNumber);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> callback.onNumberGenerated(String.valueOf(nextNumber)));
                        }
                    }
                } else {
                    // Transaction failed or was aborted, fallback to local logic
                    int lastLocal = getLastLocalInvoiceNumber();
                    int next = Math.max(lastLocal + 1, 10);
                    updateLocalCounter(next);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> callback.onNumberGenerated(String.valueOf(next)));
                    }
                }
            }
        });
    }
    
    private int getLastLocalInvoiceNumber() {
        int maxNumber = 0;
        try {
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<Invoice> invoices = db.invoiceDao().getAllSync();
            if (invoices != null) {
                for (Invoice inv : invoices) {
                    String num = inv.getInvoiceNumber();
                    if (num != null) {
                        try {
                            // Try parsing the whole string since we now use pure numbers
                            int n = Integer.parseInt(num.trim());
                            // Ignore excessively large numbers (likely old timestamps or similar)
                            if (n < 1000000000 && n > maxNumber) maxNumber = n; // Assuming invoice numbers won't exceed 1 billion
                        } catch (NumberFormatException e) {
                            // If it still has "INV-", try parsing the suffix for backward compatibility
                            if (num.startsWith("INV-")) {
                                try {
                            int n = Integer.parseInt(num.substring(4).trim());
                            // Ignore excessively large numbers (likely old timestamps or similar)
                            if (n < 1000000000 && n > maxNumber) maxNumber = n; // Assuming invoice numbers won't exceed 1 billion
                                } catch (NumberFormatException e2) { }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get last invoice error", e);
        }
        return maxNumber;
    }
    
    private void updateLocalCounter(int counter) {
        if (getContext() != null) {
            try {
                SharedPreferences prefs = getContext().getSharedPreferences("invoice_counter", android.content.Context.MODE_PRIVATE);
                // If the counter is excessively large (e.g., from old timestamp-based numbers),
                // reset it to a reasonable starting point (e.g., 9, so next invoice is 10).
                if (counter >= 1000000000) {
                    prefs.edit().putInt("last_invoice_number", 9).apply();
                    Log.w(TAG, "Resetting local invoice counter due to excessively large value.");
                } else {
                    prefs.edit().putInt("last_invoice_number", counter).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Update counter error", e);
            }
        }
    }
    
    interface InvoiceNumberCallback {
        void onNumberGenerated(String invoiceNumber);
    }

    // ============================================================
    // الدالة الرئيسية لعرض نافذة الدفع - Offline-first
    // ============================================================
    private void showCheckoutDialog() {
        if (getContext() == null) return;

        if (cart.isEmpty()) {
            Toast.makeText(getContext(), "❌ السلة فارغة! أضف منتجات أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // ✅ توليد رقم الفاتورة محلياً فوراً - لا انتظار
            final String invoiceNumber = generateLocalInvoiceNumber();

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_checkout, null);

            TextView tvTotalDisplay    = dialogView.findViewById(R.id.tv_total_display);
            TextView tvBalanceDisplay  = dialogView.findViewById(R.id.tv_balance_display);
            TextView tvCustomerBalance = dialogView.findViewById(R.id.tv_customer_balance);
            EditText etDiscount        = dialogView.findViewById(R.id.et_discount);
            EditText etPaid            = dialogView.findViewById(R.id.et_paid_amount);
            AutoCompleteTextView actvCustomer = dialogView.findViewById(R.id.actv_customer);
            RadioGroup rgPayment       = dialogView.findViewById(R.id.rg_payment_type);
            EditText etNotes           = dialogView.findViewById(R.id.et_notes);
            RadioButton rbCash         = dialogView.findViewById(R.id.rb_cash);

            // حساب المجموع
            double subtotal = 0;
            for (CartItem item : cart) subtotal += item.getFinalTotal();
            final double finalSubtotal = subtotal;

            // ✅ عرض رقم الفاتورة فوراً في عنوان الحوار
            tvTotalDisplay.setText(String.format(Locale.US,
                "الإجمالي: %.2f %s  |  فاتورة: %s", finalSubtotal, currencySymbol, invoiceNumber));

            etPaid.setText(String.format(Locale.US, "%.2f", finalSubtotal));
            etDiscount.setText("0");

            // ============================================================
            // مستمع لتغيير طريقة الدفع
            // ============================================================
            rgPayment.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_cash || checkedId == R.id.rb_card) {
                    etPaid.setText(String.format(Locale.US, "%.2f", finalSubtotal));
                    etPaid.setEnabled(true);
                    etPaid.setHint("المبلغ المدفوع");
                    // إخفاء رصيد العميل عند الدفع الفوري
                    if (tvCustomerBalance != null) tvCustomerBalance.setVisibility(View.GONE);
                } else if (checkedId == R.id.rb_credit) {
                    etPaid.setText("0");
                    etPaid.setEnabled(false);
                    etPaid.setHint("البيع الآجل - لا يوجد مبلغ مدفوع");
                    // إظهار رصيد العميل إن كان محدداً
                    if (tvCustomerBalance != null) {
                        showSelectedCustomerBalance(actvCustomer, tvCustomerBalance);
                    }
                }
                calculateRemaining(etDiscount, etPaid, tvBalanceDisplay, finalSubtotal);
            });

            TextWatcher checkWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    calculateRemaining(etDiscount, etPaid, tvBalanceDisplay, finalSubtotal);
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            etDiscount.addTextChangedListener(checkWatcher);
            etPaid.addTextChangedListener(checkWatcher);

            // ============================================================
            // ✅ إظهار رصيد العميل عند اختيار آجل وتحديد العميل
            // ============================================================
            if (allCustomers != null && !allCustomers.isEmpty()) {
                ArrayAdapter<Customer> customerAdapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_dropdown_item_1line, allCustomers);
                actvCustomer.setAdapter(customerAdapter);
            }

            actvCustomer.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // نظهر الرصيد فقط إذا كان الاختيار آجل
                    if (rgPayment.getCheckedRadioButtonId() == R.id.rb_credit && tvCustomerBalance != null) {
                        showSelectedCustomerBalance(actvCustomer, tvCustomerBalance);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            if (rbCash != null) rbCash.setChecked(true);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("💳 إتمام البيع - فاتورة: " + invoiceNumber)
                .setView(dialogView)
                .setPositiveButton("✅ تأكيد وحفظ", (d, which) -> {
                    String notes = (etNotes != null && etNotes.getText() != null)
                            ? etNotes.getText().toString().trim() : "";
                    // ✅ الحفظ فوري محلياً، Firebase يُزامن في الخلفية
                    saveInvoice(rgPayment, etDiscount, etPaid, actvCustomer, finalSubtotal, invoiceNumber, notes);
                })
                .setNegativeButton("إلغاء", null)
                .create();

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Checkout dialog error", e);
            Toast.makeText(getContext(), "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * يعرض رصيد العميل المحدد في حقل النص (للبيع الآجل فقط)
     */
    private void showSelectedCustomerBalance(AutoCompleteTextView actv, TextView tvBalance) {
        if (actv == null || tvBalance == null || allCustomers == null) return;
        String name = actv.getText() != null ? actv.getText().toString().trim() : "";
        if (name.isEmpty()) {
            tvBalance.setVisibility(View.GONE);
            return;
        }
        for (Customer c : allCustomers) {
            if (c != null && c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                double bal = c.getCurrentBalance();
                if (bal > 0) {
                    tvBalance.setText("⚠️ رصيد العميل السابق: " +
                        String.format(Locale.US, "%.2f", bal) + " " + currencySymbol);
                    tvBalance.setVisibility(View.VISIBLE);
                } else {
                    tvBalance.setText("✅ رصيد العميل: " +
                        String.format(Locale.US, "%.2f", bal) + " " + currencySymbol);
                    tvBalance.setVisibility(View.VISIBLE);
                }
                return;
            }
        }
        tvBalance.setVisibility(View.GONE);
    }
    
    // دالة مساعدة لحساب المبلغ المتبقي
    private void calculateRemaining(EditText etDiscount, EditText etPaid, TextView tvBalanceDisplay, double subtotal) {
        try {
            String discountText = etDiscount.getText() != null ? etDiscount.getText().toString() : "0";
            String paidText = etPaid.getText() != null ? etPaid.getText().toString() : "0";
            double discount = Double.parseDouble(cleanNumber(discountText.isEmpty() ? "0" : discountText));
            double paid = Double.parseDouble(cleanNumber(paidText.isEmpty() ? "0" : paidText));
            double remaining = subtotal - discount - paid;
            if (tvBalanceDisplay != null) {
                tvBalanceDisplay.setText(String.format(Locale.US, "المتبقي: %.2f %s", Math.max(0, remaining), currencySymbol));
            }
        } catch (Exception e) {
            Log.e(TAG, "Calculate remaining error", e);
        }
    }

    // ============================================================
    // دالة حفظ الفاتورة - تم تعديلها لمعالجة البيع الآجل بشكل صحيح
    // ============================================================
    private void saveInvoice(RadioGroup rg, EditText etDisc, EditText etPaid, AutoCompleteTextView actv, 
                             double subtotal, String invoiceNumber, String notes) {
        
        if (getContext() == null || getActivity() == null) {
            Log.e(TAG, "Context or Activity is null");
            return;
        }
        
        if (cart.isEmpty()) {
            Toast.makeText(getContext(), "السلة فارغة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Invoice inv = new Invoice();
            inv.setType(Invoice.TYPE_SALE);
            inv.setDate(System.currentTimeMillis());
            inv.setInvoiceNumber(invoiceNumber);
            if (notes != null && !notes.isEmpty()) inv.setNotes(notes);
            
            // حساب الخصم
            double disc = 0;
            try {
                String discountText = etDisc.getText() != null ? etDisc.getText().toString() : "0";
                disc = Double.parseDouble(cleanNumber(discountText.isEmpty() ? "0" : discountText));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Discount parse error", e);
            }
            
            inv.setSubtotal(subtotal);
            inv.setDiscount(disc);
            inv.setTotal(subtotal - disc);
            
            // تحديد حالة الدفع
            int checkedId = rg.getCheckedRadioButtonId();
            int paymentStatus;
            double paid = 0;
            
            if (checkedId == R.id.rb_cash) {
                paymentStatus = Invoice.STATUS_CASH;
                // دفع نقدي: جلب المبلغ المدخل من المستخدم
                try {
                    String paidText = etPaid.getText() != null ? etPaid.getText().toString() : "0";
                    paid = Double.parseDouble(cleanNumber(paidText.isEmpty() ? "0" : paidText));
                } catch (NumberFormatException e) {
                    paid = subtotal - disc;
                }
            } else if (checkedId == R.id.rb_card) {
                paymentStatus = Invoice.STATUS_CARD;
                // دفع بطاقة: جلب المبلغ المدخل من المستخدم
                try {
                    String paidText = etPaid.getText() != null ? etPaid.getText().toString() : "0";
                    paid = Double.parseDouble(cleanNumber(paidText.isEmpty() ? "0" : paidText));
                } catch (NumberFormatException e) {
                    paid = subtotal - disc;
                }
            } else {
                paymentStatus = Invoice.STATUS_CREDIT;
                // بيع آجل: المبلغ المدفوع = 0 دائماً
                paid = 0;
            }
            
            inv.setPaidAmount(paid);
            inv.setRemainingAmount(inv.getTotal() - paid);
            inv.setPaymentStatus(paymentStatus);

            // معالجة العميل
            String customerName = "";
            Integer customerId = null;
            
            if (actv != null && actv.getText() != null) {
                customerName = actv.getText().toString().trim();
            }
            
            if (!customerName.isEmpty() && allCustomers != null) {
                for (Customer c : allCustomers) {
                    if (c != null && c.getName() != null && c.getName().equalsIgnoreCase(customerName)) {
                        customerId = c.getId();
                        inv.setCustomerId(customerId);
                        break;
                    }
                }
            }

            // التحقق من صحة البيع الآجل
            if (paymentStatus == Invoice.STATUS_CREDIT) {
                if (customerId == null) {
                    Toast.makeText(getContext(), "⚠️ يجب اختيار عميل للبيع الآجل", Toast.LENGTH_LONG).show();
                    return;
                }
                if (inv.getRemainingAmount() <= 0) {
                    Toast.makeText(getContext(), "⚠️ المبلغ المتبقي يجب أن يكون أكبر من 0 للبيع الآجل", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            final int finalPaymentStatus = paymentStatus;
            final Integer finalCustomerId = customerId;
            final double finalPaid = paid;
            final String finalInvoiceNumber = invoiceNumber;

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = null;
                try {
                    db = AppDatabase.getInstance(getContext());
                    db.getWritableDatabase().beginTransaction();
                    
                    long invId = db.invoiceDao().insert(inv);
                    Log.d(TAG, "Invoice saved with ID: " + invId);
                    
                    if (invId <= 0) {
                        throw new Exception("Failed to insert invoice");
                    }
                    
                    List<InvoiceItem> itemsList = new ArrayList<>();
                    
                    for (CartItem ci : cart) {
                        InvoiceItem item = new InvoiceItem();
                        item.setInvoiceId((int)invId);
                        item.setProductId(ci.getProductId());
                        item.setProductName(ci.getProductName());
                        item.setQuantity(ci.getQuantity());
                        item.setUnitPrice(ci.getUnitPrice());
                        item.setTotalPrice(ci.getFinalTotal());
                        db.invoiceDao().insertItem(item);
                        db.productDao().decreaseStock(ci.getProductId(), (int) ci.getQuantity());
                        itemsList.add(item);
                    }

                    // تسجيل الحركة المالية للمدفوعات النقدية
                    if (finalPaymentStatus == Invoice.STATUS_CASH && finalPaid > 0) {
                        try {
                            CashTransaction trans = new CashTransaction();
                            trans.setType(CashTransaction.TYPE_IN);
                            trans.setAmount(finalPaid);
                            trans.setDate(System.currentTimeMillis());
                            trans.setDescription("مبيعات نقدية - فاتورة رقم: " + finalInvoiceNumber);
                            trans.setReferenceId((int)invId);
                            trans.setReferenceType("SALE");
                            db.cashTransactionDao().insert(trans);
                        } catch (Exception e) {
                            Log.e(TAG, "Cash transaction error", e);
                        }
                    }
                    
                    // تحديث رصيد العميل للبيع الآجل
                    if (finalPaymentStatus == Invoice.STATUS_CREDIT && finalCustomerId != null) {
                        try {
                            Customer customer = db.customerDao().getByIdSync(finalCustomerId);
                            if (customer != null) {
                                double oldBalance = customer.getCurrentBalance();
                                double newBalance = oldBalance + inv.getTotal();
                                customer.setCurrentBalance(newBalance);
                                db.customerDao().update(customer);
                                Log.d(TAG, "Customer balance updated: " + customer.getName() + " | " + oldBalance + " → " + newBalance);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Customer balance update error", e);
                        }
                    }
                    
                    db.getWritableDatabase().setTransactionSuccessful();
                    
                    // رفع الفاتورة إلى Firebase (في الخلفية - لا تؤثر على النتيجة)
                    uploadInvoiceToFirebase(inv, itemsList);
                    // مزامنة العداد مع Firebase
                    try { syncCounterToFirebase(Integer.parseInt(finalInvoiceNumber)); } catch (Exception ignored) {}
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String message;
                            if (finalPaymentStatus == Invoice.STATUS_CREDIT) {
                                message = "✅ تمت عملية البيع الآجل بنجاح\n" +
                                         "تم إضافة " + String.format("%.2f", inv.getTotal()) + " " + currencySymbol + " إلى حساب العميل\n" +
                                         "رقم الفاتورة: " + finalInvoiceNumber;
                            } else {
                                message = "✅ تمت عملية البيع بنجاح\nرقم الفاتورة: " + finalInvoiceNumber;
                            }
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            
                            try {
                                showPostSaleOptions(inv, itemsList);
                            } catch (Exception e) {
                                Log.e(TAG, "Post-sale options error", e);
                            }
                            
                            cart.clear();
                            updateUI();
                        });
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Save invoice error", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "❌ خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                } finally {
                    if (db != null) {
                        db.getWritableDatabase().endTransaction();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Save invoice general error", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "❌ خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }
    
    private void uploadInvoiceToFirebase(Invoice invoice, List<InvoiceItem> items) {
        if (firebaseRef == null) return;
        
        try {
            DatabaseReference invoiceRef = firebaseRef.child("app_data").child("invoices").child(String.valueOf(invoice.getId()));
            
            Map<String, Object> invoiceData = new HashMap<>();
            invoiceData.put("id", invoice.getId());
            invoiceData.put("invoiceNumber", invoice.getInvoiceNumber());
            invoiceData.put("type", invoice.getType());
            invoiceData.put("customerId", invoice.getCustomerId());
            invoiceData.put("date", invoice.getDate());
            invoiceData.put("subtotal", invoice.getSubtotal());
            invoiceData.put("taxAmount", invoice.getTaxAmount());
            invoiceData.put("discount", invoice.getDiscount());
            invoiceData.put("total", invoice.getTotal());
            invoiceData.put("paidAmount", invoice.getPaidAmount());
            invoiceData.put("remainingAmount", invoice.getRemainingAmount());
            invoiceData.put("paymentStatus", invoice.getPaymentStatus());
            invoiceData.put("notes", invoice.getNotes() != null ? invoice.getNotes() : "");
            
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (InvoiceItem item : items) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("productId", item.getProductId());
                itemData.put("productName", item.getProductName());
                itemData.put("quantity", item.getQuantity());
                itemData.put("unitPrice", item.getUnitPrice());
                itemData.put("total", item.getTotalPrice());
                itemsList.add(itemData);
            }
            invoiceData.put("items", itemsList);
            
            invoiceRef.setValue(invoiceData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Invoice uploaded to Firebase: " + invoice.getInvoiceNumber()))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase upload failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firebase upload error", e);
        }
    }

    private void showPostSaleOptions(Invoice invoice, List<InvoiceItem> items) {
        if (getContext() == null) return;
        
        String[] options = {"📄 فاتورة حرارية (PDF)", "💬 إرسال واتساب"};
        
        try {
            new AlertDialog.Builder(getContext())
                .setTitle("تم الحفظ بنجاح - " + invoice.getInvoiceNumber())
                .setItems(options, (dialog, which) -> {
                    try {
                        File pdfFile = InvoicePdfHelper.generateThermalPdf(getContext(), invoice, items);
                        if (pdfFile == null || !pdfFile.exists()) {
                            Toast.makeText(getContext(), "❌ خطأ في إنشاء PDF", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (which == 0) {
                            openFile(pdfFile);
                        } else {
                            shareToWhatsApp(pdfFile);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Post-sale action error", e);
                        Toast.makeText(getContext(), "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("إغلاق", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Post-sale dialog error", e);
            Toast.makeText(getContext(), "✅ تم حفظ الفاتورة: " + invoice.getInvoiceNumber(), Toast.LENGTH_LONG).show();
        }
    }

    private void openFile(File file) {
        if (file == null || getContext() == null) return;
        try {
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "لا يوجد تطبيق لفتح PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToWhatsApp(File file) {
        if (file == null || getContext() == null) return;
        try {
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "واتساب غير مثبت", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditCartItemDialog(CartItem item) {
        if (getContext() == null || item == null) return;
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_invoice_item_detail, null);

            TextView tvName = view.findViewById(R.id.tv_item_product_name);
            TextView tvTotalItem = view.findViewById(R.id.tv_item_total_price);
            EditText etQty = view.findViewById(R.id.et_item_quantity);
            EditText etPrice = view.findViewById(R.id.et_item_sell_price);
            Button btnDelete = view.findViewById(R.id.btn_remove_item);

            if (tvName != null) tvName.setText(item.getProductName());
            if (etQty != null) etQty.setText(String.valueOf(item.getQuantity()));
            if (etPrice != null) etPrice.setText(String.format(Locale.US, "%.2f", item.getUnitPrice()));

            TextWatcher watcher = new TextWatcher() {
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        String qtyText = etQty != null && etQty.getText() != null ? etQty.getText().toString() : "1";
                        String priceText = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString() : "0";
                        double q = Double.parseDouble(cleanNumber(qtyText));
                        double p = Double.parseDouble(cleanNumber(priceText));
                        if (tvTotalItem != null) {
                            tvTotalItem.setText(String.format(Locale.US, "%.2f %s", q * p, currencySymbol));
                        }
                    } catch (Exception e) { }
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            };
            
            if (etQty != null) etQty.addTextChangedListener(watcher);
            if (etPrice != null) etPrice.addTextChangedListener(watcher);
            
            // تحديث المجموع أول مرة
            if (etQty != null && etPrice != null && tvTotalItem != null) {
                try {
                    double q = Double.parseDouble(cleanNumber(etQty.getText().toString()));
                    double p = Double.parseDouble(cleanNumber(etPrice.getText().toString()));
                    tvTotalItem.setText(String.format(Locale.US, "%.2f %s", q * p, currencySymbol));
                } catch (Exception e) { }
            }

            AlertDialog dialog = builder.setView(view)
                    .setPositiveButton("تعديل", (d, w) -> {
                        try {
                            String qtyText = etQty != null && etQty.getText() != null ? etQty.getText().toString() : "1";
                            String priceText = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString() : "0";
                            item.setQuantity((int)Double.parseDouble(cleanNumber(qtyText)));
                            item.setUnitPrice(Double.parseDouble(cleanNumber(priceText)));
                            updateUI();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "خطأ في تحديث البيانات", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .create();

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> { 
                    cart.remove(item); 
                    updateUI(); 
                    dialog.dismiss(); 
                });
            }

            dialog.show();
            
            if (etQty != null) {
                etQty.post(() -> {
                    etQty.requestFocus();
                    etQty.selectAll();
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etQty, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Edit dialog error", e);
            Toast.makeText(getContext(), "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String cleanNumber(String input) {
        if (input == null || input.isEmpty()) return "0";
        String cleaned = input.replaceAll("[^0-9٠-٩.]", "");
        cleaned = cleaned.replace('١', '1').replace('٢', '2').replace('٣', '3')
                    .replace('٤', '4').replace('٥', '5').replace('٦', '6')
                    .replace('٧', '7').replace('٨', '8').replace('٩', '9')
                    .replace('٠', '0');
        if (cleaned.isEmpty()) return "0";
        return cleaned;
    }

    private void filterSearch(String q) {
        if (cardSearchResults == null) return;
        
        if (q.isEmpty()) { 
            cardSearchResults.setVisibility(View.GONE); 
            return; 
        }
        
        List<Product> f = new ArrayList<>();
        for (Product p : allProducts) {
            if (p != null && p.getName() != null) {
                if (p.getName().toLowerCase().contains(q.toLowerCase()) || 
                    (p.getBarcode() != null && p.getBarcode().contains(q))) {
                    f.add(p);
                }
            }
        }
        searchAdapter.submitList(f);
        cardSearchResults.setVisibility(f.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addItem(Product p) {
        if (p == null) return;
        
        boolean exists = false;
        for (CartItem item : cart) {
            if (item.getProductId() == p.getId()) { 
                item.setQuantity(item.getQuantity() + 1); 
                exists = true; 
                break; 
            }
        }
        if (!exists) {
            cart.add(new CartItem(p.getId(), p.getName(), p.getSellPrice(), p.getTaxPercentage(), 1));
        }
        updateUI();
    }

    private void updateUI() {
        if (cartAdapter != null) {
            cartAdapter.submitList(new ArrayList<>(cart));
        }
        
        double total = 0;
        for (CartItem i : cart) total += i.getFinalTotal();
        
        if (tvTotal != null) {
            tvTotal.setText(String.format(Locale.US, "%.2f %s", total, currencySymbol));
        }
        
        if (tvEmptyCart != null) {
            tvEmptyCart.setVisibility(cart.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrencySettings();
        updateUI();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cartAdapter != null) cartAdapter = null;
        if (searchAdapter != null) searchAdapter = null;
    }
}