package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensemanagement.Activitiy.LoginActivity;
import com.example.expensemanagement.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AccountFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView tvDisplayName, tvEmail;
    private Button btnOpenUpdateProfile, btnOpenChangePassword, btnLogout;

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        mAuth = FirebaseAuth.getInstance();
        initViews(view);
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        tvDisplayName = view.findViewById(R.id.tvDisplayAccountName);
        tvEmail = view.findViewById(R.id.tvDisplayAccountEmail);
        btnOpenUpdateProfile = view.findViewById(R.id.btnOpenUpdateProfile);
        btnOpenChangePassword = view.findViewById(R.id.btnOpenChangePassword);
        btnLogout = view.findViewById(R.id.btnLogoutAccount);

        btnOpenUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());
        btnOpenChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvDisplayName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() 
                ? user.getDisplayName() : "Chưa đặt tên");
            tvEmail.setText(user.getEmail());
        }
    }

    private void showUpdateProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_profile, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etDialogDisplayName);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etName.setText(user.getDisplayName());
        }

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Họ tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfile(newName);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etDialogNewPassword);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
                    String newPassword = etPassword.getText().toString().trim();
                    if (newPassword.length() < 6) {
                        Toast.makeText(getContext(), "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    changePassword(newPassword);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateProfile(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                            loadUserData(); // Refresh UI
                        } else {
                            Toast.makeText(getContext(), "Cập nhật thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void changePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Đổi mật khẩu thất bại. Bạn cần đăng nhập lại để thực hiện tác vụ này.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
