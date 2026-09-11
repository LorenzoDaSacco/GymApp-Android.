package com.gymtrackerpro.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;
import android.content.res.ColorStateList;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;

import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_IMAGE=1101, REQ_CAMERA=1102, REQ_PDF=1200;
    LinearLayout root, content, bottom; ArrayList<LinearLayout> navItems=new ArrayList<>();
    WorkoutStore store;
    boolean dark;
    int accent;
    final String[] DAYS={"LUNEDÌ","MARTEDÌ","MERCOLEDÌ","GIOVEDÌ","VENERDÌ","SABATO","DOMENICA"};

    // iPhone-style palette
    final int RED=Color.rgb(255,105,115);
    final int BG=Color.BLACK;
    final int CARD=Color.rgb(31,31,33);
    final int FIELD=Color.rgb(22,22,24);
    final int SOFT=Color.rgb(46,46,49);
    final int MUTED=Color.rgb(158,158,165);

    // Timer recupero visuale + notifica in background
    final Handler timerHandler = new Handler(Looper.getMainLooper());
    long timerExerciseId=-1, timerSetId=-1, timerEndMs=0, timerDurationMs=0;
    ProgressBar recoveryProgress;
    TextView recoveryCountdown;
    LinearLayout recoveryTimerCard;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        store=new WorkoutStore(this);
        if(store.accentIndex==0 && store.accent==Color.rgb(216,137,255)){ store.accent=RED; store.accentIndex=4; store.save(); }
        dark=store.dark; accent=store.accent;
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        build();
        RecoveryNotifications.ensureChannel(this);
        if(store.notificationsEnabled && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);
    }

    int bg(){return dark?BG:Color.rgb(248,248,250);}
    int card(){return dark?CARD:Color.WHITE;}
    int field(){return dark?FIELD:Color.rgb(244,244,246);}
    int fg(){return dark?Color.WHITE:Color.rgb(22,22,25);}
    int secondary(){return dark?MUTED:Color.rgb(105,105,112);}
    int accentColor(){return accent;}
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable shape(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable outlined(int color,float radius,int strokeColor,int stroke){GradientDrawable g=shape(color,radius);g.setStroke(dp(stroke),strokeColor);return g;}
    TextView text(String s,float size,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextColor(fg()); t.setTextSize(size);
        t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); t.setIncludeFontPadding(true); return t;
    }
    TextView label(String s,float size,int color,boolean bold){
        TextView t=text(s,size,bold); t.setTextColor(color); return t;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextColor(fg()); b.setTextSize(15); b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans",Typeface.BOLD)); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),0,dp(10),0); b.setMinHeight(0); b.setMinWidth(0);
        b.setBackground(shape(dark?SOFT:Color.rgb(235,235,238),22)); return b;
    }
    Button accentButton(String s){
        Button b=button(s); b.setTextColor(accent); b.setBackground(shape(dark?Color.rgb(55,37,60):Color.rgb(255,238,241),24)); return b;
    }
    EditText edit(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(secondary()); e.setTextColor(fg());
        e.setTextSize(16); e.setSingleLine(true); e.setPadding(dp(14),0,dp(14),0); e.setBackground(shape(field(),18));
        e.setSelectAllOnFocus(true); return e;
    }
    void margin(View v,int l,int t,int r,int b){ if(v.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);} }
    void gap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    void openNotificationSettings(){
        try{
            Intent in=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            in.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
            startActivity(in);
        }catch(Exception ignored){
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
    void build(){
        dark=store.dark; accent=store.accent;
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(22),dp(10),dp(22),dp(16));
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setClipToPadding(false); sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); nav(); setContentView(root); showDashboard();
    }

    void nav(){
        navItems.clear();
        bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(8),dp(8),dp(8),dp(8)); bottom.setBackground(shape(dark?Color.rgb(18,18,19):Color.WHITE,30));
        String[] icons={"▣","⌁","+","⚙"}; String[] names={"Scheda","Progressi","Aggiungi","Impostazioni"};
        for(int i=0;i<4;i++){
            final int k=i;
            LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
            TextView ic=label(icons[i],25,fg(),true); ic.setGravity(Gravity.CENTER);
            TextView tx=label(names[i],11,fg(),true); tx.setGravity(Gravity.CENTER);
            item.addView(ic,new LinearLayout.LayoutParams(-1,dp(28))); item.addView(tx,new LinearLayout.LayoutParams(-1,dp(22)));
            item.setPadding(dp(8),dp(6),dp(8),dp(4));
            item.setBackground(shape(Color.TRANSPARENT,24));
            item.setOnClickListener(v->{hideKeyboard();if(k==0)showDashboard();else if(k==1)showProgress();else if(k==2)showAdd();else showSettings();});
            navItems.add(item);
            bottom.addView(item,new LinearLayout.LayoutParams(0,dp(68),1));
        }
        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(78)));
    }
    void selectNav(int selected){
        for(int i=0;i<navItems.size();i++){
            LinearLayout item=navItems.get(i);
            boolean on=i==selected;
            item.setBackground(shape(on?(dark?Color.rgb(49,49,52):Color.rgb(232,232,235)):Color.TRANSPARENT,24));
            TextView ic=(TextView)item.getChildAt(0), tx=(TextView)item.getChildAt(1);
            ic.setTextColor(on?accent:fg()); tx.setTextColor(on?accent:fg());
        }
    }

    void clearPage(String title,boolean large){
        hideKeyboard(); content.removeAllViews();
        TextView h=text(title,large?40:28,true); h.setLetterSpacing(large?-.02f:0f);
        h.setGravity(large?Gravity.LEFT:Gravity.CENTER_HORIZONTAL); h.setPadding(0,dp(8),0,dp(4));
        content.addView(h,new LinearLayout.LayoutParams(-1,dp(large?58:48)));
    }
    void addPageHeader(String title,String subtitle){
        TextView h=text(title,34,true);h.setPadding(0,dp(8),0,0);content.addView(h,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView s=label(subtitle,16,secondary(),true);content.addView(s,new LinearLayout.LayoutParams(-1,dp(28)));
    }
    View pill(String left,String right){
        LinearLayout p=new LinearLayout(this);p.setGravity(Gravity.CENTER_VERTICAL);p.setPadding(dp(16),0,dp(14),0);p.setBackground(shape(dark?Color.rgb(39,39,42):Color.rgb(239,239,242),30));
        TextView a=label(left,17,secondary(),false);p.addView(a,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView b=label(right,17,accent,true);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);p.addView(b,new LinearLayout.LayoutParams(dp(88),dp(54)));
        TextView c=label("⌄",20,secondary(),true);c.setGravity(Gravity.CENTER);p.addView(c,new LinearLayout.LayoutParams(dp(26),dp(54)));
        return p;
    }
    void daySelector(){
        View p=pill("Giorno",store.selectedDay);
        p.setOnClickListener(v->chooseDay());
        content.addView(p,new LinearLayout.LayoutParams(-1,dp(54)));
    }
    void chooseDay(){
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Giorno").setSingleChoiceItems(DAYS,Math.max(0,Arrays.asList(DAYS).indexOf(store.selectedDay)),(x,which)->{
            store.selectedDay=DAYS[which];store.save();x.dismiss();showDashboard();
        }).setNegativeButton("Annulla",null).create();d.show();
    }

    void showDashboard(){
        selectNav(0);
        hideKeyboard(); content.removeAllViews();
        TextView dh=text("GYM TRACKER PRO",28,true); dh.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL); dh.setPadding(0,dp(8),0,0); content.addView(dh,new LinearLayout.LayoutParams(-1,dp(44)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView sub=label("Allenamento del giorno",17,secondary(),true);head.addView(sub,new LinearLayout.LayoutParams(0,dp(38),1));
        TextView dot=label("•",28,accent,true);dot.setGravity(Gravity.CENTER);dot.setBackground(shape(dark?Color.rgb(48,25,54):Color.rgb(255,237,240),25));
        dot.setOnClickListener(v->showSettings());head.addView(dot,new LinearLayout.LayoutParams(dp(48),dp(48)));content.addView(head);
        daySelector(); gap(10);

        List<Exercise> es=store.forDay(store.selectedDay);
        LinearLayout metrics=new LinearLayout(this);metrics.setGravity(Gravity.CENTER);
        addMetric(metrics,"Esercizi",String.valueOf(es.size()),"♙");
        addMetric(metrics,"Serie",String.valueOf(store.totalSets(es)),"▱");
        addMetric(metrics,"Completate",String.valueOf(store.completedSets(es)),"✓");
        content.addView(metrics,new LinearLayout.LayoutParams(-1,dp(92)));
        gap(10);
        int total=store.totalSets(es),done=store.completedSets(es);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,total));pb.setProgress(done);
        pb.setProgressTintList(ColorStateList.valueOf(accent));content.addView(pb,new LinearLayout.LayoutParams(-1,dp(5)));gap(10);
        Button reset=accentButton("Reset giornata");reset.setOnClickListener(v->{store.resetDay();showDashboard();});content.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));gap(4);
        if(es.isEmpty()){
            TextView empty=label("Giorno libero\nAggiungi gli esercizi che vuoi per questo giorno.",16,secondary(),false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(60),0,dp(60));content.addView(empty);
        }else for(Exercise e:es)addExerciseCard(e);
    }
    void addMetric(LinearLayout parent,String title,String value,String icon){
        LinearLayout m=new LinearLayout(this);m.setOrientation(LinearLayout.VERTICAL);m.setPadding(dp(14),dp(9),dp(10),dp(7));m.setGravity(Gravity.LEFT);
        m.setBackground(shape(card(),24));TextView i=label(icon,20,accent,true);m.addView(i,new LinearLayout.LayoutParams(-1,dp(24)));
        m.addView(text(value,28,true),new LinearLayout.LayoutParams(-1,dp(36)));m.addView(label(title,12,secondary(),false),new LinearLayout.LayoutParams(-1,dp(20)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(90),1);lp.setMargins(dp(3),0,dp(3),0);parent.addView(m,lp);
    }

    void addExerciseCard(Exercise e){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));
        c.setBackground(outlined(card(),28,dark?Color.rgb(62,37,43):Color.rgb(235,210,214),1));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(8),0,dp(8));content.addView(c,cp);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.TOP);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text(e.name,22,true));titles.addView(label(e.group,15,accent,true));titles.addView(label(e.focus,14,secondary(),false));
        head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView mod=label("Modifica",17,accent,true);mod.setGravity(Gravity.RIGHT|Gravity.TOP);mod.setPadding(dp(6),dp(3),0,0);mod.setOnClickListener(v->showExercise(e,true));
        head.addView(mod,new LinearLayout.LayoutParams(dp(100),dp(44)));c.addView(head);
        gapIn(c,6);
        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);
        info.addView(label("Recupero",14,fg(),true),new LinearLayout.LayoutParams(dp(80),dp(38)));
        TextView rec=label(e.recovery.isEmpty()?"—":e.recovery,16,Color.WHITE,true);rec.setGravity(Gravity.CENTER);rec.setBackground(shape(Color.BLACK,10));info.addView(rec,new LinearLayout.LayoutParams(dp(70),dp(38)));
        TextView tag=label(e.target.toUpperCase(Locale.ITALIAN),12,Color.WHITE,true);tag.setGravity(Gravity.CENTER);tag.setBackground(shape(dark?Color.rgb(63,39,43):Color.rgb(245,225,228),18));tag.setTextColor(dark?Color.WHITE:Color.rgb(100,50,55));
        LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(dp(108),dp(34));tl.setMargins(dp(8),0,0,0);info.addView(tag,tl);c.addView(info);
        for(int i=0;i<e.sets.size();i++) addSetRowTo(c,e,i,false);
        LinearLayout acts=new LinearLayout(this);acts.setPadding(0,dp(4),0,dp(2));
        Button plus=accentButton("+ Serie"),minus=accentButton("− Serie");
        plus.setOnClickListener(v->{store.addSet(e);showDashboard();});minus.setOnClickListener(v->{store.removeSet(e);showDashboard();});
        acts.addView(plus,new LinearLayout.LayoutParams(0,dp(46),1));acts.addView(minus,new LinearLayout.LayoutParams(0,dp(46),1));
        c.addView(acts);
        MuscleMapView map=new MuscleMapView(this,e.target);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(245));mp.setMargins(0,dp(6),0,0);c.addView(map,mp);
    }
    void gapIn(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    void addSetRowTo(LinearLayout parent,Exercise e,int idx,boolean editing){
        Set s=e.sets.get(idx);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(2),0,dp(2));
        TextView sn=label("S"+(idx+1),15,fg(),true);row.addView(sn,new LinearLayout.LayoutParams(dp(38),dp(48)));
        EditText reps=edit("");reps.setText(s.reps);reps.setTextSize(16);reps.setEnabled(editing);
        reps.setInputType(InputType.TYPE_CLASS_TEXT);reps.setOnFocusChangeListener((v,f)->{if(!f&&editing){s.reps=reps.getText().toString();store.save();}});
        row.addView(reps,new LinearLayout.LayoutParams(dp(78),dp(48)));
        if(s.isBackOff){
            TextView w=label(fmt(store.backOffWeight(e)),16,accent,true);w.setGravity(Gravity.CENTER);w.setBackground(shape(dark?Color.rgb(48,35,50):Color.rgb(247,235,238),16));
            row.addView(w,new LinearLayout.LayoutParams(dp(76),dp(48)));row.addView(label("kg",13,accent,true),new LinearLayout.LayoutParams(dp(30),dp(48)));
        }else{
            EditText w=edit("");w.setText(fmt(s.weight));w.setTextSize(16);w.setEnabled(editing);w.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);w.setOnEditorActionListener((v,id,event)->{commitWeight(e,idx,w);hideKeyboard();return true;});
            w.setOnFocusChangeListener((v,f)->{if(!f&&editing)commitWeight(e,idx,w);});row.addView(w,new LinearLayout.LayoutParams(dp(82),dp(48)));row.addView(label("kg",13,accent,true),new LinearLayout.LayoutParams(dp(30),dp(48)));
        }
        CheckBox done=new CheckBox(this);done.setChecked(s.done);done.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,Color.WHITE}));
        done.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){s.done=c;store.save();if(c)startRecovery(e,s);else stopRecovery(s.id);}});
        row.addView(done,new LinearLayout.LayoutParams(dp(48),dp(52)));parent.addView(row);
    }
    void commitWeight(Exercise e,int idx,EditText w){try{String raw=w.getText().toString().replace(",",".");if(raw.trim().isEmpty())return;double x=Double.parseDouble(raw);store.setWeight(e,idx,x);w.setText(fmt(x));}catch(Exception ignored){}}

    void showExercise(Exercise e,boolean editing){
        clearPage(e.name,false);
        TextView sub=label(e.group+"  •  "+e.focus,14,secondary(),false);content.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView mode=label(editing?"Fine":"Modifica",17,accent,true);mode.setOnClickListener(v->showExercise(e,!editing));top.addView(mode,new LinearLayout.LayoutParams(0,dp(46),1));
        TextView back=label("‹  Scheda",17,accent,true);back.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);back.setOnClickListener(v->showDashboard());top.addView(back,new LinearLayout.LayoutParams(dp(100),dp(46)));content.addView(top);
        LinearLayout recRow=new LinearLayout(this);recRow.setGravity(Gravity.CENTER_VERTICAL);recRow.addView(text("Recupero",15,true),new LinearLayout.LayoutParams(0,dp(50),1));
        EditText rec=edit("");rec.setText(e.recovery);rec.setEnabled(editing);rec.setGravity(Gravity.CENTER);rec.setTextSize(16);rec.setOnFocusChangeListener((v,f)->{if(!f&&editing){e.recovery=rec.getText().toString();store.save();}});recRow.addView(rec,new LinearLayout.LayoutParams(dp(88),dp(48)));content.addView(recRow);
        if(editing){
            Switch bo=new Switch(this);bo.setText("Back-off ultima serie\n80% della serie precedente");bo.setTextColor(fg());bo.setTextSize(14);bo.setChecked(e.backOffEnabled);bo.setButtonTintList(ColorStateList.valueOf(accent));
            bo.setOnCheckedChangeListener((b,checked)->{if(b.isPressed()){store.setBackOffEnabled(e,checked);showExercise(e,true);}});content.addView(bo,new LinearLayout.LayoutParams(-1,dp(66)));
        }
        LinearLayout setsBox=new LinearLayout(this);setsBox.setOrientation(LinearLayout.VERTICAL);setsBox.setPadding(dp(8),dp(8),dp(8),dp(8));setsBox.setBackground(shape(card(),24));content.addView(setsBox);
        for(int i=0;i<e.sets.size();i++)addSetRowTo(setsBox,e,i,editing);
        LinearLayout acts=new LinearLayout(this);acts.setPadding(0,dp(10),0,dp(2));Button plus=accentButton("+ Serie"),minus=accentButton("− Serie");
        plus.setOnClickListener(v->{store.addSet(e);showExercise(e,editing);});minus.setOnClickListener(v->{store.removeSet(e);showExercise(e,editing);});
        acts.addView(plus,new LinearLayout.LayoutParams(0,dp(48),1));acts.addView(minus,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(acts);
        Button chart=button("Grafico dei kg  ›");chart.setOnClickListener(v->showChart(e));content.addView(chart,new LinearLayout.LayoutParams(-1,dp(52)));
        MuscleMapView map=new MuscleMapView(this,e.target);content.addView(map,new LinearLayout.LayoutParams(-1,dp(270)));
    }

    void showChart(Exercise e){
        clearPage("Progressi",true);
        TextView n=text(e.name,20,true);content.addView(n,new LinearLayout.LayoutParams(-1,dp(34)));
        ArrayList<Log> logs=store.history(e);ChartView chart=new ChartView(this,logs,accent);content.addView(chart,new LinearLayout.LayoutParams(-1,dp(330)));
        TextView info=label("Storico aggiornamenti peso",15,secondary(),true);content.addView(info,new LinearLayout.LayoutParams(-1,dp(30)));
        if(logs.isEmpty())content.addView(label("Nessun aggiornamento ancora.",15,secondary(),false));
        ArrayList<Log> rev=new ArrayList<>(logs);Collections.reverse(rev);
        for(Log l:rev){TextView t=label(new SimpleDateFormat("dd/MM/yyyy  HH:mm",Locale.ITALIAN).format(new Date(l.date))+"     "+fmt(l.weight)+" kg",15,fg(),false);t.setPadding(dp(6),dp(10),dp(6),dp(10));content.addView(t);}
        TextView back=label("‹  Torna all'esercizio",16,accent,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->showExercise(e,false));content.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void showProgress(){
        selectNav(1);
        clearPage("Progressi",true);
        TextView d=label("I tuoi allenamenti sono organizzati per giornata.",17,secondary(),false);content.addView(d,new LinearLayout.LayoutParams(-1,dp(30)));
        gap(8);
        boolean any=false;
        for(String day:DAYS){
            List<Exercise> es=store.forDay(day);if(es.isEmpty())continue;any=true;
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(shape(card(),28));
            TextView h=text(day,23,true);c.addView(h,new LinearLayout.LayoutParams(-1,dp(34)));
            TextView w=label(workoutName(day),16,accent,true);c.addView(w,new LinearLayout.LayoutParams(-1,dp(28)));
            LinearLayout stats=new LinearLayout(this);stats.setPadding(0,dp(8),0,dp(8));
            miniStat(stats,String.valueOf(es.size()),"esercizi");miniStat(stats,String.valueOf(store.totalSets(es)),"serie");
            c.addView(stats,new LinearLayout.LayoutParams(-1,dp(78)));
            TextView muscles=label(dayMuscles(es),14,secondary(),true);c.addView(muscles,new LinearLayout.LayoutParams(-1,dp(26)));
            c.setOnClickListener(v->showDayProgress(day));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(205));cp.setMargins(0,dp(8),0,dp(8));content.addView(c,cp);
        }
        if(!any)content.addView(label("Nessun allenamento",16,secondary(),false));
    }
    void miniStat(LinearLayout p,String n,String lab){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(12),dp(8),dp(12),0);x.setBackground(shape(dark?Color.rgb(52,35,39):Color.rgb(249,232,234),16));x.addView(text(n,24,true));x.addView(label(lab,12,secondary(),false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(104),dp(64));lp.setMargins(0,0,dp(8),0);p.addView(x,lp);}
    String dayMuscles(List<Exercise> es){LinkedHashSet<String>s=new LinkedHashSet<>();for(Exercise e:es)s.add(e.group);return String.join("  •  ",s);}
    String workoutName(String d){if(d.equals("LUNEDÌ"))return"UPPER";if(d.equals("MARTEDÌ"))return"LOWER";if(d.equals("MERCOLEDÌ"))return"FULLBODY";return"ALLENAMENTO";}
    void showDayProgress(String day){
        clearPage(day,true);TextView w=label(workoutName(day),18,accent,true);content.addView(w,new LinearLayout.LayoutParams(-1,dp(30)));
        for(Exercise e:store.forDay(day)){LinearLayout c=new LinearLayout(this);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(18),0,dp(16),0);c.setBackground(shape(card(),22));TextView t=text(e.name+"\n"+e.sets.size()+" serie  •  max "+fmt(store.maxWeight(e))+" kg",16,true);c.addView(t,new LinearLayout.LayoutParams(0,dp(72),1));TextView ar=label("›",30,secondary(),false);c.addView(ar,new LinearLayout.LayoutParams(dp(30),dp(72)));c.setOnClickListener(v->showChart(e));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76));lp.setMargins(0,dp(5),0,dp(5));content.addView(c,lp);}
        TextView back=label("‹  Progressi",16,accent,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->showProgress());content.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void showAdd(){
        selectNav(2);
        clearPage("Aggiungi esercizio",false);
        daySelector();gap(12);
        TextView sec=label("Esercizio",17,secondary(),true);content.addView(sec,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(14),dp(8),dp(14),dp(8));form.setBackground(shape(card(),28));
        EditText name=edit("Nome");EditText reps=edit("8-10");reps.setText("8-10");form.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));gapIn(form,1);form.addView(reps,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);TextView sn=text("Serie normali: 3",17,true);sr.addView(sn,new LinearLayout.LayoutParams(0,dp(58),1));
        final int[] count={3};Button minus=button("−"),plus=button("+");sr.addView(minus,new LinearLayout.LayoutParams(dp(58),dp(48)));sr.addView(plus,new LinearLayout.LayoutParams(dp(58),dp(48)));form.addView(sr);
        Switch backOff=new Switch(this);backOff.setText("Aggiungi back-off −20%");backOff.setTextColor(fg());backOff.setTextSize(16);form.addView(backOff,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView recTitle=label("Recupero",17,fg(),true);form.addView(recTitle,new LinearLayout.LayoutParams(-1,dp(30)));
        EditText recovery=edit("2:00");recovery.setText("2:00");form.addView(recovery,new LinearLayout.LayoutParams(-1,dp(52)));
        content.addView(form,new LinearLayout.LayoutParams(-1,-2));
        gap(12);TextView wt=label("Kg per serie normale",17,secondary(),true);content.addView(wt,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout weightsBox=new LinearLayout(this);weightsBox.setOrientation(LinearLayout.VERTICAL);weightsBox.setPadding(dp(14),dp(8),dp(14),dp(8));weightsBox.setBackground(shape(card(),28));content.addView(weightsBox);
        ArrayList<EditText> weights=new ArrayList<>();
        Runnable rebuild=()->{weightsBox.removeAllViews();weights.clear();for(int i=0;i<count[0];i++){EditText w=edit("");w.setText("20");w.setTextSize(17);weights.add(w);weightsBox.addView(w,new LinearLayout.LayoutParams(-1,dp(54)));if(i<count[0]-1)gapBox(weightsBox,1);}};
        rebuild.run();
        minus.setOnClickListener(v->{if(count[0]>1){count[0]--;sn.setText("Serie normali: "+count[0]);rebuild.run();}});
        plus.setOnClickListener(v->{if(count[0]<20){count[0]++;sn.setText("Serie normali: "+count[0]);rebuild.run();}});
        gap(12);Button add=accentButton("Aggiungi alla scheda");add.setTextSize(17);add.setOnClickListener(v->{hideKeyboard();try{ArrayList<Double> ws=new ArrayList<>();for(EditText w:weights)ws.add(Double.parseDouble(w.getText().toString().replace(",",".")));store.add(store.selectedDay,name.getText().toString().trim(),reps.getText().toString().trim(),ws,recovery.getText().toString().trim(),backOff.isChecked());showDashboard();}catch(Exception ex){Toast.makeText(this,"Compila nome, serie e kg",Toast.LENGTH_SHORT).show();}});content.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));
        Button imp=button("▣  Importa scheda da foto / galleria");imp.setOnClickListener(v->showImport());content.addView(imp,new LinearLayout.LayoutParams(-1,dp(52)));
    }
    void gapBox(LinearLayout p,int h){View v=new View(this);v.setBackgroundColor(dark?Color.rgb(54,54,58):Color.rgb(225,225,229));p.addView(v,new LinearLayout.LayoutParams(-1,dp(h)));}

    void showImport(){
        clearPage("Importa scheda",true);
        TextView info=label("Foto, fotocamera o PDF. Il testo viene riconosciuto e trasformato in esercizi.",15,secondary(),false);content.addView(info,new LinearLayout.LayoutParams(-1,dp(48)));
        Button camera=accentButton("Scatta foto");camera.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},78);return;}startActivityForResult(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),REQ_CAMERA);});content.addView(camera,new LinearLayout.LayoutParams(-1,dp(54)));
        Button gallery=button("Scegli foto");gallery.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("image/*");startActivityForResult(in,REQ_IMAGE);});content.addView(gallery,new LinearLayout.LayoutParams(-1,dp(54)));
        Button pdf=button("Importa PDF");pdf.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("application/pdf");startActivityForResult(in,REQ_PDF);});content.addView(pdf,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView back=label("‹  Scheda",16,accent,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->showDashboard());content.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;if(requestCode==REQ_IMAGE&&data.getData()!=null)ocrImage(data.getData());else if(requestCode==REQ_CAMERA&&data.getExtras()!=null){Bitmap b=(Bitmap)data.getExtras().get("data");if(b!=null)ocrBitmap(b);}else if(requestCode==REQ_PDF&&data.getData()!=null)ocrPdf(data.getData());}
    void ocrImage(Uri uri){try{ocr(InputImage.fromFilePath(this,uri));}catch(Exception e){Toast.makeText(this,"Impossibile leggere l'immagine",Toast.LENGTH_SHORT).show();}}
    void ocrBitmap(Bitmap b){ocr(InputImage.fromBitmap(b,0));}
    void ocr(InputImage img){Toast.makeText(this,"Analizzo la scheda…",Toast.LENGTH_SHORT).show();TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img).addOnSuccessListener(r->showImportedPreview(r.getText())).addOnFailureListener(e->Toast.makeText(this,"OCR non riuscito",Toast.LENGTH_SHORT).show());}
    void ocrPdf(Uri uri){Toast.makeText(this,"Analizzo il PDF…",Toast.LENGTH_SHORT).show();new Thread(()->{StringBuilder all=new StringBuilder();try{ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new IOException();PdfRenderer renderer=new PdfRenderer(fd);for(int i=0;i<renderer.getPageCount();i++){PdfRenderer.Page page=renderer.openPage(i);int w=Math.max(600,page.getWidth()*2),h=Math.max(800,page.getHeight()*2);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);page.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);page.close();String text=Tasks.await(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(b,0))).getText();all.append(text).append("\n");b.recycle();}renderer.close();fd.close();final String result=all.toString();runOnUiThread(()->showImportedPreview(result));}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Impossibile leggere il PDF",Toast.LENGTH_SHORT).show());}}).start();}
    void showImportedPreview(String text){ArrayList<Imported> items=store.parseImported(text);if(items.isEmpty()){new AlertDialog.Builder(this).setTitle("Scheda non riconosciuta").setMessage("Non ho trovato esercizi nel testo. Prova con una foto più nitida.").setPositiveButton("OK",null).show();return;}LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(18),dp(4),dp(18),dp(4));list.addView(text("Controlla prima di importare ("+items.size()+" esercizi)",17,true));for(Imported it:items){list.addView(label(it.day+"  •  "+it.name+"\n"+it.sets+" serie  •  "+it.reps+"  •  recupero "+(it.recovery.isEmpty()?"—":it.recovery),14,fg(),false));}ScrollView scroll=new ScrollView(this);scroll.addView(list);new AlertDialog.Builder(this).setTitle("Importa scheda").setView(scroll).setNegativeButton("Annulla",null).setPositiveButton("Importa tutto",(d,w)->{int n=0;for(Imported it:items){store.add(it.day,it.name,it.reps,repeat(it.sets),it.recovery,false);n++;}Toast.makeText(this,"Importati "+n+" esercizi",Toast.LENGTH_LONG).show();showDashboard();}).show();}
    ArrayList<Double> repeat(int n){ArrayList<Double>x=new ArrayList<>();for(int i=0;i<Math.max(1,n);i++)x.add(20.0);return x;}

    void showSettings(){
        selectNav(3);
        clearPage("Impostazioni",true);
        TextView ap=label("Aspetto",17,secondary(),true);content.addView(ap,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout appearance=new LinearLayout(this);appearance.setOrientation(LinearLayout.VERTICAL);appearance.setPadding(dp(16),0,dp(16),0);appearance.setBackground(shape(card(),28));
        Switch theme=new Switch(this);theme.setText("Tema scuro");theme.setTextColor(fg());theme.setTextSize(17);theme.setChecked(dark);theme.setOnCheckedChangeListener((b,c)->{if(b.isPressed()){store.dark=c;store.save();build();}});appearance.addView(theme,new LinearLayout.LayoutParams(-1,dp(66)));
        TextView sep=label("Colore principale",17,fg(),true);appearance.addView(sep,new LinearLayout.LayoutParams(-1,dp(58)));
        String[] names={"Viola","Blu","Verde","Arancione","Rosso","Rosa"};int[] colors={Color.rgb(216,137,255),Color.rgb(80,135,255),Color.rgb(60,195,120),Color.rgb(245,150,55),RED,Color.rgb(245,105,160)};
        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);for(int i=0;i<names.length;i++){final int k=i;TextView c=label("●",28,colors[i],true);c.setGravity(Gravity.CENTER);c.setOnClickListener(v->{store.accentIndex=k;store.accent=colors[k];store.save();build();});chips.addView(c,new LinearLayout.LayoutParams(0,dp(54),1));}appearance.addView(chips);content.addView(appearance,new LinearLayout.LayoutParams(-1,dp(178)));

        gap(20);
        TextView nrTitle=label("Notifiche recupero",17,secondary(),true);
        content.addView(nrTitle,new LinearLayout.LayoutParams(-1,dp(32)));

        LinearLayout notifications=new LinearLayout(this);
        notifications.setOrientation(LinearLayout.VERTICAL);
        notifications.setPadding(dp(16),dp(6),dp(16),dp(10));
        notifications.setBackground(shape(card(),28));

        LinearLayout nrow=new LinearLayout(this);
        nrow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nt=label("Notifica al termine del recupero",16,fg(),true);
        nrow.addView(nt,new LinearLayout.LayoutParams(0,dp(58),1));
        Switch ns=new Switch(this);
        ns.setChecked(store.notificationsEnabled);
        ns.setOnCheckedChangeListener((b,c)->{
            if(!b.isPressed())return;
            store.notificationsEnabled=c;
            store.save();
            if(c){
                RecoveryNotifications.ensureChannel(this);
                if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);
            }
        });
        nrow.addView(ns,new LinearLayout.LayoutParams(dp(64),dp(58)));
        notifications.addView(nrow);

        TextView nd=label("Completa una serie: parte il timer visuale. Alla fine ricevi la notifica anche con l'app in background.",12,secondary(),false);
        nd.setPadding(0,0,0,dp(8));
        notifications.addView(nd,new LinearLayout.LayoutParams(-1,dp(52)));

        TextView open=label("Apri impostazioni notifiche",16,accent,true);
        open.setGravity(Gravity.CENTER_VERTICAL);
        open.setOnClickListener(v->openNotificationSettings());
        notifications.addView(open,new LinearLayout.LayoutParams(-1,dp(44)));
        content.addView(notifications,new LinearLayout.LayoutParams(-1,dp(166)));
        gap(20);TextView dat=label("Dati",17,secondary(),true);content.addView(dat,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout data=new LinearLayout(this);data.setOrientation(LinearLayout.VERTICAL);data.setPadding(dp(16),dp(14),dp(16),dp(10));data.setBackground(shape(card(),28));data.addView(text("I dati vengono salvati localmente\nsul telefono.",17,true),new LinearLayout.LayoutParams(-1,dp(70)));TextView notif=label("Richiedi notifiche",17,accent,true);notif.setPadding(0,dp(12),0,0);notif.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);});data.addView(notif,new LinearLayout.LayoutParams(-1,dp(48)));content.addView(data,new LinearLayout.LayoutParams(-1,dp(132)));
        gap(20);TextView wk=label("Settimana",17,secondary(),true);content.addView(wk,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout week=new LinearLayout(this);week.setOrientation(LinearLayout.VERTICAL);week.setPadding(dp(16),dp(4),dp(16),dp(4));week.setBackground(shape(card(),28));
        for(String d:DAYS){TextView row=label(d+"                                      "+store.forDay(d).size()+" esercizi",16,fg(),true);row.setPadding(0,dp(12),0,dp(12));week.addView(row,new LinearLayout.LayoutParams(-1,dp(54)));}content.addView(week,new LinearLayout.LayoutParams(-1,-2));
    }

    void startRecovery(Exercise e, Set s){
        try{
            String r=e.recovery==null?"":e.recovery.trim();
            int sec=parseRecovery(r);
            if(sec<=0)return;
            timerExerciseId=e.id;
            timerSetId=s.id;
            timerDurationMs=sec*1000L;
            timerEndMs=System.currentTimeMillis()+timerDurationMs;
            showRecoveryTimer(e,sec);
            if(store.notificationsEnabled) RecoveryNotifications.schedule(this,e.name,s.id,sec);
            timerHandler.removeCallbacksAndMessages(null);
            timerHandler.post(timerTick);
        }catch(Exception ignored){}
    }

    void stopRecovery(long setId){
        if(timerSetId==setId){
            timerHandler.removeCallbacksAndMessages(null);
            timerExerciseId=-1; timerSetId=-1; timerEndMs=0; timerDurationMs=0;
            recoveryTimerCard=null; recoveryProgress=null; recoveryCountdown=null;
        }
        RecoveryNotifications.cancel(this,setId);
    }

    void showRecoveryTimer(Exercise e,int sec){
        if(recoveryTimerCard==null || recoveryTimerCard.getParent()==null || timerExerciseId!=e.id){
            recoveryTimerCard=new LinearLayout(this);
            recoveryTimerCard.setOrientation(LinearLayout.VERTICAL);
            recoveryTimerCard.setPadding(dp(16),dp(14),dp(16),dp(14));
            recoveryTimerCard.setBackground(outlined(card(),24,dark?Color.rgb(70,45,75):Color.rgb(230,215,235),1));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(10),0,dp(12));
            content.addView(recoveryTimerCard,lp);
        }else recoveryTimerCard.removeAllViews();

        TextView cap=label("RECUPERO",12,accent,true);
        recoveryTimerCard.addView(cap,new LinearLayout.LayoutParams(-1,dp(24)));

        recoveryCountdown=label(formatTime(sec),30,fg(),true);
        recoveryCountdown.setGravity(Gravity.CENTER);
        recoveryTimerCard.addView(recoveryCountdown,new LinearLayout.LayoutParams(-1,dp(48)));

        recoveryProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        recoveryProgress.setMax(1000);
        recoveryProgress.setProgress(0);
        recoveryProgress.setProgressTintList(ColorStateList.valueOf(accent));
        recoveryProgress.setIndeterminate(false);
        recoveryTimerCard.addView(recoveryProgress,new LinearLayout.LayoutParams(-1,dp(10)));

        TextView hint=label("Il timer scorre automaticamente. Al termine riceverai una notifica.",11,secondary(),false);
        hint.setGravity(Gravity.CENTER);
        recoveryTimerCard.addView(hint,new LinearLayout.LayoutParams(-1,dp(30)));
    }

    String formatTime(long sec){
        return String.format(Locale.ITALIAN,"%d:%02d",sec/60,sec%60);
    }

    final Runnable timerTick=new Runnable(){
        public void run(){
            if(timerEndMs<=0)return;
            long remaining=Math.max(0,timerEndMs-System.currentTimeMillis());
            long total=Math.max(1,timerDurationMs);
            if(recoveryCountdown!=null) recoveryCountdown.setText(formatTime((remaining+999)/1000));
            if(recoveryProgress!=null) recoveryProgress.setProgress((int)Math.min(1000,Math.max(0,((total-remaining)*1000)/total)));
            if(remaining<=0){
                timerHandler.removeCallbacks(this);
                if(recoveryCountdown!=null) recoveryCountdown.setText("0:00");
                if(recoveryProgress!=null) recoveryProgress.setProgress(1000);
                return;
            }
            timerHandler.postDelayed(this,500);
        }
    };

    int parseRecovery(String r){try{String[]p=r.split(":");return p.length==2?Integer.parseInt(p[0])*60+Integer.parseInt(p[1]):Integer.parseInt(p[0]);}catch(Exception e){return 0;}}
    String fmt(double x){return x==Math.rint(x)?String.valueOf((int)x):String.format(Locale.US,"%.1f",x);}
    public static class Imported{String day,name,reps,recovery;int sets;Imported(String d,String n,String r,int s,String rec){day=d;name=n;reps=r;sets=s;recovery=rec;}}
    public static class Exercise{long id;String day,name,group,focus,target,reps,recovery="";boolean backOffEnabled;ArrayList<Set>sets=new ArrayList<>();}
    public static class Set{long id;String reps;double weight;boolean done,isBackOff;ArrayList<Log>history=new ArrayList<>();}
    public static class Log{long date;double weight;Log(long d,double w){date=d;weight=w;}}

    static class WorkoutStore{
        final Context c;ArrayList<Exercise> all=new ArrayList<>();String selectedDay="LUNEDÌ";boolean dark=true;int accent=Color.rgb(216,137,255),accentIndex=0;boolean notificationsEnabled=true;final String key="gymapp.android.v2";
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
    static class MuscleMapView extends ImageView {
        MuscleMapView(Context c,String target){super(c);setScaleType(ScaleType.CENTER_CROP);setBackground(shapeStatic(Color.rgb(232,232,234),26));int id=getResource(c,target);setImageResource(id);setPadding(0,0,0,0);}
        static int getResource(Context c,String target){String t=target==null?"full":target.toLowerCase(Locale.ITALIAN);String key="muscle_full";if(t.contains("chest"))key="muscle_chest";else if(t.contains("back"))key="muscle_back";else if(t.contains("shoulder"))key="muscle_shoulders";else if(t.contains("biceps"))key="muscle_biceps";else if(t.contains("triceps"))key="muscle_triceps";else if(t.contains("quad"))key="muscle_quads";else if(t.contains("hamstring"))key="muscle_hamstrings";return c.getResources().getIdentifier(key,"drawable",c.getPackageName());}
        static GradientDrawable shapeStatic(int color,float r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(r*3);return g;}
    }


}
