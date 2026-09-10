package com.gymtrackerpro.app;
import android.appwidget.*;import android.content.*;import android.widget.*;import java.util.*;
public class TodayWidgetProvider extends AppWidgetProvider{
 public void onUpdate(Context c,AppWidgetManager m,int[] ids){String[]d={"DOMENICA","LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO"};String day=d[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1];MainActivity.WorkoutStore s=new MainActivity.WorkoutStore(c);StringBuilder b=new StringBuilder();int n=0;for(MainActivity.Exercise e:s.forDay(day)){if(n++>0)b.append(" • ");b.append(e.name);if(n>=3)break;}for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_today);v.setTextViewText(R.id.widgetDay,day);v.setTextViewText(R.id.widgetExercises,b.length()==0?"Nessun allenamento":b.toString());m.updateAppWidget(id,v);}}
}
