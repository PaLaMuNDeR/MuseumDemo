package polimi.dima.museumdemo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Marti on 25/02/2015.
 */
public class DownloadMngr {
    private static final String READ_POI_URL = "http://expox-milano.t15.org/museum/MetaioDownload/exponats.json";
    private static String saving_name = "this.jpg";
    private ProgressDialog pDialog;
    Context mContext;
    private JSONArray mExponats = null;
    private static final String TAG_NAME = "name";
    private static final String TAG_SAVE_NAME = "save_name";
    private static final String TAG_IMAGE = "image";
    private static final String TAG_SAVE_IMAGE = "save_image";
    private static final String TAG_TRACKING_DATA = "trackingData";
    private static final String TAG_SAVE_TRACKING_DATA = "save_trackingData";
    private static final String TAG_TARGET = "target";
    private static final String TAG_SAVE_TARGET = "save_target";
    private static final String TAG_MODEL_1 = "model_1";
    private static final String TAG_SAVE_MODEL_1 = "save_model_1";
    private static final String TAG_MODEL_2 = "model_2";
    private static final String TAG_SAVE_MODEL_2 = "save_model_2";
    private static final String TAG_MODEL_3 = "model_3";
    private static final String TAG_SAVE_MODEL_3 = "save_model_3";
    private static final String TAG_MODEL_4 = "model_4";
    private static final String TAG_SAVE_MODEL_4 = "save_model_4";
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;


    public DownloadMngr(){
        start();
    }
    public void start(){
        Log.d("Download","start");

        DatabaseHandler db = new DatabaseHandler(mContext);
        JSONParser jParser = new JSONParser();
        // Feed the beast our comments url, and it spits us
        // back a JSON object. Boo-yeah Jerome.
        JSONObject json = jParser.getJSONFromUrl(READ_POI_URL);
        try {
            mExponats = json.getJSONArray("download_resources");
            for (int i = 0; i < mExponats.length(); i++) {
                JSONObject c = mExponats.getJSONObject(i);

                // gets the content of each tag
                //String name = c.getString(TAG_NAME);
                //String save_name = c.getString(TAG_SAVE_NAME);

                String image = c.getString(TAG_IMAGE);
                String save_image = c.getString(TAG_SAVE_IMAGE);
                //saving_name = save_image;
                Log.d("Download","Downloading image...");
                DownloadFileFromURL(save_image,image);
/*
                String trackingData = c.getString(TAG_TRACKING_DATA);
                String save_trackingData = c.getString(TAG_SAVE_TRACKING_DATA);
                Log.d("Download","Download Tracking");
            //    DownloadFileFromURL(trackingData,save_trackingData);

                String target = c.getString(TAG_TARGET);
                String save_target = c.getString(TAG_SAVE_TARGET);
                Log.d("Download","Download Target");
              //  DownloadFileFromURL(target,save_target);

                String model_1 = c.getString(TAG_MODEL_1);
                String save_model_1 = c.getString(TAG_SAVE_MODEL_1);
                Log.d("Download","Download Model 1");
                //DownloadFileFromURL(model_1,save_model_1);

                String model_2 = c.getString(TAG_MODEL_2);
                String save_model_2 = c.getString(TAG_SAVE_MODEL_2);
                Log.d("Download","Download Model 2");
                //DownloadFileFromURL(model_2,save_model_2);


                String model_3 = c.getString(TAG_MODEL_3);
                String save_model_3 = c.getString(TAG_SAVE_MODEL_3);
                Log.d("Download","Download Model 3");
                //DownloadFileFromURL(model_3,save_model_3);


                String model_4 = c.getString(TAG_MODEL_4);
                String save_model_4 = c.getString(TAG_SAVE_MODEL_4);
                Log.d("Download","Download Model 4");
               // DownloadFileFromURL(model_4,save_model_4);

*/
            }
        } catch (Exception e) {
            Log.e("Download", "Error 1. Error in Download Resources");
        }
    }



        protected String DownloadFileFromURL(String saved, String... f_url) {
            int count;
            try {
                /*pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                */
               // pDialog.setProgress(progress[0]);

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
                OutputStream output = new FileOutputStream("/sdcard/MuseumDemo/assets/"+saved);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                 //   publishProgress(""+(int)((total*100)/lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                Log.d("Download","Download was successful");
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;

    }

}
