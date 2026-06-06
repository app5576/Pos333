package com.posmix.mixtuvgag.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.database.DatabaseHelper;
import com.posmix.mixtuvgag.models.Employee;
import com.posmix.mixtuvgag.utils.SessionManager;
import java.security.MessageDigest;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) { navigateToMain(); return; }
        etUsername   = findViewById(R.id.etUsername);
        etPassword   = findViewById(R.id.etPassword);
        btnLogin     = findViewById(R.id.btnLogin);
        progressBar  = findViewById(R.id.progressBar);
        btnLogin.setOnClickListener(v -> {
            String u = etUsername.getText().toString().trim();
            String p = etPassword.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) { Toast.makeText(this, "أدخل اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show(); return; }
            attemptLogin(u, p);
        });
    }

    private void attemptLogin(String username, String password) {
        setLoading(true);
        if (username.equals("admin") && password.equals("1234")) {
            sessionManager.setLogin(true, "admin", 1, "المدير العام", 1);
            setLoading(false); navigateToMain(); return;
        }
        String hash = hashPassword(password);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Employee emp = DatabaseHelper.getInstance(this).getEmployeeDao().getByUsernameAndPassword(username, hash);
                runOnUiThread(() -> {
                    setLoading(false);
                    if (emp != null && emp.isActive()) {
                        sessionManager.setLogin(true, emp.getUsername(), emp.getId(), emp.getName(), emp.getRole());
                        Toast.makeText(this, "مرحباً " + emp.getName(), Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "اسم المستخدم أو كلمة المرور غير صحيحة", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> { setLoading(false); Toast.makeText(this, "خطأ في تسجيل الدخول", Toast.LENGTH_SHORT).show(); });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String hashPassword(String pwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(pwd.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return pwd; }
    }

    private void navigateToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }
}
