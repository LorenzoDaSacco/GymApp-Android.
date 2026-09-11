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
    final String[] DAYS = {"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        store=new WorkoutStore(this);
        dark=store.dark; accent=store.accent;
        build();
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);
    }

    void build(){
        dark=store.dark; accent=store.accent;
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(20,12,20,18);
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav(); setContentView(root);
        showDashboard();
    }

    int bg(){return dark?Color.rgb(0,0,0):Color.rgb(246,246,248);}
    int card(){return dark?Color.rgb(28,28,30):Color.WHITE;}
    int fg(){return dark?Color.rgb(248,248,250):Color.rgb(24,24,27);}
    int secondary(){return dark?Color.rgb(152,152,157):Color.rgb(98,98,105);}
    int divider(){return dark?Color.rgb(54,54,57):Color.rgb(225,225,230);}
    GradientDrawable premiumCard(){GradientDrawable g=box(card(),30);g.setStroke(1,dark?Color.rgb(50,50,53):Color.rgb(232,232,237));return g;}
    GradientDrawable accentFill(){GradientDrawable g=box(accentSoft(),26);g.setStroke(1,Color.argb(90,Color.red(accent),Color.green(accent),Color.blue(accent)));return g;}
    int accent(){return accent;}
    GradientDrawable box(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    GradientDrawable iosCard(){GradientDrawable g=box(card(),28);if(dark)g.setStroke(1,Color.rgb(48,48,50));return g;}
    TextView tv(String s,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(fg());t.setTextSize(size);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLetterSpacing(bold?0.005f:0f);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(fg());b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(16,0,16,0);b.setStateListAnimator(null);b.setBackground(premiumButton());return b;}
    GradientDrawable premiumButton(){GradientDrawable g=box(dark?Color.rgb(38,38,41):Color.rgb(239,239,244),22);g.setStroke(1,dark?Color.rgb(54,54,57):Color.rgb(225,225,230));return g;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(secondary());e.setTextColor(fg());e.setSingleLine(true);e.setTextSize(17);e.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));e.setIncludeFontPadding(false);e.setPadding(16,0,16,0);e.setBackground(box(dark?Color.rgb(20,20,22):Color.rgb(239,239,244),16));return e;}
    void addGap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,h));}
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}

    void nav(){
        bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER); bottom.setPadding(7,7,7,7); bottom.setElevation(18); bottom.setBackground(box(dark?Color.rgb(20,20,22):Color.WHITE,38));
        String[] icons={"▣","⌁","+","⚙"}; String[] labels={"Scheda","Progressi","Aggiungi","Impostazioni"};
        for(int i=0;i<4;i++){
            final int k=i; LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(4,4,4,4);
            TextView ic=tv(icons[i],22,true); ic.setGravity(Gravity.CENTER); TextView tx=tv(labels[i],11,true); tx.setGravity(Gravity.CENTER);
            item.addView(ic,new LinearLayout.LayoutParams(-1,28)); item.addView(tx,new LinearLayout.LayoutParams(-1,21));
            if(i==0){item.setBackground(accentFill());ic.setTextColor(accent);tx.setTextColor(accent);}
            item.setOnClickListener(v->{hideKeyboard(); if(k==0)showDashboard(); else if(k==1)showProgress(); else if(k==2)showAdd(); else showSettings();});
            bottom.addView(item,new LinearLayout.LayoutParams(0,58,1));
        }
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(-1,72); navLp.setMargins(14,6,14,12); root.addView(bottom,navLp);
    }
    int accentSoft(){return Color.argb(45,Color.red(accent),Color.green(accent),Color.blue(accent));}
    void clear(String title){
        hideKeyboard(); content.removeAllViews();
        TextView h=tv(title,27,true); h.setPadding(4,10,4,10); content.addView(h);
    }

    void showDashboard(){
        clear("");
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(4,2,4,0);
        TextView title=tv("GYM TRACKER PRO",29,true); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); top.addView(title,new LinearLayout.LayoutParams(0,48,1));
        Button imp=btn("●"); imp.setTextSize(12); imp.setTextColor(accent); imp.setBackground(box(accentSoft(),32)); imp.setOnClickListener(v->showImport()); top.addView(imp,new LinearLayout.LayoutParams(52,48)); content.addView(top);
        TextView sub=tv("Allenamento del giorno",16,true); sub.setTextColor(secondary()); sub.setPadding(4,0,4,4); content.addView(sub);

        LinearLayout dayBox=new LinearLayout(this); dayBox.setGravity(Gravity.CENTER_VERTICAL); dayBox.setPadding(20,0,18,0); dayBox.setBackground(premiumButton());
        TextView dayLabel=tv("Giorno",16,false); dayLabel.setTextColor(secondary()); dayBox.addView(dayLabel,new LinearLayout.LayoutParams(0,54,1));
        Spinner day=new Spinner(this); ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,DAYS){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(accent);v.setTextSize(16);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);return v;}};
        day.setAdapter(ad); int pos=Arrays.asList(DAYS).indexOf(store.selectedDay); day.setSelection(Math.max(0,pos));
        day.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){String d=DAYS[position];if(!d.equals(store.selectedDay)){store.selectedDay=d;store.save();showDashboard();}}});
        dayBox.addView(day,new LinearLayout.LayoutParams(145,54)); content.addView(dayBox);

        List<Exercise> es=store.forDay(store.selectedDay); LinearLayout metrics=new LinearLayout(this); metrics.setPadding(0,12,0,10);
        addMetric(metrics,"Esercizi",String.valueOf(es.size()),"♙"); addMetric(metrics,"Serie",String.valueOf(store.totalSets(es)),"▱"); addMetric(metrics,"Completate",String.valueOf(store.completedSets(es)),"✓"); content.addView(metrics,new LinearLayout.LayoutParams(-1,96));
        int total=store.totalSets(es),done=store.completedSets(es); ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,total));pb.setProgress(done);pb.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));content.addView(pb,new LinearLayout.LayoutParams(-1,7));
        LinearLayout actions=new LinearLayout(this); actions.setPadding(0,10,0,4); Button reset=btn("Reset giornata"); reset.setTextColor(accent); reset.setBackground(box(accentSoft(),24)); reset.setOnClickListener(v->{store.resetDay();showDashboard();}); actions.addView(reset,new LinearLayout.LayoutParams(0,50,1)); content.addView(actions);
        if(es.isEmpty()){TextView empty=tv("Giorno libero\nAggiungi gli esercizi che vuoi per questo giorno.",16,false);empty.setGravity(Gravity.CENTER);empty.setPadding(10,50,10,50);content.addView(empty);} else for(Exercise e:es)addExerciseCard(e);
    }

    void addMetric(LinearLayout parent,String title,String value,String icon){
        LinearLayout m=new LinearLayout(this);m.setOrientation(LinearLayout.VERTICAL);m.setPadding(14,10,10,10);m.setBackground(premiumCard());
        TextView i=tv(icon,17,true);i.setTextColor(accent);m.addView(i);m.addView(tv(value,25,true));TextView t=tv(title,13,false);t.setTextColor(secondary());m.addView(t);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(3,3,3,3);parent.addView(m,lp);
    }

    void addExerciseCard(Exercise e){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(20,20,20,20); c.setBackground(premiumCard());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,9,0,9); content.addView(c,cp);
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.TOP|Gravity.CENTER_VERTICAL);
        LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(0,0,6,0);
        TextView n=tv(e.name,21,true); titles.addView(n); TextView g=tv(e.group,13,true);g.setTextColor(accent);titles.addView(g);TextView f=tv(e.focus,13,false);f.setTextColor(secondary());titles.addView(f);
        head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        Button mod=btn("Modifica"); mod.setTextColor(accent); mod.setBackground(accentFill()); mod.setOnClickListener(v->showExercise(e,true)); head.addView(mod,new LinearLayout.LayoutParams(100,46)); c.addView(head);
        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);TextView rr=tv("Recupero",13,true);info.addView(rr);EditText rec=edit("2:00");rec.setText(e.recovery);rec.setEnabled(false);rec.setTextSize(15);rec.setGravity(Gravity.CENTER);rec.setBackground(box(Color.BLACK,10));info.addView(rec,new LinearLayout.LayoutParams(86,46));Space sp=new Space(this);info.addView(sp,new LinearLayout.LayoutParams(0,1,1));TextView tg=tv(e.target.toUpperCase(Locale.ITALIAN),11,true);tg.setTextColor(accent);tg.setGravity(Gravity.CENTER);tg.setPadding(10,0,10,0);tg.setBackground(accentFill());info.addView(tg,new LinearLayout.LayoutParams(-2,40));c.addView(info);
        for(int i=0;i<e.sets.size();i++) addCompactSetRow(c,e,i);
        LinearLayout actions=new LinearLayout(this); Button plus=btn("+ Serie"),minus=btn("− Serie");plus.setTextColor(accent);minus.setTextColor(accent);plus.setBackground(accentFill());minus.setBackground(premiumButton());plus.setOnClickListener(v->{store.addSet(e);showDashboard();});minus.setOnClickListener(v->{store.removeSet(e);showDashboard();});actions.addView(plus,new LinearLayout.LayoutParams(0,52,1));actions.addView(minus,new LinearLayout.LayoutParams(0,52,1));TextView cnt=tv(e.sets.size()+" serie",12,false);cnt.setGravity(Gravity.CENTER);cnt.setTextColor(secondary());actions.addView(cnt,new LinearLayout.LayoutParams(78,50));c.addView(actions);
        MuscleMapView map=new MuscleMapView(this,e.target);c.addView(map,new LinearLayout.LayoutParams(-1,205));
    }
    void addCompactSetRow(LinearLayout parent,Exercise e,int idx){
        Set s=e.sets.get(idx); LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,6,0,6);
        TextView sn=tv("S"+(idx+1),13,true);row.addView(sn,new LinearLayout.LayoutParams(32,48));
        EditText reps=edit(s.reps);reps.setText(s.reps);reps.setTextSize(15);reps.setGravity(Gravity.CENTER);reps.setEnabled(false);row.addView(reps,new LinearLayout.LayoutParams(78,48));
        if(s.isBackOff){TextView w=tv(fmt(store.backOffWeight(e)),15,true);w.setGravity(Gravity.CENTER);w.setTextColor(accent);w.setBackground(box(accentSoft(),12));row.addView(w,new LinearLayout.LayoutParams(82,48));TextView bo=tv("kg  Back-off −20%",10,true);bo.setTextColor(accent);row.addView(bo,new LinearLayout.LayoutParams(0,46,1));}
        else {EditText w=edit("kg");w.setText(fmt(s.weight));w.setTextSize(15);w.setGravity(Gravity.CENTER);w.setEnabled(false);row.addView(w,new LinearLayout.LayoutParams(82,48));TextView kg=tv("kg",12,true);kg.setTextColor(accent);kg.setGravity(Gravity.CENTER_VERTICAL);row.addView(kg,new LinearLayout.LayoutParams(32,46));}
        CheckBox done=new CheckBox(this);done.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,Color.LTGRAY}));done.setChecked(s.done);done.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){s.done=c;store.save();if(c)startRecovery(e,s);else RecoveryNotifications.cancel(this,s.id);}});row.addView(done,new LinearLayout.LayoutParams(48,48));parent.addView(row);
    }

    void showExercise(Exercise e, boolean editing){
        clear(e.name);
        TextView focus=tv(e.group+"  •  "+e.focus,14,false);focus.setTextColor(secondary());content.addView(focus);
        LinearLayout top=new LinearLayout(this);Button edit=btn(editing?"Fine":"Modifica");edit.setOnClickListener(v->showExercise(e,!editing));top.addView(edit,new LinearLayout.LayoutParams(0,50,1));
        Button back=btn("‹ Torna alla scheda");back.setOnClickListener(v->showDashboard());top.addView(back,new LinearLayout.LayoutParams(0,50,1));content.addView(top);
        LinearLayout recRow=new LinearLayout(this);recRow.setGravity(Gravity.CENTER_VERTICAL);TextView rl=tv("Recupero",13,true);recRow.addView(rl,new LinearLayout.LayoutParams(0,50,1));
        EditText rec=edit("2:00");rec.setText(e.recovery);rec.setEnabled(editing);rec.setInputType(1);rec.setSelectAllOnFocus(true);rec.setOnFocusChangeListener((v,has)->{if(!has && editing){e.recovery=rec.getText().toString();store.save();}});recRow.addView(rec,new LinearLayout.LayoutParams(100,48));content.addView(recRow);

        if(editing){
            Switch bo=new Switch(this);bo.setText("Back-off ultima serie\n80% della serie precedente");bo.setTextColor(fg());bo.setTextSize(13);bo.setChecked(e.backOffEnabled);bo.setOnCheckedChangeListener((b,checked)->{if(b.isPressed()){store.setBackOffEnabled(e,checked);showExercise(e,true);}});content.addView(bo);
        }
        if(editing){ Button del=btn("Elimina esercizio"); del.setTextColor(Color.rgb(255,90,100)); del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Elimina esercizio?").setMessage("L'esercizio verrà rimosso dalla scheda.").setNegativeButton("Annulla",null).setPositiveButton("Elimina",(d,w)->{store.delete(e);showDashboard();}).show()); content.addView(del,new LinearLayout.LayoutParams(-1,52)); }

        for(int i=0;i<e.sets.size();i++)addSetRow(e,i,editing);
        LinearLayout actions=new LinearLayout(this);Button plus=btn("+ Serie"),minus=btn("− Serie");plus.setOnClickListener(v->{store.addSet(e);showExercise(e,editing);});minus.setOnClickListener(v->{store.removeSet(e);showExercise(e,editing);});actions.addView(plus,new LinearLayout.LayoutParams(0,52,1));actions.addView(minus,new LinearLayout.LayoutParams(0,52,1));content.addView(actions);

        Button chart=btn("📈  Andamento del peso");chart.setOnClickListener(v->showChart(e));content.addView(chart,new LinearLayout.LayoutParams(-1,58));
        Button reset=btn("↻  Azzera serie completate");reset.setOnClickListener(v->{for(Set s:e.sets)s.done=false;store.save();showExercise(e,editing);});content.addView(reset,new LinearLayout.LayoutParams(-1,58));
        MuscleMapView map=new MuscleMapView(this,e.target);content.addView(map,new LinearLayout.LayoutParams(-1,205));
    }

    void addSetRow(Exercise e,int idx,boolean editing){
        Set s=e.sets.get(idx);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(2,5,2,5);
        TextView sn=tv("S"+(idx+1),12,true);sn.setTextColor(accent);row.addView(sn,new LinearLayout.LayoutParams(32,52));
        EditText reps=edit(s.reps);reps.setText(s.reps);reps.setEnabled(editing);reps.setInputType(1);reps.setOnFocusChangeListener((v,f)->{if(!f&&editing){s.reps=reps.getText().toString();store.save();}});row.addView(reps,new LinearLayout.LayoutParams(78,50));
        TextView sep=tv("  ",10,false);row.addView(sep,new LinearLayout.LayoutParams(5,48));
        if(s.isBackOff){
            TextView w=tv(fmt(store.backOffWeight(e)),15,true);w.setGravity(Gravity.CENTER);w.setTextColor(accent);w.setBackground(box(dark?Color.rgb(44,30,50):Color.rgb(245,235,250),14));
            row.addView(w,new LinearLayout.LayoutParams(82,50));TextView kg=tv(" kg  Back-off −20%",10,true);kg.setTextColor(accent);row.addView(kg,new LinearLayout.LayoutParams(0,48,1));
        }else{
            EditText w=edit("kg");w.setText(fmt(s.weight));w.setEnabled(editing);w.setInputType(2|8192);w.setSelectAllOnFocus(true);
            w.setOnFocusChangeListener((v,f)->{if(!f&&editing){commitWeight(e,idx,w);}});w.setOnEditorActionListener((v,id,event)->{commitWeight(e,idx,w);hideKeyboard();return true;});
            row.addView(w,new LinearLayout.LayoutParams(82,50));TextView kg=tv(" kg",11,true);kg.setTextColor(accent);row.addView(kg,new LinearLayout.LayoutParams(30,48));
        }
        CheckBox done=new CheckBox(this);done.setChecked(s.done);done.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){s.done=c;store.save();if(c)startRecovery(e,s);else RecoveryNotifications.cancel(this,s.id);}});row.addView(done,new LinearLayout.LayoutParams(48,52));
        content.addView(row);
    }
    void commitWeight(Exercise e,int idx,EditText w){
        try{String raw=w.getText().toString().replace(",",".");if(raw.trim().isEmpty()){store.setWeight(e,idx,0);return;}double x=Double.parseDouble(raw);store.setWeight(e,idx,x);w.setText(fmt(x));}catch(Exception ignored){}
    }

    void startRecovery(Exercise e, Set s){
        try{String r=e.recovery==null?"":e.recovery.trim();if(r.isEmpty())return;int sec=parseRecovery(r);if(sec<=0)return;
            Intent in=new Intent(this,RecoveryReceiver.class);in.putExtra("name",e.name);in.putExtra("setId",s.id);
            PendingIntent pi=PendingIntent.getBroadcast(this,(int)(s.id&0x7fffffff),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+sec*1000L,pi);
        }catch(Exception ignored){}
    }
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

    String workoutName(String day){
        if(day.equals("LUNEDÌ")) return "UPPER";
        if(day.equals("MARTEDÌ")) return "LOWER";
        if(day.equals("MERCOLEDÌ")) return "FULLBODY";
        return "ALLENAMENTO";
    }

    void showProgress(){
        clear(""); TextView h=tv("Progressi",38,true);h.setPadding(4,8,4,0);content.addView(h);TextView d=tv("I tuoi allenamenti sono organizzati per giornata.\nApri una scheda per vedere gli esercizi e poi entra nel singolo esercizio per il grafico dei kg.",15,false);d.setTextColor(secondary());d.setPadding(4,8,4,14);content.addView(d);
        boolean any=false;for(String day:DAYS){List<Exercise> es=store.forDay(day);if(es.isEmpty())continue;any=true;LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(16,15,16,15);c.setBackground(premiumCard());TextView dn=tv(day,22,true);c.addView(dn);TextView wn=tv(workoutName(day),13,true);wn.setTextColor(accent);c.addView(wn);LinearLayout pills=new LinearLayout(this);pills.setPadding(0,10,0,10);addMetric(pills,"esercizi",String.valueOf(es.size())," ");addMetric(pills,"serie",String.valueOf(store.totalSets(es))," ");c.addView(pills,new LinearLayout.LayoutParams(-1,78));StringBuilder mus=new StringBuilder();for(Exercise e:es){if(mus.length()>0)mus.append(" • ");mus.append(e.group);}TextView mt=tv(mus.toString(),13,false);mt.setTextColor(secondary());c.addView(mt);c.setOnClickListener(v->showDayProgress(day));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,7,0,7);content.addView(c,lp);}if(!any)content.addView(tv("Nessun allenamento",16,true));
    }
    void showDayProgress(String day){
        clear("");TextView h=tv(day,32,true);h.setPadding(4,8,4,0);content.addView(h);TextView w=tv(workoutName(day),14,true);w.setTextColor(accent);content.addView(w);Button back=btn("‹ Torna ai progressi");back.setOnClickListener(v->showProgress());content.addView(back,new LinearLayout.LayoutParams(-1,48));
        for(Exercise e:store.forDay(day)){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(16,12,16,12);row.setBackground(box(card(),20));TextView n=tv(e.name,17,true);row.addView(n,new LinearLayout.LayoutParams(0,60,1));TextView m=tv(e.sets.size()+" serie\n"+fmt(store.maxWeight(e))+" kg",12,false);m.setTextColor(secondary());m.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(m,new LinearLayout.LayoutParams(95,60));row.setOnClickListener(v->showChart(e));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,78);lp.setMargins(0,5,0,5);content.addView(row,lp);}
    }
    void showAdd(){
        clear("");TextView h=tv("Aggiungi esercizio",34,true);h.setPadding(4,8,4,8);content.addView(h);
        LinearLayout dayBox=new LinearLayout(this);dayBox.setGravity(Gravity.CENTER_VERTICAL);dayBox.setPadding(16,0,16,0);dayBox.setBackground(box(card(),26));TextView dl=tv("Giorno",16,false);dl.setTextColor(secondary());dayBox.addView(dl,new LinearLayout.LayoutParams(0,54,1));Spinner day=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,DAYS){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(accent);v.setTextSize(16);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);return v;}};day.setAdapter(a);day.setSelection(Math.max(0,Arrays.asList(DAYS).indexOf(store.selectedDay)));dayBox.addView(day,new LinearLayout.LayoutParams(145,54));content.addView(dayBox);
        TextView sec=tv("Esercizio",17,true);sec.setPadding(4,22,4,7);content.addView(sec);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,12,16,14);box.setBackground(box(card(),24));EditText name=edit("Nome");box.addView(name,new LinearLayout.LayoutParams(-1,50));EditText reps=edit("8-10");reps.setText("8-10");box.addView(reps,new LinearLayout.LayoutParams(-1,50));LinearLayout step=new LinearLayout(this);Button mi=btn("−"),pl=btn("+");TextView num=tv("Serie normali: 3",16,true);final int[] cnt={3};step.addView(num,new LinearLayout.LayoutParams(0,52,1));step.addView(mi,new LinearLayout.LayoutParams(58,52));step.addView(pl,new LinearLayout.LayoutParams(58,52));box.addView(step);content.addView(box);
        TextView sh=tv("Kg per serie normale",17,true);sh.setPadding(4,20,4,7);content.addView(sh);LinearLayout weightsBox=new LinearLayout(this);weightsBox.setOrientation(LinearLayout.VERTICAL);content.addView(weightsBox);ArrayList<EditText> weights=new ArrayList<>();Runnable rebuild=()->{weightsBox.removeAllViews();weights.clear();for(int i=0;i<cnt[0];i++){EditText w=edit("Serie "+(i+1));w.setText("20");w.setInputType(2|8192);w.setSelectAllOnFocus(true);weights.add(w);weightsBox.addView(w,new LinearLayout.LayoutParams(-1,48));}};rebuild.run();mi.setOnClickListener(v->{if(cnt[0]>1){cnt[0]--;num.setText("Serie normali: "+cnt[0]);rebuild.run();}});pl.setOnClickListener(v->{if(cnt[0]<20){cnt[0]++;num.setText("Serie normali: "+cnt[0]);rebuild.run();}});
        Switch bo=new Switch(this);bo.setText("Aggiungi back-off −20%");bo.setTextColor(fg());bo.setTextSize(15);bo.setOnCheckedChangeListener((b,c)->{if(c)Toast.makeText(this,"Back-off = 80% dell'ultima serie",Toast.LENGTH_SHORT).show();});content.addView(bo);TextView bop=tv("Il peso del back-off viene calcolato automaticamente dall'ultima serie normale.",12,false);bop.setTextColor(secondary());content.addView(bop);
        EditText recovery=edit("2:00");recovery.setText("2:00");content.addView(recovery,new LinearLayout.LayoutParams(-1,50));
        Button add=btn("Aggiungi alla scheda");add.setTextColor(accent);add.setOnClickListener(v->{hideKeyboard();try{String nm=name.getText().toString().trim();ArrayList<Double> ws=new ArrayList<>();for(EditText w:weights)ws.add(Double.parseDouble(w.getText().toString().replace(",",".")));String[] q=store.recognize(nm);if("ALTRO".equals(q[0])){showMusclePicker(day.getSelectedItem().toString(),nm,reps.getText().toString(),ws,recovery.getText().toString(),bo.isChecked());}else{store.add(day.getSelectedItem().toString(),nm,reps.getText().toString(),ws,recovery.getText().toString(),bo.isChecked());showDashboard();}}catch(Exception ex){Toast.makeText(this,"Compila nome, serie e kg",Toast.LENGTH_SHORT).show();}});content.addView(add,new LinearLayout.LayoutParams(-1,56));
        Button imp=btn("📷  Importa scheda da foto / galleria");imp.setOnClickListener(v->showImport());content.addView(imp,new LinearLayout.LayoutParams(-1,56));
    }
    void showMusclePicker(String day,String name,String reps,ArrayList<Double> ws,String rec,boolean bo){
        final String[] labels={"Petto","Dorso","Spalle","Bicipiti","Tricipiti","Quadricipiti","Femorali / glutei","Polpacci","Adduttori","Core"};
        new AlertDialog.Builder(this).setTitle("Muscolo principale").setMessage("Questo esercizio non è nel registro. Seleziona il muscolo che allena principalmente.").setSingleChoiceItems(labels,-1,(d,which)->{String[] q=muscleData(labels[which]);store.addCustom(day,name,reps,ws,rec,bo,q[0],q[1],q[2]);d.dismiss();showDashboard();}).setNegativeButton("Annulla",null).show();
    }
    String[] muscleData(String l){if(l.equals("Petto"))return new String[]{"PETTO","Pettorali e tricipiti","chest"};if(l.equals("Dorso"))return new String[]{"DORSO","Dorsali e parte alta della schiena","back"};if(l.equals("Spalle"))return new String[]{"SPALLE","Deltoidi","shoulders"};if(l.equals("Bicipiti"))return new String[]{"BICIPITI","Bicipite","biceps"};if(l.equals("Tricipiti"))return new String[]{"TRICIPITI","Tricipite","triceps"};if(l.equals("Quadricipiti"))return new String[]{"QUADRICIPITI","Quadricipiti e glutei","quads"};if(l.equals("Femorali / glutei"))return new String[]{"FEMORALI / GLUTEI","Catena posteriore","hamstrings"};if(l.equals("Polpacci"))return new String[]{"POLPACCI","Polpacci","hamstrings"};if(l.equals("Adduttori"))return new String[]{"ADDUTTORI","Adduttori","quads"};return new String[]{"CORE","Addominali e core","full"};}

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
        clear("");TextView h=tv("Impostazioni",38,true);h.setPadding(4,8,4,8);content.addView(h);TextView a=tv("Aspetto",17,true);a.setTextColor(secondary());a.setPadding(4,8,4,7);content.addView(a);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(16,8,16,8);card.setBackground(premiumCard());Switch theme=new Switch(this);theme.setText("Tema scuro");theme.setTextColor(fg());theme.setChecked(dark);theme.setTextSize(16);card.addView(theme,new LinearLayout.LayoutParams(-1,56));TextView ct=tv("Colore principale",16,true);ct.setPadding(0,8,0,5);card.addView(ct);String[] names={"Viola","Blu","Azzurro","Ciano","Verde","Lime","Arancione","Giallo","Rosso","Rosa"};int[] colors={Color.rgb(216,137,255),Color.rgb(70,130,255),Color.rgb(45,170,255),Color.rgb(50,210,210),Color.rgb(50,190,110),Color.rgb(150,210,70),Color.rgb(245,150,55),Color.rgb(245,200,60),Color.rgb(235,70,80),Color.rgb(245,90,160)};RadioGroup rg=new RadioGroup(this);for(int i=0;i<names.length;i++){RadioButton r=new RadioButton(this);r.setText(names[i]);r.setTextColor(fg());r.setChecked(store.accentIndex==i);final int k=i;r.setOnClickListener(v->{store.accentIndex=k;store.accent=colors[k];store.save();build();});rg.addView(r);}card.addView(rg);TextView current=tv("Tema attuale       "+(dark?"Scuro":"Chiaro"),16,false);current.setTextColor(secondary());card.addView(current,new LinearLayout.LayoutParams(-1,50));content.addView(card);theme.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){store.dark=c;store.save();build();}});
        TextView d=tv("Dati",17,true);d.setTextColor(secondary());d.setPadding(4,24,4,7);content.addView(d);LinearLayout data=new LinearLayout(this);data.setOrientation(LinearLayout.VERTICAL);data.setPadding(16,10,16,10);data.setBackground(premiumCard());data.addView(tv("I dati vengono salvati localmente sul telefono.",15,false));Button notif=btn("Richiedi notifiche");notif.setTextColor(accent);notif.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77));data.addView(notif,new LinearLayout.LayoutParams(-1,52));content.addView(data);
        TextView sw=tv("Settimana",17,true);sw.setTextColor(secondary());sw.setPadding(4,24,4,7);content.addView(sw);LinearLayout week=new LinearLayout(this);week.setOrientation(LinearLayout.VERTICAL);week.setPadding(16,4,16,4);week.setBackground(premiumCard());for(String day:DAYS){TextView t=tv(day+"                              "+store.forDay(day).size()+" esercizi",16,false);t.setPadding(0,12,0,12);week.addView(t);}content.addView(week);
    }

    String fmt(double x){return x==Math.rint(x)?String.valueOf((int)x):String.format(Locale.US,"%.1f",x);}

    public static class Imported{String day,name,reps,recovery;int sets;Imported(String d,String n,String r,int s,String rec){day=d;name=n;reps=r;sets=s;recovery=rec;}}
    public static class Exercise{long id;String day,name,group,focus,target,reps,recovery="";boolean backOffEnabled;ArrayList<Set>sets=new ArrayList<>();}
    public static class Set{long id;String reps;double weight;boolean done,isBackOff;ArrayList<Log>history=new ArrayList<>();}
    public static class Log{long date;double weight;Log(long d,double w){date=d;weight=w;}}

    static class WorkoutStore{
        final Context c;ArrayList<Exercise> all=new ArrayList<>();String selectedDay="LUNEDÌ";boolean dark=true;int accent=Color.rgb(216,137,255),accentIndex=0;final String key="gymapp.android.v2";
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
        void delete(Exercise e){all.remove(e);save();}
        void addCustom(String d,String n,String r,ArrayList<Double> ws,String rec,boolean bo,String g,String f,String t){
            if(n==null||n.isEmpty())return;Exercise e=new Exercise();e.id=System.nanoTime();e.day=d;e.name=n;e.reps=r;e.recovery=rec;e.group=g;e.focus=f;e.target=t;for(double w:ws){Set s=new Set();s.id=System.nanoTime()+e.sets.size();s.reps=r;s.weight=w;e.sets.add(s);}e.backOffEnabled=bo;if(bo){Set src=e.sets.get(e.sets.size()-1);Set s=new Set();s.id=System.nanoTime();s.reps=src.reps;s.weight=roundBackOff(src.weight);s.isBackOff=true;e.sets.add(s);}all.add(e);selectedDay=d;save();
        }
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
        void save(){try{SharedPreferences p=c.getSharedPreferences("gym",0);JSONArray a=new JSONArray();for(Exercise e:all){JSONObject o=new JSONObject();o.put("id",e.id);o.put("day",e.day);o.put("name",e.name);o.put("group",e.group);o.put("focus",e.focus);o.put("target",e.target);o.put("reps",e.reps);o.put("recovery",e.recovery);o.put("backOffEnabled",e.backOffEnabled);JSONArray ss=new JSONArray();for(Set s:e.sets){JSONObject z=new JSONObject();z.put("id",s.id);z.put("reps",s.reps);z.put("weight",s.weight);z.put("done",s.done);z.put("isBackOff",s.isBackOff);JSONArray hh=new JSONArray();for(Log l:s.history){JSONObject k=new JSONObject();k.put("date",l.date);k.put("weight",l.weight);hh.put(k);}z.put("history",hh);ss.put(z);}o.put("sets",ss);a.put(o);}p.edit().putString(key,a.toString()).putString("selectedDay",selectedDay).putBoolean("dark",dark).putInt("accent",accent).putInt("accentIndex",accentIndex).apply();}catch(Exception ignored){}}
        void load(){try{SharedPreferences p=c.getSharedPreferences("gym",0);dark=p.getBoolean("dark",true);accent=p.getInt("accent",Color.rgb(216,137,255));accentIndex=p.getInt("accentIndex",0);selectedDay=p.getString("selectedDay","LUNEDÌ");String raw=p.getString(key,"");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Exercise e=new Exercise();e.id=o.getLong("id");e.day=o.getString("day");e.name=o.getString("name");e.group=o.getString("group");e.focus=o.getString("focus");e.target=o.getString("target");e.reps=o.getString("reps");e.recovery=o.optString("recovery","");e.backOffEnabled=o.optBoolean("backOffEnabled",false);JSONArray ss=o.getJSONArray("sets");for(int j=0;j<ss.length();j++){JSONObject z=ss.getJSONObject(j);Set s=new Set();s.id=z.getLong("id");s.reps=z.getString("reps");s.weight=z.getDouble("weight");s.done=z.optBoolean("done",false);s.isBackOff=z.optBoolean("isBackOff",false);JSONArray hh=z.optJSONArray("history");if(hh!=null)for(int k=0;k<hh.length();k++){JSONObject q=hh.getJSONObject(k);s.history.add(new Log(q.getLong("date"),q.getDouble("weight")));}e.sets.add(s);}if(e.backOffEnabled&&(!e.sets.isEmpty()&&!e.sets.get(e.sets.size()-1).isBackOff)){e.backOffEnabled=false;}all.add(e);}}catch(Exception ignored){}}
    }

    static class ChartView extends View{
        Paint p=new Paint(1);ArrayList<Log> logs;int color;ChartView(Context c,ArrayList<Log> l,int color){super(c);logs=l;this.color=color;}
        protected void onDraw(Canvas c){int w=getWidth(),h=getHeight(),left=55,right=w-12,top=25,bottom=h-48;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.GRAY);c.drawLine(left,top,left,bottom,p);c.drawLine(left,bottom,right,bottom,p);p.setTextSize(18);p.setStyle(Paint.Style.FILL);for(int i=0;i<=6;i++){float y=bottom-(bottom-top)*i/6f;p.setColor(Color.GRAY);c.drawText(String.valueOf(i*50),8,y+6,p);p.setStrokeWidth(1);c.drawLine(left,y,right,y,p);}if(logs.isEmpty()){p.setTextSize(15);c.drawText("Nessun aggiornamento ancora",left+8,(top+bottom)/2,p);return;}long min=logs.get(0).date,max=logs.get(logs.size()-1).date;if(min==max)max=min+1;Path path=new Path();p.setColor(color);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);for(int i=0;i<logs.size();i++){Log l=logs.get(i);float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(Log l:logs){float x=left+(right-left)*(l.date-min)/(float)(max-min);float y=bottom-(bottom-top)*(float)Math.min(300,l.weight)/300f;c.drawCircle(x,y,6,p);}}
    }
}
