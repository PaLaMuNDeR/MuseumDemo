package polimi.dima.museumdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.utils.L;
import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Displays list of found beacons sorted by RSSI.
 * Starts new activity with selected beacon if activity was provided.
 *
 * @author wiktorgworek@google.com (Wiktor Gworek)
 */
public class ListBeaconsActivity extends Activity {

    private static final String TAG = ListBeaconsActivity.class.getSimpleName();
    public static final String EXTRAS_BEACON = "extrasBeacon";
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private BeaconManager beaconManager;
    private LeDeviceListAdapter adapter;
    AssetsExtracter mTask;
    CheckVersionExtracter mCheckVersion;
    RunDownload mDownload;
    // Progress Dialog
    private ProgressDialog pDialog;

    ProgressDialog myPd_bar;
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    private ArrayList<HashMap<String, String>> mExponatsList;

    private JSONArray mExponats = null;
    private static final String READ_POI_URL = "http://expox-milano.t15.org/museum/MetaioDownload/exponats.json";

    private static final String TAG_NAME = "name";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_IMAGE = "image";
    private static final String TAG_BEACON_MAC = "beaconMac";
    private static final String TAG_TRACKING_DATA = "trackingData";
    private static final String TAG_TARGET = "target";
    private static final String TAG_TYPE = "type";
    private static final String TAG_MODEL = "model";
    private static final String TAG_MODEL_1 = "model_1";
    private static final String TAG_MODEL_2 = "model_2";
    private static final String TAG_MODEL_3 = "model_3";
    private static final String TAG_MODEL_4 = "model_4";

    private static final String TAG_SAVE_TARGET = "save_target";
    private static final String TAG_SAVE_IMAGE = "save_image";
    private static final String TAG_SAVE_TRACKING_DATA = "save_trackingData";
    private static final String TAG_SAVE_MODEL_1 = "save_model_1";
    private static final String TAG_SAVE_MODEL_2 = "save_model_2";
    private static final String TAG_SAVE_MODEL_3 = "save_model_3";
    private static final String TAG_SAVE_MODEL_4 = "save_model_4";

    private Boolean newVersion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Check the Version
        mCheckVersion = new CheckVersionExtracter();
        mCheckVersion.execute(0);

        // Configure device list.
        adapter = new LeDeviceListAdapter(ListBeaconsActivity.this);
        ListView list = (ListView) findViewById(R.id.device_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(createOnItemClickListener());

        // Configure verbose debug logging.
        L.enableDebugLogging(true);

        // Configure BeaconManager.
        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                // Note that results are not delivered on UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Note that beacons reported here are already sorted by estimated
                        // distance between device and beacon.
                        if (beacons.size() == 1)
                            getActionBar().setSubtitle("Found " + beacons.size() + " exponat");
                        if (beacons.size() > 1)
                            getActionBar().setSubtitle("Found " + beacons.size() + " exponats");
                        if (beacons.size() == 0)
                            getActionBar().setSubtitle(R.string.scanning);
                        adapter.replaceWith(beacons);
                    }
                });
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        MenuItem refreshItem = menu.findItem(R.id.refresh);
        refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }

    @Override
    protected void onStop() {
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging", e);
        }

        super.onStop();
    }

    //Assets Extraction
    private class CheckVersionExtracter extends AsyncTask<Integer, Integer, Boolean> {

        //   @Override
        //  protected void onPreExecute()
        // {
        //Create a new progress dialog or something on PreExecute
        //      }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                // Extract all assets except Menu. Overwrite existing files for debug build only.
                VersionCheck();
            } catch (Exception e) {
                Log.e("Database", "Version Check failed. May be the server is down");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("Version Check", "newVersion=" + newVersion);
            if (newVersion) {
                updateDialog();
            }

        }
    }

    //Assets Extraction
    private class AssetsExtracter extends AsyncTask<Integer, Integer, Boolean> {

        //   @Override
        //  protected void onPreExecute()
        // {
        //Create a new progress dialog or something on PreExecute
        //      }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                // Extract all assets except Menu. Overwrite existing files for debug build only.
                AssetsManager.extractAllAssets(getApplicationContext(), "", BuildConfig.DEBUG);
            } catch (IOException e) {
                MetaioDebug.printStackTrace(Log.ERROR, e);
                return false;
            }
            try {
                JSONParserToDB();
            } catch (Exception e) {
                Log.e("Error with the JSON Parser",
                        "Error when parsing the JSON, may be it is not formatted properly.");
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MetaioDebug.log(Log.ERROR, "Error extracting assets, closing the application...");
                showToast("Error extracting assets, closing the application...");
                finish();
            } else {
                mDownload = new RunDownload();
                mDownload.execute(0);
            }


        }
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setMax(100);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }


    /**
     * Display a short toast message
     *
     * @param message Message to display
     */
    private void showToast(final String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
                getActionBar().setSubtitle("Bluetooth not enabled");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        getActionBar().setTitle(R.string.welcome);
        getActionBar().setSubtitle(R.string.scanning);
        adapter.replaceWith(Collections.<Beacon>emptyList());
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(ListBeaconsActivity.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    private AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //The activity is changed only if the beacon is registered.
                //The boolean variable is set in LeDeviceListAdapter.java
                if (adapter.getContinueBool(view)) {
                    Intent intent = new Intent(ListBeaconsActivity.this, DistanceBeaconActivity.class);
                    intent.putExtra(EXTRAS_BEACON, adapter.getItem(position));
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(ListBeaconsActivity.this);
                    SharedPreferences.Editor edit = sp.edit();
                    Log.d("image", adapter.getImage(view));
                    edit.putInt("exponat_id", adapter.getExponatId(view));
                    edit.putString("exponat_name", adapter.getExponatName(view));
                    edit.putString("image_resource", adapter.getImage(view));
                    Log.d("database", "exponat_id=" + adapter.getExponatId(view));
                    edit.commit();

                    startActivity(intent);
                } else {
                    showToast("This device is not registered, please choose another.");
                }
            }
        };
    }

    private void VersionCheck() {
        DatabaseHandler db = new DatabaseHandler(ListBeaconsActivity.this);
        JSONParser jParser = new JSONParser();
        // Feed the beast our comments url, and it spits us
        // back a JSON object. Boo-yeah Jerome.
        JSONObject json = jParser.getJSONFromUrl(READ_POI_URL);
        try {
            int version = json.getInt("version");
            Log.d("Database", "JSON version: " + version);
            VersionVerifier vf = db.getLastVersion();
            Log.d("Database", "Old version: " + vf.version);

            if (version != vf.version) {
                Log.d("Database", "Version Check is complete. The version is different");

                newVersion = true;
                Log.d("Version Check", "newVersion=" + newVersion);
            } else {
                Log.d("Database", "Version Check is complete. The version is NOT different");
                newVersion = false;
                Log.d("Version Check", "newVersion=" + newVersion);

            }
        } catch (Exception e) {
            Log.e("Database", "Error 2. Could not check the version");
        }
    }

    private void updateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Update")
                .setMessage("The museum has new exponats. Do you want to update the app?")
                .setIcon(R.drawable.ic_launcher)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // extract all the assets
                        mTask = new AssetsExtracter();
                        mTask.execute(0);

                        //Yes button clicked, do something
                        Toast.makeText(ListBeaconsActivity.this, "Yes button pressed",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                        //Do nothing on no
                .show();
    }


    private void JSONParserToDB() {
        DatabaseHandler db = new DatabaseHandler(ListBeaconsActivity.this);

        mExponatsList = new ArrayList<HashMap<String, String>>();

        // it's time to power up the J parser
        JSONParser jParser = new JSONParser();
        JSONObject json = jParser.getJSONFromUrl(READ_POI_URL);
        try {
            boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
            int version = json.getInt("version");
            if (isFirstRun) {
                mExponats = json.getJSONArray("exponats");
                for (int i = 0; i < mExponats.length(); i++) {
                    JSONObject c = mExponats.getJSONObject(i);

                    // gets the content of each tag
                    String name = c.getString(TAG_NAME);
                    String description = c.getString(TAG_DESCRIPTION);
                    String image = c.getString(TAG_IMAGE);
                    String beaconMac = c.getString(TAG_BEACON_MAC);
                    String trackingData = c.getString(TAG_TRACKING_DATA);
                    String target = c.getString(TAG_TARGET);
                    String type = c.getString(TAG_TYPE);
                    String model = c.getString(TAG_MODEL);

                    Log.d("Database", "Inserting...");
                    db.addExponat(new Exponat(name, description, image, beaconMac, trackingData, target, type, model));
                    // creating new HashMap
                    HashMap<String, String> map = new HashMap<String, String>();

                    // map.put(TAG_POI_ID, poi_id);
                    map.put(TAG_NAME, name);
                    map.put(TAG_DESCRIPTION, description);
                    map.put(TAG_IMAGE, image);
                    map.put(TAG_BEACON_MAC, beaconMac);
                    map.put(TAG_TRACKING_DATA, trackingData);
                    map.put(TAG_TARGET, target);
                    map.put(TAG_TYPE, type);
                    map.put(TAG_MODEL, model);

                    // adding HashList to ArrayList
                    mExponatsList.add(map);

                    // annndddd, our JSON data is up to date same with our array
                    // list
                    Log.d("hashmap", "One more added");
                }
                db.addVersion(new VersionVerifier(version));
                Log.d("Database", "New version added: " + version);
            } else {
                Log.d("Database", "JSON version: " + version);
                VersionVerifier vf = db.getLastVersion();
                Log.d("Database", "Old version: " + vf.version);

                if (version != vf.version) {

                    //Cleans the database
                    db.flushOnNewVersion();
                    Log.d("Database", "Database flushed");
                    //populates it again
                    mExponats = json.getJSONArray("exponats");
                    for (int i = 0; i < mExponats.length(); i++) {
                        JSONObject c = mExponats.getJSONObject(i);

                        // gets the content of each tag
                        String name = c.getString(TAG_NAME);
                        String description = c.getString(TAG_DESCRIPTION);
                        String image = c.getString(TAG_IMAGE);
                        String beaconMac = c.getString(TAG_BEACON_MAC);
                        String trackingData = c.getString(TAG_TRACKING_DATA);
                        String target = c.getString(TAG_TARGET);
                        String type = c.getString(TAG_TYPE);
                        String model = c.getString(TAG_MODEL);


                        Log.d("Database", "Inserting...");
                        db.addExponat(new Exponat(name, description, image, beaconMac, trackingData, target, type, model));
                        // creating new HashMap
                        HashMap<String, String> map = new HashMap<String, String>();

                        map.put(TAG_NAME, name);
                        map.put(TAG_DESCRIPTION, description);
                        map.put(TAG_IMAGE, image);
                        map.put(TAG_BEACON_MAC, beaconMac);
                        map.put(TAG_TRACKING_DATA, trackingData);
                        map.put(TAG_TARGET, target);
                        map.put(TAG_TYPE, type);
                        map.put(TAG_MODEL, model);

                        // adding HashList to ArrayList
                        mExponatsList.add(map);

                        // annndddd, our JSON data is up to date same with our array
                        // list
                        Log.d("hashmap", "One more added");
                    }
                    db.addVersion(new VersionVerifier(version));
                    Log.d("Database", "New version added: " + version);
                }
            }
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.d("hashmap", mExponatsList.toString());
        // Reading all exponats
        Log.d("Database", "Reading all exponats..");
        List<Exponat> exponats = db.getAllExponats();
        for (Exponat ex : exponats) {
            String log = "Id: " + ex.getId() + " ,Name: " + ex.getName() + " ,MAC: " + ex.getBeaconMac();
            // Writing Exponats to log
            Log.d("Database ", log);
            db.close();
        }
    }

    //Assets Extraction
    private class RunDownload extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            //Create a new progress dialog or something on PreExecute
            super.onPreExecute();
            myPd_bar = new ProgressDialog(ListBeaconsActivity.this);
            myPd_bar.setMessage("Collecting all the exponats. We are almost done...");
            myPd_bar.setTitle("Downloading...");
            myPd_bar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myPd_bar.setCancelable(true);
            myPd_bar.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                // Extract all assets except Menu. Overwrite existing files for debug build only.

                Log.d("Download", "start");

                JSONParser jParser = new JSONParser();
                JSONObject json = jParser.getJSONFromUrl(READ_POI_URL);
                try {
                    mExponats = json.getJSONArray("download_resources");
                    for (int i = 0; i < mExponats.length(); i++) {
                        JSONObject c = mExponats.getJSONObject(i);

                        // gets the content of each tag
                        String image = c.getString(TAG_IMAGE);
                        String save_image = c.getString(TAG_SAVE_IMAGE);
                        Log.d("Download", "Downloading image...");
                        DownloadFileFromURL(save_image, image);


                        String trackingData = c.getString(TAG_TRACKING_DATA);
                        String save_trackingData = c.getString(TAG_SAVE_TRACKING_DATA);
                        Log.d("Download", "Download Tracking");
                        DownloadFileFromURL(save_trackingData, trackingData);

                        String target = c.getString(TAG_TARGET);
                        String save_target = c.getString(TAG_SAVE_TARGET);
                        Log.d("Download", "Download Target");
                        DownloadFileFromURL(save_target, target);

                        String model_1 = c.getString(TAG_MODEL_1);
                        String save_model_1 = c.getString(TAG_SAVE_MODEL_1);
                        Log.d("Download", "Download Model 1");
                        DownloadFileFromURL(save_model_1, model_1);

                        String model_2 = c.getString(TAG_MODEL_2);
                        String save_model_2 = c.getString(TAG_SAVE_MODEL_2);
                        Log.d("Download", "Download Model 2");
                        DownloadFileFromURL(save_model_2, model_2);

                        String model_3 = c.getString(TAG_MODEL_3);
                        String save_model_3 = c.getString(TAG_SAVE_MODEL_3);
                        Log.d("Download", "Download Model 3");
                        DownloadFileFromURL(save_model_3, model_3);

                        String model_4 = c.getString(TAG_MODEL_4);
                        String save_model_4 = c.getString(TAG_SAVE_MODEL_4);
                        Log.d("Download", "Download Model 4");
                        DownloadFileFromURL(save_model_4, model_4);

                    }
                } catch (Exception e) {
                    Log.e("Download", "Error 1. Error in Download Resources");
                }


            } catch (Exception e) {
                Log.e("Download", "Error 0. Error when run");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            myPd_bar.dismiss();
        }
    }


    protected String DownloadFileFromURL(String saved, String... f_url) {
        int count;
        try {

            URL url = new URL(f_url[0]);
            URLConnection conection = url.openConnection();
            conection.connect();
            // getting file length
            int lenghtOfFile = conection.getContentLength();

            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            //Check whether such folder already exists
            File folder = new File(Environment.getExternalStorageDirectory() + "/MuseumDemo/assets");

            // Output stream to write file
            OutputStream output = new FileOutputStream("/sdcard/MuseumDemo/assets/" + saved);

            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called

                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();
            Log.d("Download", "Download was successful");
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

        return null;

    }
}