package de.bernd.shandschuh.sparserss;

import java.io.File;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;
import android.widget.Toast;
import de.bernd.shandschuh.sparserss.provider.FeedData;
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
	// aus MainTabActivity ////////////////////////////
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
		if (context != null) {
			if (LIGHTTHEME == null) {
				LIGHTTHEME = PreferenceManager.getDefaultSharedPreferences(context)
						.getBoolean(Strings.SETTINGS_LIGHTTHEME, true);
			}
			return LIGHTTHEME;
		}
		return true;
	}

	public static final String PREFERENCE_TEST_LIST_PREFS = "PREFERENCE_TEST_LIST_PREFS";

	public static void setTestListPrefs(Context context, boolean wert) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PREFERENCE_TEST_LIST_PREFS, wert);
		editor.commit();
	}

	/**
	 * True means Textlist, not CardView !
	 */
	public static boolean getTestListPrefs(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFERENCE_TEST_LIST_PREFS, true);
	}

	private static File imageFolder = null;

	/**
	 * /storage/emulated/0/Android/data/de.bernd.shandschuh.sparserss/files/
	 * images
	 */
	public static File getImageFolderFile(Context context) {
		if (imageFolder != null) {
			return imageFolder;
		}
		if (context == null) {
			return null;
		}
		imageFolder = context.getExternalFilesDir("images");
		if (!imageFolder.exists()) {
			imageFolder.mkdir();
		}
		return imageFolder;
	}

	public static final String PREFERENCE_VIEWER_PREFS = "PREFERENCE_VIEWER_PREFS";

	/**
	 * Konfiguriert den Viewer je Feed per Prefs
	 * 
	 * @param viewerInt
	 */
	public static void setViewerPrefs(Context context, String FeedId, int viewerInt) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREFERENCE_VIEWER_PREFS + FeedId, viewerInt);
		editor.commit();
	}

	public static int getViewerPrefs(Context context, String FeedId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(PREFERENCE_VIEWER_PREFS + FeedId, 0); // 0 Feed
	}

	public static boolean showPics(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean ret = prefs.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false);
		return !ret; // disable != showPics
	}

	public static final String SETTINGS_SHOW_COVER = "SETTINGS_SHOW_COVER";

	/**
	 * boolean per feed to load and show a Background Cover
	 */
	public static boolean showCover(Context context, String feedid) {
		if (!showPics(context)) {
			return false;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean ret = prefs.getBoolean(SETTINGS_SHOW_COVER + feedid, true);
		return ret;
	}

	public static void setShowCover(Context context, String FeedId, boolean show) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(SETTINGS_SHOW_COVER + FeedId, show);
		editor.commit();
	}

	public static String getVersionNumber(Context context) {
		String version = "?";
		try {
			PackageInfo packagInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = packagInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
		}
		;

		return version;
	}

	/**
	 * button immage size is 24 dp ( button 32 dp) ret button immage size in
	 * pixel
	 */
	public static int getButtonSizeInPixel(Context context) {
		// Resources r = context.getResources();
		// return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
		// 60, r.getDisplayMetrics());
		return (int) (24 * Resources.getSystem().getDisplayMetrics().density); // 4.0
																				// *
																				// 24
																				// =
																				// 96
	}

	public static TextDrawable getRoundButtonImage(Context context, Object colorObject, String title) {

		String letter;
		if (title == null || "".equals(title)) {
			letter = title = "F";
		} else {
			letter = title.substring(0, 1).toUpperCase();
		}

		int buttonSize = getButtonSizeInPixel(context);
		ColorGenerator generator = ColorGenerator.DEFAULT;
		int color = generator.getColor(title); // The color is specific to the
												// feedId (which shouldn't
												// change)

		TextDrawable textDrawable = TextDrawable.builder().beginConfig().width(buttonSize) // /3
																							// entfernt
																							// width
																							// in
																							// px
				.height(buttonSize) // height in px
				.endConfig().buildRound(letter, color);
		return textDrawable;
	}

	// http://stackoverflow.com/questions/20743859/imageview-rounded-corners
	public static Bitmap getRoundedBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = 12;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		// canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		// //getRoundedCornerBitmap
		canvas.drawOval(rectF, paint); // getRoundedBitmap

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static int getFeedIdZuEntryId(Context context, String id) {
		if (id == null || "".equals(id)) {
			return 0;
		}
		Uri selectUri = FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
		Cursor entryCursor = context.getContentResolver().query(selectUri, null, null, null, null);

		int feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);

		int feedId = -1;
		if (entryCursor.moveToFirst()) {
			feedId = entryCursor.getInt(feedIdPosition); // bah
		}
		entryCursor.close();
		return feedId;
	}

	public static final String SETTINGS_SHOW_BOTTOM_BAR = "SETTINGS_SHOW_BOTTOM_BAR";

	public static boolean showBottomBar(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean ret = prefs.getBoolean(SETTINGS_SHOW_BOTTOM_BAR, true);
		return ret;
	}

	public static void setShowBottomBar(Context context, boolean value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(SETTINGS_SHOW_BOTTOM_BAR, value);
		editor.commit();
	}

	
	public static BitmapImageViewTarget getRoundedImageTarget(final Context context, final ImageView imageView,
			final float radius) {
		
		return new BitmapImageViewTarget(imageView) {
			@Override
			protected void setResource(final Bitmap resource) {
				RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory
						.create(context.getResources(), resource);
				circularBitmapDrawable.setCornerRadius(radius);
				imageView.setImageDrawable(circularBitmapDrawable);
			}
		};
	}

	public static String takeFirstSrc(String strHtml) {
		if(strHtml!=null && !"".equals(strHtml)){
			int pos = strHtml.indexOf("src=\"");
			if(pos>0){
				pos+=5;
				int posEnd=strHtml.indexOf("\"", pos);
				if(posEnd>-1){
					String url=strHtml.substring(pos,posEnd);
					return url;
				}
			}
			// src auch ohne "
			pos = strHtml.indexOf("src=");
			if(pos>0){
				pos+=4;
				int posEnd=strHtml.indexOf(" ", pos);
				if(posEnd>-1){
					String url=strHtml.substring(pos,posEnd);
					return url;
				}
			}
		}
		return null;
	}


	public static final String PREFERENCE_BROWSER_PACKAGE= "PREFERENCE_BROWSER_PACKAGE";

	/**
	 * @param wert as package from browser
	 */
	public static void setBrowserPackagePrefs(Context context, String wert) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREFERENCE_BROWSER_PACKAGE, wert);
		editor.commit();
	}

	/**
	 * Letzte auswahl aus popup_menu_browser
	 * @param context
	 * @return
	 * 0 default/system browser null
	 * 1 chrome "com.android.chrome"
	 * 2 edge "com.microsoft.emmx"
	 * 3 firefox klar "org.mozilla.klar"
	 */
	public static String getBrowserPackagePrefs(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREFERENCE_BROWSER_PACKAGE,null);
	}
}
