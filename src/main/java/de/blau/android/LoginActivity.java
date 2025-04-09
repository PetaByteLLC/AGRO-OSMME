package de.blau.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            login(username, password);
        });
    }

    private void login(String username, String password) {
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).build();
        String auth = username + ":" + password;
        String encodedAuth = "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
        Request request = new Request.Builder()
                .url("https://agro.brisklyminds.com/agroadmin/ws/token/get")
                .header("Authorization", encodedAuth)
                .build();
        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    String contentType = response.header("Content-Type");

                    if (contentType != null && contentType.contains("application/json")) {
                        JSONObject responseObject = new JSONObject(responseBody);
                        String token = responseObject.getString("data");

                        saveData(token, username, password);
                        navigateToMain();
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show());
                }

            } catch (IOException | JSONException e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void saveData(String token, String username, String password) {
        Preferences prefs = App.getPreferences(this);
        prefs.setCgiToken(token);
        prefs.setAgroUsername(username);
        prefs.setAgroPassword(password);
    }

    private void navigateToMain() {
        runOnUiThread(() -> {
            Intent intent = new Intent(LoginActivity.this, Main.class);
            startActivity(intent);
            finish();
        });
    }
}
