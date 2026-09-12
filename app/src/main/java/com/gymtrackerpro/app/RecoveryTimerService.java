package com.gymtrackerpro.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

public class RecoveryTimerService extends Service {
    public static final String ACTION_STOP="com.gymtrackerpro.app.STOP_RECOVERY_TIMER";
    private static final int NOTIFICATION_ID=91234;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private long setId=-1;
    private long endAtMs=0;
    private String exerciseName="";

    private final Runnable finishRunnable=new Runnable(){
        @Override public void run(){
            if(endAtMs>0 && System.currentTimeMillis()>=endAtMs){
                cancelOngoing();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }
    };

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        if(ACTION_STOP.equals(intent.getAction())){
            long requested=intent.getLongExtra("setId",-1);
            if(requested==-1 || requested==setId){
                handler.removeCallbacksAndMessages(null);
                cancelOngoing();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        setId=intent.getLongExtra("setId",-1);
        endAtMs=intent.getLongExtra("endAtMs",0);
        exerciseName=intent.getStringExtra("name");
        if(exerciseName==null)exerciseName="Recupero";

        long remaining=Math.max(0,endAtMs-System.currentTimeMillis());
        if(remaining<=0){
            stopSelf();
            return START_NOT_STICKY;
        }

        RecoveryNotifications.ensureChannel(this);
        Notification n=buildNotification(remaining);
        startForeground(NOTIFICATION_ID,n);

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(finishRunnable,remaining+100);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(long remaining){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b=new Notification.Builder(this,"recovery")
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("Recupero in corso")
                .setContentText(exerciseName+" • "+format((remaining+999)/1000))
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS);

        if(Build.VERSION.SDK_INT>=24){
            long base=SystemClock.elapsedRealtime()+remaining;
            b.setUsesChronometer(true);
            b.setWhen(base);
            b.setChronometerCountDown(true);
        }
        return b.build();
    }

    private String format(long sec){
        return String.format(java.util.Locale.ITALIAN,"%d:%02d",sec/60,sec%60);
    }

    private void cancelOngoing(){
        NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(n!=null)n.cancel(NOTIFICATION_ID);
    }

    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){return null;}
}
