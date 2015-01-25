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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import de.bernd.shandschuh.sparserss.provider.FeedData;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class EntryActivity extends Activity implements android.widget.SeekBar.OnSeekBarChangeListener {

	/*
	 * private static final String NEWLINE = "\n";
	 * 
	 * private static final String BR = "<br/>";
	 */

	private static final String TEXT_HTML = "text/html";

	private static final String UTF8 = "utf-8";

	private static final String OR_DATE = " or date ";

	private static final String DATE = "(date=";

	private static final String AND_ID = " and _id";

	private static final String ASC = "date asc, _id desc limit 1";

	private static final String DESC = "date desc, _id asc limit 1";

	private static final String CSS = "<head><style type=\"text/css\">body {max-width: 100%}\nimg {max-width: 100%; height: auto;}\ndiv[style] {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";

	private static final String FONT_START = CSS + "<body link=\"#97ACE5\" text=\"#C0C0C0\">";

	private static final String FONT_FONTSIZE_START = CSS + "<body link=\"#97ACE5\" text=\"#C0C0C0\"><font size=\"+";

	private static final String FONTSIZE_START = "<font size=\"+";

	private static final String FONTSIZE_MIDDLE = "\">";

	private static final String FONTSIZE_END = "</font>";

	private static final String FONT_END = "</font><br/><br/><br/><br/></body>";

	private static final String BODY_START = "<body>";

	private static final String BODY_END = "<br/><br/><br/><br/></body>";

	private static final int BUTTON_ALPHA = 180;

	private static final String IMAGE_ENCLOSURE = "[@]image/";

	private static final String TEXTPLAIN = "text/plain";

	private static final String BRACKET = " (";

	private int titlePosition;

	private int datePosition;

	private int abstractPosition;

	private int linkPosition;

	private int feedIdPosition;

	private int favoritePosition;

	private int readDatePosition;

	private int enclosurePosition;

	private int authorPosition;

	private String _id;

	private String _nextId;

	private String _previousId;

	private Uri uri;

	private Uri parentUri;

	private int feedId;

	boolean favorite;

	private boolean showRead;

	private boolean canShowIcon;

	private byte[] iconBytes;

	private WebView webView;

	private WebView webView0; // only needed for the animation

	private ViewFlipper viewFlipper;

	private ImageButton nextButton;

	private ImageButton urlButton;

	private ImageButton previousButton;

	private ImageButton playButton;

	int scrollX;

	int scrollY;

	private String link;

	private LayoutParams layoutParams;

	private View content;

	private SharedPreferences preferences;

	private boolean localPictures;

	private TextView titleTextView;
	private int mAufrufart = 0;
	private static final int AUFRUFART_FEED = 0;
	private static final int AUFRUFART_BROWSER = 1;
	private static final int AUFRUFART_MOBILIZE = 2;
	private static final int AUFRUFART_INSTAPAPER = 3;
	private static final int AUFRUFART_READABILITY = 4;
	private static final int AUFRUFART_WEBVIEW = 5;

	private Activity mActivity = null;

	// private boolean viewFulscreen = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MainTabActivity.isLightTheme(this)) {
			setTheme(R.style.Theme_Light);
		}
		super.onCreate(savedInstanceState);
		mActivity = this;
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(true);

		int titleId = -1;

		if (MainTabActivity.POSTGINGERBREAD) {
			// getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
			canShowIcon = true;
			setContentView(R.layout.entry);

			try {
				/* This is a trick as com.android.internal.R.id.action_bar_title is not directly accessible */
				titleId = (Integer) Class.forName("com.android.internal.R$id").getField("action_bar_title").get(null);
			} catch (Exception exception) {

			}
		} else {
			canShowIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);
			setContentView(R.layout.entry);
			titleId = android.R.id.title;
		}

		try {
			titleTextView = (TextView) findViewById(titleId);
			titleTextView.setSingleLine(true);
			titleTextView.setHorizontallyScrolling(true);
			titleTextView.setMarqueeRepeatLimit(1);
			titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			titleTextView.setFocusable(true);
			titleTextView.setFocusableInTouchMode(true);
		} catch (Exception e) {
			// just in case for non standard android, nullpointer etc
		}

		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		showRead = getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);
		mAufrufart = getIntent().getIntExtra(EntriesListActivity.EXTRA_AUFRUFART, 0);
		feedId = 0;

		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);

		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);

		enclosurePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
		authorPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.AUTHOR);

		if (entryCursor.moveToFirst()) {
			link = entryCursor.getString(linkPosition);
			link = fixLink(link);
			timestamp = entryCursor.getLong(datePosition);
		}

		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		nextButton = (ImageButton) findViewById(R.id.next_button);
		urlButton = (ImageButton) findViewById(R.id.url_button);
		urlButton.setAlpha(BUTTON_ALPHA + 30);
		previousButton = (ImageButton) findViewById(R.id.prev_button);
		playButton = (ImageButton) findViewById(R.id.play_button);
		playButton.setAlpha(BUTTON_ALPHA);

		// verbergen
		findViewById(R.id.button_layout).setVisibility(View.GONE);
		findViewById(R.id.entry_date_layout).setVisibility(View.GONE);

		viewFlipper = (ViewFlipper) findViewById(R.id.content_flipper);

		layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		webView = new WebView(this);

		viewFlipper.addView(webView, layoutParams);

		OnKeyListener onKeyEventListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == 92 || keyCode == 94) {
						scrollUp();
						return true;
					} else if (keyCode == 93 || keyCode == 95) {
						scrollDown();
						return true;
					}
				}
				return false;
			}
		};
		webView.setOnKeyListener(onKeyEventListener);

		content = findViewById(R.id.entry_content);

		webView0 = new WebView(this);
		webView0.setOnKeyListener(onKeyEventListener);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean gestures = preferences.getBoolean(Strings.SETTINGS_GESTURESENABLED, true);

		// nicht schön, aber funkt für onDoubleTap
		// final GestureDetector gestureDetector = new GestureDetector(this, new OnGestureListener() {
		@SuppressWarnings("deprecation")
		final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

			public boolean onDown(MotionEvent e) {
				return false;
			}

			public boolean onDoubleTap(MotionEvent e) {
				// displayFullscreen();
				setNavVisibility(false);
				return false;
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (gestures) {
					if (Math.abs(velocityY) < Math.abs(velocityX)) {
						if (velocityX > 800) {
							if (previousButton.isEnabled()) {
								previousEntry(true);
							}
						} else if (velocityX < -800) {
							if (nextButton.isEnabled()) {
								nextEntry(true);
							}
						}
					} else { // mehr y als x
						if (velocityY > 800) {
							setNavVisibility(true);
						} else if (velocityY < -800) {
							setNavVisibility(false);
						}
					}
				}
				return false;
			}

			public void onLongPress(MotionEvent e) {

			}

			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if (distanceY > 0.0) {
					if (Math.abs(distanceY) > 30) {
						// setNavVisibility(false);
					}
				} else {
					if (Math.abs(distanceY) > 30) {
						// setNavVisibility(true);
					}
				}
				return false;
			}

			public void onShowPress(MotionEvent e) {

			}

			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}
		});

		OnTouchListener onTouchListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		webView.setOnTouchListener(onTouchListener);

		content.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return true; // different to the above one!
			}
		});

		webView0.setOnTouchListener(onTouchListener);

		scrollX = 0;
		scrollY = 0;

		// OnSystemUiVisibilityChangeListener onSystemUiVisibilityChangeListener = new OnSystemUiVisibilityChangeListener(){
		//
		// @Override
		// public void onSystemUiVisibilityChange(int visibility) {
		// int diff = mLastSystemUiVis ^ visibility;
		// mLastSystemUiVis = visibility;
		// if ((diff&View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0
		// && (visibility&View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
		// setNavVisibility(true);
		// }
		// }
		//
		// };
		// webView.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChangeListener);
		// displayFullscreen();

		SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mIntScalePercent = prefs.getInt(PREFERENCE_SCALE + mAufrufart, 50);

		setZoomsScale();
		MyWebViewClient myWebViewClient = new MyWebViewClient();
		webView.setWebViewClient(myWebViewClient);
		webView0.setWebViewClient(myWebViewClient);

		// 1 Browser schon aus Liste herraus
		if (mAufrufart == AUFRUFART_FEED) {
			reload();
		} else if (mAufrufart == AUFRUFART_MOBILIZE) {
			loadMoblize();
		} else if (mAufrufart == AUFRUFART_INSTAPAPER) {
			loadInstapaper();
		} else if (mAufrufart == AUFRUFART_READABILITY) {
			loadReadability();
		}
	}// onCreate

	@SuppressLint("NewApi")
	void setZoomsScale() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			webView.getSettings().setTextZoom(mIntScalePercent * 2); // % von 100 // API 14 !!
			if (webView0 != null) {
				webView0.getSettings().setTextZoom(mIntScalePercent * 2); // % von 100 // API 14 !!
			}
		}
	}

	private void loadReadability() {
		webView.loadUrl("http://www.readability.com/m?url=" + link);
	}

	private void loadInstapaper() {
		webView.loadUrl("http://www.instapaper.com/m?u=" + link);
	}

	@SuppressLint("NewApi")
	private void loadMoblize() {
		readUrl();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		webView.restoreState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}
		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		if (MainTabActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onResume(webView);
		}

		// wegen Absturz durch leere _nextId
		setupButton(previousButton, false, timestamp);
		setupButton(nextButton, true, timestamp);

	}

	public static String fixLink(String strLink) {
		if (strLink == null)
			return null;
		if (strLink.endsWith("feed/atom/")) {
			strLink = strLink.substring(0, strLink.length() - "feed/atom/".length());
		}
		return strLink;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	// DEAD -> EntriesListActivity
	// public void markRead() {
	//
	// ContentValues values = new ContentValues();
	// values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
	//
	// Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
	// int readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
	//
	// if (entryCursor.moveToFirst()) {
	//
	// if (entryCursor.isNull(readDatePosition)) {
	// getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
	// }
	// }
	// entryCursor.close();
	// }

	@SuppressLint("NewApi")
	private void reload() {
		if (_id != null && _id.equals(uri.getLastPathSegment())) {
			return;
		}

		_id = uri.getLastPathSegment();

		ContentValues values = new ContentValues();

		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());

		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);

		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);

			if (entryCursor.isNull(readDatePosition)) {
				getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
			}
			if (abstractText == null) {
				String link = entryCursor.getString(linkPosition);

				entryCursor.close();
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));

			} else {
				setTitle(entryCursor.getString(titlePosition));
				if (titleTextView != null) {
					titleTextView.requestFocus(); // restart ellipsize
				}

				int _feedId = entryCursor.getInt(feedIdPosition);

				if (feedId != _feedId) {
					if (feedId != 0) {
						iconBytes = null; // triggers re-fetch of the icon
					}
					feedId = _feedId;
				}

				if (canShowIcon) {
					if (iconBytes == null || iconBytes.length == 0) {
						Cursor iconCursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(Integer.toString(feedId)),
								new String[] { FeedData.FeedColumns._ID, FeedData.FeedColumns.ICON }, null, null, null);

						if (iconCursor.moveToFirst()) {
							iconBytes = iconCursor.getBlob(1);
						}
						iconCursor.close();
					}

					if (iconBytes != null && iconBytes.length > 0) {
						int bitmapSizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
						Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
						if (bitmap != null) {
							if (bitmap.getHeight() != bitmapSizeInDip) {
								bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
							}

							if (MainTabActivity.POSTGINGERBREAD) {
								CompatibilityHelper.setActionBarDrawable(this, new BitmapDrawable(bitmap));
							} else {
								setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bitmap));
							}
						}
					}
				}

				timestamp = entryCursor.getLong(datePosition);

				Date date = new Date(timestamp);

				StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(this).format(date)).append(' ').append(DateFormat.getTimeFormat(this).format(date));

				String author = entryCursor.getString(authorPosition);

				if (author != null) {
					dateStringBuilder.append(BRACKET).append(author).append(')');
				}

				((TextView) findViewById(R.id.entry_date)).setText(dateStringBuilder);

				final ImageView imageView = (ImageView) findViewById(android.R.id.icon);

				favorite = entryCursor.getInt(favoritePosition) == 1;

				imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
				imageView.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						// if (!viewFulscreen) {
						favorite = !favorite;
						imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
						ContentValues values = new ContentValues();

						values.put(FeedData.EntryColumns.FAVORITE, favorite ? 1 : 0);
						getContentResolver().update(uri, values, null, null);
						// }
					}
				});
				// loadData does not recognize the encoding without correct html-header
				localPictures = abstractText.indexOf(Strings.IMAGEID_REPLACEMENT) > -1;

				if (localPictures) {
					abstractText = abstractText.replace(Strings.IMAGEID_REPLACEMENT, _id + Strings.IMAGEFILE_IDSEPARATOR);
				}

				if (preferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
					abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
					webView.getSettings().setBlockNetworkImage(true);
				} else {
					if (webView.getSettings().getBlockNetworkImage()) {
						/*
						 * setBlockNetwortImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
						 */
						webView.loadData(Strings.EMPTY, TEXT_HTML, UTF8);
						webView.getSettings().setBlockNetworkImage(false);
					}
				}

				int fontsize = Integer.parseInt(preferences.getString(Strings.SETTINGS_FONTSIZE, Strings.ONE));

				/*
				 * if (abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1) { abstractText = abstractText.replace(NEWLINE, BR); }
				 */

				if (MainTabActivity.isLightTheme(this) || preferences.getBoolean(Strings.SETTINGS_BLACKTEXTONWHITE, false)) {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null,
								new StringBuilder(CSS).append(FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONTSIZE_END).toString(),
								TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(CSS).append(BODY_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.WHITE);
					content.setBackgroundColor(Color.WHITE);
				} else {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONT_END)
								.toString(), TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.BLACK);
					content.setBackgroundColor(Color.BLACK);
				}

				link = entryCursor.getString(linkPosition);
				link = fixLink(link);

				if (link != null && link.length() > 0) {
					urlButton.setEnabled(true);
					urlButton.setAlpha(BUTTON_ALPHA + 20);
					urlButton.setOnClickListener(new OnClickListener() {
						public void onClick(View view) {
							startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
						}
					});

					// ////////////////////////// AUSWERTUNG AUFRUFART
					// Uri feedUri=FeedData.EntryColumns.PARENT_URI(parentUri.getPath());
					//
					// int aufrufart=0;
					// Cursor cursor = getContentResolver().query(feedUri, FeedConfigActivity.PROJECTION, null, null, null);
					// if (cursor.moveToNext()) {
					// aufrufart=cursor.getInt(3); // 0.. {"Feed", "Browser", "Mobilize", "Instapaper"};
					// cursor.close();
					// }
					//
					// if(aufrufart==1){
					// // Browser öffnen
					// startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
					// entryCursor.close();
					// return;
					//
					// }else if (aufrufart==2){
					// readUrl();
					// entryCursor.close();
					// return;
					//
					// }else if (aufrufart==3){
					// webView.loadUrl("http://www.instapaper.com/m?u="+link);
					// entryCursor.close();
					// return;
					// }

				} else {
					urlButton.setEnabled(false);
					urlButton.setAlpha(80);
				}

				final String enclosure = entryCursor.getString(enclosurePosition);

				if (enclosure != null && enclosure.length() > 6 && enclosure.indexOf(IMAGE_ENCLOSURE) == -1) {
					playButton.setVisibility(View.VISIBLE);
					playButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							final int position1 = enclosure.indexOf(Strings.ENCLOSURE_SEPARATOR);

							final int position2 = enclosure.indexOf(Strings.ENCLOSURE_SEPARATOR, position1 + 3);

							final Uri uri = Uri.parse(enclosure.substring(0, position1));

							if (preferences.getBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED, true)) {
								Builder builder = new AlertDialog.Builder(EntryActivity.this);

								builder.setTitle(R.string.question_areyousure);
								builder.setIcon(android.R.drawable.ic_dialog_alert);
								if (position2 + 4 > enclosure.length()) {
									builder.setMessage(getString(R.string.question_playenclosure, uri,
											position2 + 4 > enclosure.length() ? Strings.QUESTIONMARKS : enclosure.substring(position2 + 3)));
								} else {
									try {
										builder.setMessage(getString(R.string.question_playenclosure, uri, (Integer.parseInt(enclosure.substring(position2 + 3)) / 1024f)
												+ getString(R.string.kb)));
									} catch (Exception e) {
										builder.setMessage(getString(R.string.question_playenclosure, uri, enclosure.substring(position2 + 3)));
									}
								}
								builder.setCancelable(true);
								builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										showEnclosure(uri, enclosure, position1, position2);
									}
								});
								builder.setNeutralButton(R.string.button_alwaysokforall, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										preferences.edit().putBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED, false).commit();
										showEnclosure(uri, enclosure, position1, position2);
									}
								});
								builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
								builder.show();
							} else {
								showEnclosure(uri, enclosure, position1, position2);
							}
						}
					});
				} else {
					playButton.setVisibility(View.GONE);
				}
				entryCursor.close();
				setupButton(previousButton, false, timestamp);
				setupButton(nextButton, true, timestamp);
				webView.scrollTo(scrollX, scrollY); // resets the scrolling
			}
		} else {
			entryCursor.close();
		}

		/*
		 * new Thread() { public void run() { sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET)); // this is slow } }.start();
		 */
	}

	private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
		try {
			startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + 3, position2)), 0);
		} catch (Exception e) {
			try {
				startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
			} catch (Throwable t) {
				Toast.makeText(EntryActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setupButton(ImageButton button, final boolean successor, long date) {
		StringBuilder queryString = new StringBuilder(DATE).append(date).append(AND_ID).append(successor ? '>' : '<').append(_id).append(')').append(OR_DATE)
				.append(successor ? '<' : '>').append(date);

		if (!showRead) {
			queryString.append(Strings.DB_AND).append(EntriesListAdapter.READDATEISNULL);
		}

		Cursor cursor = getContentResolver().query(parentUri, new String[] { FeedData.EntryColumns._ID }, queryString.toString(), null, successor ? DESC : ASC);

		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			button.setAlpha(BUTTON_ALPHA);

			final String id = cursor.getString(0);

			if (successor) {
				_nextId = id;
			} else {
				_previousId = id;
			}
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					if (successor) {
						nextEntry(false);
					} else {
						previousEntry(false);
					}
				}
			});
		} else {
			button.setEnabled(false);
			button.setAlpha(60);
		}
		cursor.close();
	}

	private void switchEntry2(String id, boolean animate, Animation inAnimation, Animation outAnimation) {
		uri = parentUri.buildUpon().appendPath(id).build();
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;

		// ausgeknippst, da viewer sich nicht mit Animation vertragen...
		// animate = false;
		if (mAufrufart > 0) {
			animate = false;
		}

		if (animate) {
			WebView dummy = webView; // switch reference
			webView = webView0;
			webView0 = dummy;
		}

		if (mAufrufart > 0) {
			// link laden
			Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
			if (entryCursor.moveToFirst()) {
				link = entryCursor.getString(linkPosition);
				link = fixLink(link);
				timestamp = entryCursor.getLong(datePosition);
			}
			entryCursor.close();

			if (mAufrufart == AUFRUFART_MOBILIZE) {
				loadMoblize();
			} else if (mAufrufart == AUFRUFART_INSTAPAPER) {
				loadInstapaper();
			} else if (mAufrufart == AUFRUFART_READABILITY) {
				loadReadability();
			}

			// markRead 2
			getContentResolver().update(uri, RSSOverview.getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);

			setupButton(previousButton, false, timestamp);
			setupButton(nextButton, true, timestamp);
		} else {
			reload();
		}

		if (animate) {
			viewFlipper.setInAnimation(inAnimation);
			viewFlipper.setOutAnimation(outAnimation);
			viewFlipper.addView(webView, layoutParams);
			viewFlipper.showNext();
			viewFlipper.removeViewAt(0);
		}
	}

	private void nextEntry(boolean animate) {
		mAnimationDirection = android.R.anim.slide_out_right;
		switchEntry(_nextId, animate, Animations.SLIDE_IN_RIGHT, Animations.SLIDE_OUT_LEFT);
	}

	private void previousEntry(boolean animate) {
		mAnimationDirection = android.R.anim.fade_out;
		switchEntry(_previousId, animate, Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (MainTabActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onPause(webView);
		}
		scrollX = webView.getScrollX();
		scrollY = webView.getScrollY();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.entry, menu);
		return true;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_markasread: {
			finish();
			break;
		}
		case R.id.url_button: {
			if (link != null) {
				// if (link.endsWith("feed/atom/")) {
				// link = link.substring(0, link.length() - "feed/atom/".length());
				// }
				// Browser öffnen
				startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
			}
			break;
		}
		case R.id.menu_instapaper: {
			if (link != null) {
				((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);

				loadInstapaper();
			}
			break;
		}

		case R.id.menu_feed: {
			_id = null;
			reload();
			// readUrl(); // TODO ???
			break;
		}

		case R.id.menu_mobilize: {
			loadMoblize();
			break;
		}

		case R.id.menu_readability: {
			loadReadability();
			break;
		}

		case R.id.menu_copytoclipboard: {
			if (link != null) {
				((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);
			}
			break;
		}
		// case R.id.menu_delete: {
		// getContentResolver().delete(uri, null, null);
		// if (localPictures) {
		// FeedData.deletePicturesOfEntry(_id);
		// }
		//
		// if (nextButton.isEnabled()) {
		// nextButton.performClick();
		// } else {
		// if (previousButton.isEnabled()) {
		// previousButton.performClick();
		// } else {
		// finish();
		// }
		// }
		// break;
		// }
		case R.id.menu_share: {
			if (link != null) {
				startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, link).setType(TEXTPLAIN), getString(R.string.menu_share)));
			}
			break;
		}
		case R.id.menu_text_scale: {
			showSeekBarDialog();
		}
			break;

		}
		return true;
	}

	public String bahtml = "";

	private void readUrl() {

		new Thread(new Runnable() {
			public void run() {

				try {
					long start = System.currentTimeMillis();// 0
					URL url = new URL(link);
					System.out.println("a" + (System.currentTimeMillis() - start));
					// InputStream is = url.openStream();
					URLConnection con = url.openConnection();
					System.out.println("b" + (System.currentTimeMillis() - start));
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8")); // 950ms
					System.out.println("c" + (System.currentTimeMillis() - start));
					// baseUrl wechselt ab con.getInputStream (open?)
					String baseUrl = con.getURL().getProtocol() + "://" + con.getURL().getHost();
					if (con.getURL().getHost().endsWith("golem.de")) {
						String[] arr = con.getURL().getPath().split("-");
						String newLink = "http://www.golem.de/pda/pda-" + arr[arr.length - 1];
						webView.loadUrl(newLink);
						return;
					}
					if (con.getURL().getHost().endsWith("heise.de")) {
						String newLink = baseUrl + con.getURL().getPath() + "?view=print";
						webView.loadUrl(newLink);
						return;
					}
					if (con.getURL().getHost().endsWith("wiwo.de")) {
						String newLink = baseUrl + con.getURL().getPath();
						int pos = newLink.lastIndexOf('/');
						if (pos > 0) {
							newLink = newLink.substring(0, pos) + "/v_detail_tab_print/" + newLink.substring(pos, newLink.length());
						}
						// bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8"));
						// webView.loadUrl(newLink);
						// Wiwo print im Browser öffnen
						startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(newLink)), 0);
						finish();
						return;
					}

					// BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
					String line = bufferedReader.readLine();
					while (line != null) {
						line = bufferedReader.readLine();
						bahtml += line;
					}
					bufferedReader.close();
					System.out.println("d" + (System.currentTimeMillis() - start));

					// bahtml = Jsoup.clean(bahtml, Whitelist.basicWithImages());
					// String baseUrl = con.getURL().getProtocol() + "://" + con.getURL().getHost();
					System.out.println("e" + (System.currentTimeMillis() - start));

					// long u=System.currentTimeMillis();
					// int pBody=bahtml.indexOf("<body");
					// System.out.println("pBody"+(System.currentTimeMillis() - u));
					// int classCon=bahtml.indexOf("class=\"con\"",pBody);
					// System.out.println("classCon"+(System.currentTimeMillis() - u));
					// bahtml=bahtml.substring(classCon);
					// System.out.println("f0 " + (System.currentTimeMillis() - start));

					Document dirty = Jsoup.parseBodyFragment(bahtml, baseUrl); // 2300ms !!
					System.out.println("f" + (System.currentTimeMillis() - start));
					Elements elements = dirty.getElementsByClass("content");
					System.out.println("g" + (System.currentTimeMillis() - start));
					if (elements.size() > 0) {
						// dirty auf content setzen, 2.parse
						bahtml = elements.first().html();
						dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
					} else {
						elements = dirty.getElementsByClass("con"); // Tagesschau
						if (elements.size() > 0) {
							bahtml = elements.first().html();
							dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
						} else {
							elements = dirty.getElementsByAttributeValue("id", "content"); // Heise
							if (elements.size() > 0) {
								bahtml = elements.first().html();
								dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
								// } else {
								// elements = dirty.getElementsByClass("hcf-content"); // Wiwo - nope, unsused!!
								// if (elements.size() > 0) {
								// bahtml = elements.first().html();
								// dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
								// }
							}
						}
					}
					Whitelist whitelist = Whitelist.basic(); // 500ms
					// Whitelist whitelist = Whitelist.basicWithImages(); // 500ms
					whitelist.addTags("h1", "h2", "h3");
					System.out.println("x" + (System.currentTimeMillis() - start));
					Cleaner cleaner2 = new Cleaner(whitelist);
					System.out.println("h" + (System.currentTimeMillis() - start));
					Document clean = cleaner2.clean(dirty);
					System.out.println("i" + (System.currentTimeMillis() - start));
					// clean.body().removeAttr(attributeKey)
					bahtml = clean.body().html();

				} catch (Exception e) {
					e.printStackTrace();
					Util.toastMessageLong(EntryActivity.this, "" + e);
				}

				final TextView view = (TextView) findViewById(R.id.entry_date);
				view.post(new Runnable() {
					public void run() {
						// bahtml="<html><body>You scored <b>192</b> points.</body></html>";
						// android.text.Html.fromHtml(instruction).toString()
						// Spanned spanned = android.text.Html.fromHtml(bahtml);
						// view.setText(spanned.toString());

						webView.loadData(bahtml, "text/html; charset=UTF-8", null);
					}
				});

			}
		}).start();

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == 92 || keyCode == 94 || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				scrollUp();
				return true;
			} else if (keyCode == 93 || keyCode == 95 || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				scrollDown();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void scrollUp() {
		if (webView != null) {
			webView.pageUp(false);
		}
	}

	private void scrollDown() {
		if (webView != null) {
			webView.pageDown(false);
		}
	}

	/**
	 * Works around android issue 6191
	 */
	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		try {
			super.unregisterReceiver(receiver);
		} catch (Exception e) {
			// do nothing
		}
	}

	// public void displayFullscreen() {
	// if (this.viewFulscreen) {
	// // content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
	// getActionBar().show();
	// viewFulscreen = false;
	// } else {
	// // int newVis = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
	// // int newVis = View.SYSTEM_UI_FLAG_FULLSCREEN;
	// // content.setSystemUiVisibility(newVis);
	// getActionBar().hide();
	// // findViewById(R.id.entry_date_layout).setVisibility(View.GONE);
	// // findViewById(R.id.button_layout).setVisibility(View.GONE);
	//
	// // SYSTEM_UI_FLAG_HIDE_NAVIGATION,
	// // int android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
	// viewFulscreen = true;
	// }
	// }

	boolean mNavVisible = true;

	private long timestamp;

	// int mBaseSystemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	// | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
	// int mLastSystemUiVis;

	// int curVis = webView.getSystemUiVisibility();
	// setNavVisibility((curVis&WebView.SYSTEM_UI_FLAG_LOW_PROFILE) != 0);

	void setNavVisibility(boolean visible) {
		if (mNavVisible == visible) {
			return;
		}
		mNavVisible = visible;
		if (!visible) {
			getActionBar().hide();
		} else {
			getActionBar().show();
		}

		// int newVis = mBaseSystemUiVisibility;
		// if (!visible) {
		// newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN;
		// }
		// final boolean changed = newVis == webView.getSystemUiVisibility();
		// // if(!changed) return;
		//
		// // Set the new desired visibility.
		// webView.setSystemUiVisibility(newVis);
		// mTitleView.setVisibility(visible ? VISIBLE : INVISIBLE);
		// mSeekView.setVisibility(visible ? VISIBLE : INVISIBLE);
	}

	private int mIntScalePercent = 50; // 0..100
	// mSeekBar.setProgress(intLaufzeitPercent);
	SeekBar mSeekBar;

	// mSeekBar = (SeekBar) findViewById(R.id.seekBar);
	// mSeekBar.setOnSeekBarChangeListener(this);

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		// progress 1..100
		mIntScalePercent = progress;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@SuppressLint("NewApi")
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		setZoomsScale();
	}

	AlertDialog.Builder mAlertDialog = null;
	public static final String PREFERENCE_SCALE = "preference_scale_readability";
	public final static String PREFS_NAME = "de.bernd.sparse.rss.preferences";

	private void showSeekBarDialog() {
		View view = getLayoutInflater().inflate(R.layout.entry_seek_bar, null);
		mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSeekBar.setProgress(mIntScalePercent);
		mAlertDialog = new AlertDialog.Builder(this);
		mAlertDialog.setTitle("Scale the Text");
		mAlertDialog.setView(view);
		mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(PREFERENCE_SCALE + mAufrufart, mIntScalePercent);
				editor.commit();
			}
		});
		mAlertDialog.show();
	}

	/**
	 * Für onPageFinished um ProzessBar an/aus zu knipsen da webview aSyncron die animation trasht
	 */
	private class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView view, String url) {
			// animate(view);
			setProgressBarIndeterminateVisibility(false);
			view.setVisibility(View.VISIBLE);
			super.onPageFinished(view, url);
		}
	}

	int mAnimationDirection = android.R.anim.slide_out_right;

	private void animate(final WebView view) {
		Animation anim = AnimationUtils.loadAnimation(getBaseContext(), mAnimationDirection);
		view.startAnimation(anim);
	}

	private void switchEntry(String id, boolean animate, Animation inAnimation, Animation outAnimation) {

		// webView.setVisibility(View.GONE);
		setProgressBarIndeterminateVisibility(true);

		uri = parentUri.buildUpon().appendPath(id).build();
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;

		if (mAufrufart > 0) {
			// link laden
			Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
			if (entryCursor.moveToFirst()) {
				link = entryCursor.getString(linkPosition);
				link = fixLink(link);
				timestamp = entryCursor.getLong(datePosition);
			}
			entryCursor.close();

			if (mAufrufart == AUFRUFART_MOBILIZE) {
				loadMoblize();
			} else if (mAufrufart == AUFRUFART_INSTAPAPER) {
				loadInstapaper();
			} else if (mAufrufart == AUFRUFART_READABILITY) {
				loadReadability();
			}

			// markRead 2
			getContentResolver().update(uri, RSSOverview.getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);

			setupButton(previousButton, false, timestamp);
			setupButton(nextButton, true, timestamp);
		} else {
			reload();
		}
	}

}
