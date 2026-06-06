package com.posmix.mixtuvgag.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Promotion;
import java.util.List;
import java.util.concurrent.Executors;

public class PromotionsViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<List<Promotion>> promotions = new MutableLiveData<>();

    public PromotionsViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
        load();
    }

    public LiveData<List<Promotion>> getPromotions() { return promotions; }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() ->
            promotions.postValue(db.getPromotionDao().getAll()));
    }

    public void save(Promotion p) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (p.getId() == 0) db.getPromotionDao().insert(p);
            else db.getPromotionDao().update(p);
            load();
        });
    }

    public void delete(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.getPromotionDao().delete(id);
            load();
        });
    }
}
