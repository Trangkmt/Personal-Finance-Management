package com.example.expensemanagement;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Chưa đăng nhập → về màn Login
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottomNav);
        setupBottomNav();

        if (savedInstanceState == null) {
            loadFragment(R.id.nav_report);
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            loadFragment(item.getItemId());
            return true;
        });
    }

    private void loadFragment(int itemId) {
        Fragment fragment;

        if (itemId == R.id.nav_report) {
            // TODO: thay bằng ReportFragment
            fragment = PlaceholderFragment.newInstance(" Báo cáo", "Thống kê & biểu đồ chi tiêu");
        } else if (itemId == R.id.nav_transaction) {
            // TODO: thay bằng TransactionFragment
            fragment = PlaceholderFragment.newInstance(" Giao dịch", "Lịch sử thu / chi");
        } else if (itemId == R.id.nav_add) {
            // TODO: thay bằng AddTransactionFragment
            fragment = PlaceholderFragment.newInstance(" Thêm giao dịch", "Nhập thu nhập hoặc chi tiêu");
        } else if (itemId == R.id.nav_wallet) {
            // TODO: thay bằng WalletFragment
            fragment = PlaceholderFragment.newInstance(" Ví", "Quản lý tài khoản & ví");
        } else {
            // nav_more — TODO: thay bằng MoreFragment
            fragment = PlaceholderFragment.newInstance(" Khác", "Ngân sách · Cài đặt");
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}