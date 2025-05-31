package de.blau.android;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton, googleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        googleButton = findViewById(R.id.google_sign_in_button);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            login(username, password);
        });

        googleButton.setOnClickListener( v -> oauth2());
    }

    private void oauth2() {
        String backendAuthUrl = AgroConstants.URL + "/login/google-link/mobile";
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        try {
            customTabsIntent.launchUrl(LoginActivity.this, Uri.parse(backendAuthUrl));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(LoginActivity.this, "Chrome Custom Tabs не найдены. Открываем в браузере...", Toast.LENGTH_SHORT).show();
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(backendAuthUrl));
                startActivity(browserIntent);
                finish();
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(LoginActivity.this, "Подходящий браузер не найден.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, "Не удалось открыть ссылку. Проверьте URL.", Toast.LENGTH_LONG).show();
        }
    }

    private void login(String username, String password) {
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).build();
        String auth = username + ":" + password;
        String encodedAuth = "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
        Request request = new Request.Builder()
                .url(AgroConstants.URL + "/ws/token/get")
                .header("Authorization", encodedAuth)
                .build();
        Request requestRole = new Request.Builder()
                .url(AgroConstants.URL + "/ws/user/data")
                .header("Authorization", encodedAuth)
                .build();
        Toast errorToast = Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT);
        new Thread(() -> {
            try (Response response = client.newCall(request).execute();
                 Response responseRole = client.newCall(requestRole).execute()) {

                if (response.isSuccessful() && responseRole.isSuccessful()) {
                    Objects.requireNonNull(response.body());
                    Objects.requireNonNull(responseRole.body());

                    String responseBody = response.body().string();
                    String responseRoleBody = responseRole.body().string();
                    String contentType = response.header("Content-Type");
                    String contentTypeRole = responseRole.header("Content-Type");

                    Objects.requireNonNull(responseRoleBody);
                    Objects.requireNonNull(contentTypeRole);

                    if (contentType != null && contentType.contains("application/json")) {
                        JSONObject responseObject = new JSONObject(responseBody);
                        String token = responseObject.getString("data");
                        JSONObject responseRoleObject = new JSONObject(responseRoleBody);
                        String role = responseRoleObject.getString("role");
                        saveData(token, username, password, role);
                        navigateToMain();
                    } else {
                        runOnUiThread(errorToast::show);
                    }
                } else {
                    runOnUiThread(errorToast::show);
                }

            } catch (NullPointerException e) {
                runOnUiThread(errorToast::show);
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void saveData(String token, String username, String password, String role) {
        Preferences prefs = App.getPreferences(this);
        prefs.setCgiToken(token);
        prefs.setAgroUserRole(role);
        prefs.setAgroPassword(password);
        prefs.setAgroPassword(username);
    }

    private void navigateToMain() {
        runOnUiThread(() -> {
            Intent intent = new Intent(LoginActivity.this, Main.class);
            startActivity(intent);
            finish();
        });
    }
}
