package com.gymtrackerpro.app;
import android.app.*;import android.content.*;
public class RecoveryReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){
  String name=i.getStringExtra("name"); long setId=i.getLongExtra("setId",System.currentTimeMillis());
  RecoveryNotifications.ensureChannel(c); NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  Notification x=new Notification.Builder(c,"recovery").setSmallIcon(R.drawable.app_icon).setContentTitle("Recupero terminato").setContentText(name==null?"Puoi ripartire con la prossima serie.":"Puoi ripartire: "+name).setAutoCancel(true).build();
  if(n!=null)n.notify((int)(setId&0x7fffffff),x);
 }
}
