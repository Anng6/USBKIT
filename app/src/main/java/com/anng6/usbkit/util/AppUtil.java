package com.anng6.usbkit.util;

import androidx.lifecycle.MutableLiveData;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class AppUtil {
    public static MutableLiveData<FileSystemManager> remoteFS = new MutableLiveData<>();
    public static String useGadget = "g1";

    public static shellResult cmd(String... command) {
        var runResult = new ArrayList<String>();
        var suResult = Shell.getCachedShell().newJob().add(command).to(runResult).exec();
        return new shellResult(suResult.getCode(), runResult);
    }

    public static String readLine(ExtendedFile file) {
        try (var reader = new BufferedReader(new InputStreamReader(file.newInputStream()))) {
            return reader.readLine();
        } catch (Exception e) {
        }
        return null;
    }

    public static void writeLine(ExtendedFile file, String str) {
        try (var writer = new BufferedWriter(new OutputStreamWriter(file.newOutputStream()))) {
            writer.write(str);
        } catch (Exception e) {
        }
    }

    public static void writeLine(ExtendedFile file, byte[] data) {
        try (var writer = file.newOutputStream()) {
            writer.write(data);
        } catch (Exception e) {
        }
    }

    public static class shellResult {
        public int code;
        public ArrayList<String> result;

        public shellResult(int code, ArrayList<String> result) {
            this.code = code;
            this.result = result;
        }
    }
}
