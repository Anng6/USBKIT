package com.anng6.usbkit.adapters;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anng6.usbkit.databinding.ItemEmptyBinding;
import com.anng6.usbkit.databinding.ItemMsBinding;
import com.anng6.usbkit.ui.fragment.MsFragment;
import com.anng6.usbkit.util.MsGadgetUtil;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class MsAdapter extends RecyclerView.Adapter<MsAdapter.ViewHolder> {
    public List<MsGadgetUtil.MsList> list = new ArrayList<>();
    private MsFragment fragment;
    private Activity activity;
    private ViewListener callback;

    public static interface ViewListener {
        public boolean onEnabledCheck(MsGadgetUtil.MsList ms, boolean newValue, String historyPath);

        public void onModifyClick(MsGadgetUtil.MsList ms);

        public void onDeleteClick(MsGadgetUtil.MsList ms);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        boolean isEmpty;
        MaterialSwitch lun;
        TextView path;
        TextView mode;
        TextView removable;
        TextView nofua;
        Button modify;
        Button delete;

        public ViewHolder(@NonNull ItemMsBinding binding) {
            super(binding.getRoot());
            this.isEmpty = false;
            this.lun = binding.lun;
            this.path = binding.path;
            this.mode = binding.mode;
            this.removable = binding.removable;
            this.nofua = binding.nofua;
            this.modify = binding.modify;
            this.delete = binding.delete;
        }

        public ViewHolder(@NonNull ItemEmptyBinding binding) {
            super(binding.getRoot());
            this.isEmpty = true;
        }
    }

    public MsAdapter(MsFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (list.isEmpty())
            return new ViewHolder(
                    ItemEmptyBinding.inflate(activity.getLayoutInflater(), parent, false));
        return new ViewHolder(ItemMsBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.isEmpty) return;
        var ms = list.get(position);
        holder.lun.setText(
                String.format("#%d %s-%s", position + 1, ms.function.substring(13), ms.lun.substring(4)));
        holder.lun.setChecked(ms.file != null);
        holder.path.setText(ms.file != null ? ms.file : "None");
        holder.mode.setText(ms.mode);
        holder.removable.setText(ms.removable ? "Yes" : "No");
        holder.nofua.setText(ms.nofua ? "Yes" : "No");
        if (callback == null) return;
        holder.lun.setOnCheckedChangeListener((v, n) -> holder.lun.setChecked(callback.onEnabledCheck(ms, n, ms.file)));
        holder.modify.setOnClickListener((v) -> callback.onModifyClick(ms));
        holder.delete.setOnClickListener((v) -> callback.onDeleteClick(ms));
    }

    public void setOnClickListener(ViewListener callback) {
        this.callback = callback;
    }

    @Override
    public int getItemCount() {
        if (list.isEmpty()) return 1;
        return list.size();
    }
}
