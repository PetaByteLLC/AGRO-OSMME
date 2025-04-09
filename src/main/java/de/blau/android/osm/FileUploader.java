package de.blau.android.osm;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.IOException;

import de.blau.android.App;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploader {
    private static final String UPLOAD_URL = "https://agro.brisklyminds.com/agroadmin/ws/file/upload";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://agro.brisklyminds.com/agroadmin/ws/file/download/%s";

//    public static String uploadFile(String path, Context context) throws IOException {
    public static String uploadFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
//            return uploadSingleFile(file, context);
            return uploadSingleFile(file);
        }
        return path;
    }

    // Загружает один файл и возвращает его id
    public static String uploadSingleFile(File file) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", getBasicAuthHeader(App.getCurrentInstance()))
                .post(requestBody)
                .build();

        try (Response response = App.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                // Предполагаем, что сервер возвращает id файла в теле ответа
                return response.body().string();  // Можно добавить обработку JSON, если сервер возвращает более сложный формат
            } else {
                System.err.println("Ошибка загрузки файла: " + file.getName() + " - Код: " + response.code());
                return null;
            }
        }
    }

    private static String getBasicAuthHeader(Context context) {
        String username = App.getPreferences(context).getAgroUsername();
        String password = App.getPreferences(context).getAgroPassword();
        String auth = username + ":" + password;
        return "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
    }
}
