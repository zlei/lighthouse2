package com.lighthousesignal.fingerprint2.activities;

import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lighthousesignal.fingerprint2.R;
import com.lighthousesignal.fingerprint2.fragments.MapListFragment;
import com.lighthousesignal.fingerprint2.fragments.ReviewFragment;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.viewpagerindicator.TabPageIndicator;

public class MainActivity extends SherlockFragmentActivity {
	private PageAdapter mPageAdapter;

	private SharedPreferences mPrefs;
	protected static ImageLoader imageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.overridePendingTransition(R.anim.slide_in_left,
				R.anim.slide_out_left);

		setContentView(R.layout.activity_main);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(false);
		setupFragments();
	}

	private void setupFragments() {
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this,
				MapListFragment.class.getName()));
		fragments
				.add(Fragment.instantiate(this, ReviewFragment.class.getName()));
		mPageAdapter = new PageAdapter(getSupportFragmentManager(), fragments);

		ViewPager pager = (ViewPager) super.findViewById(R.id.viewpager);
		pager.setAdapter(mPageAdapter);

		TabPageIndicator tabIndicator = (TabPageIndicator) findViewById(R.id.tabs);
		tabIndicator.setViewPager(pager);
	}

	private class PageAdapter extends FragmentPagerAdapter {

		private List<Fragment> mFragments;

		public PageAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			mFragments = fragments;
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return "Maps";
			case 1:
				return "Review";
			default:
				return "Title";
			}
		}
	}

	/**
	 * main menu settings options
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_settings:
			onOperationSettings();
			break;

		case R.id.action_clear_cache:
			onOperationClearCache();
			break;

		case R.id.action_logout:
			onOperationLogout();
			break;

		case R.id.action_exit:
			finish();
			break;
		}

		return true;
	}

	private void onOperationSettings() {
		Intent i_settings = new Intent(this, SettingsActivity.class);
		startActivity(i_settings);
	}

	private void onOperationClearCache() {
		imageLoader = ImageLoader.getInstance();
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.confirm_clear_cache_title))
				.setMessage(getString(R.string.confirm_clear_cache_text))
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								imageLoader.clearMemoryCache();
								imageLoader.clearDiscCache();
								Toast.makeText(MainActivity.this,
										"Cache Cleared!", Toast.LENGTH_SHORT)
										.show();
							}
						}).setNegativeButton("No", null).show();
	}

	private void onOperationLogout() {
		Intent i_login = new Intent(this, LoginActivity.class);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (mPrefs.contains(BasicActivity.PREF_LOGIN_TOKEN)) {
			mPrefs.edit().remove(BasicActivity.PREF_LOGIN_TOKEN).commit();
		}
		startActivity(i_login);
		finish();
	}
}
