package com.example.expensemanagement.Activity;

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
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.UserEntity;
import com.example.expensemanagement.model.UserSettingsEntity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private AppDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth     = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        localDb   = AppDatabase.getInstance(this);

        initViews();
        setupListeners();
    }
    private void initViews() {
        tilFullName        = findViewById(R.id.tilFullName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilPassword        = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        btnRegister        = findViewById(R.id.btnRegister);
        progressBar        = findViewById(R.id.progressBar);
        tvGoToLogin        = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvGoToLogin.setOnClickListener(v -> finish());
        etFullName.setOnFocusChangeListener((v, f)        -> { if (f) tilFullName.setError(null); });
        etEmail.setOnFocusChangeListener((v, f)           -> { if (f) tilEmail.setError(null); });
        etPassword.setOnFocusChangeListener((v, f)        -> { if (f) tilPassword.setError(null); });
        etConfirmPassword.setOnFocusChangeListener((v, f) -> { if (f) tilConfirmPassword.setError(null); });
    }
    private void attemptRegister() {
        String fullName        = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email           = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password        = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (!validateInput(fullName, email, password, confirmPassword)) return;

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.updateProfile(
                            new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName).build()
                        ).addOnCompleteListener(p -> saveUserData(firebaseUser, fullName, email));
                    }
                } else {
                    setLoading(false);
                    String msg = task.getException() != null
                        ? getFirebaseErrorMessage(task.getException().getMessage())
                        : getString(R.string.error_register_failed);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void saveUserData(FirebaseUser firebaseUser, String fullName, String email) {
        String now       = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        String userId    = firebaseUser.getUid(); // Dùng luôn Firebase UID làm User ID
        String settingId = UUID.randomUUID().toString();

        // Lưu vào Room local
        AppDatabase.executor.execute(() -> {
            try {
                UserEntity user = new UserEntity(userId, email, "firebase_auth", fullName, firebaseUser.getUid(), now, now);
                UserSettingsEntity settings = new UserSettingsEntity(settingId, userId, now);
                localDb.appDao().insertUser(user);
                localDb.appDao().insertSettings(settings);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Lưu lên Firestore
        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("user_id",       userId);
        userDoc.put("firebase_uid",  firebaseUser.getUid());
        userDoc.put("email",         email);
        userDoc.put("full_name",     fullName);
        userDoc.put("currency_code", "VND");
        userDoc.put("language",      "vi");
        userDoc.put("auth_provider", "local");
        userDoc.put("is_active",     true);
        userDoc.put("created_at",    now);
        userDoc.put("updated_at",    now);
        userDoc.put("last_login_at", now);

        firestore.collection("users").document(firebaseUser.getUid())
            .set(userDoc)
            .addOnCompleteListener(t -> {
                setLoading(false);
                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                navigateToMain();
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Đăng ký thành công! Sẽ đồng bộ khi có mạng.", Toast.LENGTH_SHORT).show();
                navigateToMain();
            });
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        boolean valid = true;
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_fullname_required)); valid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError(getString(R.string.error_fullname_short)); valid = false;
        }
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
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_confirm_required)); valid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch)); valid = false;
        }
        return valid;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? getString(R.string.loading) : getString(R.string.btn_register));
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getFirebaseErrorMessage(String error) {
        if (error == null) return getString(R.string.error_register_failed);
        if (error.contains("email address is already in use")) return "Email này đã được đăng ký.";
        if (error.contains("badly formatted"))                 return "Email không hợp lệ.";
        if (error.contains("password should be at least"))    return "Mật khẩu phải có ít nhất 6 ký tự.";
        if (error.contains("network"))                         return "Không có kết nối mạng.";
        return getString(R.string.error_register_failed);
    }
}