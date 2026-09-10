package com.gymtrackerpro.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    WorkoutStore store; LinearLayout root, content; boolean dark=true; int purple=Color.rgb(216,137,255);
    final String[] days={"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};
    static final int REQ_IMAGE = 1001;

    @Override public void onCreate(Bundle b){super.onCreate(b); store=new WorkoutStore(this); dark=store.dark; build(); if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_IMAGE && resultCode==RESULT_OK && data!=null)ocrImage(data.getData());}
    int bg(){return dark?Color.rgb(8,8,8):Color.rgb(246,246,248);} int fg(){return dark?Color.WHITE:Color.rgb(20,20,20);} int card(){return dark?Color.rgb(34,34,34):Color.WHITE;} int secondary(){return dark?Color.rgb(180,180,180):Color.DKGRAY;}
    GradientDrawable box(int color,float r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(r);return g;}
    TextView tv(String s,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(fg());t.setTextSize(size);t.setPadding(0,0,0,0);if(bold)t.setTypeface(null,1);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(purple);b.setAllCaps(false);b.setBackground(box(card(),60));return b;}
    void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg()); setContentView(root); content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL); ScrollView sc=new ScrollView(this);sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); nav(); showDashboard();}
    void nav(){LinearLayout n=new LinearLayout(this);n.setPadding(10,8,10,10);n.setGravity(Gravity.CENTER);n.setBackgroundColor(card()); String[] labels={"☷\nScheda","⌁\nProgressi","＋\nAggiungi","⚙\nImpostazioni"}; for(int i=0;i<4;i++){Button b=btn(labels[i]); final int k=i;b.setOnClickListener(v->{if(k==0)showDashboard();if(k==1)showProgress();if(k==2)showAdd();if(k==3)showSettings();});n.addView(b,new LinearLayout.LayoutParams(0,64,1));} root.addView(n,new LinearLayout.LayoutParams(-1,72));}
    void clear(String title){content.removeAllViews(); TextView h=tv(title,28,true);h.setPadding(20,24,20,18);content.addView(h);}
    void showDashboard(){clear("GYM TRACKER PRO"); TextView sub=tv("Allenamento di oggi",16,true);sub.setPadding(20,0,20,8);content.addView(sub); String[] calendarDays={"DOMENICA","LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO"}; String today=calendarDays[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY]; LinearLayout daysRow=new LinearLayout(this);daysRow.setPadding(12,4,12,14); for(String d:days){Button b=btn(d.substring(0,2)); if(d.equals(today))b.setTextColor(purple); final String day=d;b.setOnClickListener(v->{store.selectedDay=day;showDashboard();});daysRow.addView(b,new LinearLayout.LayoutParams(0,50,1));} content.addView(daysRow); TextView dt=tv(today,22,true);dt.setPadding(20,4,20,12);content.addView(dt);
        List<Exercise> es=store.forDay(today); if(es.isEmpty()){TextView empty=tv("Nessun allenamento programmato.",17,false);empty.setPadding(20,20,20,20);content.addView(empty);} else {for(Exercise e:es)addExerciseCard(e);} }
    void addExerciseCard(Exercise e){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(18,16,18,16);c.setBackground(box(card(),30)); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(14,7,14,7);content.addView(c,cp);TextView n=tv(e.name,19,true);c.addView(n);TextView info=tv(e.group+"  •  "+e.sets.size()+" serie  •  recupero "+(e.recovery.isEmpty()?"—":e.recovery),13,false);info.setTextColor(secondary());c.addView(info);c.setPadding(18,16,18,12); Button open=btn("Apri esercizio  ›");open.setOnClickListener(v->showExercise(e));c.addView(open);}
    void showExercise(Exercise e){clear(e.name); TextView focus=tv(e.group+"\n"+e.focus,14,false);focus.setTextColor(secondary());focus.setPadding(20,0,20,12);content.addView(focus); MuscleMapView map=new MuscleMapView(this,e.target);content.addView(map,new LinearLayout.LayoutParams(-1,220)); TextView h=tv("Serie",19,true);h.setPadding(20,18,20,8);content.addView(h);
        for(int i=0;i<e.sets.size();i++){
            final int idx=i;
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16,6,16,6);
            CheckBox done=new CheckBox(this);
            done.setChecked(e.sets.get(i).done);
            done.setButtonTintList(new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                    new int[]{purple,Color.GRAY}));
            done.setOnCheckedChangeListener((bb,checked)->{
                e.sets.get(idx).done=checked;
                store.save();
                if(checked)startRecovery(e);
            });
            row.addView(done,new LinearLayout.LayoutParams(48,58));
            TextView sn=tv("Serie "+(i+1),15,true);
            row.addView(sn,new LinearLayout.LayoutParams(0,58,1));
            EditText w=new EditText(this);
            w.setText(String.valueOf(e.sets.get(i).weight));
            w.setTextColor(fg());
            w.setHint("kg");
            w.setInputType(2|8192);
            w.setSingleLine();
            w.setSelectAllOnFocus(true);
            w.setBackground(box(dark?Color.rgb(48,48,48):Color.rgb(235,235,235),18));
            w.setPadding(12,0,12,0);
            w.setOnFocusChangeListener((v,f)->{
                if(!f){
                    try{
                        double x=Double.parseDouble(w.getText().toString());
                        store.setWeight(e,idx,x);
                    }catch(Exception ignored){}
                }
            });
            row.addView(w,new LinearLayout.LayoutParams(90,52));
            TextView kg=tv(" kg",13,false);
            row.addView(kg);
            content.addView(row);
        }
        LinearLayout actions=new LinearLayout(this);actions.setPadding(14,10,14,4);Button plus=btn("＋ Serie"),minus=btn("－ Serie");plus.setOnClickListener(v->{store.addSet(e);showExercise(e);});minus.setOnClickListener(v->{store.removeSet(e);showExercise(e);});actions.addView(plus,new LinearLayout.LayoutParams(0,55,1));actions.addView(minus,new LinearLayout.LayoutParams(0,55,1));content.addView(actions);
        Button chart=btn("📈  Vedi andamento del peso");chart.setOnClickListener(v->showChart(e));content.addView(chart,new LinearLayout.LayoutParams(-1,58)); Button rec=btn("⏱  Recupero: "+(e.recovery.isEmpty()?"imposta":e.recovery));rec.setOnClickListener(v->setRecovery(e));content.addView(rec,new LinearLayout.LayoutParams(-1,58)); Button back=btn("‹ Torna alla scheda");back.setOnClickListener(v->showDashboard());content.addView(back,new LinearLayout.LayoutParams(-1,58));}
    void startRecovery(Exercise e){ try{String r=e.recovery==null?"":e.recovery.trim();if(r.isEmpty())return;String[]p=r.split(":");int sec=p.length==2?Integer.parseInt(p[0])*60+Integer.parseInt(p[1]):Integer.parseInt(p[0]);Intent in=new Intent(this,RecoveryReceiver.class);in.putExtra("name",e.name);PendingIntent pi=PendingIntent.getBroadcast(this,(int)(System.currentTimeMillis()&0x7fffffff),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+sec*1000L,pi);}catch(Exception ignored){}}
    void setRecovery(Exercise e){final EditText x=new EditText(this);x.setHint("es. 2:00");x.setText(e.recovery);new AlertDialog.Builder(this).setTitle("Recupero per serie").setView(x).setPositiveButton("Salva",(d,w)->{e.recovery=x.getText().toString();store.save();showExercise(e);}).setNegativeButton("Annulla",null).show();}
    void showChart(Exercise e){clear("Progressi • "+e.name); List<Log> logs=store.history(e); double max=Math.max(300,store.maxWeight(e)); ChartView chart=new ChartView(this,logs);content.addView(chart,new LinearLayout.LayoutParams(-1,430)); TextView stats=tv("Massimo: "+fmt(store.maxWeight(e))+" kg   •   Aggiornamenti: "+logs.size(),16,true);stats.setPadding(20,18,20,12);content.addView(stats);Button back=btn("‹ Torna all'esercizio");back.setOnClickListener(v->showExercise(e));content.addView(back,new LinearLayout.LayoutParams(-1,58));}
    void showProgress(){clear("Progressi"); for(String d:days){List<Exercise> es=store.forDay(d);if(es.isEmpty())continue;Button b=btn(d+"  ·  "+es.size()+" esercizi");b.setTextSize(17);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(20,0,20,0);b.setOnClickListener(v->{clear(d);for(Exercise e:es){Button x=btn(e.name+"\nMax "+fmt(store.maxWeight(e))+" kg");x.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);x.setOnClickListener(z->showChart(e));content.addView(x,new LinearLayout.LayoutParams(-1,82));}});content.addView(b,new LinearLayout.LayoutParams(-1,72));}}
    void showAdd(){clear("Aggiungi esercizio"); Spinner day=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,days);day.setAdapter(a);content.addView(day);EditText name=new EditText(this);name.setHint("Nome esercizio");name.setTextColor(fg());content.addView(name);EditText reps=new EditText(this);reps.setHint("Ripetizioni target es. 7-9");reps.setTextColor(fg());content.addView(reps);EditText sets=new EditText(this);sets.setHint("Numero di serie");sets.setInputType(2);sets.setTextColor(fg());content.addView(sets);EditText weight=new EditText(this);weight.setHint("Peso iniziale kg");weight.setInputType(2|8192);weight.setTextColor(fg());content.addView(weight);Button add=btn("＋ Aggiungi esercizio");add.setOnClickListener(v->{try{int n=Integer.parseInt(sets.getText().toString());double w=Double.parseDouble(weight.getText().toString());store.add(day.getSelectedItem().toString(),name.getText().toString(),reps.getText().toString(),n,w);showDashboard();}catch(Exception ex){Toast.makeText(this,"Compila nome, serie e peso",Toast.LENGTH_SHORT).show();}});content.addView(add,new LinearLayout.LayoutParams(-1,60));Button imp=btn("📷 Importa scheda da foto (OCR)");imp.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");startActivityForResult(pick,REQ_IMAGE);});content.addView(imp,new LinearLayout.LayoutParams(-1,60));}
    void showSettings(){clear("Impostazioni");Switch s=new Switch(this);s.setText("Tema scuro");s.setTextColor(fg());s.setTextSize(17);s.setChecked(dark);s.setPadding(20,14,20,14);s.setOnCheckedChangeListener((b,c)->{dark=c;store.dark=c;store.save();build();});content.addView(s);TextView t=tv("Gym Tracker Pro\nDati salvati localmente sul dispositivo.",15,false);t.setTextColor(secondary());t.setPadding(20,25,20,20);content.addView(t);}
    void ocrImage(Uri uri){if(uri==null)return;try{InputImage img=InputImage.fromFilePath(this,uri);TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img).addOnSuccessListener(r->{int count=store.importText(r.getText());Toast.makeText(this,"Importati "+count+" esercizi riconosciuti",Toast.LENGTH_LONG).show();showDashboard();}).addOnFailureListener(e->Toast.makeText(this,"OCR non riuscito",Toast.LENGTH_SHORT).show());}catch(Exception e){Toast.makeText(this,"Impossibile leggere l'immagine",Toast.LENGTH_SHORT).show();}}
    String fmt(double x){return x==Math.rint(x)?String.valueOf((int)x):String.format(Locale.US,"%.1f",x);}

    public static class Exercise{long id;String day,name,group,focus,target,reps,recovery="";ArrayList<Set>sets=new ArrayList<>();}
    public static class Set{long id;String reps;double weight;boolean done;ArrayList<Log>history=new ArrayList<>();}
    public static class Log{long date;double weight;Log(long d,double w){date=d;weight=w;}}
    static class WorkoutStore{Context c;ArrayList<Exercise> all=new ArrayList<>();String selectedDay="LUNEDÌ";boolean dark=true;final String key="gymapp.android.v1";String[] days={"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};WorkoutStore(Context c){this.c=c;load();if(all.isEmpty()){initial();save();}}
      List<Exercise>forDay(String d){ArrayList<Exercise>x=new ArrayList<>();for(Exercise e:all)if(e.day.equals(d))x.add(e);return x;}
      void initial(){add0("LUNEDÌ","Spinte manubri panca 32","7-9",3,32,"PETTO (ALTO)","Pettorali superiori e tricipiti","chest","2:00");add0("LUNEDÌ","Lat Pulldown","7-9",3,20,"DORSO","Gran dorsale e bicipiti","back","2:00");add0("LUNEDÌ","Chest Press","8-10",3,20,"PETTO","Pettorali e tricipiti","chest","2:00");add0("LUNEDÌ","T-Bar prona larga","8-10",3,20,"DORSO","Dorsali e parte alta della schiena","back","2:00");add0("LUNEDÌ","Alzate laterali","10-12",4,20,"SPALLE","Deltoide laterale","shoulders","1:30");add0("LUNEDÌ","Push Down asta curva","10 RM",4,20,"TRICIPITI","Tricipite","triceps","1:30");add0("LUNEDÌ","Curl cavo basso","10 RM",4,20,"BICIPITI","Bicipite","biceps","1:30");add0("MARTEDÌ","Leg Extension","12 RM",4,62,"QUADRICIPITI","Quadricipite","quads","1:30");add0("MARTEDÌ","Leg Press 45","7-9",3,130,"GAMBE","Quadricipiti e glutei","quads","2:00");add0("MARTEDÌ","Leg Curl sdraiato","10-12",2,47,"FEMORALI","Femorali","hamstrings","1:30");add0("MARTEDÌ","Adduttori","10-12",2,30,"ADDUTTORI","Adduttori","quads","1:30");add0("MARTEDÌ","Calf Machine","8-10",4,50,"POLPACCI","Polpacci","hamstrings","1:30");add0("MERCOLEDÌ","Panca piana bilanciere","7-9",4,40,"PETTO","Pettorali e tricipiti","chest","2:30");add0("MERCOLEDÌ","Rematore bilanciere","7-9",3,40,"DORSO","Dorsali e parte alta della schiena","back","2:00");add0("MERCOLEDÌ","Lento avanti manubri panca 71","8-10",3,20,"SPALLE","Deltoidi e tricipiti","shoulders","2:00");add0("MERCOLEDÌ","Rowing","8-10",3,30,"DORSO","Schiena","back","1:30");add0("MERCOLEDÌ","Stacchi rumeni manubri","7-9",3,30,"FEMORALI / GLUTEI","Catena posteriore","hamstrings","2:00");add0("MERCOLEDÌ","Leg Curl seduto","12 RM",2,40,"FEMORALI","Femorali","hamstrings","1:30");add0("MERCOLEDÌ","Arm Curl","10 RM",3,20,"BICIPITI","Bicipite","biceps","1:30");add0("MERCOLEDÌ","French Press manubri","10 RM",3,20,"TRICIPITI","Tricipite","triceps","1:30");}
      void add0(String d,String n,String r,int num,double w,String g,String f,String t,String rec){Exercise e=new Exercise();e.id=System.nanoTime()+all.size();e.day=d;e.name=n;e.reps=r;e.group=g;e.focus=f;e.target=t;e.recovery=rec;for(int i=0;i<num;i++){Set s=new Set();s.id=e.id+i+1;s.reps=r;s.weight=w;e.sets.add(s);}all.add(e);}
      void add(String d,String n,String r,int nsets,double w){String[] q=recognize(n);Exercise e=new Exercise();e.id=System.nanoTime();e.day=d;e.name=n;e.reps=r;e.group=q[0];e.focus=q[1];e.target=q[2];for(int i=0;i<nsets;i++){Set s=new Set();s.id=e.id+i;s.reps=r;s.weight=w;e.sets.add(s);}all.add(e);save();}
      String[]recognize(String n){String x=n.toLowerCase(Locale.ITALIAN);if(x.contains("panca")||x.contains("chest"))return new String[]{"PETTO","Pettorali e tricipiti","chest"};if(x.contains("lat")||x.contains("row")||x.contains("rematore")||x.contains("t-bar"))return new String[]{"DORSO","Dorsali e parte alta della schiena","back"};if(x.contains("alzate")||x.contains("lento"))return new String[]{"SPALLE","Deltoidi","shoulders"};if(x.contains("curl")||x.contains("bicip"))return new String[]{"BICIPITI","Bicipite","biceps"};if(x.contains("push")||x.contains("french")||x.contains("tricip"))return new String[]{"TRICIPITI","Tricipite","triceps"};if(x.contains("leg extension")||x.contains("press"))return new String[]{"QUADRICIPITI","Quadricipiti e glutei","quads"};if(x.contains("curl sdraiato")||x.contains("stacchi")||x.contains("femorali"))return new String[]{"FEMORALI / GLUTEI","Catena posteriore","hamstrings"};return new String[]{"ALTRO","Muscoli vari","full"};}
      void setWeight(Exercise e,int i,double w){Set s=e.sets.get(i);if(s.weight!=w){s.weight=w;s.history.add(new Log(System.currentTimeMillis(),w));save();}}
      void addSet(Exercise e){Set s=new Set();Set last=e.sets.get(e.sets.size()-1);s.id=System.nanoTime();s.reps=last.reps;s.weight=last.weight;e.sets.add(s);save();}
      void removeSet(Exercise e){if(e.sets.size()>1)e.sets.remove(e.sets.size()-1);save();}
      ArrayList<Log>history(Exercise e){ArrayList<Log>x=new ArrayList<>();for(Set s:e.sets)x.addAll(s.history);x.sort(Comparator.comparingLong(a->a.date));return x;}
      double maxWeight(Exercise e){double m=0;for(Set s:e.sets)m=Math.max(m,s.weight);for(Log l:history(e))m=Math.max(m,l.weight);return m;}
      void save(){try{JSONArray a=new JSONArray();for(Exercise e:all){JSONObject o=new JSONObject();o.put("id",e.id);o.put("day",e.day);o.put("name",e.name);o.put("group",e.group);o.put("focus",e.focus);o.put("target",e.target);o.put("reps",e.reps);o.put("recovery",e.recovery);JSONArray ss=new JSONArray();for(Set s:e.sets){JSONObject z=new JSONObject();z.put("id",s.id);z.put("reps",s.reps);z.put("weight",s.weight);z.put("done",s.done);JSONArray hh=new JSONArray();for(Log l:s.history){JSONObject k=new JSONObject();k.put("date",l.date);k.put("weight",l.weight);hh.put(k);}z.put("history",hh);ss.put(z);}o.put("sets",ss);a.put(o);}c.getSharedPreferences("gym",0).edit().putString(key,a.toString()).putBoolean("dark",dark).apply();}catch(Exception ignored){}}
      void load(){try{android.content.SharedPreferences p=c.getSharedPreferences("gym",0);dark=p.getBoolean("dark",true);String raw=p.getString(key,"");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Exercise e=new Exercise();e.id=o.getLong("id");e.day=o.getString("day");e.name=o.getString("name");e.group=o.getString("group");e.focus=o.getString("focus");e.target=o.getString("target");e.reps=o.getString("reps");e.recovery=o.optString("recovery","");JSONArray ss=o.getJSONArray("sets");for(int j=0;j<ss.length();j++){JSONObject z=ss.getJSONObject(j);Set s=new Set();s.id=z.getLong("id");s.reps=z.getString("reps");s.weight=z.getDouble("weight");s.done=z.optBoolean("done",false);JSONArray hh=z.optJSONArray("history");if(hh!=null)for(int k=0;k<hh.length();k++){JSONObject q=hh.getJSONObject(k);s.history.add(new Log(q.getLong("date"),q.getDouble("weight")));}e.sets.add(s);}all.add(e);}}catch(Exception ignored){}}
      int importText(String text){int c=0;for(String line:text.split("\\n")){String l=line.trim();if(l.length()<4)continue;String low=l.toLowerCase(Locale.ITALIAN);if(!(low.contains("panca")||low.contains("press")||low.contains("curl")||low.contains("row")||low.contains("rematore")||low.contains("lat ")||low.contains("leg ")||low.contains("alzate")||low.contains("stacchi")||low.contains("french")||low.contains("push down")))continue;String[]q=recognize(l);Exercise e=new Exercise();e.id=System.nanoTime()+c;e.day="LUNEDÌ";e.name=l;e.group=q[0];e.focus=q[1];e.target=q[2];e.reps="8-10";for(int i=0;i<3;i++){Set s=new Set();s.id=e.id+i;s.reps=e.reps;s.weight=20;e.sets.add(s);}all.add(e);c++;}save();return c;}
    }

    static class ChartView extends View{Paint p=new Paint(1);List<Log> logs;ChartView(Context c,List<Log>l){super(c);logs=l;p.setTypeface(Typeface.create("sans",Typeface.NORMAL));setBackgroundColor(Color.TRANSPARENT);}protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.GRAY);float left=70,bottom=h-55,top=25,right=w-20;c.drawLine(left,top,left,bottom,p);c.drawLine(left,bottom,right,bottom,p);p.setTextSize(28);p.setStyle(Paint.Style.FILL);for(int i=0;i<=3;i++){float y=bottom-(bottom-top)*i/3f;c.drawText(String.valueOf(i*100),10,y+8,p);p.setColor(Color.DKGRAY);p.setStrokeWidth(1);c.drawLine(left,y,right,y,p);p.setColor(Color.GRAY);}if(logs.size()<1){p.setTextSize(18);c.drawText("Nessun aggiornamento ancora",left+10,(top+bottom)/2,p);return;}long min=logs.get(0).date,max=logs.get(logs.size()-1).date;if(max==min)max=min+1;Path path=new Path();p.setColor(Color.rgb(216,137,255));p.setStrokeWidth(6);p.setStyle(Paint.Style.STROKE);for(int i=0;i<logs.size();i++){Log l=logs.get(i);float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setTextSize(22);for(Log l:logs){float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;c.drawCircle(x,y,7,p);c.drawText(String.valueOf((int)l.weight)+"kg",Math.min(x,right-70),Math.max(y-12,top+18),p);}}
    }
    static class MuscleMapView extends View{Paint p=new Paint(1);Bitmap b;String target;MuscleMapView(Context c,String t){super(c);target=t; b=BitmapFactory.decodeResource(getResources(),com.gymtrackerpro.app.R.drawable.muscle_map); }protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();p.setColor(Color.WHITE);c.drawRoundRect(0,0,w,h,28,28,p);if(b==null)return;float scale=Math.max(w/(float)b.getWidth(),h/(float)b.getHeight());float dw=b.getWidth()*scale,dh=b.getHeight()*scale;float dx=(w-dw)/2,dy=(h-dh)/2;float focusX=0; if(target.equals("back"))focusX=0.5f;else if(target.equals("shoulders")||target.equals("chest")||target.equals("biceps")||target.equals("triceps"))focusX=0.5f;else focusX=0.5f;dx+= (0.5f-focusX)*dw;RectF dst=new RectF(dx,dy,dx+dw,dy+dh);c.save();c.clipRect(0,0,w,h);c.drawBitmap(b,null,dst,p);c.restore();}}
}
