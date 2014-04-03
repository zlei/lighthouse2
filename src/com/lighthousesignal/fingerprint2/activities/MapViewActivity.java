package com.lighthousesignal.fingerprint2.activities;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lighthousesignal.fingerprint2.R;
import com.lighthousesignal.fingerprint2.fragments.MapListFragment.MapData;
import com.lighthousesignal.fingerprint2.logs.LogWriter;
import com.lighthousesignal.fingerprint2.logs.LogWriterSensors;
import com.lighthousesignal.fingerprint2.network.INetworkTaskStatusListener;
import com.lighthousesignal.fingerprint2.network.NetworkManager;
import com.lighthousesignal.fingerprint2.network.NetworkResult;
import com.lighthousesignal.fingerprint2.network.NetworkTask;
import com.lighthousesignal.fingerprint2.utilities.AppLocationManager;
import com.lighthousesignal.fingerprint2.utilities.DataPersistence;
import com.lighthousesignal.fingerprint2.utilities.UiFactories;
import com.lighthousesignal.fingerprint2.views.MapView;
import com.lighthousesignal.fingerprint2.wifi.FingerprintManager;
import com.lighthousesignal.lsslib.WifiData;
import com.lighthousesignal.lsslib.WifiScanResult;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@SuppressLint("UseSparseArrays")
public class MapViewActivity extends Activity implements
		INetworkTaskStatusListener, OnClickListener {

	private static final String LOG_TAG = "LSS F3 MapView";

	private static final int TAG_GET_POINTS = 2;

	// get preference IDs
	private static String PREF_CUSTOMER_ID = "customer_id";
	private static String PREF_DEVELOPER_ID = "developer_id";
	// building id, image id
	private int building_ID = -1;
	private int image_ID = -1;
	// atomic counter
	private Timer mTimeTicker;
	private AtomicInteger mTimeElapsed = new AtomicInteger(0);
	// network tag key
	private static final String TAG_KEY = "TAG";
	// imageloader for image async downloading
	protected static ImageLoader imageLoader;
	// load map options
	private DisplayImageOptions options;
	// private MapView mapView;
	private MapView mapView;
	private String imageUrl;
	// map downloaded from server without painting
	private Bitmap rawMap;
	// map to draw
	private Bitmap paintMap;
	// map to show data from server
	private Bitmap serverLogMap;
	protected MapData mData;
	// download or reload map
	private Boolean reloadMap;
	protected String mFilename;
	protected boolean mIsMapLoaded = false;
	protected ListView mLSearchResult;
	protected MapData mMapData;
	protected MapView mMapView;
	// state of wifi access point scan completion
	protected boolean mScanCompleted = false;
	protected SensorManager mSensorManager;
	protected LinearLayout mViewport, mViewMap, mViewSummary;
	// private WifiSearcherAdapter mAdapter;
	private FingerprintManager mManager;
	private Bundle mBundle;
	private SharedPreferences mPrefs;
	private ConnectivityManager mConManager;
	// private WifiSnifferService mService;
	private int mWifiActiveNetwork = -1;
	private WifiManager mWifiManager;
	// stores state of Mobile Internet connection before closing one
	private boolean mConnectionMobileEnabled;
	// stores state of Wifi connection before closing one
	private boolean mConnectionWifiEnabled;
	private boolean mFlagScan = false;
	// building info get from maplist
	private String map_info;

	// UI related
	private Button button_scan_start;
	private Button button_scan_stop;
	private Button button_scan_clear;
	private Button button_scan_save;
	private Button button_home;
	private CheckBox checkbox_scan_show_logs;
	private TextView textView_map_info;

	// used for scanning notification banner
	private static final Style STYLE_INFINITE = new Style.Builder()
			.setBackgroundColorValue(Style.holoBlueLight).build();
	private static final Configuration CONFIGURATION_INFINITE = new Configuration.Builder()
			.setDuration(Configuration.DURATION_INFINITE).build();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.overridePendingTransition(R.anim.slide_in_left,
				R.anim.slide_out_left);
		setContentView(R.layout.activity_map_view);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		setImageLoaderOption();
		reloadMap = false;
		mManager = new FingerprintManager(this);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mConManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		button_scan_start = (Button) findViewById(R.id.button_scanStart);
		button_scan_stop = (Button) findViewById(R.id.button_scanStop);
		button_scan_clear = (Button) findViewById(R.id.button_scanClear);
		button_scan_save = (Button) findViewById(R.id.button_scanSave);
		button_home = (Button) findViewById(R.id.button_home);
		checkbox_scan_show_logs = (CheckBox) findViewById(R.id.checkBox_scan_show_logs);
		textView_map_info = (TextView) findViewById(R.id.map_info);

		if (savedInstanceState == null) {
			mBundle = getIntent().getExtras();
			if (mBundle == null) {
				map_info = null;
			} else {
				map_info = mBundle.getString("MAP_INFO");
				image_ID = mBundle.getInt("imageID");
				building_ID = mBundle.getInt("buildingID");
			}
		} else {
			map_info = (String) savedInstanceState.getSerializable("MAP_INFO");
		}
		textView_map_info.setText("Map info: \n" + map_info);

		button_scan_start.setOnClickListener(this);
		button_scan_stop.setOnClickListener(this);
		button_scan_clear.setOnClickListener(this);
		button_scan_save.setOnClickListener(this);
		button_home.setOnClickListener(this);
		
		//switch between log map and painted map
		checkbox_scan_show_logs
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							if (mapView.isLogPoints())
								mapView.showLogs(serverLogMap, null);
							else
								loadMap();
						} else {
							mapView.showPaintedMap();
						}
					}
				});

		imageUrl = DataPersistence.getImgUrl(this);
		downloadMap(reloadMap);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.button_scanStart:
			if (rawMap != null && mapView.getPoints().size() == 1) {
				// only start point
				initStart();
			} else {
				UiFactories.standardAlertDialog(this, "Alert",
						"Please have start point first!", null);
			}
			break;

		case R.id.button_scanStop:
			// exactly two points
			if (paintMap != null && mapView.getPoints().size() == 2) {
				initStop();
			} else {
				UiFactories.standardAlertDialog(this, "Alert",
						"Please have end point first!", null);
			}
			break;

		case R.id.button_scanSave:
			initSave();
			break;

		case R.id.button_scanClear:
			initClear();
			break;

		case R.id.button_home:
			initBackHome();
			break;
		}
	}

	/**
	 * dowanload map with imageloader, get all maps
	 * 
	 * @param update
	 */
	public void downloadMap(Boolean update) {
		// use ImageViewTouch lib to deal with image zooming and panning
		imageLoader = ImageLoader.getInstance();
		mapView = (MapView) findViewById(R.id.map_image);
		mapView.setDisplayType(DisplayType.FIT_IF_BIGGER);
		imageLoader.displayImage(imageUrl, mapView, options,
				new SimpleImageLoadingListener() {
					@Override
					public void onLoadingStarted(String imageUri, View view) {
					}

					@Override
					public void onLoadingFailed(String imageUri, View view,
							FailReason failReason) {
						String message = null;
						switch (failReason.getType()) { // fail type
						case IO_ERROR:
							message = "Input/Output error";
							break;
						case DECODING_ERROR:
							message = "Image can't be decoded";
							break;
						case NETWORK_DENIED:
							message = "Downloads are denied";
							break;
						case OUT_OF_MEMORY:
							message = "Out Of Memory error";
							break;
						case UNKNOWN:
							message = "Unknown error";
							break;
						}
						Toast.makeText(MapViewActivity.this, message,
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onLoadingComplete(String imageUri, View view,
							Bitmap loadedImage) {
						if (!reloadMap) {
							rawMap = loadedImage.copy(loadedImage.getConfig(),
									true);
							paintMap = loadedImage.copy(
									loadedImage.getConfig(), true);
							serverLogMap = loadedImage.copy(
									loadedImage.getConfig(), true);
							reloadMap = true;
						}
						// set up clickable
						mapView.startPaint(paintMap);
					}
				});
	}

	/**
	 * set options for image loader, mainly about cache issues
	 */
	private void setImageLoaderOption() {
		// cache map or not
		boolean toCacheMap;
		toCacheMap = mPrefs.getBoolean("checkbox_cache_map", true);

		options = new DisplayImageOptions.Builder()
				// .showImageForEmptyUri(R.drawable.ic_empty)
				// .showImageOnFail(R.drawable.ic_error)
				.showImageForEmptyUri(R.drawable.ic_launcher)
				.showImageOnFail(R.drawable.ic_launcher)
				.resetViewBeforeLoading(true)
				// cache to memory
				.cacheInMemory(toCacheMap)
				// cache to SD card
				.cacheOnDisc(toCacheMap).imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.displayer(new FadeInBitmapDisplayer(300)).build();
	}

	/**
	 * Load points from the server for current map
	 */
	@SuppressLint("UseValueOf")
	public void loadMap() {
		/**
		 * Loading points
		 */
		Hashtable<String, String> hash = new Hashtable<String, String>(3);
		hash.put("imageId", Integer.valueOf(image_ID).toString());
		hash.put("token", DataPersistence.getToken(this));
		NetworkTask task = new NetworkTask(this,
				DataPersistence.getServerName(this), "/logs/pars/getpoint",
				false, hash, true);
		task.setTag(TAG_KEY, new Integer(TAG_GET_POINTS));
		NetworkManager.getInstance().addTask(task);
	}

	/**
	 * network task error
	 */
	@Override
	public void nTaskErr(NetworkResult result) {
		// initTitleProgressBar(false);

		if (result.getResponseCode() == 401) {
			UiFactories.standardAlertDialog(this,
					getString(R.string.msg_error),
					getString(R.string.msg_error_network_401), null);
		} else {
			UiFactories.standardAlertDialog(this,
					getString(R.string.msg_error),
					getString(R.string.msg_error_network_unknown), null);
		}
	}

	/**
	 * network task success, get all the points from the server for current map
	 */
	@Override
	public void nTaskSucces(NetworkResult result) {
		try {
			XmlPullParser parser = XmlPullParserFactory.newInstance()
					.newPullParser();
			Log.v(LOG_TAG, new String(result.getData()).toString());
			Log.v(LOG_TAG, "url " + result.getTask().getUrl());
			parser.setInput(new ByteArrayInputStream(result.getData()), "UTF-8");
			switch (((Integer) result.getTask().getTag(TAG_KEY)).intValue()) {
			// case TAG_LOG_SUBMIT:
			// break;
			case TAG_GET_POINTS:
				parser.nextTag();
				ArrayList<HashMap<String, String>> points = new ArrayList<HashMap<String, String>>();
				if (XmlPullParser.START_TAG == parser.getEventType()) {
					if (parser.getName().equalsIgnoreCase("images")) {
						while (parser.next() != XmlPullParser.END_DOCUMENT)
							if (parser.getEventType() == XmlPullParser.START_TAG
									&& parser.getName().equalsIgnoreCase("img")) {
								HashMap<String, String> data = new HashMap<String, String>();
								data.put("point_id", parser.getAttributeValue(
										null, "segment_id"));
								data.put("point_name", parser
										.getAttributeValue(null,
												"scan_point_name"));
								data.put("point_x", parser.getAttributeValue(
										null, "point_x"));
								data.put("point_y", parser.getAttributeValue(
										null, "point_y"));
								data.put("is_used", parser.getAttributeValue(
										null, "is_used"));
								points.add(data);
							}
					}
				}
				if (!points.isEmpty())
					mapView.showLogs(serverLogMap, points);
				else
					Toast.makeText(getApplicationContext(), "No log found!",
							Toast.LENGTH_SHORT).show();
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shows alert to turn active wifi/mobile connections off
	 * 
	 * DO NOT TURN OFF FOR NOW
	 */
	public void alertActiveConnectionsTurnOff(final Runnable r) {

		// disableAllWifiNetworks();
		try {
			setMobileDataEnabled(this, false);
		} catch (Exception e1) {
			e1.printStackTrace();
			UiFactories.standardAlertDialog(this,
					getString(R.string.msg_alert),
					getString(R.string.msg_operation_failed), null);
		}

		if (r != null)
			r.run();
	}

	/**
	 * Shows alert to turn active connections on
	 */
	public void alertActiveConnectionsTurnOn() {
		// Re enable data
		try {
			// if (mConnectionMobileEnabled)
			setMobileDataEnabled(MapViewActivity.this, true);

			mConnectionMobileEnabled = false;
			mConnectionWifiEnabled = false;

		} catch (Exception e) {
			UiFactories.standardAlertDialog(this,
					getString(R.string.msg_alert),
					getString(R.string.msg_operation_failed), null);
			Log.e(LOG_TAG, "error", e);
		}
	}

	public void completeLogs() {
		if (!mScanCompleted) {
			// mService.stopScan(mMapData.imageId, mMapData.floorId,
			// mMapView.mMarker, mMapView.mMarker2);
			mScanCompleted = true;
		}
	}

	/**
	 * Summary Info
	 * 
	 * @param time
	 * @param readings
	 * @param x
	 * @param y
	 * @param map
	 * @param apCount
	 */
	public void setScanInfo(int readings, int apCount, boolean incTryCount) {
		// mNumberReadingsTv.setText(new
		// StringBuilder().append("Readings: ").append(readings));
		// mApCountTv.setText(new
		// StringBuilder().append("Total AP: ").append(apCount));

		if (incTryCount)
			// mAdapter.incTryCount();
			mManager.incTryCount();
	}

	/**
	 * Checks is mobile network connected
	 * 
	 * @return
	 */
	protected boolean isMobileInternetConnected() {
		NetworkInfo info = mConManager.getActiveNetworkInfo();
		return info != null && info.isConnectedOrConnecting()
				&& info.getType() == ConnectivityManager.TYPE_MOBILE;
	}

	/**
	 * Checks is wifi network connected
	 * 
	 * @return
	 */
	protected boolean isWiFiInternetConnected() {
		NetworkInfo info = mConManager.getActiveNetworkInfo();
		return info != null && info.isConnectedOrConnecting()
				&& info.getType() == ConnectivityManager.TYPE_WIFI;
	}

	/**
	 * initial all needed for start
	 * 
	 * @return
	 */
	private boolean initStart() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog
				.setTitle("Start Scan")
				.setPositiveButton("YES",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// if start scan
								initTicker();
								startScan();
								startSensors();
								mapView.setPointChangable(false);
								button_scan_start.setVisibility(View.GONE);
								button_scan_stop.setVisibility(View.VISIBLE);
								Toast.makeText(getApplicationContext(),
										"Scan started!", Toast.LENGTH_SHORT)
										.show();
							}
						})
				.setNegativeButton("NO", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(getApplicationContext(),
								"Scan canceled!", Toast.LENGTH_SHORT).show();
					}
				}).setCancelable(false).setMessage("make sure to start scan?")
				.show();
		return true;
	}

	/**
	 * initial all settings for stop
	 * 
	 * @return
	 */
	private boolean initStop() {
		stopScan();
		stopTicker();
		button_scan_stop.setVisibility(View.GONE);
		button_scan_save.setVisibility(View.VISIBLE);
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog
				.setTitle("Stop Scan")
				.setPositiveButton("YES",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								initSave();
								Toast.makeText(
										MapViewActivity.this,
										"Scan successed! Please save the scan.",
										Toast.LENGTH_SHORT).show();
							}
						})
				.setNegativeButton("NO", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mapView.setPointChangable(true);
						button_scan_save.setEnabled(true);
						Toast.makeText(
								MapViewActivity.this,
								"Please click on another point! Then save the scan data.",
								Toast.LENGTH_SHORT).show();
					}
				}).setCancelable(false).setMessage("Is end point correct?")
				.show();
		// mapView.clearData();
		// mapView.startPaint(paintMap);
		return true;
	}

	private boolean initSave() {
		saveScan();
		return true;
	}

	/**
	 * init all for clear button
	 * 
	 * @return
	 */
	private boolean initClear() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.map_view_confirm_clear_title))
				.setMessage(getString(R.string.map_view_confirm_clear_text))
				.setPositiveButton("YES",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								initRestart();
								Toast.makeText(getApplicationContext(),
										"Data cleared! Ready for new scan!",
										Toast.LENGTH_LONG).show();
							}
						})
				.setNegativeButton("NO", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(getApplicationContext(),
								"Clear canceled!", Toast.LENGTH_SHORT).show();
					}
				}).setCancelable(false)

				.show();
		return true;
	}

	private boolean initBackHome() {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.map_view_confirm_home_title))
				.setMessage(getString(R.string.map_view_confirm_home_text))
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						}).setNegativeButton("No", null).show();
		return true;
	}

	/**
	 * restart all
	 * 
	 * @return
	 */
	private boolean initRestart() {
		// restart all
		paintMap = rawMap.copy(rawMap.getConfig(), true);
		if (!mapView.getPoints().isEmpty())
			mapView.clearData();
		if (mManager.serviceRunning())
			clearScan();
		Crouton.clearCroutonsForActivity(MapViewActivity.this);
		mapView.updatePaint(paintMap);
		mapView.stopPaint();
		mapView.startPaint(paintMap);
		// default status for one point
		mapView.setPointChangable(true);
		button_scan_save.setVisibility(View.GONE);
		button_scan_start.setVisibility(View.VISIBLE);
		checkbox_scan_show_logs.setChecked(false);

		return true;
	}

	/**
	 * Starts scan
	 */
	protected void startScan() {
		// forgetAllWifiNetworks();
		// disableAllWifiNetworks();

		// enables scan
		if (isWifiAvailable()) {
			Runnable startScan = new Runnable() {

				@Override
				public void run() {
					// mService.startScan();
					mManager.startScans();
					mFlagScan = true;

				}
			};

			if (isWiFiInternetConnected()) {
				mConnectionWifiEnabled = true;
				mWifiActiveNetwork = mWifiManager.getConnectionInfo()
						.getNetworkId();
			}

			if (isMobileInternetConnected()) {
				mConnectionMobileEnabled = true;
			}

			if (mConnectionMobileEnabled || mConnectionWifiEnabled) {
				alertActiveConnectionsTurnOff(startScan);
			} else {
				startScan.run();
			}
			// show banner
			showCrouton("Scanning...", STYLE_INFINITE, CONFIGURATION_INFINITE);
		}
	}

	/**
	 * stop scan write all the scanned information to log
	 * 
	 */
	protected void stopScan() {

		Vector<WifiData> collection = mManager.finishScans();
		stopServices();
		// clear banner
		Crouton.clearCroutonsForActivity(this);

		Vector<WifiScanResult> stat = new Vector<WifiScanResult>();
		// Log.d("collected data ", collection.get(0).toJSONArray().toString());
		int counter = 0;
		for (int i = 0; i < collection.size(); i++) {
			LogWriter.instance().addLog(collection.get(i).toXMLLog());
			counter += collection.get(i).getNetworkCount();
			stat.addAll(collection.get(i).getScanResults());
		}
		LogWriter.instance().closeLog();
		LogWriter.instance().addScanParams(mTimeElapsed.get(), counter);
		LogWriter.instance().addScanStatistics(stat, counter);
		LogWriter.instance().addLocation(getLocationManager().getLatitude(),
				getLocationManager().getLongtitude());
		LogWriter.instance().addDeviceInfo(this);

		LogWriter.instance().addCustomerId(
				PreferenceManager.getDefaultSharedPreferences(getBaseContext())
						.getString(PREF_CUSTOMER_ID, ""));
		LogWriter.instance().addDeveloperId(
				PreferenceManager.getDefaultSharedPreferences(getBaseContext())
						.getString(PREF_DEVELOPER_ID, ""));

		// TODO:
		// LogWriter.instance().addCellTowersInfo(WifiSnifferService.this);

		// get the IDs, start and end points
		if (!(image_ID == -1 && building_ID == -1))
			LogWriter.instance().addImage(image_ID, building_ID,
					mapView.getPoints().get(1), mapView.getPoints().get(2));
		LogWriter.instance().endLog();
		LogWriterSensors.instance().endLog();

		// Toast.makeText(this, R.string.msg_map_notification_points, 3000);

		// enableButtonsAfterScan(true);

		if ((mConnectionMobileEnabled || mConnectionWifiEnabled)
				&& (mConManager.getActiveNetworkInfo() == null || !mConManager
						.getActiveNetworkInfo().isConnectedOrConnecting())) {
			alertActiveConnectionsTurnOn();
		}
	}

	/**
	 * Save scan data into log file
	 */
	public void saveScan() {
		AlertDialog saveDialog = UiFactories.saveScanDailog("Save Data", this,
				null, this);
		saveDialog.show();
		saveDialog
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (UiFactories.isSaved()) {
							initRestart();
							Toast.makeText(MapViewActivity.this,
									"Ready for new scan!", Toast.LENGTH_LONG)
									.show();
						} else
							Toast.makeText(MapViewActivity.this,
									"Please save current scan data!",
									Toast.LENGTH_LONG).show();
					}
				});
	}

	/**
	 * Stop service
	 */
	protected void stopServices() {
		/*
		 * try { if (mService != null) { mService.stopScan();
		 * mService.deInitWifi(); } unbindService(connection); stopService(new
		 * Intent(this, WifiSnifferService.class)); } catch (Exception e) {
		 * e.printStackTrace(); }
		 */
		mManager.stopScans();
		stopSensors();
	}

	/**
	 * Clear scan
	 */
	public void clearScan() {
		/**
		 * Button controls states
		 */
		// mAdapter.reset();
		mManager.reset();
		mFilename = null;

		/**
		 * Reset info
		 */
		setScanInfo(0, 0, false);

		// mTimelapsedTv.setText("Time elapsed 0s");

		LogWriter.reset();
		LogWriterSensors.reset();

		// mMapView.resetMarkers();

		// mMapView.setFirstMarker();

		mFlagScan = false;

		mScanCompleted = false;
	}

	/**
	 * Turn on/off mobile internet
	 * 
	 * @param context
	 * @param enabled
	 */
	private void setMobileDataEnabled(Context context, boolean enabled)
			throws Exception {
		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final Class<?> conmanClass = Class.forName(conman.getClass().getName());
		final Field iConnectivityManagerField = conmanClass
				.getDeclaredField("mService");
		iConnectivityManagerField.setAccessible(true);
		final Object iConnectivityManager = iConnectivityManagerField
				.get(conman);
		final Class<?> iConnectivityManagerClass = Class
				.forName(iConnectivityManager.getClass().getName());
		final Method setMobileDataEnabledMethod = iConnectivityManagerClass
				.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
		setMobileDataEnabledMethod.setAccessible(true);

		setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
	}

	/**
	 * Toggle active connection of Wifi
	 */
	private void toggleWifiActiveConnection() {
		WifiInfo connection = mWifiManager.getConnectionInfo();
		if (connection != null
				&& mConManager.getActiveNetworkInfo() != null
				&& mConManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI
				&& mConManager.getActiveNetworkInfo().isConnectedOrConnecting()) {
			// turn wifi connection off
			mWifiActiveNetwork = mWifiManager.getConnectionInfo()
					.getNetworkId();
			// mWifiManager.disableNetwork(mWifiActiveNetwork);
			Log.v(LOG_TAG, "disabled all configured networks");
		} else {
			// turn wifi connection on
			if (mWifiActiveNetwork != -1) {
				mWifiManager.enableNetwork(mWifiActiveNetwork, true);
				Log.v(LOG_TAG, "enabled wi-fi connection ");
			} else {
				Log.v(LOG_TAG, "nothing to enable ");
			}
		}
	}

	/**
	 * Is Wi-Fi enabled
	 * 
	 * @param context
	 * @return
	 */
	public boolean isWifiAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
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

	public String getSaveCheckString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getString(R.string.check_save_1)).append("\n")
				.append(LogWriter.generateFilename()).append("-")
				.append(map_info).append("\n")
				.append(getString(R.string.check_save_2));
		return stringBuilder.toString();

	}

	/**
	 * define the crouton banner behavior
	 * 
	 * @param croutonText
	 * @param croutonStyle
	 * @param configuration
	 */
	private void showCrouton(String croutonText, Style croutonStyle,
			Configuration configuration) {
		final boolean infinite = STYLE_INFINITE == croutonStyle;
		final Crouton crouton;
		crouton = Crouton.makeText(this, croutonText, croutonStyle);
		crouton.setOnClickListener(this)
				.setConfiguration(
						infinite ? CONFIGURATION_INFINITE : configuration)
				.show();
	}

	/**
	 * get location manager
	 * 
	 * @return
	 */
	private AppLocationManager getLocationManager() {
		return AppLocationManager.getInstance(this);
	}

	/**
	 * Inits time countings
	 */
	private void initTicker() {
		mTimeTicker = new Timer("Ticker");

		mTimeTicker.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mTimeElapsed.incrementAndGet();
					}
				});
			}
		}, 0, 1000);
	}

	/**
	 * dismiss ticker
	 */
	private void stopTicker() {
		mTimeTicker.cancel();
	}

	// Sensor information
	private SensorEventListener mSensorListener = new SensorEventListener() {

		private static final int X = 0;

		private static final int Y = 1;

		private static final int Z = 2;
		private Float azimuth = null;
		private HashMap<Integer, ArrayList<Object[]>> data = new HashMap<Integer, ArrayList<Object[]>>();

		private float[] mOldValues = null;

		private Long timestamp;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (timestamp == null) {
				timestamp = System.currentTimeMillis();
			}

			if (!data.containsKey(event.sensor.getType())) {
				data.put(event.sensor.getType(), new ArrayList<Object[]>());
			}

			long sensorMilis = System.currentTimeMillis();

			if (Sensor.TYPE_MAGNETIC_FIELD == event.sensor.getType()
					&& azimuth != null) {

				AppLocationManager alm = AppLocationManager
						.getInstance(MapViewActivity.this);

				GeomagneticField field = new GeomagneticField(Double.valueOf(
						alm.getLatitude()).floatValue(), Double.valueOf(
						alm.getLongtitude()).floatValue(), Double.valueOf(
						alm.getAltitude()).floatValue(),
						System.currentTimeMillis());

				double trueHeading = azimuth + field.getDeclination();

				data.get(event.sensor.getType())
						.add(new Object[] { sensorMilis, event.values[0],
								event.values[1], event.values[2], trueHeading });
			} else if (Sensor.TYPE_ORIENTATION != event.sensor.getType()
					&& event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) {
				data.get(event.sensor.getType()).add(
						new Object[] { sensorMilis, event.values[0],
								event.values[1], event.values[2] });
			} else if (Sensor.TYPE_ORIENTATION == event.sensor.getType()
					&& event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) {
				azimuth = event.values[0];
				for (int i = 0; i < event.values.length; i++) {
					event.values[i] = (float) ((event.values[i] * Math.PI) / 180.0d);
				}
				event.values[X] -= 2 * Math.PI;
				float[] deltaRotationVector = null;

				if (mOldValues != null) {
					float axisX = event.values[X];
					float axisY = event.values[Y];
					float axisZ = event.values[Z];
					float dx = mOldValues[X] - axisX;
					float dy = mOldValues[Y] - axisY;
					float dz = mOldValues[Z] - axisZ;
					float omegaMagnitude = (float) Math.sqrt(dx * dx + dy * dy
							+ dz * dz);
					dx /= omegaMagnitude;
					dy /= omegaMagnitude;
					dz /= omegaMagnitude;
					deltaRotationVector = new float[4];
					float thetaOverTwo = omegaMagnitude / 2;
					float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
					float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
					deltaRotationVector[0] = sinThetaOverTwo * dx;
					deltaRotationVector[1] = sinThetaOverTwo * dy;
					deltaRotationVector[2] = sinThetaOverTwo * dz;
					deltaRotationVector[3] = cosThetaOverTwo;
					// is nan check
					deltaRotationVector[0] = Float
							.isNaN(deltaRotationVector[0]) ? 0
							: deltaRotationVector[0];
					deltaRotationVector[1] = Float
							.isNaN(deltaRotationVector[1]) ? 0
							: deltaRotationVector[1];
					deltaRotationVector[2] = Float
							.isNaN(deltaRotationVector[2]) ? 0
							: deltaRotationVector[2];
					deltaRotationVector[3] = Float
							.isNaN(deltaRotationVector[3]) ? 0
							: deltaRotationVector[3];
				}
				mOldValues = Arrays.copyOf(event.values, event.values.length);

				if (deltaRotationVector != null) {
					data.get(event.sensor.getType()).add(
							new Object[] { sensorMilis, event.values[0],
									event.values[1], event.values[2],
									deltaRotationVector[0],
									deltaRotationVector[1],
									deltaRotationVector[2],
									deltaRotationVector[3] });
				}

			}

			if (sensorMilis - timestamp > 1000) {
				writeToLogAndClearList();
			}
		}

		protected void writeToLogAndClearList() {

			timestamp = System.currentTimeMillis();
			LogWriterSensors.instance().write(data);
			data.clear();
		}
	};

	public void startSensors() {
		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void stopSensors() {
		try {
			mSensorManager.unregisterListener(mSensorListener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		stopServices();
		super.onDestroy();
	}

}
