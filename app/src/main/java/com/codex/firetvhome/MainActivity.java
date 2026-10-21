package com.codex.firetvhome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private final int WHITE=Color.WHITE, MUTED=0xffaeb5c3;
  private final Handler handler=new Handler();
  private final ArrayList<App> apps=new ArrayList<>();
  private LinearLayout content, topBar;
  private RecyclerView ribbon;
  private AppsAdapter adapter;
  private TextView clock, date, greeting, heroTitle, wifiPill;
  private FrameLayout shell, saverLayer;
  private ScreenSaver screenSaver;
  private Typeface outfit;
  private boolean saverVisible=false;
  private final Runnable idleTask=()->showScreensaver();
  private final Runnable clockTask=new Runnable(){public void run(){updateClock();handler.postDelayed(this,30000);}};

  private static class App {
    String label, pkg, action, component;
    Drawable icon;
    App(String label,String pkg,Drawable icon){this.label=label;this.pkg=pkg;this.icon=icon;}
  }
  private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
  private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private TextView label(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setFlags(1024,1024);getWindow().setNavigationBarColor(Color.BLACK);getWindow().setStatusBarColor(Color.BLACK);
    outfit=Typeface.createFromAsset(getAssets(),"Outfit.ttf");
    buildScreen();updateClock();handler.postDelayed(clockTask,30000);resetIdleTimer();
    if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},41);
  }

  private boolean setHomeRedirectEnabled(boolean enabled){
    final String shortId="io.github.toolicious.homeonfire/.HijackService";
    final String fullId="io.github.toolicious.homeonfire/io.github.toolicious.homeonfire.HijackService";
    try{
      String current=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
      ArrayList<String> services=new ArrayList<>();
      if(current!=null&&!current.isEmpty()) for(String item:current.split(":")) if(!item.isEmpty()&&!item.equals(shortId)&&!item.equals(fullId)) services.add(item);
      if(enabled) services.add(shortId);
      String updated=android.text.TextUtils.join(":",services);
      Settings.Secure.putString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,updated);
      if(enabled) Settings.Secure.putInt(getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1);
      return true;
    }catch(SecurityException e){ return false; }
  }
  private void buildScreen(){
    shell=new FrameLayout(this);
    ImageView background=new ImageView(this);background.setImageResource(R.drawable.aft_home_background);background.setScaleType(ImageView.ScaleType.CENTER_CROP);background.setContentDescription(null);
    shell.addView(background,new FrameLayout.LayoutParams(-1,-1));
    View scrim=new View(this);scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x10000000,0x08000000,0x64000000}));
    shell.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
    content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(56),dp(30),dp(56),dp(24));content.setBackgroundColor(Color.TRANSPARENT);
    shell.addView(content,new FrameLayout.LayoutParams(-1,-1));setContentView(shell);

    FrameLayout header=new FrameLayout(this);content.addView(header,new LinearLayout.LayoutParams(-1,dp(145)));
    LinearLayout clockBlock=new LinearLayout(this);clockBlock.setOrientation(LinearLayout.VERTICAL);
    header.addView(clockBlock,new FrameLayout.LayoutParams(dp(530),-1,Gravity.LEFT|Gravity.TOP));
    LinearLayout greetingRow=new LinearLayout(this);greetingRow.setGravity(Gravity.CENTER_VERTICAL);
    ImageView brandLogo=new ImageView(this);brandLogo.setImageResource(R.drawable.aft_home_icon);brandLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
    LinearLayout.LayoutParams logoParams=new LinearLayout.LayoutParams(dp(28),dp(28));logoParams.rightMargin=dp(9);greetingRow.addView(brandLogo,logoParams);
    greeting=label("Good evening",19,0xffd8dce5);greeting.setTypeface(outfit);greetingRow.addView(greeting,new LinearLayout.LayoutParams(-1,dp(27)));
    clockBlock.addView(greetingRow,new LinearLayout.LayoutParams(-1,dp(30)));
    clock=label("--:--",55,WHITE);clock.setTypeface(outfit);clockBlock.addView(clock,new LinearLayout.LayoutParams(-1,dp(66)));
    date=label("",16,MUTED);date.setTypeface(outfit);clockBlock.addView(date,new LinearLayout.LayoutParams(-1,dp(28)));

    topBar=new LinearLayout(this);topBar.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    header.addView(topBar,new FrameLayout.LayoutParams(-2,-1,Gravity.RIGHT|Gravity.TOP));
    addTopControl("Inputs",()->showInputs());wifiPill=addTopControl(wifiName(),()->startActivity(new Intent("android.settings.WIFI_SETTINGS")));addTopControl("Launcher",()->showLauncherChooser());addTopControl("Settings",()->startActivity(new Intent("android.settings.SETTINGS")));

    heroTitle=label("Choose an app",30,WHITE);heroTitle.setTypeface(outfit);content.addView(heroTitle,new LinearLayout.LayoutParams(-1,dp(52)));
    TextView sub=label("←  →  Browse apps     OK  Open",15,0xff87909f);content.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));
    ribbon=new RecyclerView(this);ribbon.setClipToPadding(false);ribbon.setPadding(dp(0),dp(24),dp(32),dp(26));ribbon.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,0,1);content.addView(ribbon,rp);
    TextView footer=label("Your apps",14,0xff7f8794);content.addView(footer,new LinearLayout.LayoutParams(-1,dp(24)));

    loadApps();adapter=new AppsAdapter();ribbon.setAdapter(adapter);
    ribbon.post(()->focusLastOrFirst());
    saverLayer=new FrameLayout(this);saverLayer.setVisibility(View.GONE);screenSaver=new ScreenSaver(this);saverLayer.addView(screenSaver,new FrameLayout.LayoutParams(-1,-1));shell.addView(saverLayer,new FrameLayout.LayoutParams(-1,-1));
    shell.setFocusableInTouchMode(true);
  }
  private TextView addTopControl(String text,Runnable action){
    TextView v=label(text,16,WHITE);v.setGravity(Gravity.CENTER);v.setPadding(dp(17),0,dp(17),0);v.setBackground(rounded(0xff242832,24));
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(46));p.leftMargin=dp(9);topBar.addView(v,p);v.setFocusable(true);v.setOnClickListener(w->action.run());
    v.setOnFocusChangeListener((w,focused)->v.setBackground(rounded(focused?0xff424957:0xff242832,24)));
    v.setOnKeyListener((w,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
      if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){int index=topBar.indexOfChild(v),step=key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1,target=index+step;if(target>=0&&target<topBar.getChildCount())topBar.getChildAt(target).requestFocus();return true;}
      if(key==KeyEvent.KEYCODE_DPAD_DOWN){focusLastOrFirst();return true;}return false;});
    return v;
  }
  private String wifiName(){try{WifiManager m=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);String s=m.getConnectionInfo().getSSID();if(s!=null&&!s.equals("<unknown ssid>"))return s.replace("\"","");}catch(Exception ignored){}return "Wi-Fi";}
  private void loadApps(){
    PackageManager pm=getPackageManager();HashSet<String> pinned=new HashSet<>();
    addPinned(pm,pinned,"Netflix","com.netflix.ninja");addPinned(pm,pinned,"YouTube","com.amazon.firetv.youtube");addPinned(pm,pinned,"JioHotstar","in.startv.hotstar");
    try{App prime=new App("Prime Video","com.amazon.tv.launcher",primeVideoIcon(pm));apps.add(prime);pinned.add(prime.pkg);}catch(Exception ignored){}
    Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
    List<ResolveInfo> result=pm.queryIntentActivities(query,0);ArrayList<App> rest=new ArrayList<>();HashSet<String> seen=new HashSet<>(pinned);
    for(ResolveInfo info:result){String pkg=info.activityInfo.packageName;if(pkg.equals(getPackageName())||seen.contains(pkg))continue;seen.add(pkg);rest.add(new App(info.loadLabel(pm).toString(),pkg,info.loadIcon(pm)));}
    Collections.sort(rest,(a,b)->a.label.compareToIgnoreCase(b.label));apps.addAll(rest);
  }
  private void addPinned(PackageManager pm,Set<String> pinned,String label,String pkg){try{Drawable icon=pm.getApplicationIcon(pkg);apps.add(new App(label,pkg,icon));pinned.add(pkg);}catch(Exception ignored){}}
  private Drawable primeVideoIcon(PackageManager pm){
    return new PrimeVideoWordmark();
  }
  private LinearLayout makeTile(App a){
    LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(12),dp(12),dp(10));tile.setFocusable(true);
    tile.setBackground(rounded(0x3d111923,17));ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);tile.addView(icon,new LinearLayout.LayoutParams(dp(96),dp(92)));
    TextView name=label("",17,WHITE);name.setTypeface(outfit);name.setGravity(Gravity.CENTER);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(name,new LinearLayout.LayoutParams(-1,dp(34)));
    return tile;
  }
  private class AppsAdapter extends RecyclerView.Adapter<AppHolder>{
    @Override public int getItemCount(){return apps.size();}
    @Override public AppHolder onCreateViewHolder(android.view.ViewGroup parent,int type){return new AppHolder(makeTile(null));}
    @Override public void onBindViewHolder(AppHolder holder,int position){
      App a=apps.get(position);LinearLayout tile=(LinearLayout)holder.itemView;ImageView icon=(ImageView)tile.getChildAt(0);TextView name=(TextView)tile.getChildAt(1);
      icon.setImageDrawable(a.icon);name.setText(a.label);tile.setTag(position);
      RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(dp(190),dp(168));lp.setMargins(0,dp(8),dp(18),dp(16));tile.setLayoutParams(lp);
      tile.setOnFocusChangeListener((v,focused)->{v.animate().scaleX(focused?1.07f:1f).scaleY(focused?1.07f:1f).setInterpolator(new OvershootInterpolator(1.2f)).setDuration(210).start();v.setBackground(rounded(focused?0x70435b76:0x3d111923,17));if(focused){heroTitle.setText(a.label);getPreferences(0).edit().putString("last",a.pkg).apply();}});
      tile.setOnClickListener(v->launch(a));tile.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_UP){if(topBar.getChildCount()>0)topBar.getChildAt(0).requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN)return true;if(key==KeyEvent.KEYCODE_DPAD_LEFT&&position==0)return true;return false;});
    }
  }
  private static class AppHolder extends RecyclerView.ViewHolder{AppHolder(View view){super(view);}}
  private class PrimeVideoWordmark extends Drawable{
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas target){
      Rect b=getBounds();if(b.width()==0||b.height()==0)return;int save=target.save();target.translate(b.left,b.top);target.scale(b.width()/160f,b.height()/100f);
      p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(27f);p.setColor(0xff19a9e5);
      String prime="prime",video="video";target.drawText(prime,7,48,p);float x=7+p.measureText(prime)+10;target.drawText(video,x,48,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff19a9e5);
      Path smile=new Path();smile.moveTo(25,61);smile.quadTo(76,91,128,62);target.drawPath(smile,p);
      p.setStyle(Paint.Style.FILL);Path arrow=new Path();arrow.moveTo(128,62);arrow.lineTo(119,61);arrow.lineTo(125,70);arrow.close();target.drawPath(arrow,p);target.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return 160;}
    @Override public int getIntrinsicHeight(){return 100;}
  }
  private void focusLastOrFirst(){if(apps.isEmpty())return;String last=getPreferences(0).getString("last","");int found=0;for(int i=0;i<apps.size();i++)if(apps.get(i).pkg.equals(last)){found=i;break;}final int target=found;ribbon.scrollToPosition(target);ribbon.post(()->{RecyclerView.ViewHolder h=ribbon.findViewHolderForAdapterPosition(target);if(h!=null)h.itemView.requestFocus();else if(ribbon.getChildCount()>0)ribbon.getChildAt(0).requestFocus();});}
  private void launch(App app){
    if(app.label.equals("Prime Video")){
      try{if(!setHomeRedirectEnabled(false))throw new SecurityException();Intent home=new Intent(Intent.ACTION_MAIN);home.addCategory(Intent.CATEGORY_HOME);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(home);Toast.makeText(this,"Fire TV Home opened. Select Prime Video in the app row.",Toast.LENGTH_LONG).show();}
      catch(Exception e){Toast.makeText(this,"Open Prime Video from Fire TV Home on this device.",Toast.LENGTH_LONG).show();}
      return;
    }
    try{Intent i=getPackageManager().getLaunchIntentForPackage(app.pkg);if(i==null)throw new ActivityNotFoundException();i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){Toast.makeText(this,"Could not open "+app.label,Toast.LENGTH_SHORT).show();}
  }
  private void showInputs(){PopupMenu menu=new PopupMenu(this,topBar.getChildAt(0));menu.getMenu().add("HDMI 1 · use TV remote");menu.getMenu().add("HDMI 2 · use TV remote");menu.getMenu().add("AV · use TV remote");menu.getMenu().add("Live TV");menu.setOnMenuItemClickListener(item->{if(item.getTitle().toString().equals("Live TV")){try{Intent i=getPackageManager().getLaunchIntentForPackage("com.amazon.tv.livetv");if(i!=null)startActivity(i);else Toast.makeText(this,"Live TV is unavailable",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Live TV is unavailable",Toast.LENGTH_SHORT).show();}}else Toast.makeText(this,"Select the input with the Samsung remote",Toast.LENGTH_LONG).show();return true;});menu.show();}
  private void showLauncherChooser(){
    new AlertDialog.Builder(this).setTitle("Choose launcher")
      .setItems(new String[]{"AFT Home · this design","Fire TV Home · Amazon"},(dialog,which)->{
        try{
          Intent i;
          if(which==0){i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
          else{if(!setHomeRedirectEnabled(false))throw new SecurityException();i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_HOME);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
          startActivity(i);
        }catch(Exception e){Toast.makeText(this,"Could not switch launcher",Toast.LENGTH_SHORT).show();}
      }).show();
  }
  private void updateClock(){Date now=new Date();Calendar c=Calendar.getInstance();c.setTime(now);int h=c.get(Calendar.HOUR_OF_DAY);if(greeting!=null)greeting.setText(h<12?"Good morning":h<17?"Good afternoon":"Good evening");if(clock!=null)clock.setText(new SimpleDateFormat("h:mm",Locale.getDefault()).format(now));if(date!=null)date.setText(new SimpleDateFormat("EEEE · MMMM d",Locale.getDefault()).format(now));}
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants){super.onRequestPermissionsResult(requestCode,permissions,grants);if(requestCode==41&&wifiPill!=null)wifiPill.setText(wifiName());}

  private void resetIdleTimer(){handler.removeCallbacks(idleTask);if(!saverVisible)handler.postDelayed(idleTask,120000);}
  private void showScreensaver(){if(saverLayer==null||saverVisible)return;saverVisible=true;saverLayer.setVisibility(View.VISIBLE);screenSaver.invalidate();WindowManager.LayoutParams a=getWindow().getAttributes();a.screenBrightness=0.12f;getWindow().setAttributes(a);screenSaver.startMotion();}
  private void hideScreensaver(){if(!saverVisible)return;saverVisible=false;screenSaver.stopMotion();saverLayer.setVisibility(View.GONE);WindowManager.LayoutParams a=getWindow().getAttributes();a.screenBrightness=WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;getWindow().setAttributes(a);resetIdleTimer();if(ribbon!=null&&ribbon.getFocusedChild()==null)focusLastOrFirst();}
  @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){if(saverVisible){hideScreensaver();return true;}resetIdleTimer();}return super.dispatchKeyEvent(event);}
  @Override protected void onPause(){super.onPause();handler.removeCallbacks(idleTask);handler.removeCallbacks(clockTask);}
  @Override protected void onResume(){super.onResume();setHomeRedirectEnabled(true);handler.removeCallbacks(clockTask);handler.postDelayed(clockTask,1000);resetIdleTimer();}
  private class ScreenSaver extends View{
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Handler motion=new Handler(Looper.getMainLooper());
    private final String[] scenes={"01_coast.jpg","02_desert.jpg","03_hanuman.jpg","04_radha_krishna.jpg","05_vishnu.jpg","06_durga.jpg","07_shiva.jpg"};
    private Bitmap current,next;private int sceneIndex=0;private long fadeStarted;
    private static final long SCENE_DURATION=30000L,FADE_DURATION=1800L,FRAME_DELAY=60L;
    private final Runnable advance=new Runnable(){public void run(){if(!saverVisible)return;next=decodeScene((sceneIndex+1)%scenes.length);if(next==null){sceneIndex=(sceneIndex+1)%scenes.length;motion.postDelayed(this,SCENE_DURATION);return;}fadeStarted=SystemClock.uptimeMillis();invalidate();motion.postDelayed(fade,FRAME_DELAY);}};
    private final Runnable fade=new Runnable(){public void run(){if(!saverVisible||next==null)return;long elapsed=SystemClock.uptimeMillis()-fadeStarted;if(elapsed>=FADE_DURATION){if(current!=null)current.recycle();current=next;next=null;sceneIndex=(sceneIndex+1)%scenes.length;invalidate();motion.postDelayed(advance,SCENE_DURATION);return;}invalidate();motion.postDelayed(this,FRAME_DELAY);}};
    ScreenSaver(Context context){super(context);setFocusable(true);}
    void startMotion(){stopMotion();if(current==null)current=decodeScene(sceneIndex);invalidate();motion.postDelayed(advance,SCENE_DURATION);}
    void stopMotion(){motion.removeCallbacks(advance);motion.removeCallbacks(fade);if(next!=null){next.recycle();next=null;}if(current!=null){current.recycle();current=null;}}
    private Bitmap decodeScene(int index){try(java.io.InputStream in=getAssets().open("screensaver/"+scenes[index])){BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.RGB_565;options.inScaled=false;return BitmapFactory.decodeStream(in,null,options);}catch(Exception ignored){return null;}}
    private void drawScene(Canvas canvas,Bitmap bitmap,float alpha){if(bitmap==null)return;float w=getWidth(),h=getHeight();float scale=Math.max(w/bitmap.getWidth(),h/bitmap.getHeight());float dw=bitmap.getWidth()*scale,dh=bitmap.getHeight()*scale;RectF dst=new RectF((w-dw)/2,(h-dh)/2,(w+dw)/2,(h+dh)/2);paint.setAlpha(Math.round(alpha*255));canvas.drawBitmap(bitmap,null,dst,paint);paint.setAlpha(255);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();canvas.drawColor(Color.BLACK);long elapsed=next==null?FADE_DURATION:SystemClock.uptimeMillis()-fadeStarted;float blend=Math.min(1f,Math.max(0f,(float)elapsed/FADE_DURATION));drawScene(canvas,current,next==null?1f:1f-blend);if(next!=null)drawScene(canvas,next,blend);paint.setAlpha(255);
      // A subtle top-to-bottom scrim keeps the clock readable over both bright and dark scenes.
      paint.setShader(new LinearGradient(0,0,0,h*.58f,new int[]{0x50000000,0x18000000,0x00000000},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h*.58f,paint);paint.setShader(null);
      paint.setTypeface(outfit);paint.setShadowLayer(dp(5),0,dp(2),0x99000000);paint.setColor(0xfff2f3f7);paint.setTextSize(dp(58));String time=new SimpleDateFormat("h:mm",Locale.getDefault()).format(new Date());float x=w*.18f,y=h*.48f;canvas.drawText(time,x,y,paint);paint.clearShadowLayer();paint.setTextSize(dp(19));paint.setColor(0xffc8cdd6);String day=new SimpleDateFormat("EEEE · MMMM d",Locale.getDefault()).format(new Date());canvas.drawText(day,x,y+dp(35),paint);}
  }
}
