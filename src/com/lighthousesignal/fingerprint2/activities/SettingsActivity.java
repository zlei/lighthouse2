package com.lighthousesignal.fingerprint2.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.lighthousesignal.fingerprint2.R;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.overridePendingTransition(R.anim.slide_in_left,
				R.anim.slide_out_left);

		addPreferencesFromResource(R.xml.pref_general);
	}

}
