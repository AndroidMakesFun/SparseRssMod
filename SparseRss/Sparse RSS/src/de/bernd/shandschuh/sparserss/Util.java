package de.bernd.shandschuh.sparserss;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.bernd.shandschuh.sparserss.service.FetcherService;

public class Util {

	public static void toastMessage(Activity activityIn, final String Text) {
		if (activityIn == null) {
			return;
		}
		final Activity activity = activityIn;
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(activity, Text, Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	public static void toastMessageLong(Activity activityIn, final String Text) {
		if (activityIn == null) {
			return;
		}
		final Activity activity = activityIn;
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(activity, Text, Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	////////////////////////////////////////////////////////
	//    aus MainTabActivity   ////////////////////////////
	////////////////////////////////////////////////////////
	
	public static boolean isCurrentlyRefreshing(Activity activity) {
		ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FetcherService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private static Boolean LIGHTTHEME;
	
	public static boolean isLightTheme(Context context) {
		if (LIGHTTHEME == null) {
			LIGHTTHEME = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_LIGHTTHEME, true);
		}
		return LIGHTTHEME;
	}

	public static final String PREFERENCE_TEST_LIST_PREFS = "PREFERENCE_TEST_LIST_PREFS";

	public static void setTestListPrefs(Context context, boolean wert) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PREFERENCE_TEST_LIST_PREFS, wert);
		editor.commit();
	}

	public static boolean getTestListPrefs(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFERENCE_TEST_LIST_PREFS, true);
	}

}
