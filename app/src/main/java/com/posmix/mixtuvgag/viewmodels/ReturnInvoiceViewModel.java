package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.ReturnInvoice;
import java.util.List;
import java.util.concurrent.Executors;

public class ReturnInvoiceViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<List<ReturnInvoice>> returns = new MutableLiveData<>();

    public ReturnInvoiceViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
        load();
    }

    public LiveData<List<ReturnInvoice>> getReturns() { return returns; }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() ->
            returns.postValue(db.getReturnInvoiceDao().getAll()));
    }
}
