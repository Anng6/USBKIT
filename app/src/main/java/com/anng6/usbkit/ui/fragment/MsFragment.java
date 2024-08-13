package com.anng6.usbkit.ui.fragment;

import android.os.Bundle;
import android.os.Environment;
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
import com.anng6.usbkit.adapters.MsAdapter;
import com.anng6.usbkit.databinding.DialogImgBinding;
import com.anng6.usbkit.databinding.DialogMsBinding;
import com.anng6.usbkit.databinding.FragmentMsBinding;
import com.anng6.usbkit.util.AppUtil;
import com.anng6.usbkit.util.FileDialogUtil;
import com.anng6.usbkit.util.GadgetUtil;
import com.anng6.usbkit.util.MsGadgetUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import rikka.recyclerview.RecyclerViewKt;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class MsFragment extends BaseFragment implements MenuProvider {
    private FragmentMsBinding binding;
    private MsAdapter recyclerViewAdapter;
    private FileSystemManager remoteFS;
    private GadgetUtil.Gadget gadget;
    private ExtendedFile imgDir;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMsBinding.inflate(inflater, container, false);

        setupToolbar(binding.toolbar, "MStorage", R.menu.menu_mstorage);
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setSubtitle("Loading...");

        recyclerViewAdapter = new MsAdapter(this);
        recyclerViewAdapter.setOnClickListener(
                new MsAdapter.ViewListener() {
                    @Override
                    public boolean onEnabledCheck(
                            MsGadgetUtil.MsList ms, boolean newValue, String historyPath) {
                        if (newValue && historyPath == null) {
                            showHint("Err: Path is empty", Snackbar.LENGTH_SHORT);
                            return false;
                        }
                        ms.setFile(newValue ? historyPath : "");
                        return newValue;
                    }

                    @Override
                    public void onModifyClick(MsGadgetUtil.MsList ms) {
                        var dialogBinding =
                                DialogMsBinding.inflate(LayoutInflater.from(requireActivity()));
                        dialogBinding.msName.setText(ms.function.substring(13));
                        dialogBinding.path.setText(ms.file);
                        dialogBinding.mode.setText(ms.mode);
                        dialogBinding.mode.setSimpleItems(
                                requireActivity()
                                        .getResources()
                                        .getStringArray(R.array.mount_modes));
                        dialogBinding.removable.setChecked(ms.removable);
                        dialogBinding.nofua.setChecked(ms.nofua);
                        dialogBinding.pathLayout.setEndIconOnClickListener(
                                (view) -> {
                                    var remoteFS = AppUtil.remoteFS.getValue();
                                    new FileDialogUtil(requireActivity())
                                            .selectFile(
                                                    remoteFS,
                                                    imgDir.getAbsolutePath(),
                                                    (path) -> dialogBinding.path.setText(path));
                                });
                        new MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.modify)
                                .setView(dialogBinding.getRoot())
                                .setPositiveButton(
                                        R.string.save,
                                        (d, w) -> {
                                            ms.setFile("");
                                            ms.setMode(dialogBinding.mode.getText().toString());
                                            ms.setRemovable(dialogBinding.removable.isChecked());
                                            ms.setNofua(dialogBinding.nofua.isChecked());
                                            ms.setFile(dialogBinding.path.getText().toString());
                                            refreshUI();
                                        })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }

                    @Override
                    public void onDeleteClick(MsGadgetUtil.MsList ms) {
                        new MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.delete)
                                .setMessage(
                                        ms.lun.equals("lun.0")
                                                ? String.format(
                                                        getString(R.string.delete_ms), ms.function)
                                                : String.format(
                                                        getString(R.string.delete_lun),
                                                        ms.function,
                                                        ms.lun))
                                .setPositiveButton(
                                        android.R.string.ok,
                                        (d, w) -> {
                                            gadget.setUDC(false);
                                            if (ms.lun.equals("lun.0")) {
                                                if (Arrays.asList(gadget.getEnabledFunctions())
                                                        .contains(ms.function))
                                                    gadget.setFunctionEnable(ms.function, false);
                                                if (!ms.delete()) {
                                                    showHint(
                                                            "Err: Any logical unit not removed",
                                                            Snackbar.LENGTH_SHORT);
                                                }
                                            } else {
                                                ms.delete();
                                            }
                                            gadget.getMsGadget().fixConf();
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
                            DialogMsBinding.inflate(LayoutInflater.from(requireContext()));
                    dialogBinding.msLayout.setEnabled(true);
                    dialogBinding.pathLayout.setEndIconOnClickListener(
                            (view) -> {
                                var remoteFS = AppUtil.remoteFS.getValue();
                                new FileDialogUtil(requireActivity())
                                        .selectFile(
                                                remoteFS,
                                                imgDir.getAbsolutePath(),
                                                (path) -> dialogBinding.path.setText(path));
                            });

                    var msList = gadget.getMsGadget().getMsFunNameList();
                    dialogBinding.msName.setSimpleItems(msList);
                    new MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(R.string.add)
                            .setView(dialogBinding.getRoot())
                            .setPositiveButton(
                                    R.string.save,
                                    (d, w) -> {
                                        var fun =
                                                String.format(
                                                        "mass_storage.%s",
                                                        dialogBinding.msName.getText().toString());
                                        var lun = gadget.getMsGadget().getFreeLun(fun);
                                        gadget.setUDC(false);
                                        if (Arrays.asList(gadget.getEnabledFunctions())
                                                .contains(fun))
                                            gadget.setFunctionEnable(fun, false);
                                        var ms = gadget.getMsGadget().createMs(fun, lun);
                                        if (ms != null) {
                                            ms.setMode(dialogBinding.mode.getText().toString());
                                            ms.setRemovable(dialogBinding.removable.isChecked());
                                            ms.setNofua(dialogBinding.nofua.isChecked());
                                            ms.setFile(dialogBinding.path.getText().toString());
                                        } else {
                                            showHint("Err: Create failed", Snackbar.LENGTH_SHORT);
                                        }
                                        gadget.getMsGadget().fixConf();
                                        gadget.setUDC(true);
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

    public void refreshUI() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        remoteFS = AppUtil.remoteFS.getValue();
        gadget = GadgetUtil.getGadget(remoteFS, AppUtil.useGadget);
        if (gadget == null) return;
        recyclerViewAdapter.list = gadget.getMsGadget().getMsList();
        binding.recyclerView.setAdapter(recyclerViewAdapter);
        var status = gadget.getMsGadget().getMsStatus(recyclerViewAdapter.list);
        binding.toolbar.setSubtitle(
                String.format(
                        "Gadget: %s,  Mounted: %d/%d", AppUtil.useGadget, status[0], status[1]));
        binding.toolbarLayout.setSubtitle(binding.toolbar.getSubtitle());
        imgDir =
                remoteFS.getFile(
                        String.format(
                                "%s/USBKIT",
                                Environment.getExternalStorageDirectory().getAbsolutePath()));
        if (!imgDir.exists()) imgDir.mkdirs();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (remoteFS == null) return true;
        var itemId = item.getItemId();
        if (itemId == R.id.create_img) {
            var dialogBinding = DialogImgBinding.inflate(LayoutInflater.from(requireActivity()));
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.create_img)
                    .setView(dialogBinding.getRoot())
                    .setPositiveButton(
                            R.string.create,
                            (d, w) -> {
                                var snackbar = showHint("", Snackbar.LENGTH_INDEFINITE);
                                var name = dialogBinding.name.getText().toString();
                                var size =
                                        Long.parseLong(dialogBinding.size.getText().toString())
                                                * 1024;
                                var process = new long[] {0};
                                runAsync(
                                        () -> {
                                            try (var img =
                                                    new BufferedWriter(
                                                            new OutputStreamWriter(
                                                                    remoteFS.getFile(
                                                                                    String.format(
                                                                                            "%s/%s",
                                                                                            imgDir,
                                                                                            name))
                                                                            .newOutputStream()))) {
                                                var zero = new char[1024];
                                                for (; process[0] < size; process[0]++)
                                                    img.write(zero);
                                            } catch (Exception e) {
                                            }
                                        });
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (process[0] < size) {
                                            snackbar.setText(
                                                    String.format(
                                                            "Creating %s (%d%%)",
                                                            name, process[0] * 100 / size));
                                            App.getMainHandler().postDelayed(this, 200);
                                        } else {
                                            showHint(
                                                    String.format("%s creation complete! ", name),
                                                    Snackbar.LENGTH_SHORT);
                                        }
                                    }
                                }.run();
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        if (itemId == R.id.sync_ms) {
            gadget.getMsGadget().fixConf();
            showHint("Repair complete", Snackbar.LENGTH_SHORT);
        }

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
