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

package de.bernd.shandschuh.sparserss.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Xml;
import de.bernd.shandschuh.sparserss.BASE64;
import de.bernd.shandschuh.sparserss.EntriesListAdapter;
import de.bernd.shandschuh.sparserss.R;
import de.bernd.shandschuh.sparserss.RSSOverview;
import de.bernd.shandschuh.sparserss.Strings;
import de.bernd.shandschuh.sparserss.Util;
import de.bernd.shandschuh.sparserss.handler.RSSHandler;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

public class FetcherService extends IntentService {
	private static final int FETCHMODE_DIRECT = 1;
	
	private static final int FETCHMODE_REENCODE = 2;
	
	private static final String KEY_USERAGENT = "User-agent";
	
	private static final String VALUE_USERAGENT = "Mozilla/5.0";
	
	private static final String CHARSET = "charset=";
	
	private static final String COUNT = "COUNT(*)";
	
	private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
	
	private static final String LINK_RSS = "<link rel=\"alternate\" ";
	
	private static final String LINK_RSS_SLOPPY = "<link rel=alternate ";
	
	private static final String HREF = "href=\"";
	
	private static final String HTML_BODY = "<body";
	
	private static final String ENCODING = "encoding=\"";
	
	private static final String SERVICENAME = "RssFetcherService";
	
	private static final String ZERO = "0";
	
	private static final String GZIP = "gzip";
	
	private NotificationManager notificationManager;
	
	private static SharedPreferences preferences = null;
	
	private static Proxy proxy;
	
	private String mImageFolder=null; 
	
	public FetcherService() {
		super(SERVICENAME);
		HttpURLConnection.setFollowRedirects(true);
	}
		
	@Override
	public void onHandleIntent(Intent intent) {
		if (preferences == null) {
			try {
				preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(Strings.PACKAGE, 0));
			} catch (NameNotFoundException e) {
				preferences = PreferenceManager.getDefaultSharedPreferences(FetcherService.this);
			}
		}
		
		if (intent.getBooleanExtra(Strings.SCHEDULED, false)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(Strings.PREFERENCE_LASTSCHEDULEDREFRESH, SystemClock.elapsedRealtime());
			editor.commit();
		}
		
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED && intent != null) {
			if (preferences.getBoolean(Strings.SETTINGS_PROXYENABLED, false) && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || !preferences.getBoolean(Strings.SETTINGS_PROXYWIFIONLY, false))) {
				try {
					proxy = new Proxy(ZERO.equals(preferences.getString(Strings.SETTINGS_PROXYTYPE, ZERO)) ? Proxy.Type.HTTP : Proxy.Type.SOCKS, new InetSocketAddress(preferences.getString(Strings.SETTINGS_PROXYHOST, Strings.EMPTY), Integer.parseInt(preferences.getString(Strings.SETTINGS_PROXYPORT, Strings.DEFAULTPROXYPORT))));
				} catch (Exception e) {
					proxy = null;
				}
			} else {
				proxy = null;
			}

			int newCount = FetcherService.refreshFeedsStatic(FetcherService.this, intent.getStringExtra(Strings.FEEDID), networkInfo, intent.getBooleanExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, false));
					
			if (newCount > 0) {
				if (preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSENABLED, false)) {
					Cursor cursor = getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {COUNT}, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null, null);
							
					cursor.moveToFirst();
					newCount = cursor.getInt(0);
					cursor.close();
							
					String text = new StringBuilder().append(newCount).append(' ').append(getString(de.bernd.shandschuh.sparserss.R.string.newentries)).toString();
					
//					Intent notificationIntent = new Intent(FetcherService.this, MainTabActivity.class);
					Intent notificationIntent = new Intent(FetcherService.this, RSSOverview.class);
					
					PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
							
//					Notification notification = new Notification(de.bernd.shandschuh.sparserss.R.drawable.ic_statusbar_rss, text, System.currentTimeMillis());
					Builder builder = new Notification.Builder(FetcherService.this);
					builder.setContentTitle(getString(R.string.rss_feeds));
			         builder.setContentText(text);
			         builder.setContentIntent(contentIntent);
			         builder.setSmallIcon(R.drawable.icon);

//					if (preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSVIBRATE, false)) {
//						notification.defaults = Notification.DEFAULT_VIBRATE;
//					}
//					notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
//					notification.ledARGB = 0xffffffff;
//					notification.ledOnMS = 300;
//					notification.ledOffMS = 1000;
					
					String ringtone = preferences.getString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, null);
							
					if (ringtone != null && ringtone.length() > 0) {
						builder.setSound(Uri.parse(ringtone));
					}
					
					//weg mit 6
//					notification.setLatestEventInfo(FetcherService.this, getString(R.string.rss_feeds), text, contentIntent);
			        Notification notification = builder.build();
					
					notificationManager.notify(0, notification);
				} else {
					notificationManager.cancel(0);
				}
			}//newCount
			
			System.out.println("****** LADE IM BACKEND  ****");
			mImageFolder = Util.getImageFolderFile(FetcherService.this).toString();

			String[] SYNC_PROJECTION = { BaseColumns._ID, FeedData.FeedColumns.SYNC };
			Cursor cursor = FetcherService.this.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI , SYNC_PROJECTION, null, null, null);
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false) {
				String id = cursor.getString(0);
				int iSysnc = cursor.getInt(1);
				if(iSysnc!=0){
					System.out.println("Sync Feed " + id);
					myFetchFullHtml(Integer.parseInt(id));
				}
				
				cursor.moveToNext();			
			}
			cursor.close();
		}
		
		// Del old Pics
		File[] files = Util.getImageFolderFile(getBaseContext()).listFiles();
		if(files!=null){
			Date today = new Date();
//			long lastWeek = today.getTime() - 604800000l; // 1000* 60*60*24*7 = 7 Tage
			long lastWeek = today.getTime() - 259200000l; // 1000* 60*60*24*3 = 3 Tage
			for (int i = 0; i < files.length; i++) {
				File f=files[i];
				if(f.lastModified() < lastWeek){
					f.delete();
				}
			}
		}
	}

	public static final String FULLTEXTISNULL = "fulltext is null";
	
	public static final String[] ENTRY_UPDATE_PROJECTION = { BaseColumns._ID, FeedData.EntryColumns.LINK, FeedData.EntryColumns.ABSTRACT};
	public static final String sortOrder="_id DESC"; // neueste zuerst

	
	public void myFetchFullHtml(int feedId) {
		Cursor cursor;
		
		Uri parentUri = Uri.parse("content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/" + feedId + "/entries"); 

		cursor = FetcherService.this.getContentResolver().query(parentUri, ENTRY_UPDATE_PROJECTION, FULLTEXTISNULL, null, sortOrder);
		cursor.moveToFirst();
		System.out.println("FetchFullHtml for " + cursor.getCount());
		
		HtmlFetcher fetcher2 = new HtmlFetcher();
		fetcher2.setMaxTextLength(50000);
		
		boolean showCover = Util.showCover(this, ""+feedId);

		while (cursor.isAfterLast() == false) {
			String id = cursor.getString(0);
			String link = cursor.getString(1);
			String rssText = cursor.getString(2);
			System.out.println(id + " " + link);
			String imageUrl = null;
			int rssLen=0;
			if(rssText!=null){
				rssLen=rssText.length();
				imageUrl = Util.takeFirstSrc(rssText);
			}
			try {
				JResult res = fetcher2.fetchAndExtract(link, 10000, true);
				String text = res.getText();
				if(imageUrl==null){
					imageUrl = res.getImageUrl();
				}
				if(text!=null && text.length()>rssLen){
					ContentValues values = FeedDataContentProvider.createContentValuesForFulltext(text, imageUrl);
					Uri updateUri = ContentUris.withAppendedId(parentUri,Long.parseLong(id));
					FetcherService.this.getContentResolver().update(updateUri, values, null, null);
				}
				if(showCover && imageUrl!=null && !"".equals(imageUrl) && imageUrl.toLowerCase().endsWith(".jpg")){
					try {
						String pathToImage=mImageFolder + "/" + id + "_cover.jpg";
						byte[] data = FetcherService.getBytes(new URL(imageUrl).openStream());
						FileOutputStream fos = new FileOutputStream(pathToImage);
						fos.write(data);
						fos.close();
					} catch (Exception e) {
						System.err.println("Err getting image " + imageUrl + " " + e);
					}
				}
			} catch (Exception e) {
				String str="Err Sync Fulltext " + e;
				System.err.println(str);
				e.printStackTrace();
				cursor.close();
				return;
			}
			
			cursor.moveToNext();			
		}
		cursor.close();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if(RSSOverview.INSTANCE!=null){
			RSSOverview.INSTANCE.zeigeProgressBar(true);
		}
	}
	
	@Override
	public void onDestroy() {
//		if (MainTabActivity.INSTANCE != null)
//			MainTabActivity.INSTANCE.setProgressBarIndeterminateVisibility(false);
		if(RSSOverview.INSTANCE!=null)
			RSSOverview.INSTANCE.zeigeProgressBar(false);
		super.onDestroy();
	}
	
	private static int refreshFeedsStatic(Context context, String feedId, NetworkInfo networkInfo, boolean overrideWifiOnly) {
		String selection = null;
		
		if (!overrideWifiOnly && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
			selection = new StringBuilder(FeedData.FeedColumns.WIFIONLY).append("=0 or ").append(FeedData.FeedColumns.WIFIONLY).append(" IS NULL").toString(); // "IS NOT 1" does not work on 2.1
		}

		Cursor cursor = context.getContentResolver().query(feedId == null ? FeedData.FeedColumns.CONTENT_URI : FeedData.FeedColumns.CONTENT_URI(feedId), null, selection, null, null); // no managed query here
		
		int urlPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		
		int lastUpdatePosition = cursor.getColumnIndex(FeedData.FeedColumns.REALLASTUPDATE);
		
		int titlePosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		
		int fetchmodePosition = cursor.getColumnIndex(FeedData.FeedColumns.FETCHMODE);
		
		int iconPosition = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
		
		boolean imposeUserAgent = !preferences.getBoolean(Strings.SETTINGS_STANDARDUSERAGENT, false);
		
		boolean followHttpHttpsRedirects = preferences.getBoolean(Strings.SETTINGS_HTTPHTTPSREDIRECTS, true);
		
		int result = 0;
		
		RSSHandler handler = new RSSHandler(context);
		
		handler.setEfficientFeedParsing(preferences.getBoolean(Strings.SETTINGS_EFFICIENTFEEDPARSING, true));
//		handler.setFetchImages(preferences.getBoolean(Strings.SETTINGS_FETCHPICTURES, false));
		handler.setFetchImages(false);  // bah, immer ohne Bilder
		
		while(cursor.moveToNext()) {
			String id = cursor.getString(idPosition);
			
			HttpURLConnection connection = null;
			
			try {
				String feedUrl = cursor.getString(urlPosition);
				
				connection = setupConnection(feedUrl, imposeUserAgent, followHttpHttpsRedirects);
				
				String contentType = connection.getContentType();

				int fetchMode = cursor.getInt(fetchmodePosition);
				
				handler.init(new Date(cursor.getLong(lastUpdatePosition)), id, cursor.getString(titlePosition), feedUrl);
				if (fetchMode == 0) {
					if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(getConnectionInputStream(connection)));
						
						String line = null;
						
						int pos = -1, posStart = -1;
						
						while ((line = reader.readLine()) != null) {
							if (line.indexOf(HTML_BODY) > -1) {
								break;
							} else {
								pos = line.indexOf(LINK_RSS);
								
								if (pos == -1) {
									pos = line.indexOf(LINK_RSS_SLOPPY);
								}
								if (pos > -1) {
									posStart = line.indexOf(HREF, pos);

									if (posStart > -1) {
										String url = line.substring(posStart+6, line.indexOf('"', posStart+10)).replace(Strings.AMP_SG, Strings.AMP);
										
										ContentValues values = new ContentValues();
										
										if (url.startsWith(Strings.SLASH)) {
											int index = feedUrl.indexOf('/', 8);
											
											if (index > -1) {
												url = feedUrl.substring(0, index)+url;
											} else {
												url = feedUrl+url;
											}
										} else if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
											url = new StringBuilder(feedUrl).append('/').append(url).toString();
										}
										values.put(FeedData.FeedColumns.URL, url);
										context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
										connection.disconnect();
										connection = setupConnection(url, imposeUserAgent, followHttpHttpsRedirects);
										contentType = connection.getContentType();
										break;
									}
								}
							}
						}
						if (posStart == -1) { // this indicates a badly configured feed
							connection.disconnect();
							connection = setupConnection(feedUrl, imposeUserAgent, followHttpHttpsRedirects);
							contentType = connection.getContentType();
						}
						
					}
					
					if (contentType != null) {
						int index = contentType.indexOf(CHARSET);
						
						if (index > -1) {
							int index2 = contentType.indexOf(';', index);
							
							try {
								Xml.findEncodingByName(index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8));
								fetchMode = FETCHMODE_DIRECT;
							} catch (UnsupportedEncodingException usee) {
								fetchMode = FETCHMODE_REENCODE;
							}
						} else {
							fetchMode = FETCHMODE_REENCODE;
						}
						
					} else {
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getConnectionInputStream(connection)));
						
						char[] chars = new char[20];
						
						int length = bufferedReader.read(chars);

						String xmlDescription = new String(chars, 0, length);
						
						connection.disconnect();
						connection = setupConnection(connection.getURL(), imposeUserAgent, followHttpHttpsRedirects);
						
						int start = xmlDescription != null ?  xmlDescription.indexOf(ENCODING) : -1;
						
						if (start > -1) {
							try {
								Xml.findEncodingByName(xmlDescription.substring(start+10, xmlDescription.indexOf('"', start+11)));
								fetchMode = FETCHMODE_DIRECT;
							} catch (UnsupportedEncodingException usee) {
								fetchMode = FETCHMODE_REENCODE;
							}
						} else {
							fetchMode = FETCHMODE_DIRECT; // absolutely no encoding information found
						}
					}
					
					ContentValues values = new ContentValues();
					
					values.put(FeedData.FeedColumns.FETCHMODE, fetchMode);
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
				
				/* check and optionally find favicon */
				byte[] iconBytes = cursor.getBlob(iconPosition);
				
				if (iconBytes == null) {
					HttpURLConnection iconURLConnection = setupConnection(new URL(new StringBuilder(connection.getURL().getProtocol()).append(Strings.PROTOCOL_SEPARATOR).append(connection.getURL().getHost()).append(Strings.FILE_FAVICON).toString()), imposeUserAgent, followHttpHttpsRedirects);
					
					try {
						iconBytes = getBytes(getConnectionInputStream(iconURLConnection));
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.ICON, iconBytes);
						context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
					} catch (Exception e) {
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.ICON, new byte[0]); // no icon found or error
						context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
					} finally {
						iconURLConnection.disconnect();
					}
					
				}
				switch (fetchMode) {
					default:
					case FETCHMODE_DIRECT: {
						if (contentType != null) {
							int index = contentType.indexOf(CHARSET);
							
							int index2 = contentType.indexOf(';', index);
							
							InputStream inputStream = getConnectionInputStream(connection);
							
							handler.setInputStream(inputStream);
							Xml.parse(inputStream, Xml.findEncodingByName(index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8)), handler);
						} else {
							InputStreamReader reader = new InputStreamReader(getConnectionInputStream(connection));
							
							handler.setReader(reader);
							Xml.parse(reader, handler);
						}
						break;
					}
					case FETCHMODE_REENCODE: {
						ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
						
						InputStream inputStream = getConnectionInputStream(connection);
						
						byte[] byteBuffer = new byte[4096];
						
						int n;
						
						while ( (n = inputStream.read(byteBuffer)) > 0 ) {
							ouputStream.write(byteBuffer, 0, n);
						}
						
						String xmlText = ouputStream.toString();
						
						int start = xmlText != null ?  xmlText.indexOf(ENCODING) : -1;
						
						if (start > -1) {
							Xml.parse(new StringReader(new String(ouputStream.toByteArray(), xmlText.substring(start+10, xmlText.indexOf('"', start+11)))), handler);
						} else {
							// use content type
							if (contentType != null) {
								
								int index = contentType.indexOf(CHARSET);
								
								if (index > -1) {
									int index2 = contentType.indexOf(';', index);
									
									try {
										StringReader reader = new StringReader(new String(ouputStream.toByteArray(), index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8)));
										
										handler.setReader(reader);
										Xml.parse(reader, handler);
									} catch (Exception e) {
										
									}
								} else {
									StringReader reader = new StringReader(new String(ouputStream.toByteArray()));
									
									handler.setReader(reader);
									Xml.parse(reader, handler);
									
								}
							}
						}
						break;
					}
				}
				connection.disconnect();
			} catch (FileNotFoundException e) {
				if (!handler.isDone() && !handler.isCancelled()) {
					ContentValues values = new ContentValues();
					values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the fetchmode to determine it again later
					values.put(FeedData.FeedColumns.ERROR, context.getString(R.string.error_feederror));
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
			} catch (Throwable e) {
				if (!handler.isDone() && !handler.isCancelled()) {
					ContentValues values = new ContentValues();
					values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the fetchmode to determine it again later
					values.put(FeedData.FeedColumns.ERROR, e.getMessage());
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
			result += handler.getNewCount();
		}
		cursor.close();
		
		if (result > 0) {
			context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET).putExtra(Strings.COUNT, result));
		}
		return result;
	}
	
	private static final HttpURLConnection setupConnection(String url, boolean imposeUseragent, boolean followHttpHttpsRedirects) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		return setupConnection(new URL(url), imposeUseragent, followHttpHttpsRedirects);
	}
	
	private static final HttpURLConnection setupConnection(URL url, boolean imposeUseragent, boolean followHttpHttpsRedirects) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		return setupConnection(url, imposeUseragent, followHttpHttpsRedirects, 0);
	}
	
	private static final HttpURLConnection setupConnection(URL url, boolean imposeUseragent, boolean followHttpHttpsRedirects, int cycle) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		HttpURLConnection connection = proxy == null ? (HttpURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection(proxy);
		
		connection.setDoInput(true);
		connection.setDoOutput(false);
		if (imposeUseragent) {
			connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT); // some feeds need this to work properly
		}
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(30000);
		connection.setUseCaches(false);
		
		if (url.getUserInfo() != null) {
			connection.setRequestProperty("Authorization", "Basic "+BASE64.encode(url.getUserInfo().getBytes()));
		}
		connection.setRequestProperty("connection", "close"); // Workaround for android issue 7786
		connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.connect();
		
		String location = connection.getHeaderField("Location");
		
		if (location != null && (url.getProtocol().equals(Strings._HTTP) && location.startsWith(Strings.HTTPS) || url.getProtocol().equals(Strings._HTTPS) && location.startsWith(Strings.HTTP))) {
			// if location != null, the system-automatic redirect has failed which indicates a protocol change
			if (followHttpHttpsRedirects) {
				connection.disconnect();
					
				if (cycle < 5) {
					return setupConnection(new URL(location), imposeUseragent, followHttpHttpsRedirects, cycle+1);
				} else {
					throw new IOException("Too many redirects.");
				}
			} else {
				throw new IOException("https<->http redirect - enable in settings");
			}
		}
		return connection;
	}

	public static byte[] getBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[4096];
		
		int n;
		
		while ((n = inputStream.read(buffer)) > 0) {
			output.write(buffer, 0, n);
		}
		
		byte[] result  = output.toByteArray();
		
		output.close();
		inputStream.close();
		return result;
	}
	
	/**
	 * This is a small wrapper for getting the properly encoded inputstream if is is gzip compressed
	 * and not properly recognized.
	 */
	private static InputStream getConnectionInputStream(HttpURLConnection connection) throws IOException {
		InputStream inputStream = connection.getInputStream();
		
		if (GZIP.equals(connection.getContentEncoding()) && !(inputStream instanceof GZIPInputStream)) {
			return new GZIPInputStream(inputStream);
		} else {
			return inputStream;
		}
	}
}
