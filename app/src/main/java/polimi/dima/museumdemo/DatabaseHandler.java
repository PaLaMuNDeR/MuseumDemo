package polimi.dima.museumdemo;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import junit.runner.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marti on 16/02/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    Context mContext;

    // All Static variables
    // Database Version
    private static int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "exponatsManager";

    //Tables names
    private static final String TABLE_EXPONATS = "exponats";
    private static final String TABLE_VERSION = "version";

    // Exponats Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_BEACON_MAC = "beaconMac";
    private static final String KEY_TRACKING_DATA = "trackingData";
    private static final String KEY_TARGET = "target";
    private static final String KEY_TYPE = "type";
    private static final String KEY_MODEL = "model";
    private static final String KEY_VERSION = "version";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static int getDATABASE_VERSION() {
        return DATABASE_VERSION;
    }

    public static void setDATABASE_VERSION(int DATABASE_VERSION) {
        DatabaseHandler.DATABASE_VERSION = DATABASE_VERSION;
    }


    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_EXPONATS_TABLE = "CREATE TABLE " + TABLE_EXPONATS + "(" +
                KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," +
                KEY_DESCRIPTION + " TEXT," + KEY_IMAGE + " TEXT," +
                KEY_BEACON_MAC + " TEXT," + KEY_TRACKING_DATA + " TEXT," +
                KEY_TARGET + " TEXT," + KEY_TYPE + " TEXT," + KEY_MODEL + " TEXT" + ")";
        db.execSQL(CREATE_EXPONATS_TABLE);
        String CREATE_VERSION_TABLE="CREATE TABLE " + TABLE_VERSION + "(" +
                KEY_ID + " INTEGER PRIMARY KEY," + KEY_VERSION + " INTEGER" +")";
        db.execSQL(CREATE_VERSION_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPONATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSION);

//       setDATABASE_VERSION(newVersion);
  //      Log.d("Database", "DB Update. Version set to: " + newVersion);
        // Create tables again
        onCreate(db);
    }

    // Adding new exponat
    public void addExponat(Exponat exponat) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, exponat.getName()); // Exponat Name
        values.put(KEY_DESCRIPTION, exponat.getDescription()); // Exponat Description
        values.put(KEY_IMAGE, exponat.getImage()); // Exponat Image
        values.put(KEY_BEACON_MAC, exponat.getBeaconMac()); // Beacon's Mac
        values.put(KEY_TRACKING_DATA, exponat.getTrackingData()); // TrackingData file for Metaio
        values.put(KEY_TARGET, exponat.getTarget()); // Target for Metaio
        values.put(KEY_TYPE, exponat.getType()); // Type of the AR object
        values.put(KEY_MODEL, exponat.getModel()); // The AR Object

        // Inserting Row
        db.insert(TABLE_EXPONATS, null, values);
        db.close(); // Closing database connection
    }

    // Adding new version
    public void addVersion(VersionVerifier versionVerifier) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_VERSION, versionVerifier.getVersion()); // Version


        // Inserting Row
        db.insert(TABLE_VERSION, null, values);
        db.close(); // Closing database connection
    }

    // Getting single exponat
    public Exponat getExponat(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EXPONATS, new String[]{KEY_ID,
                        KEY_NAME, KEY_DESCRIPTION,
                        KEY_IMAGE, KEY_BEACON_MAC,
                        KEY_TRACKING_DATA, KEY_TARGET,
                        KEY_TYPE, KEY_MODEL}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Exponat exponat = new Exponat(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8));
        db.close();
        // return exponat
        return exponat;
    }


    // Getting All Exponats
    public List<Exponat> getAllExponats() {
        List<Exponat> exponatList = new ArrayList<Exponat>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_EXPONATS;

        SQLiteDatabase db = this.getWritableDatabase();
                Cursor cursor = db.rawQuery(selectQuery, null);

                // looping through all rows and adding to list
                if (cursor.moveToFirst()) {
                    do {
                Exponat exponat = new Exponat();
                exponat.setId(Integer.parseInt(cursor.getString(0)));
                exponat.setName(cursor.getString(1));
                exponat.setDescription(cursor.getString(2));
                exponat.setImage(cursor.getString(3));
                exponat.setBeaconMac(cursor.getString(4));
                exponat.setTrackingData(cursor.getString(5));
                exponat.setTarget(cursor.getString(6));
                exponat.setType(cursor.getString(7));
                exponat.setModel(cursor.getString(8));

                // Adding exponat  to list
                exponatList.add(exponat);
            } while (cursor.moveToNext());
        }

        // return exponat list
        return exponatList;
    }

    // Getting Last version
    public VersionVerifier getLastVersion() {
        //SQLiteDatabase db = this.getReadableDatabase();
        SQLiteDatabase db = this.getWritableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_VERSION + " ORDER BY " + KEY_ID +" DESC LIMIT 1";
        Cursor cursor = db.rawQuery(selectQuery, null);

        VersionVerifier versionVerifier = new VersionVerifier();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
        versionVerifier.setId(Integer.parseInt(cursor.getString(0)));
        versionVerifier.setVersion(Integer.parseInt(cursor.getString(1)));
        } while (cursor.moveToNext());
}
        return versionVerifier;
    }



// Getting exponat Count
    public int getExponatsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_EXPONATS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

     // Updating single exponat
    public int updateExponat(Exponat exponat) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, exponat.getName());
        values.put(KEY_DESCRIPTION, exponat.getDescription());
        values.put(KEY_IMAGE, exponat.getName());
        values.put(KEY_BEACON_MAC, exponat.getName());
        values.put(KEY_TRACKING_DATA, exponat.getName());
        values.put(KEY_TARGET, exponat.getName());
        values.put(KEY_TYPE, exponat.getName());
        values.put(KEY_MODEL, exponat.getName());

        // updating row
        return db.update(TABLE_EXPONATS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(exponat.getId()) });
    }

    // Deleting single exponat
    public void deleteExponat(Exponat exponat) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPONATS, KEY_ID + " = ?",
                new String[] { String.valueOf(exponat.getId()) });
        db.close();
    }

    //New version
    public void flushOnNewVersion() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPONATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSION);

//       setDATABASE_VERSION(newVersion);
        //      Log.d("Database", "DB Update. Version set to: " + newVersion);
        // Create tables again
        onCreate(db);
    }
}
