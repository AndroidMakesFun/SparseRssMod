/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package de.bernd.shandschuh.sparserss;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;

import de.bernd.shandschuh.sparserss.service.RssJobService;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = BootCompletedBroadcastReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
			// Huawei: wird nie aufgerufen ?!
			Log.d(TAG, "onReceive: BOOTCOMPLETE_BROADCASTRECEIVER");

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			preferences.edit().putLong(Strings.PREFERENCE_LASTSCHEDULEDREFRESH, 0).apply();
			if (preferences.getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {

				//context.startService(new Intent(context, RefreshService.class));
				// ersetzt durch folgende Zeilen:

				Log.d(TAG, "Scheduling job in BootCompletedBroadcastReceiver");
				Util.scheduleJob(context,true);

			/**
				ComponentName mServiceComponent = new ComponentName(context, RssJobService.class);
				JobInfo.Builder jobBuilder = new JobInfo.Builder(1, mServiceComponent);
				//	builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);  // nur wlan
				jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

				final String SIXTYMINUTES = "3600000";
				int time = 3600000;
				try {
					time = Math.max(60000, Integer.parseInt(preferences.getString(Strings.SETTINGS_REFRESHINTERVAL, SIXTYMINUTES)));
				} catch (Exception exception) {
                    Log.d(TAG, "EXP " + exception.toString());
				}
				jobBuilder.setMinimumLatency(time);  //2 * 1000); // wait at least 2 Sek

				jobBuilder.setOverrideDeadline(3 * 1000); // maximum delay

				PersistableBundle extras = new PersistableBundle();
				//extras.putString(Strings.FEEDID, id);
				jobBuilder.setExtras(extras);
				JobScheduler tm = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
				tm.schedule(jobBuilder.build());
			 */

				//test
				// context.sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
			}
			// context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
	}

}
