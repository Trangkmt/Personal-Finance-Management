package com.example.expensemanagement.Activitiy;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensemanagement.NetworkMonitor;
import com.example.expensemanagement.NotificationReceiver;
import com.example.expensemanagement.R;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.fragment.AccountFragment;
import com.example.expensemanagement.fragment.BudgetFragment;
import com.example.expensemanagement.fragment.HomeFragment;
import com.example.expensemanagement.fragment.StockFragment;
import com.example.expensemanagement.fragment.TransactionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private TransactionViewModel transactionViewModel;
    private NetworkMonitor networkMonitor;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Đã bật thông báo nhắc nhở", Toast.LENGTH_SHORT).show();
                    scheduleDailyReminder();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền thông báo để nhận nhắc nhở", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottomNav);
        setupBottomNav();

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_report);
            loadFragment(R.id.nav_report);
        }

        // Khởi tạo ViewModel và bắt đầu lắng nghe real-time Firestore
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.startRealtimeSync();

        // NetworkMonitor: khi bật mạng → tự flush data offline lên Firestore
        networkMonitor = new NetworkMonitor(this);

        checkNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (networkMonitor != null) networkMonitor.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (networkMonitor != null) networkMonitor.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transactionViewModel != null) transactionViewModel.stopRealtimeSync();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                scheduleDailyReminder();
            }
        } else {
            scheduleDailyReminder();
        }
    }

    private void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        if (Calendar.getInstance().after(calendar)) calendar.add(Calendar.DAY_OF_MONTH, 1);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms())
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                else
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
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
            fragment = HomeFragment.newInstance();
        } else if (itemId == R.id.nav_transaction) {
            fragment = TransactionFragment.newInstance();
        } else if (itemId == R.id.nav_add) {
            fragment = BudgetFragment.newInstance();
        } else if (itemId == R.id.nav_stock) {
            fragment = StockFragment.newInstance();
        } else {
            fragment = AccountFragment.newInstance();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
