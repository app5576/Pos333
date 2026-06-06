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
import com.posmix.mixtuvgag.models.Product;

public class LowStockAdapter extends ListAdapter<Product, LowStockAdapter.VH> {
    public LowStockAdapter() {
        super(new DiffUtil.ItemCallback<Product>() {
            public boolean areItemsTheSame(@NonNull Product a, @NonNull Product b) { return a.getId() == b.getId(); }
            public boolean areContentsTheSame(@NonNull Product a, @NonNull Product b) { return a.getStockQuantity() == b.getStockQuantity(); }
        });
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_low_stock, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Product p = getItem(pos);
        h.tvName.setText(p.getName());
        h.tvQty.setText("الكمية: " + p.getStockQuantity());
        h.tvMin.setText("الحد الأدنى: " + p.getMinStockAlert());
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvQty, tvMin;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_ls_name);
            tvQty = v.findViewById(R.id.tv_ls_qty);
            tvMin = v.findViewById(R.id.tv_ls_min);
        }
    }
}
