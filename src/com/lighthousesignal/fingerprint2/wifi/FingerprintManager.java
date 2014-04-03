package com.lighthousesignal.fingerprint2.wifi;

import java.util.Vector;

import android.content.Context;

import com.lighthousesignal.lsslib.ScanService;
import com.lighthousesignal.lsslib.ServiceManager;
import com.lighthousesignal.lsslib.WifiData;

public class FingerprintManager extends ServiceManager<FingerprintService> {

	public FingerprintManager(Context context) {
		super(context, FingerprintService.class);

		setScanMode(ScanService.MODE_CONTINUOUS);
		setScanDuration(1);
	}

	public void startScans() {
		startService();
	}

	public void stopScans() {
		stopService();
	}

	public Vector<WifiData> finishScans() {
		// Get wifi data from service from this round
		Vector<WifiData> collection = ((FingerprintService) mService).getCollectedData();
		System.out.println("fingerprintmanager collection size is " + collection.size());
		System.out.println("fingerprintmanager first elemen is" + collection.get(0).getScanResults().size());

		//reset();

		return collection;
	}

	public void incTryCount() {
		// TODO?
	}

	public void reset() {
		((FingerprintService) mService).clearCollectedData();
	}
}
