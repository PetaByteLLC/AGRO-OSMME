package de.blau.android.osm;

import android.content.Context;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

import de.blau.android.App;
import de.blau.android.R;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploader {
    private static final String UPLOAD_URL = "https://agro.brisklyminds.com/agroadmin/ws/file/upload";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://agro.brisklyminds.com/agroadmin/ws/file/download/%s";

    public static String uploadFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return uploadSingleFile(file);
        }
        return path;
    }

    public static void loadImage(String url, ImageView imageView, Context context) {
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.baseline_signal_wifi_statusbar_connected_no_internet_4_24)
                .error(R.drawable.no_image)
                .into(imageView);
    }

    // Загружает один файл и возвращает его id
    public static String uploadSingleFile(File file) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", getBasicAuthHeader())
                .post(requestBody)
                .build();

        try (Response response = App.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return String.format(DOWNLOAD_URL_TEMPLATE, response.body().string());
            } else {
                System.err.println("Ошибка загрузки файла: " + file.getName() + " - Код: " + response.code());
                return file.getPath();
            }
        }
    }

    private static String getBasicAuthHeader() {
        App currentInstance = App.getCurrentInstance();
        assert currentInstance != null;
        String username = App.getPreferences(currentInstance).getAgroUsername();
        String password = App.getPreferences(currentInstance).getAgroPassword();
        String auth = username + ":" + password;
        return "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
    }
}
