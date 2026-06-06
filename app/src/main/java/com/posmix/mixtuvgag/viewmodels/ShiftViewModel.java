package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Shift;
import java.util.List;
import java.util.concurrent.Executors;

public class ShiftViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<List<Shift>> shifts = new MutableLiveData<>();
    private final MutableLiveData<Shift> currentShift = new MutableLiveData<>();

    public ShiftViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
        loadAll();
        loadCurrentShift();
    }

    public LiveData<List<Shift>> getShifts() { return shifts; }
    public LiveData<Shift> getCurrentShift() { return currentShift; }

    public void loadAll() {
        Executors.newSingleThreadExecutor().execute(() ->
            shifts.postValue(db.getShiftDao().getAll()));
    }

    public void loadCurrentShift() {
        Executors.newSingleThreadExecutor().execute(() ->
            currentShift.postValue(db.getShiftDao().getOpenShift()));
    }

    public void openShift(Shift s) {
        Executors.newSingleThreadExecutor().execute(() -> {
            s.setOpenTime(System.currentTimeMillis());
            s.setStatus(Shift.STATUS_OPEN);
            db.getShiftDao().insert(s);
            loadAll();
            loadCurrentShift();
        });
    }

    public void closeShift(Shift s) {
        Executors.newSingleThreadExecutor().execute(() -> {
            s.setCloseTime(System.currentTimeMillis());
            s.setStatus(Shift.STATUS_CLOSED);
            db.getShiftDao().update(s);
            loadAll();
            loadCurrentShift();
        });
    }
}
