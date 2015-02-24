package polimi.dima.museumdemo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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
    Button btnShowProgress;

    // Progress Dialog
    private ProgressDialog pDialog;
    ImageView my_image;
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    // File url to download
    private static String image_url = "http://expox-milano.t15.org/museum/MetaioDownload/exponat_1_image.jpg";
    private static String xml_url = "http://expox-milano.t15.org/museum/MetaioDownload/TrackingData_MarkerlessFast.xml";
    private static String target_url = "http://expox-milano.t15.org/museum/MetaioDownload/mona_target.png";
    private static String model_url = "http://expox-milano.t15.org/museum/MetaioDownload/mona_rotated.3gp";
    private static String image_name = "exponat_1_image.jpg";
    private static String xml_name = "TrackingData_MarkerlessFast.xml";
    private static String target_name = "mona_target.png";
    private static String model = "mona_rotated.3gp";

    private static String save_name = "";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.welcome_activity);

            // show progress bar button
            btnShowProgress = (Button) findViewById(R.id.btnProgressBar);
            // Image view to show image after downloading
            my_image = (ImageView) findViewById(R.id.my_image);
            /**
             * Show Progress bar click event
             * */
            btnShowProgress.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // starting new Async Task
                    save_name=image_name;
                    new DownloadFileFromURL().execute(image_url);
                    save_name=xml_name;
                    new DownloadFileFromURL().execute(xml_url);
                    save_name=target_name;
                    new DownloadFileFromURL().execute(target_url);
                    save_name=model;
                    new DownloadFileFromURL().execute(model_url);

                }
            });
        }

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
                    File folder = new File(Environment.getExternalStorageDirectory() + "/MuseumDemo");
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdir();
                    }
                    // Output stream to write file
                    OutputStream output = new FileOutputStream("/sdcard/MuseumDemo/"+save_name);

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
                String imagePath = Environment.getExternalStorageDirectory().toString() + "/MuseumDemo/"+save_name;
                // setting downloaded into image view
              //  my_image.setImageDrawable(Drawable.createFromPath(imagePath));
            }

        }
    }