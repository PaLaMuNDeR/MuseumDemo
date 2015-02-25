package polimi.dima.museumdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Marti on 23/02/2015.
 */
public class WelcomeActivity extends Activity {

    // button to show progress dialog
    Button btnLoadModel;
    Button btnLoadTarget;
    Button btnLoadXML;
    Button btnLoadImage;
    Button btnContinue;

    ImageView my_image;
    // Progress Dialog
    private ProgressDialog pDialog;
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;
    //The JSON file with the version
   // private static String

    // File url to download
    private static String image_url = "http://expox-milano.t15.org/museum/MetaioDownload/exponat_1_image.jpg";
    private static String xml_url = "http://expox-milano.t15.org/museum/MetaioDownload/TrackingData_MarkerlessFast.xml";
    private static String target_url = "http://expox-milano.t15.org/museum/MetaioDownload/mona_target.png";
    private static String model_url = "http://expox-milano.t15.org/museum/MetaioDownload/mona_rotated.3gp";
    private static String image_name = "exponat_1_image.jpg";
    private static String xml_name = "TrackingData_MarkerlessFast.xml";
    private static String target_name = "mona_target.png";
    private static String model = "mona_rotated.3gp";
    private static final String READ_POI_URL = "http://expox-milano.t15.org/museum/MetaioDownload/exponats.json";
private Boolean newVersion = false;
    private static String save_name = "";
    CheckVersionExtracter mCheckVersion;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.welcome_activity);
// extract all the assets
            mCheckVersion = new CheckVersionExtracter();
            mCheckVersion.execute(0);

            // show progress bar button
            btnLoadImage = (Button) findViewById(R.id.btnLoadImage);
            btnLoadXML = (Button) findViewById(R.id.btnLoadXML);
            btnLoadTarget = (Button) findViewById(R.id.btnLoadTarget);
            btnLoadModel = (Button) findViewById(R.id.btnLoadModel);
            // Image view to show image after downloading
            my_image = (ImageView) findViewById(R.id.my_image);
            /**
             * Show Progress bar click event
             * */
            btnLoadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // starting new Async Task
                    save_name = image_name;
                    new DownloadFileFromURL().execute(image_url);
                }
            });
            btnLoadXML.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // starting new Async Task
                    save_name = xml_name;
                    new DownloadFileFromURL().execute(xml_url);
                }
            });
            btnLoadTarget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // starting new Async Task
                    save_name = target_name;
                    new DownloadFileFromURL().execute(target_url);
                }
            });

            btnLoadModel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // starting new Async Task
                    save_name = model;
                    new DownloadFileFromURL().execute(model_url);

                }
            });




            btnContinue = (Button) findViewById(R.id.btnContinue);
            btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(WelcomeActivity.this, ListBeaconsActivity.class);
                    startActivity(intent);
                }
            });
         /*   Log.d("Version Check","newVersion="+newVersion);
            if(newVersion){
                updateDialog();
            }
        */}
                    /**
                     * Showing Dialog
                     * */
            @Override
        protected Dialog onCreateDialog(int id) {
            switch (id) {
                case progress_bar_type:
                    pDialog = new ProgressDialog(this);
                    pDialog.setMessage("Downloading file. Please wait...");
                    pDialog.setIndeterminate(false);
                    pDialog.setMax(100);
                    pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pDialog.setCancelable(true);
                    pDialog.show();
                    return pDialog;
                default:
                    return null;
            }
        }

        /**
         * Background Async Task to download file
         * */
        class DownloadFileFromURL extends AsyncTask<String, String, String> {

            /**
             * Before starting background thread
             * Show Progress Bar Dialog
             * */
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showDialog(progress_bar_type);
            }

            /**
             * Downloading file in background thread
             * */
            @Override
            protected String doInBackground(String... f_url) {
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
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    // Output stream to write file
                    OutputStream output = new FileOutputStream("/sdcard/MuseumDemo/assets/"+save_name);

                    byte data[] = new byte[1024];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        // publishing the progress....
                        // After this onProgressUpdate will be called
                        publishProgress(""+(int)((total*100)/lenghtOfFile));

                        // writing data to file
                        output.write(data, 0, count);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }

                return null;
            }

            /**
             * Updating progress bar
             * */
            protected void onProgressUpdate(String... progress) {
                // setting progress percentage
                pDialog.setProgress(Integer.parseInt(progress[0]));
            }

            /**
             * After completing background task
             * Dismiss the progress dialog
             * **/
            @Override
            protected void onPostExecute(String file_url) {
                // dismiss the dialog after the file was downloaded
                dismissDialog(progress_bar_type);

                // Displaying downloaded image into image view
                // Reading image path from sdcard
                String imagePath = Environment.getExternalStorageDirectory().toString() + "/MuseumDemo/assets/"+save_name;
                // setting downloaded into image view
              //  my_image.setImageDrawable(Drawable.createFromPath(imagePath));

            }

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
                Log.e("Database","Version Check failed. May be the server is down");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("Version Check","newVersion="+newVersion);
            if(newVersion){
                updateDialog();
            }

        }
    }
    private void VersionCheck() {
        DatabaseHandler db = new DatabaseHandler(WelcomeActivity.this);
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
                Log.d("Database","Version Check is complete. The version is different");

            newVersion=true;
                Log.d("Version Check","newVersion="+newVersion);
            }
            else{
                Log.d("Database","Version Check is complete. The version is NOT different");
            newVersion=false;
                Log.d("Version Check","newVersion="+newVersion);

            }
        } catch (Exception e) {
            Log.e("Database", "Error 2. Could not check the version");
        }
    }
   private void updateDialog(){
       AlertDialog.Builder builder = new AlertDialog.Builder(this);
       builder
               .setTitle("Erase hard drive")
               .setMessage("Are you sure?")
               .setIcon(android.R.drawable.ic_dialog_alert)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       //Yes button clicked, do something
                       Toast.makeText(WelcomeActivity.this, "Yes button pressed",
                               Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("No", null)						//Do nothing on no
               .show();
    }
    }