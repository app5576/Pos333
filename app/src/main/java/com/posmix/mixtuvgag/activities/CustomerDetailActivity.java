package com.posmix.mixtuvgag.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable; // تمت إضافتها لدعم @Nullable
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.adapters.CustomerHistoryAdapter;
import com.posmix.mixtuvgag.database.AppDatabase;
import com.posmix.mixtuvgag.models.CashTransaction;
import com.posmix.mixtuvgag.models.Customer;
import com.posmix.mixtuvgag.models.Invoice;
import com.posmix.mixtuvgag.models.InvoiceItem;
import com.posmix.mixtuvgag.utils.CurrencyHelper;
import com.posmix.mixtuvgag.viewmodels.CustomerDetailViewModel;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import com.posmix.mixtuvgag.databinding.ActivityCustomerDetailBinding;
import com.posmix.mixtuvgag.utils.EnhancedPrintHelper;

public class CustomerDetailActivity extends AppCompatActivity {

    private ActivityCustomerDetailBinding binding;
    private CustomerDetailViewModel viewModel;
    private int customerId;
    private Customer currentCustomer;
    private CustomerHistoryAdapter customerHistoryAdapter;

    // عرف الثابت في بداية الكلاس
    private static final int REQUEST_CODE_EDIT_INVOICE = 200;

    private static final Locale ENGLISH_LOCALE = Locale.US;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", ENGLISH_LOCALE);

    // لفلترة التاريخ
    private Calendar calFrom;
    private Calendar calTo;
    private boolean filterFromSet = false;
    private boolean filterToSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تفاصيل العميل");
        }

        customerId = getIntent().getIntExtra(CustomersActivity.EXTRA_CUSTOMER_ID, -1);
        if (customerId == -1) {
            Toast.makeText(this, "خطأ: لم يتم تحديد العميل.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(CustomerDetailViewModel.class);

        setupSwipeRefresh();
        setupCustomerDetails();
        setupCustomerHistoryRecyclerView();
        setupEditCustomerButton();
        setupPdfReportButton();
        setupPaymentButton();
        setupSyncButton();
        setupDateFilter();

        viewModel.loadCustomer(customerId);
        viewModel.loadOutstandingInvoices(customerId);
        viewModel.loadCustomerHistory(customerId);
    }

    // ==================== الإعداد ====================

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
                Color.parseColor("#1976D2"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#E91E63")
        );
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.syncData();
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "تم تحديث البيانات", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSyncButton() {
        binding.btnSync.setOnClickListener(v -> {
            binding.swipeRefreshLayout.setRefreshing(true);
            viewModel.syncData();
            binding.swipeRefreshLayout.postDelayed(() -> {
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show();
            }, 800);
        });
    }

    private void setupDateFilter() {
        calFrom = Calendar.getInstance();
        calTo = Calendar.getInstance();

        // اختيار تاريخ البداية
        binding.tvDateFrom.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calFrom.set(year, month, dayOfMonth, 0, 0, 0);
                        calFrom.set(Calendar.MILLISECOND, 0);
                        filterFromSet = true;
                        binding.tvDateFrom.setText(dateFormat.format(calFrom.getTime()));
                        binding.tvDateFrom.setTextColor(Color.parseColor("#1976D2"));
                    },
                    calFrom.get(Calendar.YEAR),
                    calFrom.get(Calendar.MONTH),
                    calFrom.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        // اختيار تاريخ النهاية
        binding.tvDateTo.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calTo.set(year, month, dayOfMonth, 23, 59, 59);
                        calTo.set(Calendar.MILLISECOND, 999);
                        filterToSet = true;
                        binding.tvDateTo.setText(dateFormat.format(calTo.getTime()));
                        binding.tvDateTo.setTextColor(Color.parseColor("#1976D2"));
                    },
                    calTo.get(Calendar.YEAR),
                    calTo.get(Calendar.MONTH),
                    calTo.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        // تطبيق الفلتر
        binding.btnApplyFilter.setOnClickListener(v -> {
            if (!filterFromSet || !filterToSet) {
                Toast.makeText(this, "الرجاء اختيار تاريخ البداية والنهاية", Toast.LENGTH_SHORT).show();
                return;
            }
            if (calFrom.getTimeInMillis() > calTo.getTimeInMillis()) {
                Toast.makeText(this, "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.loadCustomerHistoryFiltered(customerId, calFrom.getTimeInMillis(), calTo.getTimeInMillis());
            Toast.makeText(this, "تم تطبيق الفلتر", Toast.LENGTH_SHORT).show();
        });

        // إلغاء الفلتر
        binding.btnClearFilter.setOnClickListener(v -> {
            filterFromSet = false;
            filterToSet = false;
            binding.tvDateFrom.setText("اختر تاريخ");
            binding.tvDateTo.setText("اختر تاريخ");
            binding.tvDateFrom.setTextColor(Color.parseColor("#424242"));
            binding.tvDateTo.setTextColor(Color.parseColor("#424242"));
            viewModel.clearDateFilter();
            Toast.makeText(this, "تم إلغاء الفلتر", Toast.LENGTH_SHORT).show();
        });

        // مراقبة حالة الفلتر
        viewModel.getIsFilterActive().observe(this, isActive -> {
            if (isActive != null && isActive) {
                binding.tvFilterActiveBadge.setVisibility(View.VISIBLE);
                binding.tvFilterRangeLabel.setVisibility(View.VISIBLE);
                binding.tvFilterRangeLabel.setText(
                    dateFormat.format(new Date(viewModel.getFilterFromDate())) + " - " +
                    dateFormat.format(new Date(viewModel.getFilterToDate()))
                );
                binding.tvHistoryTitle.setText("الحسابات (مفلتر)");
            } else {
                binding.tvFilterActiveBadge.setVisibility(View.GONE);
                binding.tvFilterRangeLabel.setVisibility(View.GONE);
                binding.tvHistoryTitle.setText(getString(R.string.customer_detail_outstanding_invoices_title));
            }
        });
    }

    private void setupCustomerDetails() {
        viewModel.getCustomer().observe(this, customer -> {
            if (customer != null) {
                currentCustomer = customer;
                binding.tvDetailCustomerName.setText(customer.getName());
                binding.tvDetailCustomerPhone.setText(getString(R.string.customer_detail_phone_label) + " " + formatNumber(customer.getPhone() != null ? customer.getPhone() : getString(R.string.not_specified)));
                binding.tvDetailCustomerEmail.setText(getString(R.string.customer_detail_email_label) + " " + (customer.getEmail() != null ? customer.getEmail() : getString(R.string.not_specified)));
                binding.tvDetailCustomerAddress.setText(getString(R.string.customer_detail_address_label) + " " + (customer.getAddress() != null ? customer.getAddress() : getString(R.string.not_specified)));

                String balanceText = String.format(ENGLISH_LOCALE, "%s %.2f %s",
                        getString(R.string.customer_detail_balance_label),
                        customer.getCurrentBalance(), CurrencyHelper.getSymbol());
                binding.tvDetailCustomerBalance.setText(balanceText);

                if (customer.getCurrentBalance() > 0) {
                    binding.tvDetailCustomerBalance.setTextColor(getColor(android.R.color.holo_red_dark));
                } else {
                    binding.tvDetailCustomerBalance.setTextColor(getColor(R.color.primary));
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    List<Invoice> outstandingInvoicesList = db.invoiceDao().getCreditInvoicesByCustomerSync(customer.getId());
                    double totalCredit = db.invoiceDao().getTotalCreditByCustomer(customer.getId());

                    runOnUiThread(() -> {
                        binding.tvSummaryCount.setText(getString(R.string.customer_detail_unpaid_invoices_count, outstandingInvoicesList.size()));
                        binding.tvSummaryTotal.setText(String.format(ENGLISH_LOCALE, "%.2f %s", totalCredit, CurrencyHelper.getSymbol()));
                        binding.tvTotalCredit.setText(String.format(ENGLISH_LOCALE, "%s: %.2f %s",
                                getString(R.string.customer_detail_total_credit_label), totalCredit, CurrencyHelper.getSymbol()));
                    });
                });
            }
        });
    }

    private void setupCustomerHistoryRecyclerView() {
        binding.rvCustomerInvoices.setLayoutManager(new LinearLayoutManager(this));
        customerHistoryAdapter = new CustomerHistoryAdapter(new CustomerHistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onInvoiceClick(Invoice invoice) {
                showInvoiceOptionsDialog(invoice);
            }

            @Override
            public void onCashTransactionClick(CashTransaction transaction) {
                showCashTransactionOptionsDialog(transaction);
            }
        });
        binding.rvCustomerInvoices.setAdapter(customerHistoryAdapter);

        viewModel.getCustomerHistory().observe(this, history -> {
            if (history != null) {
                customerHistoryAdapter.submitList(history);
                if (history.isEmpty()) {
                    binding.tvEmptyInvoices.setVisibility(View.VISIBLE);
                    binding.rvCustomerInvoices.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyInvoices.setVisibility(View.GONE);
                    binding.rvCustomerInvoices.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupEditCustomerButton() {
        binding.btnEditCustomerDetail.setOnClickListener(v -> {
            if (currentCustomer != null) {
                showEditCustomerDialog(currentCustomer);
            }
        });
    }

    private void setupPdfReportButton() {
        binding.btnGeneratePdfReport.setOnClickListener(v -> {
            if (currentCustomer != null) {
                generateCustomerPdfReport(currentCustomer);
            }
        });
    }

    private void setupPaymentButton() {
        if (binding.btnMakePayment != null) {
            binding.btnMakePayment.setOnClickListener(v -> {
                if (currentCustomer != null) {
                    showPaymentDialogForCustomer();
                }
            });
        }
    }

    // ==================== حوارات الفواتير والسندات ====================

    /**
     * عرض خيارات الفاتورة (تعديل، طباعة، حذف)
     */
    private void showInvoiceOptionsDialog(Invoice invoice) {
        String[] options = {
            "✏️ تعديل الفاتورة",
            "🖨️ طباعة الفاتورة",
            "📄 تصدير PDF",
            "🗑️ حذف الفاتورة"
        };

        new AlertDialog.Builder(this)
            .setTitle("خيارات الفاتورة - " + invoice.getInvoiceNumber())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        // تعديل الفاتورة - نقل جميع البيانات
                        openEditInvoiceActivity(invoice);
                        break;
                    case 1:
                        printInvoice(invoice);
                        break;
                    case 2:
                        exportInvoiceToPdf(invoice);
                        break;
                    case 3:
                        confirmDeleteInvoice(invoice);
                        break;
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showCashTransactionOptionsDialog(CashTransaction transaction) {
        String[] options = {"تعديل السند", "طباعة السند", "حذف السند"};

        new AlertDialog.Builder(this)
            .setTitle("سند: " + transaction.getDescription())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: showEditPaymentDialog(transaction); break;
                    case 1: printPaymentReceipt(transaction); break;
                    case 2: confirmDeletePayment(transaction); break;
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    // ==================== تسديد الفواتير ====================

    private void showPaymentDialogForCustomer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        EditText etAmount = view.findViewById(R.id.et_payment_amount);

        TextView tvCurrentCredit = view.findViewById(R.id.tv_current_credit);
        if (tvCurrentCredit != null) {
            tvCurrentCredit.setVisibility(View.VISIBLE);
            tvCurrentCredit.setText(String.format(ENGLISH_LOCALE, "إجمالي الدين الحالي: %.2f %s",
                    currentCustomer.getCurrentBalance(), CurrencyHelper.getSymbol()));
        }

        builder.setTitle(getString(R.string.customer_detail_make_payment) + " - " + currentCustomer.getName())
                .setView(view)
                .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                    String str = etAmount.getText().toString().trim();
                    if (str.isEmpty()) { Toast.makeText(this, "الرجاء إدخال المبلغ", Toast.LENGTH_SHORT).show(); return; }
                    double amount = Double.parseDouble(str);
                    if (amount <= 0) { Toast.makeText(this, "الرجاء إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show(); return; }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        currentCustomer.setCurrentBalance(currentCustomer.getCurrentBalance() - amount);
                        db.customerDao().update(currentCustomer);

                        CashTransaction trans = new CashTransaction();
                        trans.setType(CashTransaction.TYPE_IN);
                        trans.setAmount(amount);
                        trans.setDate(System.currentTimeMillis());
                        trans.setDescription(getString(R.string.customer_detail_make_payment) + " من العميل: " + currentCustomer.getName());
                        trans.setReferenceId(currentCustomer.getId());
                        trans.setReferenceType("CUSTOMER_PAYMENT");
                        db.cashTransactionDao().insert(trans);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "تم التسديد بنجاح", Toast.LENGTH_SHORT).show();
                            viewModel.syncData();
                        });
                    });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showPaySpecificInvoiceDialog(Invoice invoice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        EditText etAmount = view.findViewById(R.id.et_payment_amount);
        TextView tvPaymentInfo = view.findViewById(R.id.tv_payment_info);

        tvPaymentInfo.setText(String.format(ENGLISH_LOCALE, "المبلغ المتبقي على فاتورة رقم %s: %.2f %s",
                invoice.getInvoiceNumber(), invoice.getRemainingAmount(), CurrencyHelper.getSymbol()));
        etAmount.setText(String.format(ENGLISH_LOCALE, "%.2f", invoice.getRemainingAmount()));

        builder.setTitle(getString(R.string.customer_detail_make_payment))
            .setView(view)
            .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                String str = etAmount.getText().toString().trim();
                if (str.isEmpty()) { Toast.makeText(this, "الرجاء إدخال المبلغ", Toast.LENGTH_SHORT).show(); return; }
                double paymentAmount = Double.parseDouble(str);
                if (paymentAmount <= 0) { Toast.makeText(this, "الرجاء إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show(); return; }

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    db.invoiceDao().recordPayment(invoice.getId(), paymentAmount);

                    if (currentCustomer != null) {
                        currentCustomer.setCurrentBalance(currentCustomer.getCurrentBalance() - paymentAmount);
                        db.customerDao().update(currentCustomer);
                    }

                    CashTransaction trans = new CashTransaction();
                    trans.setType(CashTransaction.TYPE_IN);
                    trans.setAmount(paymentAmount);
                    trans.setDate(System.currentTimeMillis());
                    trans.setDescription(String.format(ENGLISH_LOCALE, "تسديد فاتورة رقم %s من %s",
                            invoice.getInvoiceNumber(), currentCustomer.getName()));
                    trans.setReferenceId(invoice.getId());
                    trans.setReferenceType("INVOICE_PAYMENT");
                    db.cashTransactionDao().insert(trans);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "تم تسديد الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                        viewModel.syncData();
                    });
                });
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    // ==================== تعديل وحذف السندات ====================

    private void showEditPaymentDialog(CashTransaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cash_transaction, null);
        EditText etAmount = view.findViewById(R.id.et_transaction_amount);
        EditText etDesc = view.findViewById(R.id.et_transaction_desc);

        etAmount.setText(String.format(ENGLISH_LOCALE, "%.2f", transaction.getAmount()));
        etDesc.setText(transaction.getDescription());

        builder.setTitle("تعديل سند الدفعة")
            .setView(view)
            .setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                String amountStr = etAmount.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                if (amountStr.isEmpty()) { Toast.makeText(this, "الرجاء إدخال المبلغ", Toast.LENGTH_SHORT).show(); return; }
                try {
                    double oldAmount = transaction.getAmount();
                    double newAmount = Double.parseDouble(amountStr);
                    transaction.setAmount(newAmount);
                    transaction.setDescription(desc);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        db.cashTransactionDao().update(transaction);

                        if (currentCustomer != null && transaction.getType() == CashTransaction.TYPE_IN) {
                            double balanceDiff = newAmount - oldAmount;
                            currentCustomer.setCurrentBalance(currentCustomer.getCurrentBalance() - balanceDiff);
                            db.customerDao().update(currentCustomer);
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(this, "تم تعديل السند بنجاح", Toast.LENGTH_SHORT).show();
                            viewModel.syncData();
                        });
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "مبلغ غير صحيح", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void confirmDeletePayment(CashTransaction transaction) {
        new AlertDialog.Builder(this)
            .setTitle("حذف سند الدفعة")
            .setMessage("هل أنت متأكد من حذف هذا السند؟ سيتم عكس تأثيره على رصيد العميل.")
            .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);

                    if (currentCustomer != null && transaction.getType() == CashTransaction.TYPE_IN) {
                        currentCustomer.setCurrentBalance(currentCustomer.getCurrentBalance() + transaction.getAmount());
                        db.customerDao().update(currentCustomer);
                    }

                    db.cashTransactionDao().delete(transaction);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "تم حذف السند بنجاح", Toast.LENGTH_SHORT).show();
                        viewModel.syncData();
                    });
                });
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void printPaymentReceipt(CashTransaction transaction) {
        String receiptText = buildPaymentReceiptText(transaction, currentCustomer.getName(), "MicroPOS");
        Intent intent = new Intent(this, PrintActivity.class);
        intent.putExtra(PrintActivity.EXTRA_INVOICE_TEXT, receiptText);
        startActivity(intent);
    }

    private String buildPaymentReceiptText(CashTransaction transaction, String customerName, String storeName) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("        ").append(storeName).append("\n");
        sb.append("================================\n");
        sb.append("سند قبض\n");
        sb.append("--------------------------------\n");
        sb.append("الرقم: ").append(transaction.getId()).append("\n");
        sb.append("التاريخ: ").append(dateFormat.format(new Date(transaction.getDate()))).append("\n");
        sb.append("العميل: ").append(customerName).append("\n");
        sb.append("الوصف: ").append(transaction.getDescription()).append("\n");
        sb.append("--------------------------------\n");
        sb.append("المبلغ: ").append(String.format(Locale.US, "%.2f %s", transaction.getAmount(), CurrencyHelper.getSymbol())).append("\n");
        sb.append("================================\n");
        sb.append("       شكراً لتعاملكم معنا\n");
        sb.append("================================\n");
        return sb.toString();
    }

    // ==================== تعديل وحذف الفواتير ====================

    /**
     * فتح صفحة تعديل الفاتورة مع نقل جميع البيانات
     */
    private void openEditInvoiceActivity(Invoice invoice) {
        Intent intent = new Intent(this, EditInvoiceActivity.class);
        
        // نقل جميع بيانات الفاتورة الأساسية
        intent.putExtra("invoice_id", invoice.getId());
        intent.putExtra("invoice_number", invoice.getInvoiceNumber());
        intent.putExtra("invoice_type", invoice.getType());
        intent.putExtra("invoice_date", invoice.getDate());
        intent.putExtra("invoice_subtotal", invoice.getSubtotal());
        intent.putExtra("invoice_discount", invoice.getDiscount());
        intent.putExtra("invoice_total", invoice.getTotal());
        intent.putExtra("invoice_paid", invoice.getPaidAmount());
        intent.putExtra("invoice_remaining", invoice.getRemainingAmount());
        intent.putExtra("invoice_payment_status", invoice.getPaymentStatus());
        intent.putExtra("invoice_notes", invoice.getNotes() != null ? invoice.getNotes() : "");
        
        // نقل بيانات العميل إذا وجدت
        if (invoice.getCustomerId() != null) {
            intent.putExtra("customer_id", invoice.getCustomerId());
            // يمكن أيضاً جلب اسم العميل من قاعدة البيانات
            if (currentCustomer != null) {
                intent.putExtra("customer_name", currentCustomer.getName());
            }
        }
        
        startActivityForResult(intent, REQUEST_CODE_EDIT_INVOICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_EDIT_INVOICE && resultCode == RESULT_OK) {
            // تم تعديل الفاتورة بنجاح، قم بتحديث البيانات
            Toast.makeText(this, "تم تحديث الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
            refreshCustomerData(); // قم بتحديث عرض بيانات العميل والفاتورة
        }
    }

    /**
     * تحديث بيانات العميل بعد التعديل
     */
    private void refreshCustomerData() {
        if (viewModel != null && currentCustomer != null) {
            viewModel.loadCustomer(currentCustomer.getId());
            viewModel.loadOutstandingInvoices(currentCustomer.getId());
            viewModel.loadCustomerHistory(currentCustomer.getId());
        }
    }

    private void printInvoice(Invoice invoice) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<InvoiceItem> items = AppDatabase.getInstance(this)
                    .invoiceDao().getItemsForInvoiceSync(invoice.getId());

            String customerName = "عميل نقدي";
            if (invoice.getCustomerId() != null) {
                Customer customer = AppDatabase.getInstance(this).customerDao().getByIdSync(invoice.getCustomerId());
                if (customer != null) customerName = customer.getName();
            }

            String invoiceText = EnhancedPrintHelper.buildInvoiceText(
                    invoice, items, customerName, "MicroPOS",
                    EnhancedPrintHelper.PrinterSize.SIZE_58MM);

            final String finalText = invoiceText;
            runOnUiThread(() -> {
                Intent intent = new Intent(this, PrintActivity.class);
                intent.putExtra(PrintActivity.EXTRA_INVOICE_ID, invoice.getId());
                intent.putExtra(PrintActivity.EXTRA_INVOICE_TEXT, finalText);
                startActivity(intent);
            });
        });
    }

    /**
     * تصدير فاتورة واحدة إلى ملف PDF
     */
    private void exportInvoiceToPdf(Invoice invoice) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<InvoiceItem> items = AppDatabase.getInstance(this).invoiceDao().getItemsForInvoiceSync(invoice.getId());
                String customerName = "عميل نقدي";
                if (invoice.getCustomerId() != null && currentCustomer != null) {
                    customerName = currentCustomer.getName();
                }

                PdfDocument document = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                Paint paintTitle = new Paint();
                paintTitle.setTypeface(Typeface.DEFAULT_BOLD);
                paintTitle.setTextSize(18);
                paintTitle.setColor(Color.parseColor("#1565C0"));

                Paint paintNormal = new Paint();
                paintNormal.setTextSize(12);
                paintNormal.setColor(Color.DKGRAY);
                
                Paint paintBold = new Paint();
                paintBold.setTypeface(Typeface.DEFAULT_BOLD);
                paintBold.setTextSize(12);
                paintBold.setColor(Color.BLACK);

                int y = 40;
                int margin = 30;

                canvas.drawText("فاتورة رقم: " + invoice.getInvoiceNumber(), margin, y, paintTitle);
                y += 30;
                canvas.drawText("التاريخ: " + dateFormat.format(new Date(invoice.getDate())), margin, y, paintNormal);
                y += 20;
                canvas.drawText("العميل: " + customerName, margin, y, paintNormal);
                y += 30;

                canvas.drawText("الأصناف:", margin, y, paintBold);
                y += 20;

                if (items != null) {
                    for (InvoiceItem item : items) {
                        String itemText = String.format(Locale.US, "• %s | %d x %.2f = %.2f",
                                item.getProductName(), (int) item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
                        canvas.drawText(itemText, margin, y, paintNormal);
                        y += 20;
                    }
                }

                y += 10;
                canvas.drawText("المجموع: " + String.format(Locale.US, "%.2f", invoice.getSubtotal()), margin, y, paintBold);
                y += 20;
                canvas.drawText("الخصم: " + String.format(Locale.US, "%.2f", invoice.getDiscount()), margin, y, paintBold);
                y += 20;
                canvas.drawText("الإجمالي: " + String.format(Locale.US, "%.2f", invoice.getTotal()), margin, y, paintBold);
                y += 20;
                canvas.drawText("المدفوع: " + String.format(Locale.US, "%.2f", invoice.getPaidAmount()), margin, y, paintBold);
                y += 20;
                canvas.drawText("المتبقي: " + String.format(Locale.US, "%.2f", invoice.getRemainingAmount()), margin, y, paintBold);

                document.finishPage(page);

                String fileName = "invoice_" + invoice.getInvoiceNumber() + "_" + System.currentTimeMillis() + ".pdf";
                File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir == null) {
                    runOnUiThread(() -> Toast.makeText(this, "خطأ في الوصول لمجلد التنزيلات", Toast.LENGTH_SHORT).show());
                    document.close();
                    return;
                }
                File file = new File(downloadsDir, fileName);
                document.writeTo(new FileOutputStream(file));
                document.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "تم تصدير الفاتورة إلى PDF بنجاح", Toast.LENGTH_LONG).show();
                    Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "مشاركة الفاتورة"));
                });

            } catch (Exception e) {
                Log.e("CustomerDetail", "Error exporting invoice PDF", e);
                runOnUiThread(() -> Toast.makeText(this, "خطأ في تصدير PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // تم تغيير الاسم من showDeleteInvoiceConfirmation إلى confirmDeleteInvoice ليتطابق مع الكود الجديد
    private void confirmDeleteInvoice(Invoice invoice) {
        new AlertDialog.Builder(this)
            .setTitle("حذف الفاتورة")
            .setMessage("هل أنت متأكد من حذف الفاتورة رقم " + invoice.getInvoiceNumber() + "؟ لا يمكن التراجع عن هذا الإجراء.")
            .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> deleteInvoice(invoice))
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    private void deleteInvoice(Invoice invoice) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            try {
                db.getWritableDatabase().beginTransaction();

                List<InvoiceItem> items = db.invoiceDao().getItemsForInvoiceSync(invoice.getId());
                if (items != null) {
                    for (InvoiceItem item : items) {
                        db.productDao().increaseStock(item.getProductId(), (int) item.getQuantity());
                    }
                }

                if (currentCustomer != null) {
                    if (invoice.getRemainingAmount() > 0 || invoice.getPaymentStatus() == Invoice.STATUS_CREDIT) {
                        currentCustomer.setCurrentBalance(currentCustomer.getCurrentBalance() - invoice.getRemainingAmount());
                        db.customerDao().update(currentCustomer);
                    }
                }

                db.getWritableDatabase().delete("invoice_items", "invoice_id=?", new String[]{String.valueOf(invoice.getId())});
                db.invoiceDao().delete(invoice);
                db.getWritableDatabase().setTransactionSuccessful();

                runOnUiThread(() -> {
                    Toast.makeText(this, "تم حذف الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                    refreshCustomerData(); // تحديث البيانات بعد الحذف
                });
            } catch (Exception e) {
                Log.e("CustomerDetailActivity", "Error deleting invoice", e);
                runOnUiThread(() -> Toast.makeText(this, "خطأ في حذف الفاتورة: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                db.getWritableDatabase().endTransaction();
            }
        });
    }

    private void showInvoiceDetails(Invoice invoice) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<InvoiceItem> items = AppDatabase.getInstance(this).invoiceDao().getItemsForInvoiceSync(invoice.getId());

            StringBuilder details = new StringBuilder();
            details.append("تفاصيل الفاتورة\n\n");
            details.append("رقم الفاتورة: ").append(formatNumber(invoice.getInvoiceNumber())).append("\n");
            details.append("التاريخ: ").append(dateFormat.format(new Date(invoice.getDate()))).append("\n");

            String customerName = "عميل نقدي";
            if (invoice.getCustomerId() != null) {
                Customer customer = AppDatabase.getInstance(this).customerDao().getByIdSync(invoice.getCustomerId());
                if (customer != null) customerName = customer.getName();
            }
            details.append("العميل: ").append(customerName).append("\n\n");
            details.append("--- الأصناف ---\n");

            if (items != null) {
                for (InvoiceItem item : items) {
                    details.append("• ").append(item.getProductName())
                           .append(" | ").append(String.format(Locale.US, "%d", (int) item.getQuantity()))
                           .append(" x ").append(String.format(Locale.US, "%.2f", item.getUnitPrice()))
                           .append(" = ").append(String.format(Locale.US, "%.2f", item.getTotalPrice())).append("\n");
                }
            }

            details.append("\n--- الملخص ---\n");
            details.append("المجموع: ").append(String.format(Locale.US, "%.2f", invoice.getSubtotal())).append("\n");
            details.append("الخصم: ").append(String.format(Locale.US, "%.2f", invoice.getDiscount())).append("\n");
            details.append("الإجمالي: ").append(String.format(Locale.US, "%.2f", invoice.getTotal())).append("\n");
            details.append("المدفوع: ").append(String.format(Locale.US, "%.2f", invoice.getPaidAmount())).append("\n");
            details.append("المتبقي: ").append(String.format(Locale.US, "%.2f", invoice.getRemainingAmount())).append("\n");

            String status;
            switch (invoice.getPaymentStatus()) {
                case Invoice.STATUS_CASH: status = "نقدي"; break;
                case Invoice.STATUS_CREDIT: status = "آجل"; break;
                case Invoice.STATUS_PARTIAL: status = "جزئي"; break;
                case Invoice.STATUS_CARD: status = "بطاقة"; break;
                default: status = "غير معروف";
            }
            details.append("الحالة: ").append(status).append("\n");

            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                    .setTitle("تفاصيل الفاتورة")
                    .setMessage(details.toString())
                    .setPositiveButton("موافق", null)
                    .setNegativeButton("تعديل", (d, w) -> openEditInvoiceActivity(invoice))
                    .show();
            });
        });
    }

    // ==================== تعديل بيانات العميل ====================

    private void showEditCustomerDialog(Customer customer) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_customer, null);
        EditText etName = view.findViewById(R.id.et_customer_name);
        EditText etPhone = view.findViewById(R.id.et_customer_phone);
        EditText etAddress = view.findViewById(R.id.et_customer_address);
        EditText etEmail = view.findViewById(R.id.et_customer_email);

        etName.setText(customer.getName());
        etPhone.setText(customer.getPhone());
        etAddress.setText(customer.getAddress());
        etEmail.setText(customer.getEmail());

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.customer_detail_edit_customer))
            .setView(view)
            .setPositiveButton(getString(R.string.btn_save), (d, w) -> {
                customer.setName(etName.getText().toString().trim());
                customer.setPhone(etPhone.getText().toString().trim());
                customer.setAddress(etAddress.getText().toString().trim());
                customer.setEmail(etEmail.getText().toString().trim());
                viewModel.updateCustomer(customer);
                Toast.makeText(this, getString(R.string.success_message), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show();
    }

    // ==================== إنشاء PDF محسّن ====================

    private void generateCustomerPdfReport(Customer customer) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PdfDocument document = new PdfDocument();

                // إعداد الصفحة بحجم A4
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                Paint paintTitle = new Paint();
                paintTitle.setTypeface(Typeface.DEFAULT_BOLD);
                paintTitle.setTextSize(18);
                paintTitle.setColor(Color.parseColor("#1565C0"));

                Paint paintHeader = new Paint();
                paintHeader.setTypeface(Typeface.DEFAULT_BOLD);
                paintHeader.setTextSize(13);
                paintHeader.setColor(Color.BLACK);

                Paint paintNormal = new Paint();
                paintNormal.setTextSize(11);
                paintNormal.setColor(Color.DKGRAY);

                Paint paintLine = new Paint();
                paintLine.setColor(Color.parseColor("#BDBDBD"));
                paintLine.setStrokeWidth(1);

                Paint paintRed = new Paint();
                paintRed.setTextSize(11);
                paintRed.setColor(Color.parseColor("#C62828"));

                Paint paintGreen = new Paint();
                paintGreen.setTextSize(11);
                paintGreen.setColor(Color.parseColor("#2E7D32"));

                Paint paintBg = new Paint();
                paintBg.setColor(Color.parseColor("#E3F2FD"));

                int pageWidth = 595;
                int margin = 30;
                int y = 40;

                // رأس الصفحة
                canvas.drawRect(0, 0, pageWidth, 70, paintBg);
                canvas.drawText("كشف حساب عميل", margin, y, paintTitle);
                y += 20;
                canvas.drawText("MicroPOS", margin, y, paintNormal);
                String generatedDate = "تاريخ الطباعة: " + dateFormat.format(new Date());
                canvas.drawText(generatedDate, margin, y + 15, paintNormal);
                y += 50;

                // خط فاصل
                canvas.drawLine(margin, y, pageWidth - margin, y, paintLine);
                y += 15;

                // معلومات العميل
                canvas.drawText("معلومات العميل", margin, y, paintHeader);
                y += 18;
                canvas.drawText("الاسم: " + customer.getName(), margin, y, paintNormal);
                y += 16;
                canvas.drawText("الهاتف: " + (customer.getPhone() != null ? customer.getPhone() : "-"), margin, y, paintNormal);
                y += 16;
                canvas.drawText("العنوان: " + (customer.getAddress() != null ? customer.getAddress() : "-"), margin, y, paintNormal);
                y += 16;

                // فلتر التاريخ إذا كان نشطاً
                Boolean filterActive = viewModel.getIsFilterActive().getValue();
                String dateRangeInfo = "";
                if (filterActive != null && filterActive) {
                    dateRangeInfo = "الفترة: " + dateFormat.format(new Date(viewModel.getFilterFromDate()))
                            + " الى " + dateFormat.format(new Date(viewModel.getFilterToDate()));
                    canvas.drawText(dateRangeInfo, margin, y, paintRed);
                    y += 16;
                }

                double totalCredit = AppDatabase.getInstance(this).invoiceDao().getTotalCreditByCustomer(customer.getId());
                canvas.drawText(String.format(Locale.US, "إجمالي الدين الحالي: %.2f %s", totalCredit, CurrencyHelper.getSymbol()), margin, y, paintRed);
                y += 20;

                // خط فاصل
                canvas.drawLine(margin, y, pageWidth - margin, y, paintLine);
                y += 15;

                // جلب الفواتير حسب الفلتر
                List<Invoice> invoices;
                if (filterActive != null && filterActive) {
                    invoices = AppDatabase.getInstance(this).invoiceDao()
                            .getAllInvoicesByCustomerBetweenSync(customer.getId(),
                                    viewModel.getFilterFromDate(), viewModel.getFilterToDate());
                } else {
                    invoices = AppDatabase.getInstance(this).invoiceDao()
                            .getAllInvoicesByCustomerSync(customer.getId());
                }

                // رأس جدول الفواتير
                if (invoices != null && !invoices.isEmpty()) {
                    canvas.drawText("الفواتير", margin, y, paintHeader);
                    y += 18;

                    // رأس الجدول
                    Paint paintTableHeader = new Paint();
                    paintTableHeader.setTypeface(Typeface.DEFAULT_BOLD);
                    paintTableHeader.setTextSize(10);
                    paintTableHeader.setColor(Color.WHITE);
                    Paint paintTableBg = new Paint();
                    paintTableBg.setColor(Color.parseColor("#1976D2"));
                    canvas.drawRect(margin, y - 12, pageWidth - margin, y + 5, paintTableBg);
                    canvas.drawText("رقم الفاتورة", margin + 5, y, paintTableHeader);
                    canvas.drawText("التاريخ", margin + 100, y, paintTableHeader);
                    canvas.drawText("الاجمالي", margin + 200, y, paintTableHeader);
                    canvas.drawText("المدفوع", margin + 290, y, paintTableHeader);
                    canvas.drawText("المتبقي", margin + 380, y, paintTableHeader);
                    canvas.drawText("الحالة", margin + 460, y, paintTableHeader);
                    y += 18;

                    double grandTotal = 0;
                    double grandPaid = 0;
                    double grandRemaining = 0;

                    for (int i = 0; i < invoices.size(); i++) {
                        // إضافة صفحة جديدة إذا اقتربنا من النهاية
                        if (y > 790) {
                            document.finishPage(page);
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = 40;
                            // إعادة رسم رأس الجدول
                            canvas.drawRect(margin, y - 12, pageWidth - margin, y + 5, paintTableBg);
                            canvas.drawText("رقم الفاتورة", margin + 5, y, paintTableHeader);
                            canvas.drawText("التاريخ", margin + 100, y, paintTableHeader);
                            canvas.drawText("الاجمالي", margin + 200, y, paintTableHeader);
                            canvas.drawText("المدفوع", margin + 290, y, paintTableHeader);
                            canvas.drawText("المتبقي", margin + 380, y, paintTableHeader);
                            canvas.drawText("الحالة", margin + 460, y, paintTableHeader);
                            y += 18;
                        }

                        Invoice inv = invoices.get(i);
                        grandTotal += inv.getTotal();
                        grandPaid += inv.getPaidAmount();
                        grandRemaining += inv.getRemainingAmount();

                        // خلفية متبادلة
                        if (i % 2 == 0) {
                            Paint rowBg = new Paint();
                            rowBg.setColor(Color.parseColor("#F5F5F5"));
                            canvas.drawRect(margin, y - 12, pageWidth - margin, y + 5, rowBg);
                        }

                        String statusStr;
                        Paint statusPaint;
                        switch (inv.getPaymentStatus()) {
                            case Invoice.STATUS_CREDIT:
                                statusStr = "آجل";
                                statusPaint = paintRed;
                                break;
                            case Invoice.STATUS_PARTIAL:
                                statusStr = "جزئي";
                                statusPaint = paintRed;
                                break;
                            default:
                                statusStr = "مدفوع";
                                statusPaint = paintGreen;
                                break;
                        }

                        canvas.drawText(inv.getInvoiceNumber(), margin + 5, y, paintNormal);
                        canvas.drawText(dateFormat.format(new Date(inv.getDate())), margin + 100, y, paintNormal);
                        canvas.drawText(String.format(Locale.US, "%.2f", inv.getTotal()), margin + 200, y, paintNormal);
                        canvas.drawText(String.format(Locale.US, "%.2f", inv.getPaidAmount()), margin + 290, y, paintNormal);
                        canvas.drawText(String.format(Locale.US, "%.2f", inv.getRemainingAmount()), margin + 380, y, paintNormal);
                        canvas.drawText(statusStr, margin + 460, y, statusPaint);
                        y += 18;
                    }

                    // خط قبل الإجمالي
                    canvas.drawLine(margin, y, pageWidth - margin, y, paintLine);
                    y += 12;

                    // صف الإجماليات
                    Paint paintTotals = new Paint();
                    paintTotals.setTypeface(Typeface.DEFAULT_BOLD);
                    paintTotals.setTextSize(11);
                    paintTotals.setColor(Color.BLACK);
                    canvas.drawText("الإجمالي:", margin + 5, y, paintTotals);
                    canvas.drawText(String.format(Locale.US, "%.2f", grandTotal), margin + 200, y, paintTotals);
                    canvas.drawText(String.format(Locale.US, "%.2f", grandPaid), margin + 290, y, paintTotals);

                    Paint paintRedBold = new Paint();
                    paintRedBold.setTypeface(Typeface.DEFAULT_BOLD);
                    paintRedBold.setTextSize(11);
                    paintRedBold.setColor(Color.parseColor("#C62828"));
                    canvas.drawText(String.format(Locale.US, "%.2f %s", grandRemaining, CurrencyHelper.getSymbol()), margin + 380, y, paintRedBold);
                    y += 25;
                } else {
                    canvas.drawText("لا توجد فواتير لهذا العميل.", margin, y, paintNormal);
                    y += 20;
                }

                // ملاحظة ختامية
                canvas.drawLine(margin, y, pageWidth - margin, y, paintLine);
                y += 15;
                canvas.drawText("تم انشاء هذا التقرير بواسطة MicroPOS - " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date()), margin, y, paintNormal);

                document.finishPage(page);

                // حفظ الملف
                String fileName = "customer_" + customer.getId() + "_" + System.currentTimeMillis() + ".pdf";
                File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir == null) {
                    runOnUiThread(() -> Toast.makeText(this, "خطأ في الوصول لمجلد التنزيلات", Toast.LENGTH_SHORT).show());
                    document.close();
                    return;
                }
                File file = new File(downloadsDir, fileName);
                document.writeTo(new FileOutputStream(file));
                document.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "تم إنشاء PDF بنجاح: " + fileName, Toast.LENGTH_LONG).show();
                    Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, getString(R.string.action_share_pdf_text)));
                });

            } catch (Exception e) {
                Log.e("CustomerDetail", "Error generating PDF", e);
                runOnUiThread(() -> Toast.makeText(this, "خطأ في إنشاء PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ==================== دوال مساعدة ====================

    private String formatNumber(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') {
                builder.append((char) (c - '\u0660' + '0'));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.syncData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}