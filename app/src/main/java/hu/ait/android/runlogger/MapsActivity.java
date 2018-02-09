package hu.ait.android.runlogger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;
import hu.ait.android.runlogger.data.Run;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, RunningLocationManager.NewLocationListener {


    private TextView tvDistance;
    private TextView tvSpeed;
    private TextView tvAvgPace;
    private Location lastLocation = null;
    private Button btnGo;
    private ImageButton btnLocation;
    private Boolean startRun = false;
    private Boolean firstLocQueried = false;
    private Boolean socialPref = true;
    private Boolean locPermissionGranted = false;
    private RunningLocationManager runningLocationManager;
    private GoogleMap mMap;
    private LinearLayout layoutMaps;
    private float distanceMeters;
    private int minutes, seconds;
    private static DecimalFormat df2 = new DecimalFormat(".##");
    private static String UNITS = "metric";
    private FloatingActionButton fabSendRun;
    private int pausedSec = 0;
    private int pausedMin = 0;
    private ImageView imgAttach;
    private ImageButton btnCamera;
    private ArrayList<LatLng> points;
    public Polyline line;
    private TextView timerTextView;
    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);
            minutes = (seconds / 60) + pausedMin;
            seconds = (seconds % 60) + pausedSec;
            timerTextView.setText(String.format("%d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setContent();
        setBtnGo();
        setFab();
        setBtnLoc();
        setBtnCamera();
        requestLocPermission();
        setMapFragment();
    }

    private void requestLocPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Toast...
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101);
        } else {
            runningLocationManager.startLocationMonitoring();
            locPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                runningLocationManager.startLocationMonitoring();
                locPermissionGranted = true;
            } else {
                Toast.makeText(this, "Location permission not granted. Please grant before running.", Toast.LENGTH_SHORT).show();
                btnGo.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setBtnCamera() {
        btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setClickable(false);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachClick();
            }
        });
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean cameraPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_CAMERA, true);
        if (cameraPref) {
            btnCamera.setVisibility(View.VISIBLE);
            btnCamera.setClickable(true);
        } else {
            btnCamera.setVisibility(View.GONE);
        }
    }

    private void setBtnLoc() {
        btnLocation = findViewById(R.id.btnLocation);
        btnLocation.setVisibility(View.INVISIBLE);

        btnLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (UNITS == "metric") {
                    showSnackBarMessage("Radial accuracy: " + lastLocation.getAccuracy() + "m");
                } else {
                    double accuracyInFeet = lastLocation.getAccuracy() * 3.28;
                    showSnackBarMessage("Radial accuracy: " + df2.format(accuracyInFeet) + "ft");
                }

            }
        });
    }

    private void setFab() {
        fabSendRun = (FloatingActionButton) findViewById(R.id.fabSendRun);
        fabSendRun.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (socialPref) {
                    if (imgAttach.getVisibility() == View.GONE) {
                        if((pausedMin !=0) || (pausedSec !=0)){
                            uploadRun();
                        }else{
                            Toast.makeText(MapsActivity.this, "Please run a little distance before submitting.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        try {
                            uploadRunWithImage();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "Run finished! To share your next run, " +
                            "enable social running in Settings.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void setBtnGo() {
        btnGo = findViewById(R.id.btnGo);
        btnGo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startRun = true;
                distanceMeters = 0;
                Button b = (Button) view;
                if (b.getText().equals("Pause")) {
                    startRun = false;
                    pausedMin = minutes;
                    pausedSec = seconds;
                    fabSendRun.setVisibility(View.VISIBLE);
                    timerHandler.removeCallbacks(timerRunnable);
                    b.setText(R.string.btn_go_run);
                } else {
                    startRun = true;
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    b.setText(R.string.btn_pause_run);
                    fabSendRun.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void setContent() {
        runningLocationManager = new RunningLocationManager(this, this);
        timerTextView = findViewById(R.id.timerTextView);
        tvDistance = findViewById(R.id.tvDistance);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvAvgPace = findViewById(R.id.tvAvgPace);
        imgAttach = findViewById(R.id.imgAttach);
        layoutMaps = findViewById(R.id.layoutMaps);
        points = new ArrayList<LatLng>();
    }

    private void uploadRun(String... imageUrl) {
        String key = FirebaseDatabase.getInstance().getReference().child("posts").push().getKey();
        Run newRun = new Run(
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), tvDistance.getText().toString() + " run",
                "Distance: " + tvDistance.getText().toString() + ",\nAverage Pace: " + tvAvgPace.getText().toString());

        if (imageUrl != null && imageUrl.length > 0) {
            newRun.setImgUrl(imageUrl[0]);
        }

        FirebaseDatabase.getInstance().getReference().child("posts").child(key).setValue(newRun).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(MapsActivity.this, "Run finished!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    public void uploadRunWithImage() throws Exception {
        imgAttach.setDrawingCacheEnabled(true);
        imgAttach.buildDrawingCache();
        Bitmap bitmap = imgAttach.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageInBytes = baos.toByteArray();

        StorageReference newImageImagesRef = getStorageReference();

        UploadTask uploadTask = newImageImagesRef.putBytes(imageInBytes);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(MapsActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                uploadRun(taskSnapshot.getDownloadUrl().toString());
            }
        });
    }

    @NonNull
    private StorageReference getStorageReference() throws UnsupportedEncodingException {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String newImage = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg";
        StorageReference newImageRef = storageRef.child(newImage);
        StorageReference newImageImagesRef = storageRef.child("images/" + newImage);
        newImageRef.getName().equals(newImageImagesRef.getName());
        newImageRef.getPath().equals(newImageImagesRef.getPath());
        return newImageImagesRef;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setTrafficEnabled(true);
    }

    public void showSnackBarMessage(String message) {
        Snackbar.make(layoutMaps,
                message,
                Snackbar.LENGTH_LONG
        ).setAction("Hide", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //...
            }
        }).show();
    }

    private void changeMapView() {
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void redrawLine() {
        mMap.clear();

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        line = mMap.addPolyline(options);
    }

    public void attachClick() {
        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intentCamera, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 101 && resultCode == RESULT_OK) {
            Bitmap img = (Bitmap) data.getExtras().get("data");
            imgAttach.setImageBitmap(img);
            imgAttach.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNewLocation(Location location) {

        if (firstLocQueried) {
            distanceMeters += Math.abs(location.distanceTo(lastLocation));
            btnLocation.setVisibility(View.VISIBLE);
            setPreferences();
        }
        lastLocation = location;
        firstLocQueried = true;

        if (startRun) {
            changeMapView();
            if (UNITS == "metric") {
                setMetric(location);
            } else {
                setImperial(location);
            }
            addToPolyLine(location);
        }
    }

    private void addToPolyLine(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        points.add(latLng);
        redrawLine();
    }

    private void setImperial(Location location) {
        double pace = 26.8224 / location.getSpeed();
        int secPace = (int) (60 * (pace % 1.0));
        int minPace = (int) pace;
        if (minPace <= 20) {
            tvSpeed.setText(String.format("%s Min/Mi", String.format(" %d:%02d", minPace, secPace)));
        }
        double dist = (double) distanceMeters / 1609.34;
        tvDistance.setText(String.format(" %s Mi", df2.format(dist)));
        double avgPace = seconds / dist;
        int avgMin = (int) (avgPace / 60);
        int avgSec = (int) (avgPace % 60);
        if(avgMin <= 20){
            tvAvgPace.setText(String.format("%s Min/Mi", String.format(" %d:%02d", avgMin, avgSec)));
        }
    }

    private void setMetric(Location location) {
        double pace = 16.6666667 / location.getSpeed();
        int minPace = (int) pace;
        int secPace = (int) (60 * (pace % 1.0));
        if (minPace <= 30) {
            tvSpeed.setText(String.format(" %d:%02d", minPace, secPace) + " Min/Km");
        }
        double dist = (double) distanceMeters / 1000;
        tvDistance.setText(String.format(" %s Km", df2.format(dist)));
        double avgPace = seconds / dist;
        int avgMin = (int) (avgPace / 60);
        int avgSec = (int) (avgPace % 60);
        if(avgMin<= 20){
            tvAvgPace.setText(String.format(" %d:%02d", avgMin, avgSec) + " Min/Km");
        }
    }

    private void setPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean unitsPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_UNITS, true);
        socialPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SOCIAL, true);
        if (unitsPref) {
            UNITS = "imperial";
        } else {
            UNITS = "metric";
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("This run will be deleted if you exit.")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        MapsActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        btnGo.setText(R.string.btn_go_run);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locPermissionGranted) {
            runningLocationManager.stopLocationMonitoring();
        }
    }
}
