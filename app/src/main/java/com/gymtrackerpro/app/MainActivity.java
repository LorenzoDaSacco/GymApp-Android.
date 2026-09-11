package com.gymtrackerpro.app;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.graphics.PorterDuff;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;

import org.json.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_IMAGE = 1101, REQ_CAMERA = 1102;
    LinearLayout root, content, bottom;
    WorkoutStore store;
    boolean dark;
    int accent;
    final int purple = Color.rgb(216,137,255);
    final Handler timerHandler = new Handler(Looper.getMainLooper());
    long timerExerciseId = -1, timerSetId = -1, timerEndMs = 0, timerDurationMs = 0;
    ProgressBar recoveryProgress;
    TextView recoveryCountdown;
    LinearLayout recoveryTimerCard;
    final String[] DAYS = {"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        store=new WorkoutStore(this);
        dark=store.dark; accent=store.accent;
        build();
        RecoveryNotifications.ensureChannel(this);
        if(store.notificationsEnabled && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);
    }

    void build(){
        dark=store.dark; accent=store.accent;
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER_HORIZONTAL); content.setPadding(22,14,22,18);
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav(); setContentView(root);
        showDashboard();
    }

    int bg(){return dark?Color.rgb(10,10,12):Color.rgb(247,247,249);}
    int card(){return dark?Color.rgb(24,24,27):Color.WHITE;}
    int fg(){return dark?Color.WHITE:Color.rgb(25,25,28);}
    int secondary(){return dark?Color.rgb(180,180,188):Color.rgb(95,95,102);}
    int accent(){return accent;}
    GradientDrawable box(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    TextView tv(String s,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(fg());t.setTextSize(size);t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(fg());b.setAllCaps(false);b.setTextSize(14);b.setBackground(box(dark?Color.rgb(34,34,38):Color.rgb(238,238,242),18));return b;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(secondary());e.setTextColor(fg());e.setSingleLine(true);e.setPadding(12,0,12,0);e.setBackground(box(dark?Color.rgb(40,40,45):Color.rgb(238,238,242),14));return e;}
    void addGap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,h));}
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    void addFull(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,h));}
    void sectionLabel(String text){TextView t=tv(text,14,true);t.setTextColor(secondary());t.setPadding(4,18,4,8);addFull(t,48);}
    void cardView(LinearLayout v,int top,int bottom){v.setPadding(18,16,18,16);v.setBackground(box(card(),28));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,top,0,bottom);content.addView(v,lp);}
    void openNotificationSettings(){try{Intent in=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);in.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(in);}catch(Exception ignored){startActivity(new Intent(Settings.ACTION_SETTINGS));}}

    void nav(){
        bottom=new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(10,7,10,9);
        bottom.setBackground(box(dark?Color.rgb(20,20,22):Color.WHITE,30));
        String[] labels={"Scheda","Progressi","Aggiungi","Impostazioni"};
        String[] icons={"▣","⌁","+","⚙"};
        for(int i=0;i<4;i++){
            LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(6,4,6,4);
            TextView icon=tv(icons[i],22,true); icon.setGravity(Gravity.CENTER);
            TextView label=tv(labels[i],11,true); label.setGravity(Gravity.CENTER);
            item.addView(icon,new LinearLayout.LayoutParams(-1,27)); item.addView(label,new LinearLayout.LayoutParams(-1,22));
            final int k=i; item.setOnClickListener(v->{hideKeyboard(); if(k==0)showDashboard(); else if(k==1)showProgress(); else if(k==2)showAdd(); else showSettings();});
            bottom.addView(item,new LinearLayout.LayoutParams(0,58,1));
        }
        root.addView(bottom,new LinearLayout.LayoutParams(-1,74));
    }

    void clear(String title){
        hideKeyboard(); content.removeAllViews();
        TextView h=tv(title,29,true); h.setGravity(Gravity.CENTER); h.setPadding(4,8,4,12); content.addView(h,new LinearLayout.LayoutParams(-1,58));
    }

    void showDashboard(){
        clear("GYM TRACKER PRO");
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView sub=tv("Allenamento del giorno",15,true); top.addView(sub,new LinearLayout.LayoutParams(0,48,1));
        Button imp=btn("📷"); imp.setTextColor(accent); imp.setOnClickListener(v->showImport()); top.addView(imp,new LinearLayout.LayoutParams(58,48)); content.addView(top);

        Spinner day=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,DAYS){
            @Override public View getView(int p, View c, android.view.ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(fg());v.setTextSize(16);return v;}
        };
        day.setAdapter(ad); int pos=Arrays.asList(DAYS).indexOf(store.selectedDay); day.setSelection(Math.max(0,pos));
        day.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){String d=DAYS[position];if(!d.equals(store.selectedDay)){store.selectedDay=d;store.save();showDashboard();}}});
        content.addView(day,new LinearLayout.LayoutParams(-1,52));

        List<Exercise> es=store.forDay(store.selectedDay);
        LinearLayout metrics=new LinearLayout(this); metrics.setPadding(0,10,0,8);
        addMetric(metrics,"Esercizi",String.valueOf(es.size()),"●");
        addMetric(metrics,"Serie",String.valueOf(store.totalSets(es)),"▦");
        addMetric(metrics,"Completate",String.valueOf(store.completedSets(es)),"✓");
        content.addView(metrics,new LinearLayout.LayoutParams(-1,94));

        int total=store.totalSets(es), done=store.completedSets(es);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); pb.setMax(Math.max(1,total));pb.setProgress(done);pb.setProgressTintList(android.content.res.ColorStateList.valueOf(accent)); content.addView(pb,new LinearLayout.LayoutParams(-1,8));
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,10,0,4);
        Button reset=btn("Reset giornata"); reset.setOnClickListener(v->{store.resetDay();showDashboard();});
        Button add=btn("+ Aggiungi esercizio"); add.setOnClickListener(v->showAdd()); actions.addView(reset,new LinearLayout.LayoutParams(0,52,1));actions.addView(add,new LinearLayout.LayoutParams(0,52,1));content.addView(actions);
        if(es.isEmpty()){TextView empty=tv("Giorno libero\nAggiungi gli esercizi che vuoi per questo giorno.",16,false);empty.setGravity(Gravity.CENTER);empty.setPadding(10,50,10,50);content.addView(empty);}
        else for(Exercise e:es)addExerciseCard(e);
    }

    void addMetric(LinearLayout parent,String title,String value,String icon){
        LinearLayout m=new LinearLayout(this);m.setOrientation(LinearLayout.VERTICAL);m.setPadding(12,8,8,8);m.setBackground(box(card(),18));
        TextView i=tv(icon,17,true);i.setTextColor(accent);m.addView(i);m.addView(tv(value,21,true));TextView t=tv(title,12,false);t.setTextColor(secondary());m.addView(t);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(3,3,3,3);parent.addView(m,lp);
    }

    void addExerciseCard(Exercise e){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(16,14,16,14);c.setBackground(box(card(),26));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,8,0,8);content.addView(c,cp);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv(e.name,18,true));TextView g=tv(e.group,12,true);g.setTextColor(accent);titles.addView(g);TextView f=tv(e.focus,12,false);f.setTextColor(secondary());titles.addView(f);head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        Button mod=btn("Modifica");mod.setOnClickListener(v->showExercise(e,true));head.addView(mod,new LinearLayout.LayoutParams(105,48));c.addView(head);
        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);TextView r=tv("Recupero  "+(e.recovery.isEmpty()?"—":e.recovery),12,true);r.setTextColor(secondary());info.addView(r,new LinearLayout.LayoutParams(0,40,1));TextView target=tv(e.target,11,true);target.setTextColor(accent);info.addView(target);c.addView(info);
        Button open=btn("Apri esercizio  ›");open.setOnClickListener(v->showExercise(e,false));c.addView(open,new LinearLayout.LayoutParams(-1,50));
    }

    void showExercise(Exercise e, boolean editing){
        clear(e.name);
        TextView focus=tv(e.group+"  •  "+e.focus,13,false);focus.setTextColor(secondary());content.addView(focus);
        LinearLayout top=new LinearLayout(this);Button edit=btn(editing?"Fine":"Modifica");edit.setOnClickListener(v->showExercise(e,!editing));top.addView(edit,new LinearLayout.LayoutParams(0,50,1));
        Button back=btn("‹ Torna alla scheda");back.setOnClickListener(v->showDashboard());top.addView(back,new LinearLayout.LayoutParams(0,50,1));content.addView(top,new LinearLayout.LayoutParams(-1,56));
        if(timerExerciseId==e.id && timerEndMs>0 && timerEndMs>System.currentTimeMillis()) showRecoveryTimer(e,(int)Math.max(1,(timerEndMs-System.currentTimeMillis()+999)/1000));
        LinearLayout recRow=new LinearLayout(this);recRow.setGravity(Gravity.CENTER_VERTICAL);TextView rl=tv("Recupero",13,true);recRow.addView(rl,new LinearLayout.LayoutParams(0,50,1));
        EditText rec=edit("2:00");rec.setText(e.recovery);rec.setEnabled(editing);rec.setInputType(1);rec.setSelectAllOnFocus(true);rec.setOnFocusChangeListener((v,has)->{if(!has && editing){e.recovery=rec.getText().toString();store.save();}});recRow.addView(rec,new LinearLayout.LayoutParams(100,48));content.addView(recRow);

        if(editing){
            Switch bo=new Switch(this);bo.setText("Back-off ultima serie\n80% della serie precedente");bo.setTextColor(fg());bo.setTextSize(13);bo.setChecked(e.backOffEnabled);bo.setOnCheckedChangeListener((b,checked)->{if(b.isPressed()){store.setBackOffEnabled(e,checked);showExercise(e,true);}});content.addView(bo,new LinearLayout.LayoutParams(-1,64));
        }

        for(int i=0;i<e.sets.size();i++)addSetRow(e,i,editing);
        LinearLayout actions=new LinearLayout(this);Button plus=btn("+ Serie"),minus=btn("− Serie");plus.setOnClickListener(v->{store.addSet(e);showExercise(e,editing);});minus.setOnClickListener(v->{store.removeSet(e);showExercise(e,editing);});actions.addView(plus,new LinearLayout.LayoutParams(0,52,1));actions.addView(minus,new LinearLayout.LayoutParams(0,52,1));content.addView(actions);

        Button chart=btn("📈  Andamento del peso");chart.setOnClickListener(v->showChart(e));content.addView(chart,new LinearLayout.LayoutParams(-1,54));
        Button reset=btn("↻  Azzera serie completate");reset.setOnClickListener(v->{for(Set s:e.sets)s.done=false;store.save();showExercise(e,editing);});content.addView(reset,new LinearLayout.LayoutParams(-1,54));
        MuscleMapView map=new MuscleMapView(this,e.target);content.addView(map,new LinearLayout.LayoutParams(-1,185));
    }

    void addSetRow(Exercise e,int idx,boolean editing){
        Set s=e.sets.get(idx);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(2,5,2,5);
        TextView sn=tv("S"+(idx+1),12,true);sn.setTextColor(accent);row.addView(sn,new LinearLayout.LayoutParams(32,52));
        EditText reps=edit(s.reps);reps.setText(s.reps);reps.setEnabled(editing);reps.setInputType(1);reps.setOnFocusChangeListener((v,f)->{if(!f&&editing){s.reps=reps.getText().toString();store.save();}});row.addView(reps,new LinearLayout.LayoutParams(70,48));
        TextView sep=tv("  ",10,false);row.addView(sep,new LinearLayout.LayoutParams(5,48));
        if(s.isBackOff){
            TextView w=tv(fmt(store.backOffWeight(e)),15,true);w.setGravity(Gravity.CENTER);w.setTextColor(accent);w.setBackground(box(dark?Color.rgb(44,30,50):Color.rgb(245,235,250),14));
            row.addView(w,new LinearLayout.LayoutParams(76,48));TextView kg=tv(" kg  Back-off −20%",10,true);kg.setTextColor(accent);row.addView(kg,new LinearLayout.LayoutParams(0,48,1));
        }else{
            EditText w=edit("kg");w.setText(fmt(s.weight));w.setEnabled(editing);w.setInputType(2|8192);w.setSelectAllOnFocus(true);
            w.setOnFocusChangeListener((v,f)->{if(!f&&editing){commitWeight(e,idx,w);}});w.setOnEditorActionListener((v,id,event)->{commitWeight(e,idx,w);hideKeyboard();return true;});
            row.addView(w,new LinearLayout.LayoutParams(76,48));TextView kg=tv(" kg",11,true);kg.setTextColor(accent);row.addView(kg,new LinearLayout.LayoutParams(30,48));
        }
        CheckBox done=new CheckBox(this);done.setChecked(s.done);done.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){s.done=c;store.save();if(c)startRecovery(e,s);else stopRecovery(s.id);}});row.addView(done,new LinearLayout.LayoutParams(48,52));
        content.addView(row);
    }
    void commitWeight(Exercise e,int idx,EditText w){
        try{String raw=w.getText().toString().replace(",",".");if(raw.trim().isEmpty()){store.setWeight(e,idx,0);return;}double x=Double.parseDouble(raw);store.setWeight(e,idx,x);w.setText(fmt(x));}catch(Exception ignored){}
    }

    void startRecovery(Exercise e, Set s){
        try{
            String r=e.recovery==null?"":e.recovery.trim(); int sec=parseRecovery(r);
            if(sec<=0)return;
            timerExerciseId=e.id; timerSetId=s.id; timerDurationMs=sec*1000L; timerEndMs=System.currentTimeMillis()+timerDurationMs;
            showRecoveryTimer(e,sec);
            if(store.notificationsEnabled) RecoveryNotifications.schedule(this,e.name,s.id,sec);
            timerHandler.removeCallbacksAndMessages(null);
            timerHandler.post(timerTick);
        }catch(Exception ignored){}
    }
    void stopRecovery(long setId){
        if(timerSetId==setId){timerHandler.removeCallbacksAndMessages(null);timerExerciseId=-1;timerSetId=-1;timerEndMs=0;timerDurationMs=0;recoveryTimerCard=null;recoveryProgress=null;recoveryCountdown=null;}
        RecoveryNotifications.cancel(this,setId);
    }
    void showRecoveryTimer(Exercise e,int sec){
        if(recoveryTimerCard==null || recoveryTimerCard.getParent()==null || timerExerciseId!=e.id){
            recoveryTimerCard=new LinearLayout(this); recoveryTimerCard.setOrientation(LinearLayout.VERTICAL); recoveryTimerCard.setPadding(18,14,18,14); recoveryTimerCard.setBackground(box(dark?Color.rgb(30,24,33):Color.rgb(248,241,250),24));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,12);content.addView(recoveryTimerCard,lp);
        }else recoveryTimerCard.removeAllViews();
        TextView label=tv("RECUPERO",12,true);label.setTextColor(accent);recoveryTimerCard.addView(label,new LinearLayout.LayoutParams(-1,24));
        recoveryCountdown=tv(formatTime(sec),30,true);recoveryCountdown.setGravity(Gravity.CENTER);recoveryTimerCard.addView(recoveryCountdown,new LinearLayout.LayoutParams(-1,48));
        recoveryProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);recoveryProgress.setMax(1000);recoveryProgress.setProgress(0);recoveryProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));recoveryProgress.setIndeterminate(false);recoveryTimerCard.addView(recoveryProgress,new LinearLayout.LayoutParams(-1,10));
        TextView hint=tv("La barra avanza automaticamente. Alla fine riceverai la notifica.",11,false);hint.setTextColor(secondary());hint.setGravity(Gravity.CENTER);recoveryTimerCard.addView(hint,new LinearLayout.LayoutParams(-1,28));
    }
    String formatTime(long sec){return String.format(Locale.ITALIAN,"%d:%02d",sec/60,sec%60);}
    final Runnable timerTick=new Runnable(){public void run(){
        if(timerEndMs<=0)return; long remaining=Math.max(0,timerEndMs-System.currentTimeMillis()); long total=Math.max(1,timerDurationMs);
        if(recoveryCountdown!=null)recoveryCountdown.setText(formatTime((remaining+999)/1000));
        if(recoveryProgress!=null)recoveryProgress.setProgress((int)Math.min(1000,Math.max(0,((total-remaining)*1000)/total)));
        if(remaining<=0){timerHandler.removeCallbacks(this);if(recoveryCountdown!=null)recoveryCountdown.setText("0:00");if(recoveryProgress!=null)recoveryProgress.setProgress(1000);return;}
        timerHandler.postDelayed(this,500);
    }};
    int parseRecovery(String r){try{String[]p=r.split(":");return p.length==2?Integer.parseInt(p[0])*60+Integer.parseInt(p[1]):Integer.parseInt(p[0]);}catch(Exception e){return 0;}}

    void showChart(Exercise e){
        clear("Progressi • "+e.name);
        TextView desc=tv("Ogni nuovo peso confermato aggiunge un punto allo storico.",13,false);desc.setTextColor(secondary());content.addView(desc);
        ArrayList<Log> logs=store.history(e); ChartView chart=new ChartView(this,logs,accent);content.addView(chart,new LinearLayout.LayoutParams(-1,340));
        LinearLayout metrics=new LinearLayout(this);addMetric(metrics,"Massimo",fmt(store.maxWeight(e))+" kg","↑");addMetric(metrics,"Aggiornamenti",String.valueOf(logs.size()),"•");content.addView(metrics,new LinearLayout.LayoutParams(-1,90));
        if(!logs.isEmpty()){
            TextView h=tv("Storico",17,true);h.setPadding(4,14,4,8);content.addView(h);
            ArrayList<Log> rev=new ArrayList<>(logs);Collections.reverse(rev);for(Log l:rev){TextView t=tv(new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.ITALIAN).format(new Date(l.date))+"                         "+fmt(l.weight)+" kg",13,false);t.setPadding(8,8,8,8);content.addView(t);}
        }
        Button back=btn("‹ Torna all'esercizio");back.setOnClickListener(v->showExercise(e,false));content.addView(back,new LinearLayout.LayoutParams(-1,54));
    }

    void showProgress(){
        clear("Progressi");TextView d=tv("Allenamenti, progressi e storico dei kg.",14,false);d.setTextColor(secondary());content.addView(d);
        boolean any=false;for(String day:DAYS){List<Exercise> es=store.forDay(day);if(es.isEmpty())continue;any=true;Button b=btn(day+"   •   "+workoutName(day)+"   •   "+es.size()+" esercizi");b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(16,0,16,0);b.setOnClickListener(v->showDayProgress(day));content.addView(b,new LinearLayout.LayoutParams(-1,68));}
        if(!any){TextView t=tv("Nessun allenamento",16,true);content.addView(t);}
    }
    String workoutName(String d){if(d.equals("LUNEDÌ"))return"UPPER";if(d.equals("MARTEDÌ"))return"LOWER";if(d.equals("MERCOLEDÌ"))return"FULLBODY";return"ALLENAMENTO";}
    void showDayProgress(String day){
        clear(day+"  •  "+workoutName(day));Button back=btn("‹ Torna ai progressi");back.setOnClickListener(v->showProgress());content.addView(back,new LinearLayout.LayoutParams(-1,50));
        for(Exercise e:store.forDay(day)){Button b=btn(e.name+"\n"+e.sets.size()+" serie  •  massimo "+fmt(store.maxWeight(e))+" kg");b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(16,0,16,0);b.setOnClickListener(v->showChart(e));content.addView(b,new LinearLayout.LayoutParams(-1,78));}
    }

    void showAdd(){
        clear("Aggiungi esercizio");Button back=btn("‹ Torna alla scheda");back.setOnClickListener(v->showDashboard());content.addView(back,new LinearLayout.LayoutParams(-1,50));
        Spinner day=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,DAYS);day.setAdapter(a);day.setSelection(Math.max(0,Arrays.asList(DAYS).indexOf(store.selectedDay)));content.addView(day,new LinearLayout.LayoutParams(-1,52));
        EditText name=edit("Nome");EditText reps=edit("Ripetizioni (es. 7-9)");reps.setText("8-10");EditText recovery=edit("Recupero (es. 2:00)");recovery.setText("2:00");
        content.addView(name,new LinearLayout.LayoutParams(-1,50));content.addView(reps,new LinearLayout.LayoutParams(-1,50));
        TextView sh=tv("Serie normali",15,true);sh.setPadding(4,12,4,5);content.addView(sh);
        LinearLayout weightsBox=new LinearLayout(this);weightsBox.setOrientation(LinearLayout.VERTICAL);content.addView(weightsBox);
        ArrayList<EditText> weights=new ArrayList<>();final int[] count={3};
        Runnable rebuild=()->{weightsBox.removeAllViews();weights.clear();for(int i=0;i<count[0];i++){EditText w=edit("Serie "+(i+1)+" kg");w.setInputType(2|8192);w.setText("20");weights.add(w);weightsBox.addView(w,new LinearLayout.LayoutParams(-1,48));}};
        rebuild.run();
        LinearLayout steppers=new LinearLayout(this);Button minus=btn("−"),plus=btn("+");TextView num=tv("  3 serie  ",15,true);minus.setOnClickListener(v->{if(count[0]>1){count[0]--;num.setText("  "+count[0]+" serie  ");rebuild.run();}});plus.setOnClickListener(v->{if(count[0]<20){count[0]++;num.setText("  "+count[0]+" serie  ");rebuild.run();}});steppers.addView(minus,new LinearLayout.LayoutParams(70,48));steppers.addView(num,new LinearLayout.LayoutParams(0,48,1));steppers.addView(plus,new LinearLayout.LayoutParams(70,48));content.addView(steppers);
        Switch backOff=new Switch(this);backOff.setText("Aggiungi back-off −20%");backOff.setTextColor(fg());content.addView(backOff);
        TextView boPreview=tv("Il back-off viene calcolato automaticamente dall'ultima serie normale.",12,false);boPreview.setTextColor(secondary());content.addView(boPreview);
        content.addView(recovery,new LinearLayout.LayoutParams(-1,50));
        Button add=btn("Aggiungi alla scheda");add.setOnClickListener(v->{hideKeyboard();try{ArrayList<Double> ws=new ArrayList<>();for(EditText w:weights)ws.add(Double.parseDouble(w.getText().toString().replace(",",".")));store.add(day.getSelectedItem().toString(),name.getText().toString().trim(),reps.getText().toString().trim(),ws,recovery.getText().toString().trim(),backOff.isChecked());showDashboard();}catch(Exception ex){Toast.makeText(this,"Compila nome, serie e kg",Toast.LENGTH_SHORT).show();}});content.addView(add,new LinearLayout.LayoutParams(-1,56));
        Button imp=btn("📷  Importa scheda da foto / galleria");imp.setOnClickListener(v->showImport());content.addView(imp,new LinearLayout.LayoutParams(-1,56));
    }

    void showImport(){
        clear("Importa scheda");
        Button back=btn("‹ Torna alla scheda");back.setOnClickListener(v->showDashboard());content.addView(back,new LinearLayout.LayoutParams(-1,50));
        TextView info=tv("Importa una scheda da foto, fotocamera o PDF. Il testo viene riconosciuto e trasformato in esercizi con serie, ripetizioni e recupero.",13,false);info.setTextColor(secondary());content.addView(info);
        Button camera=btn("📷  Scatta foto");camera.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},78);return;}
            Intent in=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(in,REQ_CAMERA);
        });content.addView(camera,new LinearLayout.LayoutParams(-1,54));
        Button gallery=btn("🖼  Scegli foto");gallery.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("image/*");startActivityForResult(in,REQ_IMAGE);});content.addView(gallery,new LinearLayout.LayoutParams(-1,54));
        Button pdf=btn("📄  Importa PDF");pdf.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("application/pdf");startActivityForResult(in,1200);});content.addView(pdf,new LinearLayout.LayoutParams(-1,54));
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null)return;
        if(requestCode==REQ_IMAGE && data.getData()!=null)ocrImage(data.getData());
        else if(requestCode==REQ_CAMERA && data.getExtras()!=null){Bitmap b=(Bitmap)data.getExtras().get("data");if(b!=null)ocrBitmap(b);}
        else if(requestCode==1200 && data.getData()!=null)ocrPdf(data.getData());
    }

    void ocrImage(Uri uri){try{InputImage img=InputImage.fromFilePath(this,uri);ocr(img);}catch(Exception e){Toast.makeText(this,"Impossibile leggere l'immagine",Toast.LENGTH_SHORT).show();}}
    void ocrBitmap(Bitmap b){ocr(InputImage.fromBitmap(b,0));}
    void ocr(InputImage img){
        Toast.makeText(this,"Analizzo la scheda…",Toast.LENGTH_SHORT).show();
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img)
            .addOnSuccessListener(r->showImportedPreview(r.getText()))
            .addOnFailureListener(e->Toast.makeText(this,"OCR non riuscito",Toast.LENGTH_SHORT).show());
    }
    void ocrPdf(Uri uri){
        Toast.makeText(this,"Analizzo il PDF…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            StringBuilder all=new StringBuilder();
            try{
                ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");
                if(fd==null)throw new IOException();
                PdfRenderer renderer=new PdfRenderer(fd);
                for(int i=0;i<renderer.getPageCount();i++){
                    PdfRenderer.Page page=renderer.openPage(i);
                    int w=Math.max(600,page.getWidth()*2), h=Math.max(800,page.getHeight()*2);
                    Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                    b.eraseColor(Color.WHITE); page.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); page.close();
                    String text=Tasks.await(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(b,0))).getText();
                    all.append(text).append("\n");
                    b.recycle();
                }
                renderer.close();fd.close();
                final String result=all.toString();
                runOnUiThread(()->showImportedPreview(result));
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Impossibile leggere il PDF",Toast.LENGTH_SHORT).show());}
        }).start();
    }

    void showImportedPreview(String text){
        ArrayList<Imported> items=store.parseImported(text);
        if(items.isEmpty()){new AlertDialog.Builder(this).setTitle("Scheda non riconosciuta").setMessage("Non ho trovato righe nel formato esercizio × serie × ripetizioni. Prova con una foto più nitida.").setPositiveButton("OK",null).show();return;}
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(18,4,18,4);
        TextView count=tv("Controlla prima di importare ("+items.size()+" esercizi)",17,true);list.addView(count);
        for(Imported it:items){TextView row=tv(it.day+"  •  "+it.name+"\n"+it.sets+" serie  •  "+it.reps+"  •  recupero "+(it.recovery.isEmpty()?"—":it.recovery),13,false);row.setPadding(0,10,0,10);list.addView(row);}
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        new AlertDialog.Builder(this).setTitle("Importa scheda").setView(scroll).setNegativeButton("Annulla",null).setPositiveButton("Importa tutto",(d,w)->{int n=0;for(Imported it:items){store.add(it.day,it.name,it.reps,repeat(20,it.sets),it.recovery,false);n++;}Toast.makeText(this,"Importati "+n+" esercizi",Toast.LENGTH_LONG).show();showDashboard();}).show();
    }
    ArrayList<Double> repeat(double value,int n){ArrayList<Double>x=new ArrayList<>();for(int i=0;i<Math.max(1,n);i++)x.add(value);return x;}

    void showSettings(){
        clear("Impostazioni");

        sectionLabel("Aspetto");
        LinearLayout appearance=new LinearLayout(this); appearance.setOrientation(LinearLayout.VERTICAL);
        LinearLayout themeRow=new LinearLayout(this); themeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView tl=tv("Tema scuro",16,true); themeRow.addView(tl,new LinearLayout.LayoutParams(0,58,1));
        Switch theme=new Switch(this); theme.setChecked(dark); theme.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
        theme.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){store.dark=c;store.save();build();}}); themeRow.addView(theme,new LinearLayout.LayoutParams(64,58)); appearance.addView(themeRow);
        TextView cl=tv("Colore principale",16,true); cl.setPadding(0,12,0,6); appearance.addView(cl,new LinearLayout.LayoutParams(-1,48));
        String[] names={"Viola","Blu","Verde","Arancione","Rosso","Rosa"};
        int[] colors={Color.rgb(216,137,255),Color.rgb(70,130,255),Color.rgb(50,190,110),Color.rgb(245,150,55),Color.rgb(255,95,105),Color.rgb(245,90,160)};
        LinearLayout swatches=new LinearLayout(this); swatches.setGravity(Gravity.CENTER);
        for(int i=0;i<names.length;i++){
            final int k=i; TextView dot=new TextView(this); dot.setText("●"); dot.setTextSize(23); dot.setGravity(Gravity.CENTER); dot.setTextColor(colors[i]);
            dot.setContentDescription(names[i]); dot.setPadding(4,0,4,0); dot.setOnClickListener(v->{store.accentIndex=k;store.accent=colors[k];store.save();build();});
            swatches.addView(dot,new LinearLayout.LayoutParams(0,48,1));
        }
        appearance.addView(swatches,new LinearLayout.LayoutParams(-1,54));
        TextView current=tv("Tema attuale   "+(dark?"Scuro":"Chiaro"),13,false); current.setTextColor(secondary()); current.setGravity(Gravity.CENTER_VERTICAL); appearance.addView(current,new LinearLayout.LayoutParams(-1,42));
        cardView(appearance,0,14);

        sectionLabel("Notifiche recupero");
        LinearLayout notif=new LinearLayout(this); notif.setOrientation(LinearLayout.VERTICAL);
        LinearLayout nr=new LinearLayout(this); nr.setGravity(Gravity.CENTER_VERTICAL);
        TextView nt=tv("Notifica al termine del recupero",15,true); nr.addView(nt,new LinearLayout.LayoutParams(0,56,1));
        Switch ns=new Switch(this); ns.setChecked(store.notificationsEnabled); ns.setOnCheckedChangeListener((b,c)->{if(!b.isPressed())return;store.notificationsEnabled=c;store.save();if(c&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);}); nr.addView(ns,new LinearLayout.LayoutParams(64,56)); notif.addView(nr);
        TextView nd=tv("Quando completi una serie parte il timer. Alla fine ricevi una notifica anche se l'app è in background.",12,false); nd.setTextColor(secondary()); nd.setPadding(0,0,0,10); notif.addView(nd,new LinearLayout.LayoutParams(-1,48));
        Button open=btn("Apri impostazioni notifiche"); open.setTextColor(accent); open.setOnClickListener(v->openNotificationSettings()); notif.addView(open,new LinearLayout.LayoutParams(-1,50));
        cardView(notif,0,14);

        sectionLabel("Dati");
        LinearLayout data=new LinearLayout(this); data.setOrientation(LinearLayout.VERTICAL);
        TextView d=tv("I dati vengono salvati localmente sul telefono.",15,true); d.setPadding(0,0,0,8); data.addView(d,new LinearLayout.LayoutParams(-1,54));
        Button req=btn("Richiedi notifiche"); req.setTextColor(accent); req.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);else openNotificationSettings();}); data.addView(req,new LinearLayout.LayoutParams(-1,50));
        cardView(data,0,14);

        sectionLabel("Settimana");
        LinearLayout week=new LinearLayout(this); week.setOrientation(LinearLayout.VERTICAL);
        for(String day:DAYS){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView a=tv(day,14,true);row.addView(a,new LinearLayout.LayoutParams(0,52,1));TextView n=tv(store.forDay(day).size()+" esercizi",14,true);n.setTextColor(secondary());n.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(n,new LinearLayout.LayoutParams(0,52,1));week.addView(row);if(!day.equals(DAYS[DAYS.length-1])){View line=new View(this);line.setBackgroundColor(dark?Color.rgb(55,55,59):Color.rgb(225,225,228));week.addView(line,new LinearLayout.LayoutParams(-1,1));}}
        cardView(week,0,20);
    }

    String fmt(double x){return x==Math.rint(x)?String.valueOf((int)x):String.format(Locale.US,"%.1f",x);}

    public static class Imported{String day,name,reps,recovery;int sets;Imported(String d,String n,String r,int s,String rec){day=d;name=n;reps=r;sets=s;recovery=rec;}}
    public static class Exercise{long id;String day,name,group,focus,target,reps,recovery="";boolean backOffEnabled;ArrayList<Set>sets=new ArrayList<>();}
    public static class Set{long id;String reps;double weight;boolean done,isBackOff;ArrayList<Log>history=new ArrayList<>();}
    public static class Log{long date;double weight;Log(long d,double w){date=d;weight=w;}}

    static class WorkoutStore{
        final Context c;ArrayList<Exercise> all=new ArrayList<>();String selectedDay="LUNEDÌ";boolean dark=true;int accent=Color.rgb(216,137,255),accentIndex=0;final String key="gymapp.android.v2";boolean notificationsEnabled=true;
        final String[] days={"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};
        WorkoutStore(Context c){this.c=c;load();if(all.isEmpty()){initial();save();}}
        List<Exercise>forDay(String d){ArrayList<Exercise>x=new ArrayList<>();for(Exercise e:all)if(e.day.equals(d))x.add(e);return x;}
        int totalSets(List<Exercise> es){int n=0;for(Exercise e:es)n+=e.sets.size();return n;}
        int completedSets(List<Exercise> es){int n=0;for(Exercise e:es)for(Set s:e.sets)if(s.done)n++;return n;}
        void initial(){
            add0("LUNEDÌ","Spinte manubri panca 32","7-9",3,32,"PETTO (ALTO)","Pettorali superiori e tricipiti","chest","2:00");
            add0("LUNEDÌ","Lat Pulldown","7-9",3,20,"DORSO","Gran dorsale e bicipiti","back","2:00");
            add0("LUNEDÌ","Chest Press","8-10",3,20,"PETTO","Pettorali e tricipiti","chest","2:00");
            add0("LUNEDÌ","T-Bar prona larga","8-10",3,20,"DORSO","Dorsali e parte alta della schiena","back","2:00");
            add0("LUNEDÌ","Alzate laterali","10-12",4,20,"SPALLE","Deltoide laterale","shoulders","1:30");
            add0("LUNEDÌ","Push Down asta curva","10 RM",4,20,"TRICIPITI","Tricipite","triceps","1:30");
            add0("LUNEDÌ","Curl cavo basso","10 RM",4,20,"BICIPITI","Bicipite","biceps","1:30");
            add0("MARTEDÌ","Leg Extension","12 RM",4,62,"QUADRICIPITI","Quadricipite","quads","1:30");
            add0("MARTEDÌ","Leg Press 45","7-9",3,130,"GAMBE","Quadricipiti e glutei","quads","2:00");
            add0("MARTEDÌ","Leg Curl sdraiato","10-12",2,47,"FEMORALI","Femorali","hamstrings","1:30");
            add0("MARTEDÌ","Adduttori","10-12",2,30,"ADDUTTORI","Adduttori","quads","1:30");
            add0("MARTEDÌ","Calf Machine","8-10",4,50,"POLPACCI","Polpacci","hamstrings","1:30");
            add0("MERCOLEDÌ","Panca piana bilanciere","7-9",4,40,"PETTO","Pettorali e tricipiti","chest","2:30");
            add0("MERCOLEDÌ","Rematore bilanciere","7-9",3,40,"DORSO","Dorsali e parte alta della schiena","back","2:00");
            add0("MERCOLEDÌ","Lento avanti manubri panca 71","8-10",3,20,"SPALLE","Deltoidi e tricipiti","shoulders","2:00");
            add0("MERCOLEDÌ","Rowing","8-10",3,30,"DORSO","Schiena","back","1:30");
            add0("MERCOLEDÌ","Stacchi rumeni manubri","7-9",3,30,"FEMORALI / GLUTEI","Catena posteriore","hamstrings","2:00");
            add0("MERCOLEDÌ","Leg Curl seduto","12 RM",2,40,"FEMORALI","Femorali","hamstrings","1:30");
            add0("MERCOLEDÌ","Arm Curl","10 RM",3,20,"BICIPITI","Bicipite","biceps","1:30");
            add0("MERCOLEDÌ","French Press manubri","10 RM",3,20,"TRICIPITI","Tricipite","triceps","1:30");
        }
        void add0(String d,String n,String r,int num,double w,String g,String f,String t,String rec){
            Exercise e=new Exercise();e.id=System.nanoTime()+all.size();e.day=d;e.name=n;e.reps=r;e.group=g;e.focus=f;e.target=t;e.recovery=rec;
            for(int i=0;i<num;i++){Set s=new Set();s.id=e.id+i+1;s.reps=r;s.weight=w;e.sets.add(s);}all.add(e);
        }
        void setWeight(Exercise e,int i,double w){if(i<0||i>=e.sets.size()||e.sets.get(i).isBackOff)return;Set s=e.sets.get(i);if(s.weight!=w){s.weight=w;s.history.add(new Log(System.currentTimeMillis(),w));}refreshBackOff(e);save();}
        double backOffWeight(Exercise e){if(!e.backOffEnabled||e.sets.size()<2)return 0;return roundBackOff(e.sets.get(e.sets.size()-2).weight);}
        double roundBackOff(double w){return Math.round(w*0.8*2)/2.0;}
        void setBackOffEnabled(Exercise e,boolean enabled){
            if(enabled){if(e.backOffEnabled)return;Set source=e.sets.get(e.sets.size()-1);Set bo=new Set();bo.id=System.nanoTime();bo.reps=source.reps;bo.weight=roundBackOff(source.weight);bo.isBackOff=true;e.sets.add(bo);e.backOffEnabled=true;}
            else{if(!e.backOffEnabled)return;if(!e.sets.isEmpty())e.sets.remove(e.sets.size()-1);e.backOffEnabled=false;}refreshBackOff(e);save();
        }
        void refreshBackOff(Exercise e){if(!e.backOffEnabled||e.sets.size()<2)return;Set prev=e.sets.get(e.sets.size()-2),bo=e.sets.get(e.sets.size()-1);bo.weight=roundBackOff(prev.weight);bo.reps=prev.reps;bo.isBackOff=true;bo.done=false;}
        void addSet(Exercise e){
            if(e.sets.isEmpty())return;
            if(e.backOffEnabled){Set bo=e.sets.remove(e.sets.size()-1);Set prev=e.sets.get(e.sets.size()-1);Set s=copySet(prev);s.id=System.nanoTime();s.isBackOff=false;e.sets.add(s);e.sets.add(bo);refreshBackOff(e);}
            else{Set s=copySet(e.sets.get(e.sets.size()-1));s.id=System.nanoTime();s.isBackOff=false;e.sets.add(s);}save();
        }
        Set copySet(Set src){Set s=new Set();s.reps=src.reps;s.weight=src.weight;s.done=false;s.isBackOff=false;return s;}
        void removeSet(Exercise e){int regular=e.backOffEnabled?e.sets.size()-1:e.sets.size();if(regular<=1)return;int idx=e.backOffEnabled?e.sets.size()-2:e.sets.size()-1;e.sets.remove(idx);refreshBackOff(e);save();}
        ArrayList<Log>history(Exercise e){ArrayList<Log>x=new ArrayList<>();for(Set s:e.sets)x.addAll(s.history);x.sort(Comparator.comparingLong(a->a.date));return x;}
        double maxWeight(Exercise e){double m=0;for(Set s:e.sets)m=Math.max(m,s.weight);for(Log l:history(e))m=Math.max(m,l.weight);return m;}
        void resetDay(){for(Exercise e:forDay(selectedDay))for(Set s:e.sets)s.done=false;save();}
        void add(String d,String n,String r,ArrayList<Double> ws,String rec,boolean bo){
            if(n==null||n.isEmpty())return;String[] q=recognize(n);Exercise e=new Exercise();e.id=System.nanoTime();e.day=d;e.name=n;e.reps=r;e.recovery=rec;e.group=q[0];e.focus=q[1];e.target=q[2];
            for(double w:ws){Set s=new Set();s.id=System.nanoTime()+e.sets.size();s.reps=r;s.weight=w;e.sets.add(s);}
            e.backOffEnabled=bo;if(bo){Set src=e.sets.get(e.sets.size()-1);Set s=new Set();s.id=System.nanoTime();s.reps=src.reps;s.weight=roundBackOff(src.weight);s.isBackOff=true;e.sets.add(s);}all.add(e);selectedDay=d;save();
        }
        String[] recognize(String n){String x=n.toLowerCase(Locale.ITALIAN);
            if(x.contains("panca")||x.contains("chest"))return new String[]{"PETTO","Pettorali e tricipiti","chest"};
            if(x.contains("lat")||x.contains("row")||x.contains("rematore")||x.contains("t-bar"))return new String[]{"DORSO","Dorsali e parte alta della schiena","back"};
            if(x.contains("alzate")||x.contains("lento"))return new String[]{"SPALLE","Deltoidi","shoulders"};
            if(x.contains("curl")||x.contains("bicip"))return new String[]{"BICIPITI","Bicipite","biceps"};
            if(x.contains("push")||x.contains("french")||x.contains("tricip"))return new String[]{"TRICIPITI","Tricipite","triceps"};
            if(x.contains("leg extension")||x.contains("press"))return new String[]{"QUADRICIPITI","Quadricipiti e glutei","quads"};
            if(x.contains("stacchi")||x.contains("femorali")||x.contains("leg curl"))return new String[]{"FEMORALI / GLUTEI","Catena posteriore","hamstrings"};
            return new String[]{"ALTRO","Muscoli vari","full"};
        }
        int importText(String text,String day){int n=0;for(Imported it:parseImported(text)){add(it.day,it.name,it.reps,repeatWeights(it.sets),it.recovery,false);n++;}return n;}
        ArrayList<Double> repeatWeights(int n){ArrayList<Double>x=new ArrayList<>();for(int i=0;i<Math.max(1,n);i++)x.add(20.0);return x;}
        ArrayList<Imported> parseImported(String text){
            ArrayList<Imported> result=new ArrayList<>();String day="LUNEDÌ";
            String[] lines=text.split("\\n");
            for(String raw:lines){
                String l=raw.trim();if(l.length()<3)continue;
                String upper=l.toUpperCase(Locale.ITALIAN).replace("  "," ");
                String[] dayNames={"LUNEDÌ","LUNEDI","MARTEDÌ","MARTEDI","MERCOLEDÌ","MERCOLEDI","GIOVEDÌ","GIOVEDI","VENERDÌ","VENERDI","SABATO","DOMENICA"};
                for(String dn:dayNames)if(upper.contains(dn)&&upper.length()<30){day=normalizeDay(dn);break;}
                if(upper.contains("GIORNO 1"))day="LUNEDÌ";if(upper.contains("GIORNO 2"))day="MARTEDÌ";if(upper.contains("GIORNO 3"))day="MERCOLEDÌ";
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*[xX×]\\s*(\\d+(?:[-–]\\d+)?|RM)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(l);
                if(!m.find())continue;
                int sets=Integer.parseInt(m.group(1));String reps=m.group(2).replace("–","-");String before=l.substring(0,m.start()).replace("—"," ").replace("-"," ").trim();
                if(before.length()<3)continue;String recovery=parseRecoveryText(l);
                if(before.matches("(?i).*\\b(UPPER|LOWER|FULLBODY|GIORNO)\\b.*"))continue;
                result.add(new Imported(day,before,reps,sets,recovery));
            }return result;
        }
        String normalizeDay(String d){if(d.equals("LUNEDI"))return"LUNEDÌ";if(d.equals("MARTEDI"))return"MARTEDÌ";if(d.equals("MERCOLEDI"))return"MERCOLEDÌ";if(d.equals("GIOVEDI"))return"GIOVEDÌ";if(d.equals("VENERDI"))return"VENERDÌ";return d;}
        String parseReps(String l){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+\\s*[-–]\\s*\\d+|\\d+\\s*RM)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(l);return m.find()?m.group(1).replace("–","-"):"8-10";}
        String parseRecoveryText(String l){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d{1,2}:\\d{2})").matcher(l);return m.find()?m.group(1):"";}
        void save(){try{SharedPreferences p=c.getSharedPreferences("gym",0);JSONArray a=new JSONArray();for(Exercise e:all){JSONObject o=new JSONObject();o.put("id",e.id);o.put("day",e.day);o.put("name",e.name);o.put("group",e.group);o.put("focus",e.focus);o.put("target",e.target);o.put("reps",e.reps);o.put("recovery",e.recovery);o.put("backOffEnabled",e.backOffEnabled);JSONArray ss=new JSONArray();for(Set s:e.sets){JSONObject z=new JSONObject();z.put("id",s.id);z.put("reps",s.reps);z.put("weight",s.weight);z.put("done",s.done);z.put("isBackOff",s.isBackOff);JSONArray hh=new JSONArray();for(Log l:s.history){JSONObject k=new JSONObject();k.put("date",l.date);k.put("weight",l.weight);hh.put(k);}z.put("history",hh);ss.put(z);}o.put("sets",ss);a.put(o);}p.edit().putString(key,a.toString()).putString("selectedDay",selectedDay).putBoolean("dark",dark).putInt("accent",accent).putInt("accentIndex",accentIndex).putBoolean("notificationsEnabled",notificationsEnabled).apply();}catch(Exception ignored){}}
        void load(){try{SharedPreferences p=c.getSharedPreferences("gym",0);dark=p.getBoolean("dark",true);accent=p.getInt("accent",Color.rgb(216,137,255));accentIndex=p.getInt("accentIndex",0);notificationsEnabled=p.getBoolean("notificationsEnabled",true);selectedDay=p.getString("selectedDay","LUNEDÌ");String raw=p.getString(key,"");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Exercise e=new Exercise();e.id=o.getLong("id");e.day=o.getString("day");e.name=o.getString("name");e.group=o.getString("group");e.focus=o.getString("focus");e.target=o.getString("target");e.reps=o.getString("reps");e.recovery=o.optString("recovery","");e.backOffEnabled=o.optBoolean("backOffEnabled",false);JSONArray ss=o.getJSONArray("sets");for(int j=0;j<ss.length();j++){JSONObject z=ss.getJSONObject(j);Set s=new Set();s.id=z.getLong("id");s.reps=z.getString("reps");s.weight=z.getDouble("weight");s.done=z.optBoolean("done",false);s.isBackOff=z.optBoolean("isBackOff",false);JSONArray hh=z.optJSONArray("history");if(hh!=null)for(int k=0;k<hh.length();k++){JSONObject q=hh.getJSONObject(k);s.history.add(new Log(q.getLong("date"),q.getDouble("weight")));}e.sets.add(s);}if(e.backOffEnabled&&(!e.sets.isEmpty()&&!e.sets.get(e.sets.size()-1).isBackOff)){e.backOffEnabled=false;}all.add(e);}}catch(Exception ignored){}}
    }

    static class ChartView extends View{
        Paint p=new Paint(1);ArrayList<Log> logs;int color;ChartView(Context c,ArrayList<Log> l,int color){super(c);logs=l;this.color=color;}
        protected void onDraw(Canvas c){int w=getWidth(),h=getHeight(),left=55,right=w-12,top=25,bottom=h-48;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.GRAY);c.drawLine(left,top,left,bottom,p);c.drawLine(left,bottom,right,bottom,p);p.setTextSize(18);p.setStyle(Paint.Style.FILL);for(int i=0;i<=6;i++){float y=bottom-(bottom-top)*i/6f;p.setColor(Color.GRAY);c.drawText(String.valueOf(i*50),8,y+6,p);p.setStrokeWidth(1);c.drawLine(left,y,right,y,p);}if(logs.isEmpty()){p.setTextSize(15);c.drawText("Nessun aggiornamento ancora",left+8,(top+bottom)/2,p);return;}long min=logs.get(0).date,max=logs.get(logs.size()-1).date;if(min==max)max=min+1;Path path=new Path();p.setColor(color);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);for(int i=0;i<logs.size();i++){Log l=logs.get(i);float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(Log l:logs){float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;c.drawCircle(x,y,6,p);}}
    }
}
