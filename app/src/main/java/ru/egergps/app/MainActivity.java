package ru.egergps.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends Activity {
    private MapView map;
    private TextView status;
    private LocationManager locationManager;
    private LocationListener listener;
    private Marker position;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 57, 37));

        TextView title = new TextView(this);
        title.setText("ЕГЕРЬ GPS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(16, 24, 16, 16);
        root.addView(title);

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(8.0);
        map.getController().setCenter(new GeoPoint(52.0, 55.5));
        root.addView(map, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(8, 8, 8, 8);
        Button locate = new Button(this);
        locate.setText("Моё местоположение");
        controls.addView(locate, new LinearLayout.LayoutParams(0, -2, 1));
        Button point = new Button(this);
        point.setText("Добавить точку");
        controls.addView(point, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls);

        status = new TextView(this);
        status.setText("Карта OSM. Границы угодий пока не загружены.");
        status.setTextColor(Color.WHITE);
        status.setPadding(12, 8, 12, 18);
        root.addView(status);
        setContentView(root);

        locate.setOnClickListener(v -> startLocation());
        point.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) map.getMapCenter();
            Marker marker = new Marker(map);
            marker.setPosition(center);
            marker.setTitle("Путевая точка");
            map.getOverlays().add(marker);
            map.invalidate();
            status.setText("Точка добавлена (пока без сохранения)");
        });
    }

    private void startLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                GeoPoint p = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                if (position == null) {
                    position = new Marker(map);
                    position.setTitle("Вы здесь");
                    map.getOverlays().add(position);
                }
                position.setPosition(p);
                map.getController().setZoom(15.0);
                map.getController().animateTo(p);
                map.invalidate();
                status.setText(String.format(java.util.Locale.US, "GPS: %.5f, %.5f", loc.getLatitude(), loc.getLongitude()));
            }
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, listener);
            status.setText("Ожидание GPS...");
        } catch (Exception e) {
            status.setText("GPS недоступен: " + e.getMessage());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 10 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startLocation();
    }
    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override protected void onPause() { if (map != null) map.onPause(); super.onPause(); }
    @Override protected void onDestroy() {
        if (locationManager != null && listener != null) locationManager.removeUpdates(listener);
        super.onDestroy();
    }
}
