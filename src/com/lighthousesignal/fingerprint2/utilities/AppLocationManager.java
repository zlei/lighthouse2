package com.lighthousesignal.fingerprint2.utilities;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import com.lighthousesignal.fingerprint2.R;

/**
 * Application Location Manager
 */
public class AppLocationManager implements LocationListener {
	
	private static volatile WeakReference<AppLocationManager> mInstance;
	
	public static AppLocationManager getInstance(Context context) {
		System.out.println("AppLocationManager, context null? " + (context == null));
		SharedPreferences prefs = DataPersistence.getPrefs(context);
		if (mInstance == null || mInstance.get() == null ) {
			mInstance = new WeakReference<AppLocationManager>(
					new AppLocationManager(context, prefs));
		}
		return mInstance.get();
	}
	
	/**
	 * States List
	 */
	private static String[] mStates;

	/**
	 * State Key in preferences
	 */
	public static final String PREFS_STATE_KEY = "state_key";

	private Location loc;

	private Context mCtx;

	private LocationManager mlocManager;

	private SharedPreferences mPrefs;

	public AppLocationManager(Context ctx, SharedPreferences pref) {
		mCtx = ctx;
		mPrefs = pref;
		mlocManager = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);
		mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
				0, this);

		// states
		System.out.println("Context is null?" + (ctx == null));
		mStates = ctx.getResources().getStringArray(R.array.States);
	}

	public int getActiveState() {
		return mPrefs.getInt(PREFS_STATE_KEY, -1);
	}

	public double getAltitude() {
		try {
			loc = mlocManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			return loc.getAltitude();
		} catch (Exception e) {
			return 0;
		}
	}

	public double getLatitude() {
		int state = getActiveState();
		if (state != 0 && state != -1) {
			double lat = 0;
			try {
				lat = Double.parseDouble(mStates[state + 2]);
			} catch (Exception e) {
			}
			return lat;
		}
		try {
			loc = mlocManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			return loc.getLatitude();
		} catch (Exception e) {

		}

		return 0;
	}

	public String[] getStatesList() {
		int activeState = getActiveState();

		//Log.v(BasicActivity.LOG_TAG, "get active option for state "
			//	+ activeState);

		String[] names = new String[mStates.length / 3];
		for (int i = 0; i < mStates.length; i += 3) {
			names[i / 3] = (activeState == i) ? new StringBuilder().append("@")
					.append(mStates[i]).toString() : mStates[i];
		}

		return names;
	}

	public double getLongtitude() {
		int state = getActiveState();
		if (state != 0 && state != -1) {
			double lng = 0;
			try {
				lng = Double.parseDouble(mStates[state + 1]);
			} catch (Exception e) {
			}
			return lng;
		}
		try {
			loc = mlocManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			return loc.getLongitude();
		} catch (Exception e) {

		}
		return 0;
	}

	@Override
	public void onLocationChanged(Location loc) {
		this.loc = loc;
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public static String[] getStates() {
		return mStates;
	}

	/**
	 * Write to preferences
	 */
	public boolean writeActiveOption(int num) {
		return mPrefs.edit().putInt(PREFS_STATE_KEY, num).commit();
	}

}
