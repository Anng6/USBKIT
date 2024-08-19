package com.anng6.usbkit.util;

import android.text.TextUtils;

import com.topjohnwu.superuser.nio.ExtendedFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HidGadgetUtil {
    public static final int HID_KEYBOARD = 1;
    public static final int HID_MOUSE = 2;
    public static final byte[] HID_DESC_KEYBOARD =
            new byte[] {
                5, 1, 9, 6, -95, 1, 5, 7, 25, -32, 41, -25, 21, 0, 37, 1, 117, 1, -107, 8, -127, 2,
                -107, 1, 117, 8, -127, 3, -107, 5, 117, 1, 5, 8, 25, 1, 41, 5, -111, 2, -107, 1,
                117, 3, -111, 3, -107, 6, 117, 8, 21, 0, 37, 101, 5, 7, 25, 0, 41, 101, -127, 0, -64
            };
    public static final byte[] HID_DESC_MOUSE =
            new byte[] {
                5, 1, 9, 2, -95, 1, 9, 1, -95, 0, 5, 9, 25, 1, 41, 5, 21, 0, 37, 1, -107, 5, 117, 1,
                -127, 2, -107, 1, 117, 3, -127, 1, 5, 1, 9, 48, 9, 49, 9, 56, 21, -127, 37, 127,
                117, 8, -107, 3, -127, 6, -64, -64
            };
    GadgetUtil.Gadget gadget;

    public static String getProtocolName(int id) {
        return switch (id) {
            case HID_KEYBOARD -> "Keyboard";
            case HID_MOUSE -> "Mouse";
            default -> "None";
        };
    }

    public static int getProtocolId(String name) {
        return switch (name) {
            case "Keyboard" -> HID_KEYBOARD;
            case "Mouse" -> HID_MOUSE;
            default -> 0;
        };
    }
    
    public static String getReportLength(int id) {
        return switch (id) {
            case HID_KEYBOARD -> "8";
            case HID_MOUSE -> "4";
            default -> "0";
        };
    }
    
    public static byte[] getReportDesc(int id) {
        return switch (id) {
            case HID_KEYBOARD -> HID_DESC_KEYBOARD;
            case HID_MOUSE -> HID_DESC_MOUSE;
            default -> new byte[0];
        };
    }

    public static class HidList {
        public String function;
        public int type;
        private HidGadgetUtil context;
        private ExtendedFile tool;

        public HidList(HidGadgetUtil context, String function, int type) {
            this.function = function;
            this.type = type;
            this.context = context;
            this.tool =
                    context.gadget.remoteFS.getFile(
                            String.format("%s/functions/%s", context.gadget.gadgetPath, function));
        }

        public boolean getEnabled() {
            return context.getEnabledHidList().contains(function);
        }

        public void setEnable(boolean enable) {
            context.gadget.setFunctionEnable(function, enable);
        }

        public boolean delete() {
            return tool.delete();
        }
    }

    public HidGadgetUtil(GadgetUtil.Gadget gadget) {
        this.gadget = gadget;
    }

    public String[] getHidFunList() {
        var list = new ArrayList<String>();
        var funs = gadget.getFunctions();
        if (funs == null) return new String[0];
        for (var fun : funs) if (fun.startsWith("hid.")) list.add(fun);
        return list.toArray(new String[0]);
    }

    public HidList getHid(String fun) {
        var hid = gadget.remoteFS.getFile(String.format("%s/functions/%s", gadget.gadgetPath, fun));
        var type = Integer.parseInt(AppUtil.readLine(hid.getChildFile("protocol")));
        return new HidList(this, fun, type);
    }

    public List<HidList> getHidList() {
        var list = new ArrayList<HidList>();
        var funs = getHidFunList();
        for (var fun : funs) list.add(getHid(fun));
        return list;
    }

    public HidList createHid(String fun, int type) {
        if (fun.startsWith("hid.")) {
            var hid =
                    gadget.remoteFS.getFile(
                            String.format("%s/functions/%s", gadget.gadgetPath, fun));
            if (hid.mkdirs()) {
                AppUtil.writeLine(hid.getChildFile("protocol"), String.valueOf(type));
                AppUtil.writeLine(hid.getChildFile("subclass"), "1");
                AppUtil.writeLine(hid.getChildFile("report_length"), getReportLength(type));
                AppUtil.writeLine(hid.getChildFile("report_desc"), getReportDesc(type));
                return getHid(fun);
            }
        }
        return null;
    }

    public ArrayList<String> getEnabledHidList() {
        var list = new ArrayList<String>();
        var funs = gadget.getEnabledFunctions();
        for (var fun : funs) if (fun.startsWith("hid.")) list.add(fun);
        return list;
    }

    public int[] getHidStatus(List<HidList> list) {
        return new int[] {getEnabledHidList().size(), list.size()};
    }
}
