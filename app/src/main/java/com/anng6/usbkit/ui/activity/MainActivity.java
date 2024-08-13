package com.anng6.usbkit.ui.activity;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.anng6.usbkit.IRootService;
import com.anng6.usbkit.R;
import com.anng6.usbkit.databinding.ActivityMainBinding;
import com.anng6.usbkit.util.AppUtil;
import com.anng6.usbkit.util.GadgetUtil;
import com.anng6.usbkit.util.RootServiceUtil;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    public RootServiceUtil rootService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        var navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_main);
        var navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (AppUtil.remoteFS.getValue() == null) {
            if (rootService == null) {
                rootService = new RootServiceUtil(this);
                rootService.setListener(
                        new RootServiceUtil.RootServiceListener() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                IRootService ipc = IRootService.Stub.asInterface(service);
                                try {
                                    var uid = ipc.getUid();
                                    if (uid == 0) {
                                        var binder = ipc.getFileSystemService();
                                        var remoteFS = FileSystemManager.getRemote(binder);
                                        AppUtil.remoteFS.postValue(remoteFS);
                                    } else {
                                        var rootServiceConn = rootService.getConn();
                                        if (rootServiceConn != null)
                                            RootService.unbind(rootService.getConn());
                                        showHint(
                                                String.format(
                                                        "Err: RootService uid(%d) is not 0", uid),
                                                Snackbar.LENGTH_INDEFINITE,
                                                R.string.retry,
                                                (v) -> rootService.start());
                                    }
                                } catch (Exception e) {
                                }
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                AppUtil.remoteFS.postValue(null);
                                showHint(
                                        "Err: RootService disconnected",
                                        Snackbar.LENGTH_INDEFINITE,
                                        R.string.retry,
                                        (v) -> rootService.start());
                            }

                            @Override
                            public void onBindingDied(ComponentName name) {
                                AppUtil.remoteFS.postValue(null);
                                showHint(
                                        "Err: RootService binding died",
                                        Snackbar.LENGTH_INDEFINITE,
                                        R.string.retry,
                                        (v) -> rootService.start());
                            }

                            @Override
                            public void onNullBinding(ComponentName name) {
                                AppUtil.remoteFS.postValue(null);
                                showHint(
                                        "Err: RootService null binding",
                                        Snackbar.LENGTH_INDEFINITE,
                                        R.string.retry,
                                        (v) -> rootService.start());
                            }

                            @Override
                            public void onNoRootException() {
                                AppUtil.remoteFS.postValue(null);
                                showHint(
                                        R.string.no_root,
                                        Snackbar.LENGTH_INDEFINITE,
                                        R.string.retry,
                                        (v) -> rootService.start());
                            }
                        });
            }
            rootService.start();
        }
        AppUtil.remoteFS.observe(
                this,
                (remoteFS) -> {
                    if (remoteFS != null && !GadgetUtil.isAvailable(remoteFS))
                        showHint(
                                "Err: USB-Gadget ConfigFS not available",
                                Snackbar.LENGTH_INDEFINITE);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
