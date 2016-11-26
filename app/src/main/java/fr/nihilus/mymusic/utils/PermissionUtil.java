package fr.nihilus.mymusic.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionUtil {

    public static final String EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final int EXTERNAL_STORAGE_REQUEST = 99;

    public static boolean hasExternalStoragePermission(@NonNull Context ctx) {
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestExternalStoragePermission(Activity activity) {
        String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(activity, permissions, EXTERNAL_STORAGE_REQUEST);
    }
}
