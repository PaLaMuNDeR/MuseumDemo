package polimi.dima.museumdemo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Displays basic information about beacon.
 *
 * @author wiktor@estimote.com (Wiktor Gworek)
 *         edited by martianev@gmail.com (Martin Anev)
 */
public class LeDeviceListAdapter extends BaseAdapter {

    private ArrayList<Beacon> beacons;
    private LayoutInflater inflater;
    Context mContext;

    private ArrayList<HashMap<String, String>> mExponatsList;
    private JSONArray mExponats = null;

    private static final String TAG_NAME = "name";
    private static final String TAG_IMAGE = "image";
    private static final String TAG_BEACON_MAC = "beaconMac";
    public String image_value;

    public LeDeviceListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.beacons = new ArrayList<Beacon>();
        mContext = this.inflater.getContext();
        Parser();
    }

    public void replaceWith(Collection<Beacon> newBeacons) {
        this.beacons.clear();
        this.beacons.addAll(newBeacons);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Beacon getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflateIfRequired(view, position, parent);
        bind(getItem(position), view);
        return view;
    }

    private void bind(Beacon beacon, View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        DatabaseHandler db = new DatabaseHandler(mContext);

        List<Exponat> exponats = db.getAllExponats();
        Log.d("Beacon Mac", beacon.getMacAddress());

        //For every row of the DB - checks
        // whether the Mac address of the beacon matches
        for (Exponat ex : exponats) {

            String b_mac_db = ex.getBeaconMac();

            if (beacon.getMacAddress().equals(b_mac_db)) {
                //Extract the name from the DB
                String name_value = ex.getName();
                //Set the name of the view to the name from the DB
                holder.macTextResource=name_value;
                holder.macTextView.setText(String.format("%s (%.2fm)", holder.macTextResource, Utils.computeAccuracy(beacon)));
                //Extract the description from the DB
                String description = ex.getDescription();
                //Sets the description
                holder.descriptionTextView.setText(description);
                //Extract the id from the DB
                int id = ex.getId();
                //Set the macId of the holder
                holder.macId = id;
                //Extract the image location from the DB
                image_value = ex.getImage();
                //Set the image of the object
                holder.macImageResource = image_value;
                int image_source = mContext.getResources().getIdentifier("polimi.dima.museumdemo:drawable/" + image_value, null, null);
                holder.macImageView.setImageResource(image_source);
                //Sets that the holder is acknowledged to continue to the next activity
                holder.macContinueBool = Boolean.TRUE;


                //This break assures that it will not check the other rows
                //Do not touch it, otherwise the views will be all mixed around
                break;
            } else {
                holder.macTextView.setText(String.format("%s (%.2fm)",
                        "Unknown device",
                        Utils.computeAccuracy(beacon)));
                int image_source = mContext.getResources().getIdentifier("polimi.dima.museumdemo:drawable/beacon_gray", null, null);
                holder.macImageView.setImageResource(image_source);
                holder.macContinueBool = Boolean.FALSE;
            }
        }
        db.close();
    }


    private View inflateIfRequired(View view, int position, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.device_item, null);
            view.setTag(new ViewHolder(view));
        }
        return view;
    }

    //Every row in our list
    public class ViewHolder {
        final TextView macTextView;
        final TextView descriptionTextView;
        final ImageView macImageView;
        public String macImageResource;
        public String macTextResource;
        public Boolean macContinueBool;
        public int macId;

        ViewHolder(View view) {
            macTextView = (TextView) view.findViewWithTag("mac");
            descriptionTextView = (TextView) view.findViewWithTag("description");
            macImageView = (ImageView) view.findViewWithTag("image");
            macId = 0;
        }

    }

    void Parser() {

        mExponatsList = new ArrayList<HashMap<String, String>>();

        String string = loadJSONFromAsset();
        try {
            JSONObject json = new JSONObject(string);
            mExponats = json.getJSONArray("exponats");

            for (int i = 0; i < mExponats.length(); i++) {
                JSONObject c = mExponats.getJSONObject(i);

                // gets the content of each tag
                String name = c.getString(TAG_NAME);
                String image = c.getString(TAG_IMAGE);
                String beaconMac = c.getString(TAG_BEACON_MAC);

                // creating new HashMap
                HashMap<String, String> map = new HashMap<String, String>();

                // map.put(TAG_POI_ID, poi_id);
                map.put(TAG_NAME, name);
                map.put(TAG_IMAGE, image);
                map.put(TAG_BEACON_MAC, beaconMac);

                // adding HashList to ArrayList
                mExponatsList.add(map);

                // annndddd, our JSON data is up to date same with our array
                // list
                Log.d("hashmap", "One more added");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("hashmap", mExponatsList.toString());
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {

            InputStream is = mContext.getAssets().open("exponats.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");

            Log.d("Hashmap", "I read it");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public String getImage(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        String image_resource = holder.macImageResource;
        return image_resource;
    }
    public String getExponatName(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        String exponat_name = holder.macTextResource;
        return exponat_name;
    }

    public Integer getExponatId(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Integer exponat_id = holder.macId;
        return exponat_id;
    }

    public Boolean getContinueBool(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Boolean continueBool = holder.macContinueBool;
        return continueBool;
    }

}
