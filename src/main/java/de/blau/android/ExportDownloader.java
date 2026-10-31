package de.blau.android;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.blau.android.util.Notifications;
import de.blau.android.util.ScreenMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads field exports into the public Downloads folder and shows the progress in a notification.
 *
 * The server can take several minutes to build an export before it sends the first byte. Android's DownloadManager
 * has a fixed ~20 s read timeout and then retries, restarting the export on the server each time, so we download
 * ourselves with a long read timeout instead.
 */
public final class ExportDownloader {

    private static final String TAG = "ExportDownloader";

    /** nginx waits up to 300 s for the backend, so wait as long */
    private static final long READ_TIMEOUT_MINUTES    = 5;
    private static final long CONNECT_TIMEOUT_SECONDS = 30;

    private static final int  NOTIFICATION_ID       = 0x4578706f; // "Expo"
    private static final long PROGRESS_INTERVAL_MS  = 500;
    private static final int  BUFFER_SIZE           = 64 * 1024;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean   RUNNING  = new AtomicBoolean(false);

    private static OkHttpClient client;

    private ExportDownloader() {
        // static only
    }

    /** where the file ended up */
    private static final class Saved {
        final String name;
        final Uri    uri;

        Saved(@NonNull String name, @NonNull Uri uri) {
            this.name = name;
            this.uri = uri;
        }
    }

    /** reports copied bytes */
    private interface ProgressListener {
        void onProgress(long done, long total);
    }

    /**
     * Start downloading an export in the background
     *
     * @param context any Context
     * @param url the export URL
     * @param fileName the file name to use in Downloads
     */
    public static void download(@NonNull Context context, @NonNull String url, @NonNull String fileName) {
        final Context appContext = context.getApplicationContext();
        if (!RUNNING.compareAndSet(false, true)) {
            ScreenMessage.toastTopInfo(context, "Экспорт уже выполняется, ход виден в уведомлениях");
            return;
        }
        ScreenMessage.toastTopInfo(context, "Готовим файл, ход виден в уведомлениях");
        showPreparing(appContext, fileName);
        EXECUTOR.execute(() -> {
            String message;
            Saved saved = null;
            try {
                saved = fetchAndSave(appContext, url, fileName);
                message = "Файл сохранён в «Загрузки»: " + saved.name;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Export timed out " + url, e);
                message = "Сервер слишком долго готовит файл, попробуйте позже";
            } catch (ExportHttpException e) {
                Log.e(TAG, "Export failed " + url + " code " + e.code);
                message = e.code == 504 ? "Сервер не успел подготовить файл, попробуйте позже" : "Ошибка экспорта (код " + e.code + ")";
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Export failed " + url, e);
                message = "Не удалось скачать файл: проверьте интернет и попробуйте ещё раз";
            } finally {
                RUNNING.set(false);
            }
            if (saved != null) {
                showDone(appContext, saved, message);
            } else {
                showFailed(appContext, message);
            }
            final String msg = message;
            final boolean success = saved != null;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    ScreenMessage.toastTopInfo(appContext, msg);
                } else {
                    ScreenMessage.toastTopError(appContext, msg);
                }
            });
        });
    }

    // ---------------------------------------------------------------- notifications

    @NonNull
    private static NotificationCompat.Builder baseNotification(@NonNull Context context) {
        return Notifications.builder(context).setSmallIcon(R.drawable.logo_simplified).setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    /**
     * Server is still building the file, we don't know how long it takes
     */
    private static void showPreparing(@NonNull Context context, @NonNull String fileName) {
        notify(context, baseNotification(context).setContentTitle("Экспорт: " + fileName).setContentText("Сервер готовит файл…")
                .setProgress(0, 0, true).setOngoing(true));
    }

    /**
     * Data is arriving
     *
     * @param total size in bytes or -1 if the server didn't say
     */
    private static void showProgress(@NonNull Context context, @NonNull String fileName, long done, long total) {
        NotificationCompat.Builder b = baseNotification(context).setContentTitle("Экспорт: " + fileName).setOngoing(true);
        if (total > 0) {
            int percent = (int) Math.min(100, done * 100 / total);
            b.setProgress(100, percent, false).setContentText("Скачивание " + percent + "% · " + formatSize(done) + " из " + formatSize(total));
        } else {
            b.setProgress(0, 0, true).setContentText("Скачивание · " + formatSize(done));
        }
        notify(context, b);
    }

    private static void showDone(@NonNull Context context, @NonNull Saved saved, @NonNull String message) {
        NotificationCompat.Builder b = baseNotification(context).setContentTitle("Экспорт готов").setContentText(message + ". Нажмите, чтобы открыть")
                .setAutoCancel(true);
        PendingIntent open = openIntent(context, saved);
        if (open != null) {
            b.setContentIntent(open);
        }
        notify(context, b);
    }

    private static void showFailed(@NonNull Context context, @NonNull String message) {
        notify(context, baseNotification(context).setContentTitle("Экспорт не удался").setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)).setAutoCancel(true));
    }

    private static void notify(@NonNull Context context, @NonNull NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // notifications not allowed (Android 13+ permission), the toasts still tell the user
            Log.w(TAG, "Can't show notification " + e.getMessage());
        }
    }

    /**
     * Tapping the finished notification opens the file with a matching app
     */
    @Nullable
    private static PendingIntent openIntent(@NonNull Context context, @NonNull Saved saved) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(saved.uri, mimeType(saved.name)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // chooser shows "no app can open this" instead of failing silently, e.g. for .gpkg
            Intent chooser = Intent.createChooser(view, "Открыть файл").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            return PendingIntent.getActivity(context, NOTIFICATION_ID, chooser, flags);
        } catch (RuntimeException e) {
            Log.w(TAG, "Can't create open intent " + e.getMessage());
            return null;
        }
    }

    @NonNull
    private static String formatSize(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%d КБ", Math.max(1, bytes / 1024));
        }
        return String.format(Locale.getDefault(), "%.1f МБ", bytes / (1024.0 * 1024.0));
    }

    // ---------------------------------------------------------------- download

    /**
     * Get a client with long timeouts, sharing the app's connection pool and user agent
     *
     * @return the OkHttpClient
     */
    @NonNull
    private static synchronized OkHttpClient getClient() {
        if (client == null) {
            client = App.getHttpClient().newBuilder().connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_MINUTES, TimeUnit.MINUTES).writeTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS) // no overall limit, the file itself can be large
                    .build();
        }
        return client;
    }

    /**
     * Download the export and write it to Downloads
     *
     * @param context application Context
     * @param url export URL
     * @param fileName wanted file name
     * @return where the file was saved
     * @throws IOException on network or storage errors
     */
    @NonNull
    private static Saved fetchAndSave(@NonNull Context context, @NonNull String url, @NonNull String fileName) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ExportHttpException(response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response");
            }
            final long total = body.contentLength(); // -1 if unknown
            showProgress(context, fileName, 0, total);
            final long[] lastUpdate = { 0 };
            ProgressListener listener = (done, size) -> {
                long now = SystemClock.elapsedRealtime();
                if (now - lastUpdate[0] >= PROGRESS_INTERVAL_MS) { // don't flood the notification manager
                    lastUpdate[0] = now;
                    showProgress(context, fileName, done, size);
                }
            };
            try (InputStream in = body.byteStream()) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? saveToMediaStore(context, in, fileName, total, listener)
                        : saveToPublicDir(context, in, fileName, total, listener);
            }
        }
    }

    /**
     * Android 10+: save through MediaStore, no storage permission needed
     */
    @NonNull
    private static Saved saveToMediaStore(@NonNull Context context, @NonNull InputStream in, @NonNull String fileName, long total,
            @NonNull ProgressListener listener) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType(fileName));
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Can't create file in Downloads");
        }
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("Can't open file in Downloads");
            }
            copy(in, out, total, listener);
        } catch (IOException | RuntimeException e) {
            resolver.delete(uri, null, null); // don't leave a broken half file behind
            throw e;
        }
        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return new Saved(fileName, uri);
    }

    /**
     * Android 5-9: write directly into the public Downloads directory
     */
    @NonNull
    private static Saved saveToPublicDir(@NonNull Context context, @NonNull InputStream in, @NonNull String fileName, long total,
            @NonNull ProgressListener listener) throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Can't create Downloads directory");
        }
        File file = uniqueFile(dir, fileName);
        try (OutputStream out = new FileOutputStream(file)) {
            copy(in, out, total, listener);
        } catch (IOException | RuntimeException e) {
            file.delete(); // NOSONAR don't leave a broken half file behind
            throw e;
        }
        Uri uri = FileProvider.getUriForFile(context, context.getString(R.string.content_provider), file);
        return new Saved(file.getName(), uri);
    }

    /**
     * Find a free name like "exported (1).xlsx" so earlier exports are not overwritten
     */
    @NonNull
    private static File uniqueFile(@NonNull File dir, @NonNull String fileName) {
        File file = new File(dir, fileName);
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; file.exists(); i++) {
            file = new File(dir, base + " (" + i + ")" + ext);
        }
        return file;
    }

    private static void copy(@NonNull InputStream in, @NonNull OutputStream out, long total, @NonNull ProgressListener listener) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long done = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            done += read;
            listener.onProgress(done, total);
        }
        out.flush();
    }

    @NonNull
    private static String mimeType(@NonNull String fileName) {
        if (fileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (fileName.endsWith(".geojson")) {
            return "application/geo+json";
        } else if (fileName.endsWith(".gpkg")) {
            return "application/geopackage+sqlite3";
        }
        return "application/octet-stream";
    }

    /**
     * Non 2xx answer from the server
     */
    private static final class ExportHttpException extends IOException {
        private static final long serialVersionUID = 1L;

        final int code;

        ExportHttpException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }
}
