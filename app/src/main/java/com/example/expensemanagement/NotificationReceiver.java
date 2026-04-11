package com.example.expensemanagement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.showNotification(
                context,
                "Nhắc nhở ghi chép",
                "Đừng quên ghi lại các khoản chi tiêu hôm nay của bạn nhé!"
        );
    }
}