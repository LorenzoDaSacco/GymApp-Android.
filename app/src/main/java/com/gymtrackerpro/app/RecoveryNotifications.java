package com.gymtrackerpro.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class RecoveryNotifications {
    private RecoveryNotifications() {}

    public static void cancel(Context context, long setId) {
        Intent intent = new Intent(context, RecoveryReceiver.class);
        intent.putExtra("setId", setId);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                (int) (setId & 0x7fffffff),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pending != null) {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarm != null) alarm.cancel(pending);
            pending.cancel();
        }
    }
}
