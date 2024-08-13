package com.anng6.usbkit.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Process;

import androidx.annotation.NonNull;

import com.anng6.usbkit.BuildConfig;
import com.anng6.usbkit.IRootService;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

class AIDLService extends RootService {
    class RootIPC extends IRootService.Stub {
        @Override
        public int getUid() {
            return Process.myUid();
        }

        @Override
        public IBinder getFileSystemService() {
            return FileSystemManager.getService();
        }
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new RootIPC();
    }
}

public class RootServiceUtil {
    public Context context;
    public RootServiceListener callback;
    public AIDLConnection aidlConn;

    public static interface RootServiceListener {
        public void onServiceConnected(ComponentName name, IBinder service);

        public void onServiceDisconnected(ComponentName name);

        public void onBindingDied(ComponentName name);

        public void onNullBinding(ComponentName name);

        public void onNoRootException();
    }

    class AIDLConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            aidlConn = this;
            if (callback != null) callback.onServiceConnected(name, service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            aidlConn = null;
            if (callback != null) callback.onServiceDisconnected(name);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            aidlConn = null;
            if (callback != null) callback.onBindingDied(name);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            aidlConn = null;
            if (callback != null) callback.onNullBinding(name);
        }
    }

    public void setListener(RootServiceListener callback) {
        this.callback = callback;
    }

    public AIDLConnection getConn() {
        return aidlConn;
    }

    public boolean isAlive() {
        return getConn() != null;
    }

    public void start() {
        if (!isAlive()) {
            Shell.getShell(
                    (shell) -> {
                        if (shell.isRoot()) {
                            Intent intent = new Intent(context, AIDLService.class);
                            RootService.bind(intent, new AIDLConnection());
                        } else {
                            try {
                                shell.close();
                            } catch (Exception e) {
                            }
                            if (callback != null) callback.onNoRootException();
                        }
                    });
        }
    }

    public void stop() {
        if (isAlive()) RootService.unbind(aidlConn);
    }

    public RootServiceUtil(@NonNull Context context) {
        this.context = context;
    }
}
