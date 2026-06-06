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
import com.posmix.mixtuvgag.models.ReturnInvoice;
import com.posmix.mixtuvgag.utils.CurrencyHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReturnInvoicesAdapter extends ListAdapter<ReturnInvoice, ReturnInvoicesAdapter.VH> {
    public interface Listener { void onItemClick(ReturnInvoice r); }
    private final Listener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar"));

    public ReturnInvoicesAdapter(Listener l) {
        super(new DiffUtil.ItemCallback<ReturnInvoice>() {
            public boolean areItemsTheSame(@NonNull ReturnInvoice a, @NonNull ReturnInvoice b) { return a.getId() == b.getId(); }
            public boolean areContentsTheSame(@NonNull ReturnInvoice a, @NonNull ReturnInvoice b) { return a.getId() == b.getId(); }
        });
        this.listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_return_invoice, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ReturnInvoice r = getItem(pos);
        h.tvNumber.setText(r.getReturnNumber());
        h.tvDate.setText(sdf.format(new Date(r.getDate())));
        h.tvTotal.setText(CurrencyHelper.format(r.getTotal()));
        h.tvOriginal.setText("فاتورة أصل: " + (r.getOriginalInvoiceNumber() != null ? r.getOriginalInvoiceNumber() : "-"));
        String party = r.getType() == ReturnInvoice.TYPE_SALE_RETURN ?
            (r.getCustomerName() != null ? r.getCustomerName() : "عميل نقدي") :
            (r.getSupplierName() != null ? r.getSupplierName() : "مورد غير محدد");
        h.tvParty.setText(party);
        h.tvType.setText(r.getType() == ReturnInvoice.TYPE_SALE_RETURN ? "مرتجع مبيعات" : "مرتجع مشتريات");
        h.itemView.setOnClickListener(v -> listener.onItemClick(r));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNumber, tvDate, tvTotal, tvOriginal, tvParty, tvType;
        VH(View v) {
            super(v);
            tvNumber = v.findViewById(R.id.tv_return_number);
            tvDate = v.findViewById(R.id.tv_return_date);
            tvTotal = v.findViewById(R.id.tv_return_total);
            tvOriginal = v.findViewById(R.id.tv_return_original);
            tvParty = v.findViewById(R.id.tv_return_party);
            tvType = v.findViewById(R.id.tv_return_type);
        }
    }
}
