package com.dirmove;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 123;
    private static final String PREFS_NAME = "DirMovePrefs";
    private static final String KEY_SELECTED_DIRS = "selectedDirs";

    private RecyclerView recyclerView;
    private DirectoryAdapter adapter;
    private List<DirectoryModel> directoryList = new ArrayList<>();
    private Button btnMove;
    private ProgressBar progressBar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        btnMove = findViewById(R.id.btnMove);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DirectoryAdapter(directoryList);
        recyclerView.setAdapter(adapter);

        btnMove.setOnClickListener(v -> handleMoveFiles());

        if (checkPermissions()) {
            loadDirectories();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_PERMISSION);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION) {
            if (checkPermissions()) {
                loadDirectories();
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDirectories();
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadDirectories() {
        directoryList.clear();
        String sdCardPath = StorageHelper.getSdCardPath(this);
        if (sdCardPath == null) {
            Toast.makeText(this, "SD card not found", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> savedSelectedDirs = prefs.getStringSet(KEY_SELECTED_DIRS, null);

        if (savedSelectedDirs == null) {
            // First run
            List<DirectoryModel> scanned = StorageHelper.scanForMatchingDirectories(this);
            directoryList.addAll(scanned);
            saveSelectedDirectories();
        } else {
            // Subsequent runs
            String internalPath = StorageHelper.getInternalStoragePath();
            File internalDir = new File(internalPath);
            File[] files = internalDir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        File sdMatch = new File(sdCardPath, file.getName());
                        if (sdMatch.exists() && sdMatch.isDirectory()) {
                            boolean isSelected = savedSelectedDirs.contains(file.getAbsolutePath());
                            directoryList.add(new DirectoryModel(file.getName(), file.getAbsolutePath(), isSelected));
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveSelectedDirectories() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> selectedPaths = new HashSet<>();
        for (DirectoryModel dir : directoryList) {
            if (dir.isSelected()) {
                selectedPaths.add(dir.getPath());
            }
        }
        prefs.edit().putStringSet(KEY_SELECTED_DIRS, selectedPaths).apply();
    }

    private void handleMoveFiles() {
        saveSelectedDirectories();
        String sdCardPath = StorageHelper.getSdCardPath(this);
        if (sdCardPath == null) {
            Toast.makeText(this, "SD card not found", Toast.LENGTH_LONG).show();
            return;
        }

        btnMove.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            int movedCount = 0;
            for (DirectoryModel dir : directoryList) {
                if (dir.isSelected()) {
                    File sdMatch = new File(sdCardPath, dir.getName());
                    if (StorageHelper.moveFiles(dir.getPath(), sdMatch.getAbsolutePath())) {
                        movedCount++;
                    }
                }
            }
            final int finalMovedCount = movedCount;
            mainHandler.post(() -> {
                btnMove.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Successfully moved " + finalMovedCount + " directories", Toast.LENGTH_SHORT).show();
                loadDirectories(); // Refresh list after move
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
