package com.dirmove;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper {

    private static final String TAG = "StorageHelper";

    public static List<String> getExternalStorageDirectories(Context context) {
        List<String> results = new ArrayList<>();
        File[] externalDirs = context.getExternalFilesDirs(null);
        for (File file : externalDirs) {
            if (file != null) {
                String path = file.getAbsolutePath();
                if (path.contains("/Android/data/")) {
                    results.add(path.split("/Android/data/")[0]);
                }
            }
        }
        return results;
    }

    public static String getInternalStoragePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getSdCardPath(Context context) {
        List<String> storageDirs = getExternalStorageDirectories(context);
        String internalPath = getInternalStoragePath();
        for (String dir : storageDirs) {
            if (!dir.equals(internalPath)) {
                return dir;
            }
        }
        return null;
    }

    public static List<DirectoryModel> scanForMatchingDirectories(Context context) {
        List<DirectoryModel> matchingDirs = new ArrayList<>();
        String internalPath = getInternalStoragePath();
        String sdCardPath = getSdCardPath(context);

        if (sdCardPath == null) {
            return matchingDirs;
        }

        File internalDir = new File(internalPath);
        File[] files = internalDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File sdMatch = new File(sdCardPath, file.getName());
                    if (sdMatch.exists() && sdMatch.isDirectory()) {
                        matchingDirs.add(new DirectoryModel(file.getName(), file.getAbsolutePath(), true));
                    }
                }
            }
        }

        return matchingDirs;
    }

    public static boolean moveFiles(String sourcePath, String destPath) {
        File source = new File(sourcePath);
        File dest = new File(destPath);

        if (!source.exists()) {
            return false;
        }

        if (source.isDirectory()) {
            if (!dest.exists()) {
                if (!dest.mkdirs()) return false;
            }
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!moveFiles(file.getAbsolutePath(), new File(dest, file.getName()).getAbsolutePath())) {
                        return false;
                    }
                }
            }
            return source.delete();
        } else {
            try {
                copyFile(source, dest);
                return source.delete();
            } catch (IOException e) {
                Log.e(TAG, "Error moving file: " + source.getAbsolutePath(), e);
                return false;
            }
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
}
