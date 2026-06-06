package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Employee;
import java.util.List;
import java.util.concurrent.Executors;

public class EmployeesViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<List<Employee>> employees = new MutableLiveData<>();

    public EmployeesViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
        loadEmployees();
    }

    public LiveData<List<Employee>> getEmployees() { return employees; }

    public void loadEmployees() {
        Executors.newSingleThreadExecutor().execute(() ->
            employees.postValue(db.getEmployeeDao().getAll()));
    }

    public void save(Employee e) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (e.getId() == 0) {
                e.setCreatedAt(System.currentTimeMillis());
                db.getEmployeeDao().insert(e);
            } else {
                db.getEmployeeDao().update(e);
            }
            loadEmployees();
        });
    }

    public void delete(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.getEmployeeDao().delete(id);
            loadEmployees();
        });
    }
}
