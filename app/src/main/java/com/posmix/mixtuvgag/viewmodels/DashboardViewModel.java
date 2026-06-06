package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Product;
import java.util.*;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<DashboardData> dashboardData = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
    }

    public LiveData<DashboardData> getDashboardData() { return dashboardData; }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DashboardData data = new DashboardData();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            long dayStart = cal.getTimeInMillis();
            long dayEnd = System.currentTimeMillis();

            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            long weekStart = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, 1);
            long monthStart = cal.getTimeInMillis();

            data.todaySales = db.getInvoiceTotalByDateRange(dayStart, dayEnd, 1);
            data.todayPurchases = db.getInvoiceTotalByDateRange(dayStart, dayEnd, 2);
            data.weekSales = db.getInvoiceTotalByDateRange(weekStart, dayEnd, 1);
            data.monthSales = db.getInvoiceTotalByDateRange(monthStart, dayEnd, 1);
            data.todayInvoiceCount = db.getInvoiceCountByDateRange(dayStart, dayEnd, 1);
            data.totalProducts = db.getProductCount();
            data.lowStockProducts = db.getLowStockCount();
            data.totalCustomers = db.getCustomerCount();
            data.pendingDebts = db.getTotalPendingDebts();
            data.lowStockList = db.getLowStockProducts();
            data.openShift = db.getShiftDao().getOpenShift();
            dashboardData.postValue(data);
        });
    }

    public static class DashboardData {
        public double todaySales, todayPurchases, weekSales, monthSales, pendingDebts;
        public int todayInvoiceCount, totalProducts, lowStockProducts, totalCustomers;
        public List<Product> lowStockList;
        public com.posmix.mixtuvgag.models.Shift openShift;
    }
}
