package com.example.expensemanagement.Activitiy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensemanagement.AppDatabase;
import com.example.expensemanagement.FirestoreSyncHelper;
import com.example.expensemanagement.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister;

    private FirebaseAuth mAuth;
    private AppDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth   = FirebaseAuth.getInstance();
        localDb = AppDatabase.getInstance(this);

        initViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Nếu đã có session Firebase → pull data rồi mới vào app
        // KHÔNG navigateToMain() ngay, phải sync trước
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            setLoading(true);
            FirestoreSyncHelper syncHelper = new FirestoreSyncHelper(localDb.appDao());
            syncHelper.pullAllForUser(currentUser.getUid(), () ->
                    runOnUiThread(() -> {
                        setLoading(false);
                        navigateToMain();
                    })
            );
        }
    }

    private void initViews() {
        tilEmail       = findViewById(R.id.tilEmail);
        tilPassword    = findViewById(R.id.tilPassword);
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        progressBar    = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
        etEmail.setOnFocusChangeListener((v, f)    -> { if (f) tilEmail.setError(null); });
        etPassword.setOnFocusChangeListener((v, f) -> { if (f) tilPassword.setError(null); });
    }

    private void attemptLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (!validateInput(email, password)) return;

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Cập nhật last_login local
                            AppDatabase.executor.execute(() ->
                                    localDb.appDao().updateLastLogin(uid, now)
                            );

                            // ── PULL từ Firestore về Room ──────────────────────────
                            FirestoreSyncHelper syncHelper = new FirestoreSyncHelper(localDb.appDao());
                            syncHelper.pullAllForUser(uid, () ->
                                    runOnUiThread(() -> {
                                        setLoading(false);
                                        Toast.makeText(this, "Đồng bộ dữ liệu thành công!", Toast.LENGTH_SHORT).show();
                                        navigateToMain();
                                    })
                            );
                            // ────────────────────────────────────────────────────────
                        } else {
                            setLoading(false);
                            navigateToMain();
                        }
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? getFirebaseErrorMessage(task.getException().getMessage())
                                : getString(R.string.error_login_failed);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required)); valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid)); valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_required)); valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short)); valid = false;
        }
        return valid;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? getString(R.string.loading) : getString(R.string.btn_login));
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getFirebaseErrorMessage(String error) {
        if (error == null) return getString(R.string.error_login_failed);
        if (error.contains("no user record"))      return "Email chưa được đăng ký.";
        if (error.contains("password is invalid")) return "Mật khẩu không đúng.";
        if (error.contains("badly formatted"))     return "Email không hợp lệ.";
        if (error.contains("blocked"))             return "Tài khoản bị tạm khóa.";
        if (error.contains("network"))             return "Không có kết nối mạng.";
        return getString(R.string.error_login_failed);
    }
}