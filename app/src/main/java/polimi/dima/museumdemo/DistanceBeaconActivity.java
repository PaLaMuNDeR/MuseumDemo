package polimi.dima.museumdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.List;

/**
 * Visualizes distance from beacon to the device.
 *
 * @author wiktor@estimote.com (Wiktor Gworek)
 */
public class DistanceBeaconActivity extends Activity {

    private static final String TAG = DistanceBeaconActivity.class.getSimpleName();

    // Y positions are relative to height of bg_distance image.
    private static final double RELATIVE_START_POS = 320.0 / 1110.0;
    private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;

    private BeaconManager beaconManager;
    private Beacon beacon;
    private Region region;

    private View dotView;
    private int startY = -1;
    private int segmentLength = -1;

    //The distance trigger for starting new event (meters)
    private double distanceTrigger = 0.8;
    private AnimatedGifImageView animatedArrowLeft;
    private AnimatedGifImageView animatedArrowRight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.distance_view);
        dotView = findViewById(R.id.dot);
        animatedArrowLeft = ((AnimatedGifImageView)findViewById(R.id.arrowLeft));
        animatedArrowRight = ((AnimatedGifImageView)findViewById(R.id.arrowRight));
        animatedArrowLeft.setAnimatedGif(R.raw.arrow_anim,
                AnimatedGifImageView.TYPE.AS_IS);
animatedArrowRight.setAnimatedGif(R.raw.arrow_anim,
                AnimatedGifImageView.TYPE.AS_IS);


        //Gets the image that should be the target
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(DistanceBeaconActivity.this);
        String image_resource = sp.getString("image_resource", "");
        String exponat_name = sp.getString("exponat_name", "");
        ImageView image_exponat = (ImageView) findViewById(R.id.exponatView);
        getActionBar().setTitle(exponat_name);
        getActionBar().setSubtitle("Get closer to the object to receive more information");

        //int id = getResources().getIdentifier("polimi.dima.museumdemo:drawable/" + image_resource, null, null);
        //image_exponat.setImageResource(id);
        String imagePath = Environment.getExternalStorageDirectory().toString() + "/MuseumDemo/assets/"+image_resource;
        image_exponat.setImageDrawable(Drawable.createFromPath(imagePath));
        beacon = getIntent().getParcelableExtra(ListBeaconsActivity.EXTRAS_BEACON);

        region = new Region("regionid", beacon.getProximityUUID(), beacon.getMajor(), beacon.getMinor());
        if (beacon == null) {
            Toast.makeText(this, "Beacon not found in intent extras", Toast.LENGTH_LONG).show();
            finish();
        }

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
                // Note that results are not delivered on UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Just in case if there are multiple beacons with the same uuid, major, minor.
                        Beacon foundBeacon = null;
                        for (Beacon rangedBeacon : rangedBeacons) {
                            if (rangedBeacon.getMacAddress().equals(beacon.getMacAddress())) {
                                foundBeacon = rangedBeacon;
                            }
                        }
                        if (foundBeacon != null) {
                            updateDistanceView(foundBeacon);
                        }
                    }
                });
            }
        });

        final View view = findViewById(R.id.sonar);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                startY = (int) (RELATIVE_START_POS * view.getMeasuredHeight());
                int stopY = (int) (RELATIVE_STOP_POS * view.getMeasuredHeight());
                segmentLength = stopY - startY;

                dotView.setVisibility(View.VISIBLE);
                dotView.setTranslationY(computeDotPosY(beacon));

            }
        });
        Toast.makeText(this, "Get closer to the object to receive more information", Toast.LENGTH_LONG).show();

    }

    private void updateDistanceView(Beacon foundBeacon) {
        if (segmentLength == -1) {
            return;
        }

        dotView.animate().translationY(computeDotPosY(foundBeacon)).start();

    }

    private int computeDotPosY(Beacon beacon) {
        // Let's put dot at the end of the scale when it's further than 6m.
        double distance = Math.min(Utils.computeAccuracy(beacon), 6.0);
        //If we are within 'distanceTrigger' m we will trigger an activity
        if (distance < distanceTrigger) {
            Log.e("Accuracy", "Accuracy is: " + Utils.computeAccuracy(beacon));
            Intent intent = new Intent(DistanceBeaconActivity.this, MetaioActivity.class);
            startActivity(intent);
            finish();
        }
        return startY + (int) (segmentLength * (distance / 6.0));


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(region);
                } catch (RemoteException e) {
                    Toast.makeText(DistanceBeaconActivity.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        beaconManager.disconnect();

        super.onStop();
    }
}
