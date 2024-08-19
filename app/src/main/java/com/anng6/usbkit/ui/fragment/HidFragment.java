package com.anng6.usbkit.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.anng6.usbkit.App;
import com.anng6.usbkit.R;
import com.anng6.usbkit.adapters.HidAdapter;
import com.anng6.usbkit.databinding.DialogHidBinding;
import com.anng6.usbkit.databinding.FragmentHidBinding;
import com.anng6.usbkit.util.AppUtil;
import com.anng6.usbkit.util.GadgetUtil;
import com.anng6.usbkit.util.HidGadgetUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.nio.FileSystemManager;

import rikka.recyclerview.RecyclerViewKt;

public class HidFragment extends BaseFragment implements MenuProvider {
    private FragmentHidBinding binding;
    private HidAdapter recyclerViewAdapter;
    private FileSystemManager remoteFS;
    public GadgetUtil.Gadget gadget;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHidBinding.inflate(inflater, container, false);

        setupToolbar(binding.toolbar, "Hid");
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setSubtitle("Loading...");

        recyclerViewAdapter = new HidAdapter(this);
        recyclerViewAdapter.setOnClickListener(
                new HidAdapter.ViewListener() {
                    @Override
                    public boolean onEnabledCheck(HidGadgetUtil.HidList hid, boolean newValue) {
                        hid.setEnable(newValue);
                        gadget.setUDC(true);
                        refreshSubtitle();
                        return hid.getEnabled();
                    }

                    @Override
                    public void onUseClick(HidGadgetUtil.HidList hid) {
                        // TODO

                    }

                    @Override
                    public void onDeleteClick(HidGadgetUtil.HidList hid) {
                        new MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.delete)
                                .setMessage(
                                        String.format(getString(R.string.delete_hid), hid.function))
                                .setPositiveButton(
                                        android.R.string.ok,
                                        (d, w) -> {
                                            gadget.setUDC(false);
                                            hid.setEnable(false);
                                            hid.delete();
                                            gadget.setUDC(true);
                                            refreshUI();
                                        })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.recyclerView.setAdapter(recyclerViewAdapter);

        binding.appBar.setLiftable(true);
        binding.recyclerView
                .getBorderViewDelegate()
                .setBorderVisibilityChangedListener(
                        (top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);

        binding.swipeRefreshLayout.setOnRefreshListener(
                () -> App.getMainHandler().postDelayed(() -> refreshUI(), 1000));
        binding.fab.setOnClickListener(
                (v) -> {
                    if (gadget == null) return;
                    var dialogBinding =
                            DialogHidBinding.inflate(LayoutInflater.from(requireContext()));
                    dialogBinding.hidLayout.setEnabled(true);

                    new MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(R.string.add)
                            .setView(dialogBinding.getRoot())
                            .setPositiveButton(
                                    R.string.save,
                                    (d, w) -> {
                                        var fun =
                                                String.format(
                                                        "hid.%s",
                                                        dialogBinding.hidName.getText().toString());
                                        var hid =
                                                gadget.getHidGadget()
                                                        .createHid(
                                                                fun,
                                                                HidGadgetUtil.getProtocolId(
                                                                        dialogBinding
                                                                                .type
                                                                                .getText()
                                                                                .toString()));
                                        if (hid != null) {
                                            // TODO
                                        } else {
                                            showHint("Err: Create failed", Snackbar.LENGTH_SHORT);
                                        }
                                        refreshUI();
                                    })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                });

        AppUtil.remoteFS.observe(
                getViewLifecycleOwner(),
                (remoteFS) -> {
                    refreshUI();
                });
        return binding.getRoot();
    }

    public void updateSubtitle(String str) {
        if (binding == null) return;
        binding.toolbar.setSubtitle(str);
        binding.toolbarLayout.setSubtitle(binding.toolbar.getSubtitle());
    }

    public void refreshSubtitle() {
        if (gadget == null) return;
        var status = gadget.getHidGadget().getHidStatus(recyclerViewAdapter.list);
        updateSubtitle(
                String.format(
                        "Gadget: %s,  Enabled: %d/%d", AppUtil.useGadget, status[0], status[1]));
    }

    public void refreshUI() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        remoteFS = AppUtil.remoteFS.getValue();
        gadget = GadgetUtil.getGadget(remoteFS, AppUtil.useGadget);
        if (gadget == null) {
            updateSubtitle("Initialization failed");
            return;
        }
        recyclerViewAdapter.list = gadget.getHidGadget().getHidList();
        binding.recyclerView.setAdapter(recyclerViewAdapter);
        refreshSubtitle();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (gadget == null) return true;
        var itemId = item.getItemId();

        return true;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
