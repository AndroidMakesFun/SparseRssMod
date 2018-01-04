package de.bernd.shandschuh.sparserss;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class SendLogActivity extends Activity {
	// bah
	private String[] emails = { "android.makes.fun@gmail.com" };
	private StringBuilder log = new StringBuilder();

	public final static String TAG = "AndroidLogCollector";//$NON-NLS-1$
	public final static String LINE_SEPARATOR = System.getProperty("line.separator");//$NON-NLS-1$

	public static final String ACTION_SEND_LOG = "com.xtralogic.logcollector.intent.action.SEND_LOG";//$NON-NLS-1$
	public static final String EXTRA_SEND_INTENT_ACTION = "com.xtralogic.logcollector.intent.extra.SEND_INTENT_ACTION";//$NON-NLS-1$
	public static final String EXTRA_DATA = "com.xtralogic.logcollector.intent.extra.DATA";//$NON-NLS-1$
	public static final String EXTRA_ADDITIONAL_INFO = "com.xtralogic.logcollector.intent.extra.ADDITIONAL_INFO";//$NON-NLS-1$
	public static final String EXTRA_SHOW_UI = "com.xtralogic.logcollector.intent.extra.SHOW_UI";//$NON-NLS-1$
	public static final String EXTRA_FILTER_SPECS = "com.xtralogic.logcollector.intent.extra.FILTER_SPECS";//$NON-NLS-1$
	public static final String EXTRA_FORMAT = "com.xtralogic.logcollector.intent.extra.FORMAT";//$NON-NLS-1$
//	public static final String EXTRA_BUFFER = "com.xtralogic.logcollector.intent.extra.BUFFER";//$NON-NLS-1$
	public static final String EXTRA_BUFFER = "events"; //('main' (default), 'radio', 'events')

	final int MAX_LOG_MESSAGE_LENGTH = 100000;

	private AlertDialog mMainDialog;
	private Intent mSendIntent;
	private CollectLogTask mCollectLogTask;
	private ProgressDialog mProgressDialog;
	private String mAdditonalInfo;
	private boolean mShowUi;
	private String[] mFilterSpecs;
	private String mFormat;
	private String mBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSendIntent = null;

		Intent intent = getIntent();
		if (null != intent) {
			String action = intent.getAction();
			if (ACTION_SEND_LOG.equals(action)) {
				String extraSendAction = intent.getStringExtra(EXTRA_SEND_INTENT_ACTION);
				if (extraSendAction == null) {
					Log.e(TAG, "Quiting, EXTRA_SEND_INTENT_ACTION is not supplied");//$NON-NLS-1$
					finish();
					return;
				}

				mSendIntent = new Intent(extraSendAction);
				mSendIntent.setType("text/plain");//$NON-NLS-1$

				// String[] emails = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
				if (emails != null) {
					mSendIntent.putExtra(Intent.EXTRA_EMAIL, emails);
				}

				String[] ccs = intent.getStringArrayExtra(Intent.EXTRA_CC);
				if (ccs != null) {
					mSendIntent.putExtra(Intent.EXTRA_CC, ccs);
				}

				String[] bccs = intent.getStringArrayExtra(Intent.EXTRA_BCC);
				if (bccs != null) {
					mSendIntent.putExtra(Intent.EXTRA_BCC, bccs);
				}

				String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
				if (subject != null) {
					mSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
				}

				mAdditonalInfo = intent.getStringExtra(EXTRA_ADDITIONAL_INFO);
				mShowUi = intent.getBooleanExtra(EXTRA_SHOW_UI, false);
				mFilterSpecs = intent.getStringArrayExtra(EXTRA_FILTER_SPECS);
				mFormat = intent.getStringExtra(EXTRA_FORMAT);
				mBuffer = intent.getStringExtra(EXTRA_BUFFER);
			}
		}

		if (null == mSendIntent) {
			// standalone application
			mShowUi = true;
			mSendIntent = new Intent(Intent.ACTION_SEND);
			mSendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.message_subject));
			mSendIntent.setType("text/plain");//$NON-NLS-1$
			if (emails != null) {
				mSendIntent.putExtra(Intent.EXTRA_EMAIL, emails);
			}

//			 mAdditonalInfo = getString(R.string.device_info_fmt, getVersionNumber(this), Build.MODEL, Build.VERSION.RELEASE, getFormattedKernelVersion(), Build.DISPLAY);
			mFormat = "time";
		}

		if (mShowUi) {
			mMainDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.main_dialog_text))
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							collectAndSendLog();
						}
					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							finish();
						}
					}).show();
		} else {
			collectAndSendLog();
		}
	}

	@SuppressWarnings("unchecked")
	void collectAndSendLog() {
		// bah
		// mAdditonalInfo = getString(R.string.device_info_fmt, getVersionNumber(this), Build.MODEL, Build.VERSION.RELEASE, getFormattedKernelVersion(), Build.DISPLAY);
		log.append("Log:");
		log.append(LINE_SEPARATOR);
		log.append("Version: "+ Util.getVersionNumber(this));
		log.append(LINE_SEPARATOR);
		log.append("Model: "+ Build.MODEL);
		log.append(LINE_SEPARATOR);
		log.append("Release: " + Build.VERSION.RELEASE);
		log.append(LINE_SEPARATOR);
		log.append("Kernel: "+ getFormattedKernelVersion());
		log.append(LINE_SEPARATOR);
		log.append(LINE_SEPARATOR);
		log.append("Display: "+ Build.DISPLAY);
		log.append(LINE_SEPARATOR);


		 /*Usage: logcat [options] [filterspecs]
        options include:
          -s              Set default filter to silent.
                          Like specifying filterspec '*:s'
          -f <filename>   Log to file. Default to stdout
          -r [<kbytes>]   Rotate log every kbytes. (16 if unspecified). Requires -f
          -n <count>      Sets max number of rotated logs to <count>, default 4
          -v <format>     Sets the log print format, where <format> is one of:

                          brief process tag thread raw time threadtime long

          -c              clear (flush) the entire log and exit
          -d              dump the log and then exit (don't block)
          -g              get the size of the log's ring buffer and exit
          -b <buffer>     request alternate ring buffer
                          ('main' (default), 'radio', 'events')
          -B              output the log in binary
        filterspecs are a series of
          <tag>[:priority]

        where <tag> is a log component tag (or * for all) and priority is:
          V    Verbose
          D    Debug
          I    Info
          W    Warn
          E    Error
          F    Fatal
          S    Silent (supress all output)

        '*' means '*:d' and <tag> by itself means <tag>:v

        If not specified on the commandline, filterspec is set from ANDROID_LOG_TAGS.
        If no filterspec is found, filter defaults to '*:I'

        If not specified with -v, format is set from ANDROID_PRINTF_LOG
        or defaults to "brief"*/

		ArrayList<String> list = new ArrayList<String>();
		
		if (mFormat != null) {
			list.add("-v");
			list.add(mFormat);
		}

		if (mBuffer != null) {
			list.add("-b");
			list.add(mBuffer);
		}

		if (mFilterSpecs != null) {
			for (String filterSpec : mFilterSpecs) {
				list.add(filterSpec);
			}
		}

		mCollectLogTask = (CollectLogTask) new CollectLogTask().execute(list);
	}

	private class CollectLogTask extends AsyncTask<ArrayList<String>, Void, StringBuilder> {
		@Override
		protected void onPreExecute() {
			showProgressDialog(getString(R.string.acquiring_log_progress_dialog_message));
		}

		@Override
		protected StringBuilder doInBackground(ArrayList<String>... params) {
			// final StringBuilder log = new StringBuilder();
			try {
				ArrayList<String> commandLine = new ArrayList<String>();
				commandLine.add("logcat");//$NON-NLS-1$
				commandLine.add("-d");//$NON-NLS-1$
				ArrayList<String> arguments = ((params != null) && (params.length > 0)) ? params[0] : null;
				if (null != arguments) {
					commandLine.addAll(arguments);
				}

				Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = bufferedReader.readLine()) != null) {
					log.append(line);
					log.append(LINE_SEPARATOR);
				}
			} catch (IOException e) {
				Log.e(TAG, "CollectLogTask.doInBackground failed", e);//$NON-NLS-1$
			}

			return log;
		}

		@Override
		protected void onPostExecute(StringBuilder loge) {
			if (null != log) {
				// truncate if necessary
				int keepOffset = Math.max(log.length() - MAX_LOG_MESSAGE_LENGTH, 0);
				if (keepOffset > 0) {
					log.delete(0, keepOffset);
				}

				if (mAdditonalInfo != null) {
					log.insert(0, LINE_SEPARATOR);
					log.insert(0, mAdditonalInfo);
				}

				mSendIntent.putExtra(Intent.EXTRA_TEXT, log.toString());
				startActivity(Intent.createChooser(mSendIntent, getString(R.string.chooser_title)));
				dismissProgressDialog();
				dismissMainDialog();
				finish();
			} else {
				dismissProgressDialog();
				showErrorDialog(getString(R.string.failed_to_get_log_message));
			}
		}
	}

	void showErrorDialog(String errorMessage) {
		new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(errorMessage).setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						finish();
					}
				}).show();
	}

	void dismissMainDialog() {
		if (null != mMainDialog && mMainDialog.isShowing()) {
			mMainDialog.dismiss();
			mMainDialog = null;
		}
	}

	void showProgressDialog(String message) {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cancellCollectTask();
				finish();
			}
		});
		mProgressDialog.show();
	}

	private void dismissProgressDialog() {
		if (null != mProgressDialog && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	void cancellCollectTask() {
		if (mCollectLogTask != null && mCollectLogTask.getStatus() == AsyncTask.Status.RUNNING) {
			mCollectLogTask.cancel(true);
			mCollectLogTask = null;
		}
	}

	@Override
	protected void onPause() {
		cancellCollectTask();
		dismissProgressDialog();
		dismissMainDialog();

		super.onPause();
	}


	private String getFormattedKernelVersion() {
		String procVersionStr;

		try {
			BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
			try {
				procVersionStr = reader.readLine();
			} finally {
				reader.close();
			}

			final String PROC_VERSION_REGEX = "\\w+\\s+" + /* ignore: Linux */
			"\\w+\\s+" + /* ignore: version */
			"([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
			"\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
			"\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
			"([^\\s]+)\\s+" + /* group 3: #26 */
			"(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
			"(.+)"; /* group 4: date */

			Pattern p = Pattern.compile(PROC_VERSION_REGEX);
			Matcher m = p.matcher(procVersionStr);

			if (!m.matches()) {
				Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
				return "Unavailable";
			} else if (m.groupCount() < 4) {
				Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
				return "Unavailable";
			} else {
				return (new StringBuilder(m.group(1)).append("\n").append(m.group(2)).append(" ").append(m.group(3)).append("\n").append(m.group(4))).toString();
			}
		} catch (IOException e) {
			Log.e(TAG, "IO Exception when getting kernel version for Device Info screen", e);

			return "Unavailable";
		}
	}
}