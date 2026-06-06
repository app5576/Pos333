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
import com.posmix.mixtuvgag.models.Employee;

public class EmployeesAdapter extends ListAdapter<Employee, EmployeesAdapter.VH> {
    public interface Listener {
        void onEdit(Employee e);
        void onDelete(Employee e);
    }
    private final Listener listener;

    public EmployeesAdapter(Listener l) {
        super(new DiffUtil.ItemCallback<Employee>() {
            public boolean areItemsTheSame(@NonNull Employee a, @NonNull Employee b) { return a.getId() == b.getId(); }
            public boolean areContentsTheSame(@NonNull Employee a, @NonNull Employee b) { return a.getId() == b.getId() && a.getName().equals(b.getName()); }
        });
        this.listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Employee e = getItem(pos);
        h.tvName.setText(e.getName());
        h.tvRole.setText(e.getRoleName());
        h.tvUsername.setText("@" + e.getUsername());
        h.tvStatus.setText(e.isActive() ? "نشط" : "غير نشط");
        h.btnEdit.setOnClickListener(v -> listener.onEdit(e));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(e));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvUsername, tvStatus;
        ImageButton btnEdit, btnDelete;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_employee_name);
            tvRole = v.findViewById(R.id.tv_employee_role);
            tvUsername = v.findViewById(R.id.tv_employee_username);
            tvStatus = v.findViewById(R.id.tv_employee_status);
            btnEdit = v.findViewById(R.id.btn_edit_employee);
            btnDelete = v.findViewById(R.id.btn_delete_employee);
        }
    }
}
