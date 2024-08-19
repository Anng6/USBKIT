package com.anng6.usbkit.util;

import android.text.TextUtils;

import android.util.Log;
import com.anng6.usbkit.App;
import com.topjohnwu.superuser.nio.ExtendedFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MsGadgetUtil {
    public static final String MS_RO = "Read-Only";
    public static final String MS_RW = "Read-Write";
    public static final String MS_CD = "CD-Rom";
    GadgetUtil.Gadget gadget;

    public static class MsList {
        public String function;
        public String lun;
        public String file;
        public String mode;
        public boolean removable;
        public boolean nofua;
        private ExtendedFile tool;

        public MsList(
                MsGadgetUtil context,
                String function,
                String lun,
                String file,
                String mode,
                boolean removable,
                boolean nofua) {
            this.function = function;
            this.lun = lun;
            this.file = file;
            this.mode = mode;
            this.removable = removable;
            this.nofua = nofua;
            this.tool =
                    context.gadget.remoteFS.getFile(
                            String.format("%s/functions/%s/%s", context.gadget.gadgetPath, function, lun));
        }

        public void setFile(String path) {
            this.file = path;
            if (TextUtils.isEmpty(path)) path = "\n";
            AppUtil.writeLine(tool.getChildFile("file"), path);
        }

        public void setMode(String mode) {
            var ro = mode.equals(MS_RO) ? "1" : "0";
            var cd = mode.equals(MS_CD) ? "1" : "0";
            AppUtil.writeLine(tool.getChildFile("ro"), ro);
            AppUtil.writeLine(tool.getChildFile("cdrom"), cd);
            this.mode = mode;
        }

        public void setRemovable(boolean removable) {
            AppUtil.writeLine(tool.getChildFile("removable"), removable ? "1" : "0");
            this.removable = removable;
        }

        public void setNofua(boolean nofua) {
            AppUtil.writeLine(tool.getChildFile("nofua"), nofua ? "1" : "0");
            this.nofua = nofua;
        }

        public boolean delete() {
            if (lun.equals("lun.0")) return tool.getParentFile().delete();
            return tool.delete();
        }
    }

    public MsGadgetUtil(GadgetUtil.Gadget gadget) {
        this.gadget = gadget;
    }

    public String[] getMsFunList() {
        var list = new ArrayList<String>();
        var funs = gadget.getFunctions();
        if (funs == null) return new String[0];
        for (var fun : funs) if (fun.startsWith("mass_storage.")) list.add(fun);
        return list.toArray(new String[0]);
    }

    public String[] getMsFunNameList() {
        var list = getMsFunList();
        for (var i = 0; i < list.length; i++) list[i] = list[i].substring(13);
        return list;
    }

    public MsList getMs(String fun, String lun) {
        var ms =
                gadget.remoteFS.getFile(
                        String.format("%s/functions/%s/%s", gadget.gadgetPath, fun, lun));
        var path = AppUtil.readLine(ms.getChildFile("file"));
        var ro = Integer.parseInt(AppUtil.readLine(ms.getChildFile("ro"))) == 1;
        var cdrom = Integer.parseInt(AppUtil.readLine(ms.getChildFile("cdrom"))) == 1;
        var mode = cdrom ? MS_CD : (ro ? MS_RO : MS_RW);
        var removable = Integer.parseInt(AppUtil.readLine(ms.getChildFile("removable"))) == 1;
        var nofua = Integer.parseInt(AppUtil.readLine(ms.getChildFile("nofua"))) == 1;
        return new MsList(this, fun, lun, path, mode, removable, nofua);
    }

    public List<MsList> getMsList() {
        var list = new ArrayList<MsList>();
        var funs = getMsFunList();
        for (var fun : funs) {
            var luns =
                    gadget.remoteFS
                            .getFile(String.format("%s/functions/%s", gadget.gadgetPath, fun))
                            .list();
            if (luns == null) continue;
            for (var lun : luns) {
                if (!lun.startsWith("lun")) continue;
                list.add(getMs(fun, lun));
            }
        }
        return list;
    }

    public String getFreeLun(String fun) {
        if (fun.startsWith("mass_storage.")) {
            var luns =
                    gadget.remoteFS
                            .getFile(String.format("%s/functions/%s", gadget.gadgetPath, fun))
                            .list();
            if (luns != null) {
                var lunList = Arrays.asList(luns);
                for (var lun : luns) {
                    var lunNumber = Integer.valueOf(lun.substring(4));
                    var nextLun = String.format("lun.%d", lunNumber + 1);
                    if (!lunList.contains(nextLun)) return nextLun;
                }
            }
        }
        return "lun.0";
    }

    public MsList createMs(String fun, String lun) {
        if (fun.startsWith("mass_storage.") && lun.startsWith("lun.")) {
            if (gadget.remoteFS
                    .getFile(
                            lun.equals("lun.0")
                                    ? String.format("%s/functions/%s", gadget.gadgetPath, fun)
                                    : String.format(
                                            "%s/functions/%s/%s", gadget.gadgetPath, fun, lun))
                    .mkdirs()) return getMs(fun, lun);
        }
        return null;
    }

    public int[] getMsStatus(List<MsList> list) {
        int mounted = 0;
        for (var ms : list) if (!TextUtils.isEmpty(ms.file)) mounted++;
        return new int[] {mounted, list.size()};
    }

    public void fixConf() {
        gadget.removeSystemConf();
        var funs = getMsFunList();
        if (funs != null) for (var fun : funs) gadget.setFunctionEnable(fun, true);
    }
}
