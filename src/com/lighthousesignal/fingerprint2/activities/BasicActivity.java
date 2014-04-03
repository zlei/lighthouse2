package com.lighthousesignal.fingerprint2.activities;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.lighthousesignal.fingerprint2.R;
import com.lighthousesignal.fingerprint2.utilities.AppLocationManager;
import com.lighthousesignal.fingerprint2.utilities.DataPersistence;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class BasicActivity extends Activity {

	protected SharedPreferences mPreferences;
	public static final String TAG_KEY = "TAG_KEY";
	public static final String LOG_TAG = "FINGERPRINT2";
	static protected WeakReference<AppLocationManager> mLocManager;
	public static String PREF_LOGIN_TOKEN = "login_token";

	public static String PREF_LOGIN_USERNAME = "login_username";

	public static String PREF_LOGIN_PASS = "login_password";

	public static String PREF_CUSTOMER_ID = "customer_id";

	public static String PREF_DEVELOPER_ID = "developer_id";

	public static String PREF_BUILDING_ID = "";

	public static String PREF_FLOOR_ID = "";

	public static String PREF_IMG_URL = "";

	public static final int INTENT_LOGIN_CODE = 100;

	protected String mFilename;

	public SharedPreferences getLocalPreferences() {
		return mPreferences;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		/**
		 * Create global configuration and initialize ImageLoader with this
		 * configuration
		 */
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).build();
		ImageLoader.getInstance().init(config);
	}

	/**
	 * Saves login information
	 * 
	 * @param token
	 * @param username
	 * @param pass
	 */
	public void saveToken(String token, String username, String pass) {
		mPreferences.edit().putString(PREF_LOGIN_TOKEN, token)
				.putString(PREF_LOGIN_USERNAME, username)
				.putString(PREF_LOGIN_PASS, pass).commit();
	}

	/**
	 * Gets token
	 */
	public String getToken() {
		return mPreferences.getString(PREF_LOGIN_TOKEN, "");
	}

	/**
	 * Has token
	 * 
	 * @return
	 */
	public boolean hasToken() {
		return mPreferences.contains(PREF_LOGIN_TOKEN);
	}

	/**
	 * eraseToken
	 */
	public void eraseToken() {
		if (mPreferences.contains(PREF_LOGIN_TOKEN)) {
			mPreferences.edit().remove(PREF_LOGIN_TOKEN).commit();
		}
	}

	/**
	 * save current building id
	 * 
	 * @param building_id
	 */
	public void saveBuildingID(String building_id) {
		mPreferences.edit().putString(PREF_BUILDING_ID, building_id).commit();
	}

	/**
	 * get current building id
	 */
	public String getBuildingID() {
		return mPreferences.getString(PREF_BUILDING_ID, "");
	}

	/**
	 * save current floor id
	 */
	public void saveFloorID(String floor_id) {
		mPreferences.edit().putString(PREF_FLOOR_ID, floor_id).commit();
	}

	/**
	 * get current floor id
	 */
	public String getFloorID() {
		return mPreferences.getString(PREF_FLOOR_ID, "");
	}

	/**
	 * save current img url
	 */
	public void saveImgUrl(String imageName) {
		String serverName = DataPersistence.getServerName(this);
		String imageUrl = new StringBuilder()
				.append(serverName + getString(R.string.plans_url))
				.append(imageName).toString();
		mPreferences.edit().putString(PREF_IMG_URL, imageUrl).commit();
	}

	/**
	 * get current img url
	 */
	public String getImgUrl() {
		return mPreferences.getString(PREF_IMG_URL, "");
	}

	/**
	 * Internet connection is available or not
	 * 
	 */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null) {
			return false;
		}
		return cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	/**
	 * Is Wi-Fi enabled
	 * 
	 * @param context
	 * @return
	 */
	public boolean isWifiAvailable() {
		// Log.d("Fingerprint", "Checking if wifi is available.");
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
			// Log.d("Fingerprint", "Connectivity Manager is not null");
			networkInfo = connectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		return networkInfo == null ? false : networkInfo.isAvailable();
	}

	/**
	 * Is gps enabled
	 * 
	 * @return
	 */
	public boolean isGpsEnabled() {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		return provider.contains("gps");
	}

	/**
	 * safe long to int convert
	 * 
	 * @param l
	 * @return
	 */
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(l
					+ " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}
}
