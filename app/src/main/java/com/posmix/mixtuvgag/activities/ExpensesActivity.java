package com.posmix.mixtuvgag.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.adapters.ExpensesAdapter;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Expense;
import com.posmix.mixtuvgag.viewmodels.ExpensesViewModel;
import androidx.appcompat.widget.Toolbar;
import java.util.List;
import java.util.concurrent.Executors;

public class ExpensesActivity extends AppCompatActivity {

    private ExpensesViewModel vm;
    private ExpensesAdapter adapter;
    private DatabaseHelper db;
    private List<String> categoryNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("المصاريف");
        }

        db = new DatabaseHelper(this);

        RecyclerView rv = findViewById(R.id.rv_expenses);
        FloatingActionButton fab = findViewById(R.id.fab_add_expense);

        vm = new ViewModelProvider(this).get(ExpensesViewModel.class);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter(expense -> confirmDeleteExpense(expense));
        rv.setAdapter(adapter);

        vm.getExpenses().observe(this, expenses -> adapter.submitList(expenses));

        // Load categories from DB (non-blocking)
        Executors.newSingleThreadExecutor().execute(() -> {
            categoryNames = db.getExpenseCategoryDao().getAllNames();
            runOnUiThread(() -> fab.setOnClickListener(v -> showAddExpenseDialog()));
        });
        fab.setOnClickListener(v -> showAddExpenseDialog());
    }

    private void showAddExpenseDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_expense, null);
        Spinner spinnerCategory = view.findViewById(R.id.spinner_expense_category);
        EditText etAmount = view.findViewById(R.id.et_expense_amount);
        EditText etNotes = view.findViewById(R.id.et_expense_notes);

        String[] cats;
        if (categoryNames != null && !categoryNames.isEmpty()) {
            cats = categoryNames.toArray(new String[0]);
        } else {
            cats = new String[]{"إيجار","رواتب","كهرباء وماء","هاتف وإنترنت","صيانة","مواصلات","مستلزمات مكتبية","تسويق وإعلان","أخرى"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        spinnerCategory.setAdapter(adapter);

        new AlertDialog.Builder(this)
            .setTitle("إضافة مصروف")
            .setView(view)
            .setPositiveButton("حفظ", (d, w) -> {
                String category = spinnerCategory.getSelectedItem().toString();
                String amountStr = etAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(this, "أدخل المبلغ", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount;
                try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) {
                    Toast.makeText(this, "مبلغ غير صحيح", Toast.LENGTH_SHORT).show();
                    return;
                }
                Expense expense = new Expense();
                expense.setCategory(category);
                expense.setAmount(amount);
                expense.setNotes(etNotes.getText().toString().trim());
                expense.setDate(System.currentTimeMillis());
                vm.insert(expense);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void confirmDeleteExpense(Expense expense) {
        new AlertDialog.Builder(this)
            .setTitle("حذف مصروف")
            .setMessage("هل تريد حذف هذا المصروف؟")
            .setPositiveButton("حذف", (d, w) -> vm.delete(expense))
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
