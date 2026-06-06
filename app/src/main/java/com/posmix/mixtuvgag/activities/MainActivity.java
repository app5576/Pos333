package com.posmix.mixtuvgag.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.utils.FullSyncManager;
import com.posmix.mixtuvgag.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ProgressBar syncProgress;
    private ImageView syncIcon;
    private TextView syncStatus;
    private FullSyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) { navigateToLogin(); return; }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String empName = sessionManager.getEmployeeName();
        if (getSupportActionBar() != null && empName != null && !empName.isEmpty())
            getSupportActionBar().setSubtitle("مرحباً " + empName);

        syncProgress = findViewById(R.id.sync_progress);
        syncIcon     = findViewById(R.id.sync_icon);
        syncStatus   = findViewById(R.id.sync_status);
        Button btnSyncNow = findViewById(R.id.btn_sync_now);
        syncManager = new FullSyncManager(this);
        syncStatus.setText(syncManager.getLastSyncTime());
        btnSyncNow.setOnClickListener(v -> startSyncOperation());

        // ===== البطاقات الأساسية =====
        click(R.id.card_sales,      SalesActivity.class);
        click(R.id.card_purchases,  PurchasesActivity.class);
        click(R.id.card_customers,  CustomersActivity.class);
        click(R.id.card_suppliers,  SuppliersActivity.class);
        click(R.id.card_inventory,  InventoryActivity.class);
        click(R.id.card_expenses,   ExpensesActivity.class);
        click(R.id.card_cashbox,    CashFundActivity.class);
        click(R.id.card_reports,    ReportsActivity.class);
        click(R.id.card_settings,   SettingsActivity.class);

        // ===== البطاقات الجديدة =====
        click(R.id.card_promotions, PromotionsActivity.class);
        click(R.id.card_returns,    ReturnInvoiceActivity.class);
        click(R.id.card_employees,  EmployeesActivity.class);
        click(R.id.card_shifts,     ShiftManagementActivity.class);
        click(R.id.card_profit_loss,ProfitLossActivity.class);
        click(R.id.card_debts,      DebtsActivity.class);
    }

    private void click(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, target)));
    }

    private void startSyncOperation() {
        setUiEnabled(false);
        syncManager.syncAllData(new FullSyncManager.SyncCallback() {
            @Override public void onSyncStart()   { runOnUiThread(() -> syncStatus.setText("جاري البدء...")); }
            @Override public void onSyncProgress(String msg) { runOnUiThread(() -> syncStatus.setText(msg)); }
            @Override public void onSyncComplete(String msg) {
                runOnUiThread(() -> { syncStatus.setText("تم التزامن"); Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); setUiEnabled(true); });
            }
            @Override public void onSyncError(String err) {
                runOnUiThread(() -> { syncStatus.setText("خطأ"); Toast.makeText(MainActivity.this, "فشل: " + err, Toast.LENGTH_LONG).show(); setUiEnabled(true); });
            }
        });
    }

    private void setUiEnabled(boolean enabled) {
        syncProgress.setVisibility(enabled ? View.GONE : View.VISIBLE);
        syncIcon.setVisibility(enabled ? View.VISIBLE : View.GONE);
        View btn = findViewById(R.id.btn_sync_now);
        if (btn != null) btn.setEnabled(enabled);
    }

    private void navigateToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.main_menu, menu); return true; }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) { sessionManager.logout(); navigateToLogin(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
