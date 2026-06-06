package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.AppDatabase;
import com.posmix.mixtuvgag.models.CashTransaction;
import com.posmix.mixtuvgag.models.Customer;
import com.posmix.mixtuvgag.models.Invoice;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerDetailViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final ExecutorService exec;

    private final MutableLiveData<Customer> customer = new MutableLiveData<>();
    private LiveData<List<Invoice>> outstandingInvoices;
    private final MutableLiveData<List<Object>> customerHistory = new MutableLiveData<>();

    // حالة الفلترة بالتاريخ
    private final MutableLiveData<Boolean> isFilterActive = new MutableLiveData<>(false);
    private long filterFromDate = 0;
    private long filterToDate = 0;
    private int currentCustomerId = -1;

    public CustomerDetailViewModel(Application app) {
        super(app);
        db = AppDatabase.getInstance(app);
        exec = Executors.newSingleThreadExecutor();
    }

    public LiveData<Customer> getCustomer() {
        return customer;
    }

    public LiveData<List<Invoice>> getOutstandingInvoices() {
        return outstandingInvoices;
    }

    public LiveData<List<Object>> getCustomerHistory() {
        return customerHistory;
    }

    public LiveData<Boolean> getIsFilterActive() {
        return isFilterActive;
    }

    public long getFilterFromDate() {
        return filterFromDate;
    }

    public long getFilterToDate() {
        return filterToDate;
    }

    public void loadCustomer(int customerId) {
        this.currentCustomerId = customerId;
        exec.execute(() -> {
            Customer c = db.customerDao().getByIdSync(customerId);
            customer.postValue(c);
        });
    }

    public void loadOutstandingInvoices(int customerId) {
        outstandingInvoices = db.invoiceDao().getCreditInvoicesByCustomer(customerId);
    }

    public void loadCustomerHistory(int customerId) {
        this.currentCustomerId = customerId;
        exec.execute(() -> {
            List<Object> history = new ArrayList<>();

            List<Invoice> creditInvoices = db.invoiceDao().getCreditInvoicesByCustomerSync(customerId);
            if (creditInvoices != null && !creditInvoices.isEmpty()) {
                history.addAll(creditInvoices);
            }

            List<CashTransaction> cashTransactions = db.cashTransactionDao().getForCustomer(customerId);
            if (cashTransactions != null && !cashTransactions.isEmpty()) {
                history.addAll(cashTransactions);
            }

            sortAndPost(history);
        });
    }

    // تحميل حسابات العميل مع فلتر التاريخ
    public void loadCustomerHistoryFiltered(int customerId, long fromDate, long toDate) {
        this.currentCustomerId = customerId;
        this.filterFromDate = fromDate;
        this.filterToDate = toDate;
        this.isFilterActive.postValue(true);

        exec.execute(() -> {
            List<Object> history = new ArrayList<>();

            List<Invoice> creditInvoices = db.invoiceDao()
                    .getCreditInvoicesByCustomerBetweenSync(customerId, fromDate, toDate);
            if (creditInvoices != null && !creditInvoices.isEmpty()) {
                history.addAll(creditInvoices);
            }

            List<CashTransaction> cashTransactions = db.cashTransactionDao()
                    .getForCustomerBetween(customerId, fromDate, toDate);
            if (cashTransactions != null && !cashTransactions.isEmpty()) {
                history.addAll(cashTransactions);
            }

            sortAndPost(history);
        });
    }

    // مسح فلتر التاريخ والعودة للعرض الكامل
    public void clearDateFilter() {
        this.filterFromDate = 0;
        this.filterToDate = 0;
        this.isFilterActive.postValue(false);
        if (currentCustomerId != -1) {
            loadCustomerHistory(currentCustomerId);
        }
    }

    // مزامنة (إعادة تحميل) البيانات مع مراعاة الفلتر الحالي
    public void syncData() {
        if (currentCustomerId == -1) return;
        loadCustomer(currentCustomerId);
        loadOutstandingInvoices(currentCustomerId);
        Boolean filterActive = isFilterActive.getValue();
        if (filterActive != null && filterActive && filterFromDate > 0 && filterToDate > 0) {
            loadCustomerHistoryFiltered(currentCustomerId, filterFromDate, filterToDate);
        } else {
            loadCustomerHistory(currentCustomerId);
        }
    }

    private void sortAndPost(List<Object> history) {
        Collections.sort(history, (o1, o2) -> {
            long date1 = getDateFromObject(o1);
            long date2 = getDateFromObject(o2);
            return Long.compare(date2, date1);
        });
        customerHistory.postValue(history);
    }

    private long getDateFromObject(Object obj) {
        if (obj instanceof Invoice) {
            return ((Invoice) obj).getDate();
        } else if (obj instanceof CashTransaction) {
            return ((CashTransaction) obj).getDate();
        }
        return 0;
    }

    public void updateCustomer(Customer c) {
        exec.execute(() -> {
            db.customerDao().update(c);
            loadCustomer(c.getId());
            loadCustomerHistory(c.getId());
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        exec.shutdown();
    }
}
