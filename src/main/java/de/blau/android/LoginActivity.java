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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Objects.requireNonNull(getSupportActionBar()).hide();
        usernameInput = findViewById(R.id.etUsername);
        passwordInput = findViewById(R.id.etPassword);
        Button loginButton = findViewById(R.id.btnLogin);

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
                        JSONObject responseRoleObject = new JSONObject(responseRoleBody);

                        Preferences prefs = App.getPreferences(this);
                        prefs.setCgiToken(responseObject.getString("data"));
                        prefs.setAgroUserRole(responseRoleObject.getString("role"));
                        prefs.setAgroPassword(password);
                        prefs.setAgroUsername(username);

                        try {
                            JSONObject partner = responseRoleObject.getJSONObject("partner");
                            prefs.setAgroPersonName(partner.getString("firstName"));
                            prefs.setAgroPersonSurName(partner.getString("lastName"));
                            prefs.setAgroPersonMobile(partner.getString("mobilePhone"));
                        } catch (Exception ignored) {}
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


    private void navigateToMain() {
        runOnUiThread(() -> {
            Intent intent = new Intent(LoginActivity.this, Main.class);
            startActivity(intent);
            finish();
        });
    }
}
