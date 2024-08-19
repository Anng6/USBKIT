package com.anng6.usbkit.util;

import android.text.TextUtils;

import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.util.ArrayList;

public class GadgetUtil {
    private static String configFSPath;
    private static boolean available;

    public static String getConfigFSPath() {
        if (configFSPath != null) return configFSPath;
        var run = AppUtil.cmd("mount|grep ' type configfs '|awk '{print $3}'");
        if (run.code == 0) {
            configFSPath = run.result.get(0);
            return configFSPath;
        }
        return null;
    }

    public static boolean isAvailable(FileSystemManager remoteFS) {
        if (available) return true;
        var run = AppUtil.cmd("gunzip -c /proc/config.gz|grep 'CONFIG_USB_CONFIGFS=y'");
        if (run.code == 0 && getConfigFSPath() != null) {
            if (remoteFS.getFile(String.format("%s/usb_gadget", getConfigFSPath())).exists()) {
                available = true;
                return true;
            }
        }
        return false;
    }

    public static String[] getGadgetList(FileSystemManager remoteFS) {
        if (remoteFS == null || !isAvailable(remoteFS)) return null;
        return remoteFS.getFile(String.format("%s/usb_gadget", getConfigFSPath())).list();
    }

    public static Gadget getGadget(FileSystemManager remoteFS, String gadgetName) {
        if (remoteFS == null || !isAvailable(remoteFS)) return null;
        return new Gadget(remoteFS, gadgetName);
    }

    public static class Gadget {
        public FileSystemManager remoteFS;
        public String gadgetPath;

        public Gadget(FileSystemManager remoteFS, String gadgetName) {
            this.remoteFS = remoteFS;
            this.gadgetPath = String.format("%s/usb_gadget/%s", getConfigFSPath(), gadgetName);
        }

        public MsGadgetUtil getMsGadget() {
            return new MsGadgetUtil(this);
        }

        public String[] getFunctions() {
            return remoteFS.getFile(String.format("%s/functions", gadgetPath)).list();
        }

        public ExtendedFile getConfFile() {
            var configs = remoteFS.getFile(String.format("%s/configs", gadgetPath)).listFiles();
            if (configs != null)
                for (var config : configs) if (config.getName().endsWith(".1")) return config;
            return null;
        }

        public ExtendedFile[] getConfList() {
            var list = new ArrayList<ExtendedFile>();
            var configs = getConfFile();
            if (configs == null) return null;
            var links = configs.listFiles();
            if (links == null) return null;
            for (var link : links) if (link.isSymlink()) list.add(link);
            return list.toArray(new ExtendedFile[0]);
        }

        public String[] getEnabledFunctions() {
            var list = new ArrayList<String>();
            var confList = getConfList();
            if (confList == null) return new String[0];
            for (var conf : confList) {
                try {
                    list.add(conf.getCanonicalFile().getName());
                } catch (Exception e) {
                }
            }
            return list.toArray(new String[0]);
        }

        public void setFunctionEnable(String fun, boolean enable) {
            var configFile = getConfFile();
            if (configFile == null) return;
            var config = configFile.getChildFile(fun);
            if (config == null) return;
            if (enable) {
                try {
                    config.createNewSymlink(String.format("%s/functions/%s", gadgetPath, fun));
                } catch (Exception e) {
                }
            } else {
                config.delete();
            }
        }

        public void removeAllConf() {
            var confs = getConfList();
            if (confs == null) return;
            for (var conf : confs) conf.delete();
        }

        public void removeSystemConf() {
            var confs = getConfList();
            if (confs == null) return;
            for (var conf : confs) {
                var confName = conf.getName();
                if (confName.length() == 2 && confName.startsWith("f")) conf.delete();
            }
        }

        public String getController() {
            var controller = AppUtil.cmd("getprop vendor.usb.controller");
            if (controller.code == 0) return controller.result.get(0);
            return null;
        }

        public boolean getUDC() {
            return !TextUtils.isEmpty(
                    AppUtil.readLine(remoteFS.getFile(String.format("%s/UDC", gadgetPath))));
        }

        public void setUDC(boolean enable) {
            if (getUDC() == enable) return;
            var controller = getController();
            if (!TextUtils.isEmpty(controller))
                AppUtil.writeLine(
                        remoteFS.getFile(String.format("%s/UDC", gadgetPath)),
                        enable ? controller : "\n");
        }

        public String[] getInfo() {
            var idVendor =
                    AppUtil.readLine(remoteFS.getFile(String.format("%s/idVendor", gadgetPath)));
            var idProduct =
                    AppUtil.readLine(remoteFS.getFile(String.format("%s/idProduct", gadgetPath)));
            var manufacturer =
                    AppUtil.readLine(
                            remoteFS.getFile(
                                    String.format("%s/strings/0x409/manufacturer", gadgetPath)));
            var product =
                    AppUtil.readLine(
                            remoteFS.getFile(
                                    String.format("%s/strings/0x409/product", gadgetPath)));
            var serialnumber =
                    AppUtil.readLine(
                            remoteFS.getFile(
                                    String.format("%s/strings/0x409/serialnumber", gadgetPath)));
            return new String[] {idVendor, idProduct, manufacturer, product, serialnumber};
        }

        public void setInfo(String[] info) {
            if (info.length != 5) return;
            AppUtil.writeLine(remoteFS.getFile(String.format("%s/idVendor", gadgetPath)), info[0]);
            AppUtil.writeLine(remoteFS.getFile(String.format("%s/idProduct", gadgetPath)), info[1]);
            AppUtil.writeLine(
                    remoteFS.getFile(String.format("%s/strings/0x409/manufacturer", gadgetPath)),
                    info[2]);
            AppUtil.writeLine(
                    remoteFS.getFile(String.format("%s/strings/0x409/product", gadgetPath)),
                    info[3]);
            AppUtil.writeLine(
                    remoteFS.getFile(String.format("%s/strings/0x409/serialnumber", gadgetPath)),
                    info[4]);
        }
    }
}
