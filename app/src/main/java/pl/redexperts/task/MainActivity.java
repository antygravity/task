package pl.redexperts.task;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import static pl.redexperts.task.utils.MixUtils.formatDist;

public class MainActivity extends Activity {

    private GoogleMap googleMap;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    private Map<String, String> markers = new HashMap<>();

    private Marker currentMarker;

    private final static String TAG = "Task-MainActivity";

    private static final String JSON_ENDPOINT = "https://dl.dropboxusercontent.com/u/6556265/test.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initImageLoader();

        checkLocationsEnabled();

        checkInternetConnectionEnabled();

        // Loading map
        initializeMap();

        loadLocationFromJson();
    }

    private void loadLocationFromJson() {
        try {
            URL[] url = new URL[1];
            url[0] = new URL(JSON_ENDPOINT);
            new JsonDataAsyncTask().execute(url);

        } catch (Exception e) {
            Log.e(TAG, "Exception caught:", e);
        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initializeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_google_maps), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServicesAvailability();
        initializeMap();
        loadLocationFromJson();
    }

    private class JsonDataAsyncTask extends AsyncTask<URL, Void, LocationData> {

        @Override
        protected LocationData doInBackground(URL... url) {

            LocationData locationData = null;

            ObjectMapper mapper = new ObjectMapper();
            try {
                locationData = mapper.readValue(url[0], LocationData.class);
                Log.i(TAG, locationData.toString());
            } catch (IOException e) {
                Log.e(TAG, "Error while read JSON data from url:" + url[0] + ", ", e);
            }

            return locationData;
        }

        @Override
        protected void onPostExecute(final LocationData locationData) {
            if (locationData != null && locationData.getLocation() != null && googleMap != null) {
                googleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

                final LatLng remoteLng = new LatLng(locationData.getLocation().getLatitude(), locationData.getLocation().getLongitude());
                MarkerOptions markerOptions = new MarkerOptions().position(remoteLng).title(locationData.getText()).flat(true);

                // adding remote marker
                Marker marker = googleMap.addMarker(markerOptions);
                markers.put(marker.getId(), locationData.getImage());

                googleMap.setMyLocationEnabled(true);

                // adding local marker
                googleMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {

                    @Override
                    public void onMyLocationChange(android.location.Location location) {
                        LatLng myLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.addMarker(new MarkerOptions().position(myLng).title(getResources().getString(R.string.my_location)));

                        double distance = SphericalUtil.computeDistanceBetween(remoteLng, myLng);
                        TextView distanceView = (TextView) findViewById(R.id.text_distance);
                        distanceView.setText(getResources().getString(R.string.distance) + formatDist(distance));

                        zoomToMarkers(myLng, remoteLng);
                    }
                });
            }
        }
    }

    private void zoomToMarkers(LatLng myLng, LatLng remoteLng) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(remoteLng);
        builder.include(myLng);
        LatLngBounds bounds = builder.build();

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
    }


    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        public View view;

        CustomInfoWindowAdapter() {
            view = getLayoutInflater().inflate(R.layout.custom_info_window, null, false);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            if (currentMarker != null
                    && currentMarker.isInfoWindowShown()) {
                currentMarker.hideInfoWindow();
                currentMarker.showInfoWindow();
            }

            return null;
        }

        @Override
        public View getInfoContents(final Marker marker) {
            currentMarker = marker;

            String url = markers.get(marker.getId());

            final ImageView image = (ImageView) view.findViewById(R.id.location_image);

            if (url != null) {
                imageLoader.displayImage(url, image,
                        new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri,
                                                          View view, Bitmap loadedImage) {
                                super.onLoadingComplete(imageUri, view,
                                        loadedImage);
                                getInfoWindow(marker);
                            }
                        });
            } else {
                image.setImageResource(R.drawable.ic_launcher);
            }

            if (marker.getTitle() != null) {
                TextView textView = (TextView) view.findViewById(R.id.title);
                textView.setText(marker.getTitle());
            }

            return view;
        }
    }

    private void initImageLoader() {
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config);
    }

    public void checkGooglePlayServicesAvailability() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 69);
            dialog.setCancelable(false);
            dialog.show();
        }

        Log.d("GooglePlayServicesUtil Check", "Result is: " + resultCode);
    }

    public void checkLocationsEnabled() {
        // Get Location Manager and check for GPS & Network location services
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.alert_title_location));
            builder.setMessage(getResources().getString(R.string.alert_message_location));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });

            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    private void checkInternetConnectionEnabled() {
        if (!isNetworkAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.alert_title_internet));
            builder.setMessage(getResources().getString(R.string.alert_message_internet));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                }
            });

            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}