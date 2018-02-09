package hu.ait.android.runlogger;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class RunningLocationManager implements LocationListener {

    public interface NewLocationListener {
        public void onNewLocation(Location location);
    }

    private Context context;
    private NewLocationListener newLocationListener;
    private LocationManager locationManager;

    public RunningLocationManager(Context context, NewLocationListener newLocationListener) {
        this.context = context;
        this.newLocationListener = newLocationListener;
    }

    public void startLocationMonitoring() throws SecurityException {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 15, this);

        //DO NOT USE THIS ON THE EMULATOR
        // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

    }

    public void stopLocationMonitoring() {
        locationManager.removeUpdates(this);
    }


    @Override
    public void onLocationChanged(Location location) {
        newLocationListener.onNewLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
