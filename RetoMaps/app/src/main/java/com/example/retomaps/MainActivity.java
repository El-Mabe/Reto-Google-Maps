package com.example.retomaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap nMap;

    private TextView texto;
    private double latitud;
    private  double longitud;

    private LocationManager locationM;

    private ArrayList<MarkerOptions> marcadores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        marcadores = new ArrayList<MarkerOptions>();
        texto = findViewById(R.id.texto);
    }

    public double calcularDistancia(double lat1, double ln1, double lat2, double ln2){
        double radioTierra = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(ln2-ln1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (radioTierra * c);

        dist = Math.round(dist);
        Log.d("Lat1", lat1+"");
        Log.d("Long1", ln1+"");

        return dist;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        nMap = googleMap;

        //si tengo permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED  ){
            //Encuentra mi ubicación
            nMap.setMyLocationEnabled(true);

            //Boton para encontrarme
            nMap.getUiSettings().setMyLocationButtonEnabled(true);

            locationM = (LocationManager) getSystemService(LOCATION_SERVICE);

            //Recuerda mi ultima ubicación
            locationM.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            LocationListener locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    latitud = location.getLatitude();
                    longitud = location.getLongitude();
                    nMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitud, longitud), 18));
                    MarkerOptions elMasCercano = calcularMasCercano();
                    if (elMasCercano != null){
                        texto.setText("El lugar más cercano es: " + elMasCercano.getTitle());
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            locationM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1 , locationListener);

            nMap.setOnMapLongClickListener((latLng)->{

                Geocoder geoCoder = new Geocoder(MainActivity.this);
                List<Address> direcciones = null;
                try {
                    direcciones = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                }catch (IOException e){
                    e.printStackTrace();
                }
                MarkerOptions marker = new MarkerOptions().position(latLng).title(""+direcciones.get(0).getAddressLine(0));
                nMap.addMarker(marker).setTitle(""+direcciones.get(0).getAddressLine(0));
                marcadores.add(marker);
                MarkerOptions elMasCercano= calcularMasCercano();
                if (elMasCercano != null) {
                    if (calcularDistancia(latitud,longitud,elMasCercano.getPosition().latitude,elMasCercano.getPosition().longitude)<= 100){
                        texto.setText("Usted está en: " +"\n"+elMasCercano.getTitle());
                    }else{
                        texto.setText("El lugar más cercano es: " +"\n"+elMasCercano.getTitle());
                    }

                }

            });

            nMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {

                @Override
                public void onMyLocationClick(@NonNull Location location) {

                    Geocoder geoCoder = new Geocoder(MainActivity.this);
                    List<Address> direciones= null;
                    try {
                        direciones= geoCoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    AlertDialog.Builder alerta = new AlertDialog.Builder(MainActivity.this);
                    alerta.setTitle("Su abicación");
                    alerta.setMessage("Usted está en: "+ direciones.get(0).getAddressLine(0));
                    alerta.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alerta.show();

                }
            });
            nMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    double calculo=calcularDistancia(latitud, longitud, marker.getPosition().latitude, marker.getPosition().longitude);
                    marker.setSnippet(calculo+" metros");
                    return false;
                }
            });
        }else{
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 99);
        }

    }
    public MarkerOptions calcularMasCercano() {

        double minDistancia = Double.MAX_VALUE;
        LatLng cali = new LatLng(3.42158, -76.5205);
        nMap.addMarker(new MarkerOptions().position(cali).title("Marker in Cali"));
        MarkerOptions esteMarcador = new MarkerOptions().position(cali);

        for (int i = 0; i < marcadores.size(); i++) {
            if (calcularDistancia(latitud, longitud, marcadores.get(i).getPosition().latitude, marcadores.get(i).getPosition().longitude) < minDistancia) {
                minDistancia = calcularDistancia(latitud, longitud, marcadores.get(i).getPosition().latitude, marcadores.get(i).getPosition().longitude);
                esteMarcador = marcadores.get(i);

            }
        }

        return esteMarcador;
    }

}
