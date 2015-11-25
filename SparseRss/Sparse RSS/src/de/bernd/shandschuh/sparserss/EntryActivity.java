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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent.OnFinished;
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
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import de.bernd.shandschuh.sparserss.provider.FeedData;

public class EntryActivity extends AppCompatActivity implements android.widget.SeekBar.OnSeekBarChangeListener {

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
	private ImageButton markAsReadButton;
	private ImageButton readabilityButton;

	private ImageButton urlButton;

	private ImageButton previousButton;

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

	private EntryActivity mActivity = null;

	private long timestamp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MainTabActivity.isLightTheme(this)) {
			setTheme(R.style.Theme_Light);
		}
		mAufrufart = getIntent().getIntExtra(EntriesListActivity.EXTRA_AUFRUFART, 0);
		if (mAufrufart != AUFRUFART_FEED) {
			// gegen das flackern der ActionBar Inhalt hinter actionbar
			// getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}

		super.onCreate(savedInstanceState);

//		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//		setProgressBarIndeterminateVisibility(true);
//		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//		setSupportProgressBarIndeterminateVisibility(true);

		// Hide on content scroll requires an overlay action bar, so request one
		// supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
//		supportRequestWindowFeature(AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR_OVERLAY);

		setContentView(R.layout.entry);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
		zeigeProgressBar(true);

		mActivity = this;

		int titleId = -1;

//		 canShowIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);
		canShowIcon = false;

		titleId = android.R.id.title;

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
		// mAufrufart = getIntent().getIntExtra(EntriesListActivity.EXTRA_AUFRUFART, 0);
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
			
			//hierher kopiert - tilte immer ermitteln
			Date date = new Date(timestamp);

			StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(this).format(date)).append(' ').append(DateFormat.getTimeFormat(this).format(date));

			String txtTitel = entryCursor.getString(titlePosition);
			((TextView) findViewById(R.id.entry_date)).setText(txtTitel + "  " + dateStringBuilder);
		}

		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		nextButton = (ImageButton) findViewById(R.id.next_button);
		markAsReadButton = (ImageButton) findViewById(R.id.menu_markasread2);
		markAsReadButton.setAlpha(BUTTON_ALPHA );
		readabilityButton = (ImageButton) findViewById(R.id.menu_readability2);
		urlButton = (ImageButton) findViewById(R.id.url_button);
		urlButton.setAlpha(BUTTON_ALPHA + 30);
		previousButton = (ImageButton) findViewById(R.id.prev_button);

		// verbergen
//		findViewById(R.id.button_layout).setVisibility(View.GONE);
//		findViewById(R.id.entry_date_layout).setVisibility(View.GONE);

		viewFlipper = (ViewFlipper) findViewById(R.id.content_flipper);
		nestedScrollView = (View) findViewById(R.id.nested_scroll_view);

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

		@SuppressWarnings("deprecation")
		final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

			public boolean onDown(MotionEvent e) {
				return false;
			}

			public boolean onDoubleTap(MotionEvent e) {
				// setNavVisibility(false);
				finish();
				return false;
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (Math.abs(velocityY) < Math.abs(velocityX)) {
					if (gestures) {
						if (velocityX > 800) {
							if (previousButton.isEnabled()) {
								previousEntry(true);
							}
						} else if (velocityX < -800) {
							if (nextButton.isEnabled()) {
								nextEntry(true);
							}
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

			public boolean onSingleTapUp(MotionEvent e) {
		        final ActionBar bar = mActivity.getSupportActionBar();
                if (bar.isShowing()) {
                    bar.hide();
                    findViewById(R.id.button_layout).setVisibility(View.INVISIBLE);
                } else {
                    bar.show();
                    findViewById(R.id.button_layout).setVisibility(View.VISIBLE);
                }
				return false;
			}

			public void onShowPress(MotionEvent e) {

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

		SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mIntScalePercent = prefs.getInt(PREFERENCE_SCALE + mAufrufart, 50);

		setZoomsScale();
		MyWebViewClient myWebViewClient = new MyWebViewClient(){
			public void onPageFinished(WebView view, String url) {
				nestedScrollView.scrollTo(0, 0);
				zeigeProgressBar(false);
			};
		};

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
		} else if (mAufrufart == AUFRUFART_WEBVIEW) {
			loadWebview();
		}
		setHomeButtonActive();
		
		markAsReadButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		readabilityButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				loadReadability();
			}
		});
		
		// setWebViewListener();
	}// onCreate

	void setZoomsScale() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			webView.getSettings().setTextZoom(mIntScalePercent * 2); // % von 100 // API 14 !!
			if (webView0 != null) {
				webView0.getSettings().setTextZoom(mIntScalePercent * 2); // % von 100 // API 14 !!
			}
		}
	}

	private void loadWebview() {
		zeigeProgressBar(true);
		webView.loadUrl(link);
	}

	private void loadReadability() {
		zeigeProgressBar(true);
		webView.loadUrl("http://www.readability.com/m?url=" + link);
	}

	private void loadInstapaper() {
		// webView.getSettings().setJavaScriptEnabled(false);
		// webView.getSettings().setDomStorageEnabled(true);
		// webView.loadUrl("http://www.instapaper.com/m?u=" + link);
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.instapaper.com/m?u=" + link)));
	}

	private void loadMoblize() {
		zeigeProgressBar(true);
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
				String txtTitel = entryCursor.getString(titlePosition);
				setTitle(txtTitel);
				if (titleTextView != null) {
					titleTextView.requestFocus(); // restart ellipsize
				}
				// TextView titelTextView=(TextView) findViewById(R.id.entry_titel);
				// titelTextView.setSingleLine();
				// titelTextView.setText(txtTitel);

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

//							setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bitmap));
//							getSupportActionBar().setIcon(new BitmapDrawable(bitmap));
//							getSupportActionBar().setLogo(new BitmapDrawable(bitmap));
//					        int flags = 0;
//					        int change = getSupportActionBar().getDisplayOptions() ^ flags;
//					        getSupportActionBar().setDisplayOptions(change, flags);
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

//				((TextView) findViewById(R.id.entry_date)).setText(dateStringBuilder);
				((TextView) findViewById(R.id.entry_date)).setText(txtTitel + "  " + dateStringBuilder);

				favorite = entryCursor.getInt(favoritePosition) == 1;

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
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
						}
					});

				} else {
					urlButton.setEnabled(false);
					urlButton.setAlpha(80);
				}

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
//        MenuItem markasreadItem = menu.add(R.string.contextmenu_markasread); //@string/contextmenu_markasread
        MenuItem markasreadItem = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread); //@string/contextmenu_markasread
        MenuItemCompat.setShowAsAction(markasreadItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        markasreadItem.setIcon(android.R.drawable.ic_menu_revert);

        MenuItem browserItem = menu.add(0,R.id.url_button,0,R.string.contextmenu_browser);
        MenuItemCompat.setShowAsAction(browserItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        browserItem.setIcon(android.R.drawable.ic_menu_view);

        MenuItem readabilityItem = menu.add(0,R.id.menu_readability,0,"Readability");
        MenuItemCompat.setShowAsAction(readabilityItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        readabilityItem.setIcon(R.drawable.readability_400);

		getMenuInflater().inflate(R.menu.entry, menu);
		return true;
	}

	public String bahtml = null;
	public String mNewLink = null;

	private void readUrl() {
		zeigeProgressBar(true);
		new AsyncReadUrl().execute();
	}

	class AsyncReadUrl extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

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
					mNewLink = newLink;
					return null;
				}
				if (con.getURL().getHost().endsWith("heise.de")) {
					String newLink = baseUrl + con.getURL().getPath() + "?view=print";
					mNewLink = newLink;
					return null;
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
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(newLink)));
					finish();
					return null;
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

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mNewLink != null) {
				webView.loadUrl(mNewLink);
			} else if (bahtml != null) {
				webView.loadData(bahtml, "text/html; charset=UTF-8", null);
			}
		}
	}

	private void readUrl_alt() {

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
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(newLink)));
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

	boolean mNavVisible = true;


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

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		setZoomsScale();
	}

	AlertDialog.Builder mAlertDialog = null;
	public static final String PREFERENCE_SCALE = "preference_scale_readability";
	public final static String PREFS_NAME = "de.bernd.sparse.rss.preferences";

	
	// Scale the Text
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
			zeigeProgressBar(false);
			view.setVisibility(View.VISIBLE);
			super.onPageFinished(view, url);
		}
	}

	int mAnimationDirection = android.R.anim.slide_out_right;
	
	private ProgressBar progressBar;

	private View nestedScrollView;

	private void animate(final WebView view) {
		Animation anim = AnimationUtils.loadAnimation(getBaseContext(), mAnimationDirection);
		view.startAnimation(anim);
	}

	private void switchEntry(String id, boolean animate, Animation inAnimation, Animation outAnimation) {

		// webView.setVisibility(View.GONE);
		zeigeProgressBar(true);

		uri = parentUri.buildUpon().appendPath(id).build();
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;

		if (animate && mAufrufart == 0) {
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

		if (animate && mAufrufart == 0) {
			viewFlipper.setInAnimation(inAnimation);
			viewFlipper.setOutAnimation(outAnimation);
			viewFlipper.addView(webView, layoutParams);
			viewFlipper.showNext();
			viewFlipper.removeViewAt(0);
		}
		
//		new Handler().postDelayed(new Runnable() {
//
//            @Override
//            public void run() {
//                webView.scrollTo(0,0);
//                System.out.println("Scrolled");
//            }
//        }, 1000);
	}

	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = getSupportActionBar();
//		actionBar7.setHideOnContentScrollEnabled(true); //knallt mit Toolbar
		actionBar7.setHomeButtonEnabled(true);
		// durchsichtige Actionbar
//		actionBar7.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#51000000")));
		android.app.ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.hide(); //immer weil doppelt...
		}

		//Up Button, funkt per Default automatisch
        int flags = 0;
        flags = ActionBar.DISPLAY_HOME_AS_UP|ActionBar.DISPLAY_SHOW_TITLE;
        int change = actionBar7.getDisplayOptions() ^ flags;
        actionBar7.setDisplayOptions(change, flags);

        
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			finish();
			return true;
		}
		case R.id.menu_markasread:
		case R.id.menu_markasread2: {
			finish();
			break;
		}
		case R.id.url_button: {
			if (link != null) {
				// if (link.endsWith("feed/atom/")) {
				// link = link.substring(0, link.length() - "feed/atom/".length());
				// }
				// Browser öffnen
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
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

		// case R.id.menu_webview: {
		// loadWebview();
		// break;
		// }

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
		return super.onOptionsItemSelected(item);
	}
	
	private void zeigeProgressBar(boolean zeigen){
		if(zeigen){
			progressBar.setVisibility(View.VISIBLE); 
		}else{
			progressBar.setVisibility(View.INVISIBLE);  
		}
	}
}
