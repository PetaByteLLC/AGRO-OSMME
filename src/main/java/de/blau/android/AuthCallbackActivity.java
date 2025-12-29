package de.blau.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.blau.android.prefs.Preferences;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthCallbackActivity extends AppCompatActivity {

    private static final String TAG = "AuthCallbackActivity";
    private TextView tvStatus;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_callback);
        tvStatus = findViewById(R.id.tv_auth_status);
        Log.d(TAG, "onCreate: Intent received: " + getIntent());
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "Intent is null.");
            return;
        }

        String action = intent.getAction();
        Uri data = intent.getData();

        Log.d(TAG, "Action: " + action);
        Log.d(TAG, "Data: " + data);

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            tvStatus.setText("Deep Link получен:\n" + data + "\n\n");
            if ("agroapp".equals(data.getScheme()) && "auth".equals(data.getHost())) {
                String backendToken = data.getQueryParameter("code");
                Log.i(TAG, "Backend Token: " + backendToken);
                tvStatus.append("Токен бэкенда: " + backendToken + "\n");
                Future<Boolean> future = saveAuthDataCallable(backendToken);

                new Thread(() -> {
                    try {
                        boolean success = future.get(); // ждем завершения
                        runOnUiThread(() -> {
                            Intent mainAppIntent = new Intent(AuthCallbackActivity.this, Main.class);
                            mainAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainAppIntent);
                            finish();
                        });
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                Log.w(TAG, "Получен deep link, но он не соответствует ожидаемой схеме/хосту.");
                tvStatus.append("Получен неожиданный deep link.\n");
            }
        } else {
            Log.d(TAG, "Activity запущена не через ACTION_VIEW или data is null.");
            tvStatus.append("Activity запущена не через deep link.\n");
        }
    }

    private Future<Boolean> saveAuthDataCallable(String backendToken) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Boolean> callable = () -> {
            Preferences prefs = App.getPreferences(this);
            OkHttpClient client = App.getHttpClient();
            try {
                Request request = new Request.Builder()
                        .url(AgroConstants.ESI_URL + "/auth")
                        .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), backendToken))
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) return false;

                    JSONObject responseObject = new JSONObject(response.body().string());
                    String accessToken = "Bearer " + responseObject.getString("accessToken");

                    Request request2 = new Request.Builder()
                            .url(AgroConstants.URL + "/ws/public/mapi/auth-cgi")
                            .header("Authorization", accessToken)
                            .get()
                            .build();

                    try (Response response2 = client.newCall(request2).execute()) {
                        if (!response2.isSuccessful() || response2.body() == null) return false;

                        JSONObject responseObject2 = new JSONObject(response2.body().string());
                        prefs.setCgiToken(responseObject2.getString("token"));
                        prefs.setAgroUserRole(responseObject2.getString("userRole"));
                        prefs.setAgroUsername(responseObject2.getString("username"));
                        return true;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return false;
            }
        };

        return executor.submit(callable);
    }
}