package com.posmix.mixtuvgag.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.models.Shift;
import com.posmix.mixtuvgag.utils.CurrencyHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShiftsAdapter extends ListAdapter<Shift, ShiftsAdapter.VH> {
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar"));

    public ShiftsAdapter() {
        super(new DiffUtil.ItemCallback<Shift>() {
            public boolean areItemsTheSame(@NonNull Shift a, @NonNull Shift b) { return a.getId() == b.getId(); }
            public boolean areContentsTheSame(@NonNull Shift a, @NonNull Shift b) { return a.getId() == b.getId() && a.getStatus() == b.getStatus(); }
        });
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Shift s = getItem(pos);
        h.tvEmployee.setText(s.getEmployeeName() != null ? s.getEmployeeName() : "غير محدد");
        h.tvOpenTime.setText("فتح: " + sdf.format(new Date(s.getOpenTime())));
        h.tvStatus.setText(s.isOpen() ? "مفتوحة" : "مغلقة");
        h.tvSales.setText("مبيعات: " + CurrencyHelper.format(s.getTotalSales()));
        if (!s.isOpen() && s.getCloseTime() > 0) {
            h.tvCloseTime.setText("إغلاق: " + sdf.format(new Date(s.getCloseTime())));
        } else {
            h.tvCloseTime.setText("");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmployee, tvOpenTime, tvCloseTime, tvStatus, tvSales;
        VH(View v) {
            super(v);
            tvEmployee = v.findViewById(R.id.tv_shift_employee);
            tvOpenTime = v.findViewById(R.id.tv_shift_open_time);
            tvCloseTime = v.findViewById(R.id.tv_shift_close_time);
            tvStatus = v.findViewById(R.id.tv_shift_status);
            tvSales = v.findViewById(R.id.tv_shift_sales);
        }
    }
}
