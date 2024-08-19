package com.anng6.usbkit.adapters;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anng6.usbkit.databinding.ItemEmptyBinding;
import com.anng6.usbkit.databinding.ItemHidBinding;
import com.anng6.usbkit.ui.fragment.HidFragment;
import com.anng6.usbkit.util.HidGadgetUtil;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class HidAdapter extends RecyclerView.Adapter<HidAdapter.ViewHolder> {
    public List<HidGadgetUtil.HidList> list = new ArrayList<>();
    private HidFragment fragment;
    private Activity activity;
    private ViewListener callback;

    public static interface ViewListener {
        public boolean onEnabledCheck(HidGadgetUtil.HidList hid, boolean newValue);

        public void onUseClick(HidGadgetUtil.HidList hid);

        public void onDeleteClick(HidGadgetUtil.HidList hid);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        boolean isEmpty;
        MaterialSwitch hid;
        TextView type;
        Button use;
        Button delete;

        public ViewHolder(@NonNull ItemHidBinding binding) {
            super(binding.getRoot());
            this.isEmpty = false;
            this.hid = binding.hid;
            this.type = binding.type;
            this.use = binding.use;
            this.delete = binding.delete;
        }

        public ViewHolder(@NonNull ItemEmptyBinding binding) {
            super(binding.getRoot());
            this.isEmpty = true;
        }
    }

    public HidAdapter(HidFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (list.isEmpty())
            return new ViewHolder(
                    ItemEmptyBinding.inflate(activity.getLayoutInflater(), parent, false));
        return new ViewHolder(ItemHidBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.isEmpty) return;
        var hid = list.get(position);
        holder.hid.setChecked(hid.getEnabled());
        holder.hid.setText(
                String.format("#%d %s", position + 1, hid.function.substring(4)));
        holder.type.setText(HidGadgetUtil.getProtocolName(hid.type));
        if (callback == null) return;
        holder.hid.setOnCheckedChangeListener((v, n) -> holder.hid.setChecked(callback.onEnabledCheck(hid, n)));
        holder.use.setOnClickListener((v) -> callback.onUseClick(hid));
        holder.delete.setOnClickListener((v) -> callback.onDeleteClick(hid));
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
