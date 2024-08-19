package com.anng6.usbkit.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.anng6.usbkit.R;
import com.anng6.usbkit.databinding.DialogFileBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileDialogUtil {
    private Activity activity;
    private MaterialAlertDialogBuilder fileDialog;
    private DialogFileBinding binding;
    private String currentPath;

    public FileDialogUtil(Activity activity) {
        this.activity = activity;
        binding = DialogFileBinding.inflate(LayoutInflater.from(activity));
        fileDialog =
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.select_file)
                        .setView(binding.getRoot());
    }

    public static interface onSelectFileListener {
        public void onSelectFile(String path);
    }

    private void refreshFileList(FileSystemManager remoteFS) {
        binding.path.setText(currentPath);
        if (remoteFS == null) return;
        var files = remoteFS.getFile(currentPath).listFiles();
        List<ExtendedFile> fileList = new ArrayList<>();
        if (files != null) {
            fileList = Arrays.asList(files);
            Collections.sort(
                    fileList,
                    new Comparator<ExtendedFile>() {
                        @Override
                        public int compare(ExtendedFile o1, ExtendedFile o2) {
                            if (o1.isDirectory() && o2.isFile()) return -1;
                            if (o1.isFile() && o2.isDirectory()) return 1;
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
        }

        var fileAdp = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);

        if (!"/".equals(currentPath)) fileAdp.add("../");
        for (var file : fileList) {
            if (file.isDirectory()) {
                fileAdp.add(file.getName() + "/");
            } else {
                fileAdp.add(file.getName());
            }
        }
        binding.list.setAdapter(fileAdp);
    }

    public void selectFile(FileSystemManager remoteFS, String dir, onSelectFileListener callback) {
        currentPath = dir;
        if (!currentPath.endsWith("/")) currentPath += "/";
        var dialog = fileDialog.show();
        binding.list.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (((TextView) view).getText().equals("../")) {
                        currentPath =
                                remoteFS.getFile(currentPath).getParentFile().getAbsolutePath();
                        if (!"/".equals(currentPath)) currentPath += "/";
                        refreshFileList(remoteFS);
                    } else {
                        currentPath += ((TextView) view).getText();
                        if (remoteFS.getFile(currentPath).isDirectory()) {
                            refreshFileList(remoteFS);
                        } else {
                            dialog.dismiss();
                            callback.onSelectFile(currentPath);
                        }
                    }
                });
        refreshFileList(remoteFS);
    }
}
