package de.bernd.shandschuh.sparserss;

import android.app.Activity;
import android.widget.Toast;

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
}
