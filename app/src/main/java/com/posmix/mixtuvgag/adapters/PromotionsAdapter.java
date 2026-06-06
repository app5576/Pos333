package com.posmix.mixtuvgag.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.models.Promotion;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PromotionsAdapter extends ListAdapter<Promotion, PromotionsAdapter.VH> {
    public interface Listener { void onEdit(Promotion p); void onDelete(Promotion p); }
    private final Listener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar"));

    public PromotionsAdapter(Listener l) {
        super(new DiffUtil.ItemCallback<Promotion>() {
            public boolean areItemsTheSame(@NonNull Promotion a, @NonNull Promotion b) { return a.getId() == b.getId(); }
            public boolean areContentsTheSame(@NonNull Promotion a, @NonNull Promotion b) { return a.getId() == b.getId(); }
        });
        this.listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Promotion p = getItem(pos);
        h.tvName.setText(p.getName());
        h.tvCode.setText("كود: " + (p.getCode() != null ? p.getCode() : "-"));
        h.tvType.setText(p.getTypeName());
        String val = p.getType() == Promotion.TYPE_PERCENTAGE ? p.getDiscountValue() + "%" :
                     p.getType() == Promotion.TYPE_FIXED ? p.getDiscountValue() + " ر.س" :
                     "اشتري " + p.getBuyQty() + " واحصل على " + p.getGetQty();
        h.tvValue.setText(val);
        h.tvDates.setText(sdf.format(new Date(p.getStartDate())) + " - " + sdf.format(new Date(p.getEndDate())));
        h.tvStatus.setText(p.isActive() ? "نشطة" : "متوقفة");
        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCode, tvType, tvValue, tvDates, tvStatus;
        ImageButton btnEdit, btnDelete;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_promo_name);
            tvCode = v.findViewById(R.id.tv_promo_code);
            tvType = v.findViewById(R.id.tv_promo_type);
            tvValue = v.findViewById(R.id.tv_promo_value);
            tvDates = v.findViewById(R.id.tv_promo_dates);
            tvStatus = v.findViewById(R.id.tv_promo_status);
            btnEdit = v.findViewById(R.id.btn_edit_promo);
            btnDelete = v.findViewById(R.id.btn_delete_promo);
        }
    }
}
