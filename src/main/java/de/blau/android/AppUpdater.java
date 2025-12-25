package de.blau.android;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppUpdater {

    private final Activity activity;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/PetaByteLLC/AGRO-OSMME/refs/heads/dev/src/main/assets/update-changelog.json";

    public AppUpdater(Activity activity) {
        this.activity = activity;
        this.context = activity;
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                String jsonResponse = fetchUpdateInfoFromServer();
                if (jsonResponse == null) {
                    throw new Exception("Не удалось получить данные с сервера.");
                }

                JSONObject jsonObject = new JSONObject(jsonResponse);
                UpdateInfo updateInfo = new UpdateInfo(jsonObject);

                handleUpdateCheck(updateInfo);

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Ошибка проверки обновления", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private String fetchUpdateInfoFromServer() throws Exception {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(UPDATE_URL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(8000); // 8 секунд
            urlConnection.setReadTimeout(8000);    // 8 секунд
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Ошибка сервера, код: " + responseCode);
            }

            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            return buffer.toString();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final Exception e) {
                    // ignore
                }
            }
        }
    }

    private void handleUpdateCheck(UpdateInfo updateInfo) {
        long currentVersionCode;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentVersionCode = pInfo.getLongVersionCode();
            } else {
                currentVersionCode = pInfo.versionCode;
            }
        } catch (Exception e) {
            currentVersionCode = -1;
        }

        if (updateInfo.getVersionCode() > currentVersionCode) {
            activity.runOnUiThread(() -> showUpdateDialog(updateInfo));
        }
    }

    private void showUpdateDialog(UpdateInfo updateInfo) {
        new AlertDialog.Builder(context)
                .setTitle("Доступно обновление")
                .setMessage("Новая версия: " + updateInfo.getVersionName() + "\n\nЧто нового:\n" + updateInfo.getChangelog())
                .setPositiveButton("Обновить", (dialog, which) -> checkPermissionsAndDownload(updateInfo))
                .setCancelable(false)
                .show();
    }

    private void checkPermissionsAndDownload(UpdateInfo updateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(context)
                        .setTitle("Требуется разрешение")
                        .setMessage("Для установки обновлений приложению необходимо разрешение на установку из неизвестных источников.")
                        .setPositiveButton("Настройки", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                            activity.startActivity(intent);
                        })
                        .setCancelable(false)
                        .show();
                return;
            }
        }
        downloadAndInstallApk(updateInfo);
    }
    
    private void downloadAndInstallApk(UpdateInfo updateInfo) {
        Toast.makeText(context, "Начало загрузки...", Toast.LENGTH_SHORT).show();

        String fileName = "app-update-" + updateInfo.getVersionName() + ".apk";
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.getApkUrl()));
        request.setTitle("Загрузка обновления");
        request.setDescription("Скачивание " + updateInfo.getVersionName());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setMimeType("application/vnd.android.package-archive");

        downloadManager.enqueue(request);
    }
}