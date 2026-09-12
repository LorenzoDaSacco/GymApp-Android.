package com.gymtrackerpro.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class RecoveryNotifications {
    private static final String CHANNEL="recovery";
    private RecoveryNotifications() {}

    public static void ensureChannel(Context context){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager n=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(n!=null){
                NotificationChannel channel=new NotificationChannel(CHANNEL,"Recupero",NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Timer di recupero e avviso di fine recupero");
                n.createNotificationChannel(channel);
            }
        }
    }

    public static void schedule(Context context,String name,long setId,int seconds){
        ensureChannel(context);
        Intent in=new Intent(context,RecoveryReceiver.class);
        in.putExtra("name",name);
        in.putExtra("setId",setId);
        PendingIntent pi=PendingIntent.getBroadcast(context,(int)(setId&0x7fffffff),in,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        long at=System.currentTimeMillis()+seconds*1000L;
        if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        else am.set(AlarmManager.RTC_WAKEUP,at,pi);
    }

    public static void startVisibleTimer(Context context,String name,long setId,long endAtMs,long durationMs){
        Intent i=new Intent(context,RecoveryTimerService.class);
        i.putExtra("name",name);
        i.putExtra("setId",setId);
        i.putExtra("endAtMs",endAtMs);
        i.putExtra("durationMs",durationMs);
        if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i); else context.startService(i);
    }

    public static void stopVisibleTimer(Context context,long setId){
        Intent i=new Intent(context,RecoveryTimerService.class);
        i.setAction(RecoveryTimerService.ACTION_STOP);
        i.putExtra("setId",setId);
        context.startService(i);
    }

    public static void cancel(Context context,long setId){
        Intent intent=new Intent(context,RecoveryReceiver.class);
        intent.putExtra("setId",setId);
        PendingIntent pending=PendingIntent.getBroadcast(context,(int)(setId&0x7fffffff),intent,
                PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(pending!=null){
            AlarmManager alarm=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            if(alarm!=null)alarm.cancel(pending);
            pending.cancel();
        }
    }
}
