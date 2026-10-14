package ru.egergps.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.widget.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(4,24,19), PANEL=Color.rgb(10,36,29), ORANGE=Color.rgb(255,139,0);
    private MapView map;
    private FrameLayout body;
    private TextView heading, message;
    private LocationManager locationManager;
    private LocationListener listener;
    private Marker position;
    private final ArrayList<GeoPoint> points=new ArrayList<>();
    private final ArrayList<String> names=new ArrayList<>();
    private boolean tracking=false;

    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String text,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(12),dp(10),dp(12),dp(10));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setBackgroundColor(BG);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void divider(LinearLayout parent){View line=new View(this);line.setBackgroundColor(0xff24463b);parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        LinearLayout root=vertical();
        LinearLayout top=horizontal();top.setBackgroundColor(BG);
        heading=label("ЕГЕРЬ",21,Color.WHITE,true);
        TextView gps=label("GPS",21,ORANGE,true);gps.setPadding(0,0,0,0);
        top.addView(heading);top.addView(gps);
        View spacer=new View(this);top.addView(spacer,new LinearLayout.LayoutParams(0,dp(1),1));
        TextView search=label("⌕",28,Color.WHITE,false);top.addView(search);
        search.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Поиск").setMessage("Поиск населённых пунктов добавим в следующем обновлении.").setPositiveButton("Понятно",null).show());
        TextView menu=label("☰",24,Color.WHITE,false);top.addView(menu);
        menu.setOnClickListener(v->showMenu());
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(56)));
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=horizontal();nav.setBackgroundColor(BG);
        String[] tabs={"▧\nКарта","♧\nТочки","⌁\nМаршруты","⇩\nЗагрузки","⚙\nНастройки"};
        for(int i=0;i<tabs.length;i++){
            final int index=i;
            TextView tab=label(tabs[i],14,i==0?ORANGE:Color.WHITE,false);
            SpannableString tabText=new SpannableString(tabs[i]);
            int newline=tabs[i].indexOf("\n");
            if(newline>0)tabText.setSpan(new RelativeSizeSpan(2.1f),0,newline,0);
            tab.setText(tabText);
            tab.setIncludeFontPadding(false);
            tab.setGravity(Gravity.CENTER);tab.setPadding(dp(1),dp(5),dp(1),dp(5));tab.setMaxLines(2);
            nav.addView(tab,new LinearLayout.LayoutParams(0,dp(84),1));
            tab.setOnClickListener(v->select(index));
        }
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(84)));
        // Keep application controls outside Android status and navigation bars.
        root.setOnApplyWindowInsetsListener((view,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                top=bars.top;bottom=bars.bottom;
            }else{
                top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0,top,0,bottom);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
        showMap();
    }
    private void select(int index){
        if(index==0)showMap();
        else if(index==1)showPoints();
        else if(index==2)showRoutes();
        else if(index==3)showDownloads();
        else showSettings();
    }
    private void showMap(){
        body.removeAllViews();
        if(map==null){
            map=new MapView(this);
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(8.0);
            map.getController().setCenter(new GeoPoint(52.0,55.5));
        }else if(map.getParent() instanceof android.view.ViewGroup){
            ((android.view.ViewGroup)map.getParent()).removeView(map);
        }
        body.addView(map,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout side=vertical();side.setBackgroundColor(PANEL);
        String[] actions={"➤","+","−","⊙"};
        for(int i=0;i<actions.length;i++){
            final int index=i;
            TextView b=label(actions[i],23,Color.WHITE,true);b.setGravity(Gravity.CENTER);
            side.addView(b,new LinearLayout.LayoutParams(dp(48),dp(52)));
            divider(side);
            b.setOnClickListener(v->{
                if(index==0||index==3)startLocation();
                else map.getController().setZoom(map.getZoomLevelDouble()+(index==1?1:-1));
            });
        }
        FrameLayout.LayoutParams sideParams=new FrameLayout.LayoutParams(dp(48),-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        sideParams.rightMargin=dp(12);body.addView(side,sideParams);
        LinearLayout bottom=vertical();bottom.setBackgroundColor(PANEL);
        message=label("Карта OSM • границы угодий пока не загружены",12,0xffb9ccc4,false);
        bottom.addView(message);
        LinearLayout buttons=horizontal();
        TextView locate=label("⌖ Моё место",15,Color.WHITE,true);
        TextView add=label("＋ Добавить точку",15,Color.WHITE,true);
        locate.setGravity(Gravity.CENTER);add.setGravity(Gravity.CENTER);add.setBackgroundColor(0xff1b6a40);
        buttons.addView(locate,new LinearLayout.LayoutParams(0,dp(48),1));
        buttons.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));
        locate.setOnClickListener(v->startLocation());
        add.setOnClickListener(v->addPoint());
        bottom.addView(buttons);
        FrameLayout.LayoutParams bottomParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        body.addView(bottom,bottomParams);
    }
    private void showPanel(String title){
        body.removeAllViews();LinearLayout page=vertical();body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        page.addView(label("‹   "+title,20,Color.WHITE,true));
        divider(page);
    }
    private LinearLayout page(){
        return (LinearLayout)body.getChildAt(0);
    }
    private void showPoints(){
        showPanel("Мои точки");
        if(points.isEmpty())page().addView(label("Пока нет сохранённых точек. Добавьте точку на карте.",15,0xffb9ccc4,false));
        for(int i=0;i<points.size();i++){
            final int idx=i;GeoPoint p=points.get(i);
            TextView item=label("📍 "+names.get(i)+"\nN "+String.format(Locale.US,"%.5f",p.getLatitude())+"   E "+String.format(Locale.US,"%.5f",p.getLongitude()),15,Color.WHITE,false);
            page().addView(item);divider(page());
            item.setOnClickListener(v->{showMap();map.getController().animateTo(points.get(idx));map.getController().setZoom(15.0);});
        }
        TextView add=label("＋ Добавить точку на карте",17,ORANGE,true);page().addView(add);add.setOnClickListener(v->showMap());
    }
    private void showRoutes(){
        showPanel("Мои маршруты");
        page().addView(label(tracking?"Запись маршрута включена":"Запись маршрута выключена",17,Color.WHITE,false));
        TextView toggle=label(tracking?"■ Остановить":"● Начать запись",17,ORANGE,true);
        page().addView(toggle);toggle.setOnClickListener(v->{tracking=!tracking;showRoutes();});
        page().addView(label("Сохранение трека и расчёт расстояния находятся в разработке.",13,0xffb9ccc4,false));
    }
    private void showDownloads(){
        showPanel("Офлайн-карты");
        page().addView(label("Оренбургская область     |     Башкортостан",16,ORANGE,true));
        page().addView(label("Загрузка карт по районам будет подключена после подготовки проверенных картографических данных.",15,Color.WHITE,false));
    }
    private void showSettings(){
        showPanel("Настройки");
        page().addView(label("ЕГЕРЬ GPS • версия 1.2 (тестовая)",17,ORANGE,true));
        page().addView(label("Тёмная тема включена",15,Color.WHITE,false));
        page().addView(label("Карты: OpenStreetMap",15,Color.WHITE,false));
    }
    private void showMenu(){
        String[] options={"Обозначения на карте","Тип карты","Информация о приложении"};
        new AlertDialog.Builder(this).setItems(options,(d,which)->{
            if(which==0)new AlertDialog.Builder(this).setTitle("Обозначения").setMessage("🟢 Общедоступные угодья\n🟠 Закреплённые угодья\n🔴 Охраняемые территории\n🔵 Моё местоположение\n📍 Мои точки\n\nГраницы угодий появятся после загрузки достоверных данных.").setPositiveButton("Закрыть",null).show();
            if(which==1)new AlertDialog.Builder(this).setTitle("Тип карты").setItems(new String[]{"OpenStreetMap","Топографическая (позже)","Спутник (позже)"},(dialog,item)->{if(item!=0)Toast.makeText(this,"Этот слой пока недоступен",Toast.LENGTH_SHORT).show();}).show();
            if(which==2)showSettings();
        }).show();
    }
    private void addPoint(){
        GeoPoint center=(GeoPoint)map.getMapCenter();
        EditText name=new EditText(this);name.setSingleLine(true);name.setText("Моя точка "+(points.size()+1));
        new AlertDialog.Builder(this).setTitle("Новая точка").setView(name)
            .setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(d,w)->{
                points.add(center);names.add(name.getText().toString());
                Marker marker=new Marker(map);marker.setPosition(center);marker.setTitle(name.getText().toString());
                map.getOverlays().add(marker);map.invalidate();
                message.setText("Точка добавлена • сохранение между запусками пока не реализовано");
            }).show();
    }
    private void startLocation(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);return;
        }
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        if(listener!=null)locationManager.removeUpdates(listener);
        listener=new LocationListener(){
            @Override public void onLocationChanged(Location loc){
                GeoPoint p=new GeoPoint(loc.getLatitude(),loc.getLongitude());
                if(position==null){position=new Marker(map);position.setTitle("Вы здесь");map.getOverlays().add(position);}
                position.setPosition(p);map.getController().setZoom(15.0);map.getController().animateTo(p);map.invalidate();
                if(message!=null)message.setText(String.format(Locale.US,"GPS: %.5f, %.5f",loc.getLatitude(),loc.getLongitude()));
            }
        };
        try{
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,5,listener);
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3000,5,listener);
            if(message!=null)message.setText("Определяем местоположение...");
        }catch(Exception e){Toast.makeText(this,"Геолокация недоступна",Toast.LENGTH_LONG).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==10&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startLocation();
    }
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
    @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
    @Override protected void onDestroy(){if(locationManager!=null&&listener!=null)locationManager.removeUpdates(listener);super.onDestroy();}
}
