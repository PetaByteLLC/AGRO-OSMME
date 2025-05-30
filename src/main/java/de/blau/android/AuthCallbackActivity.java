package de.blau.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.blau.android.prefs.Preferences;

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

            if ("agromap".equals(data.getScheme()) && "googleauth".equals(data.getHost())) {
                String backendToken = data.getQueryParameter("token");
                String status = data.getQueryParameter("status");
                String errorMessage = data.getQueryParameter("error_message"); // или "error"
                String userName = data.getQueryParameter("name");
                String role = data.getQueryParameter("role");

                Log.i(TAG, "Backend Token: " + backendToken);
                Log.i(TAG, "Status: " + status);
                Log.i(TAG, "Error Message: " + errorMessage);
                Log.i(TAG, "User Name: " + userName);
                Log.i(TAG, "User Email: " + role);

                tvStatus.append("Статус: " + status + "\n");

                if ("success".equalsIgnoreCase(status) && backendToken != null) {
                    tvStatus.append("Токен бэкенда: " + backendToken + "\n");
                    tvStatus.append("Пользователь: " + userName + " (" + role + ")\n");

                    saveAuthData(backendToken, userName, role);

                    Toast.makeText(this, "Аутентификация успешна!", Toast.LENGTH_LONG).show();

                    Intent mainAppIntent = new Intent(this, Main.class);
                    mainAppIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainAppIntent);
                    finish();
                } else {
                    String errorToDisplay = errorMessage != null ? errorMessage : "Неизвестная ошибка аутентификации.";
                    tvStatus.append("Ошибка: " + errorToDisplay + "\n");
                    Toast.makeText(this, "Ошибка аутентификации: " + errorToDisplay, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w(TAG, "Получен deep link, но он не соответствует ожидаемой схеме/хосту.");
                tvStatus.append("Получен неожиданный deep link.\n");
            }
        } else {
            Log.d(TAG, "Activity запущена не через ACTION_VIEW или data is null.");
            tvStatus.append("Activity запущена не через deep link.\n");
        }
    }

    private void saveAuthData(String backendToken, String userName, String role) {
        Preferences prefs = App.getPreferences(this);
        prefs.setCgiToken(backendToken);
        prefs.setAgroUsername(userName);
        prefs.setAgroUserRole(role);
    }
}