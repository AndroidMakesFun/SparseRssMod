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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import com.amulyakhare.textdrawable.TextDrawable;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.FeedData.EntryColumns;
import de.bernd.shandschuh.sparserss.provider.FeedData.FeedColumns;
import de.jetwick.snacktory.ArticleTextExtractor;
import de.jetwick.snacktory.JResult;

public class EntriesListAdapter extends ResourceCursorAdapter {

	public class Struktur {
		String linkGrafik;
		String text;
	}

	protected static final int STATE_NEUTRAL = 0;
	
	protected static final int STATE_ALLREAD = 1;
	
	protected static final int STATE_ALLUNREAD = 2;
	
	protected int titleColumnPosition;
	
	protected int abstractColumnPosition;
	protected int fulltextColumnPosition;
	
	protected int dateColumn;
	
	protected int readDateColumn;
	
	protected int favoriteColumn;
	
	protected int idColumn;
	
	protected int feedIconColumn;
	
	protected int feedNameColumn;
	
	protected int linkColumn;
	protected int grafikLinkColumn;
	
	public static final String SQLREAD = "length(readdate) ASC, ";
	
	public static final String READDATEISNULL = "readdate is null";

	protected boolean showRead;
	
	protected static Activity mActivity;
	
	protected Uri uri;
	
	protected boolean showFeedInfo;
	
	protected int forcedState;
	
	protected Vector<Long> markedAsRead;
	
	protected Vector<Long> markedAsUnread;
	
	protected Vector<Long> favorited;
	
	protected Vector<Long> unfavorited;
	
	protected DateFormat dateFormat;
	
	protected DateFormat timeFormat;
	
	protected int buttonSize;
	protected int densityDpi;

	public Date today= new Date();

	@ColorInt
	protected int colorPrimary=Color.GREEN;
	@ColorInt
	protected int colorSecondary=Color.RED;

	/**
	 * @param context EntriesListActivity / RecycleListActivity
	 * @param uri irgentwas mit entries
	 * @param showFeedInfo	per EXTRA_SHOWFEEDINFO
	 * @param autoreload per EXTRA_AUTORELOAD
	 * @param layout R.layout.entrylistitem oder R.layout.recyclelistitem
	 * @param iFeedFilter per EXTRA_SHOWFEEDFILTER
	 * @param bResetSearchFilter true im cons f?r createManagedCursor()
	 */
	public EntriesListAdapter(Activity context, Uri uri, boolean showFeedInfo, boolean autoreload, int layout, int iFeedFilter, boolean bResetSearchFilter) {
		super(context, layout, createManagedCursor(context, uri, true, iFeedFilter, bResetSearchFilter), autoreload);

		today.setHours(0);
		today.setMinutes(0);

		showRead = true;
		this.uri = uri;

		Cursor cursor = getCursor();
		// System.out.println("Count: " + cursor.getCount());
		
		titleColumnPosition = cursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		fulltextColumnPosition = cursor.getColumnIndex(FeedData.EntryColumns.FULLTEXT);
		abstractColumnPosition = cursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		dateColumn = cursor.getColumnIndex(FeedData.EntryColumns.DATE);
		readDateColumn = cursor.getColumnIndex(FeedData.EntryColumns.READDATE);
		favoriteColumn = cursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		idColumn = cursor.getColumnIndex(FeedData.EntryColumns._ID);
		linkColumn = cursor.getColumnIndex(FeedData.EntryColumns.LINK);
		grafikLinkColumn = cursor.getColumnIndex(FeedData.EntryColumns.GRAFIKLINK);
		this.showFeedInfo = showFeedInfo;
		if (showFeedInfo) {
			feedIconColumn = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
			feedNameColumn = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		}
		forcedState = STATE_NEUTRAL;
		markedAsRead = new Vector<Long>();
		markedAsUnread = new Vector<Long>();
		favorited = new Vector<Long>();
		unfavorited = new Vector<Long>();
		dateFormat = android.text.format.DateFormat.getDateFormat(context);
		timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		
		buttonSize=Util.getButtonSizeInPixel(context);
		densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
		mDateFromLastShownUpHere=0;

		colorPrimary = Util.fetchPrimaryColor(context);
		colorSecondary = Util.fetchSecondaryColor(context);
	}

	//protected int mPosEmpty=-1;
	protected int mParentChildCount=0;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//position in view 0,...

		//System.out.println(" getItemViewType:" + getViewTypeCount()); //immer 1
		//System.out.println("  XX:" + position); // in ListView/Adapter
		// System.out.println("  cc:" + parent.getChildCount()); // 0..3 dann konstant 3
		//if(xx>cc)  xx-cc-1 == gelesen zB 4-3-1==0 gelesen bei XX=4
		mParentChildCount = parent.getChildCount();
		//if(position > parent.getChildCount()){
		//	mPosEmpty=position-parent.getChildCount()-1; //0..   -> id:39
		//}else{
		//	mPosEmpty=-1;
		//}
		return super.getView(position, convertView, parent);
	}

	protected static ArrayList<String> mListeIdsAsString = new ArrayList<String>();

	/** filled in createManagedCursor */
	private static String mStrSortOrder="";

	/**
	 * @param uri Uri durchreichen, wird erst später in mUri gesetzt!
	 */
	public static void ermittleAlleIDs(Uri uri){
		mListeIdsAsString.clear();
		Cursor cursor = mActivity.getContentResolver().query(uri, null, mSelectionFilter, null, mStrSortOrder);
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			final String id = cursor.getString(0);
			mListeIdsAsString.add(id);
			cursor.moveToNext();
		}
		cursor.close();
	}

	/**
	 * Marks Scrolled Away Entries as Read
	 * @param position: Current  cursor.getPosition()
	 */
	protected void markFreeAsRead(int position) {
		if(position > mParentChildCount){  // 4>3
			//int offset = mParentChildCount + 1;
			//if(offset>-1){
			//offset = offset*-1;
			//System.out.println("  offset:" + offset);
			//cursor.move(offset);
			//long readID = cursor.getLong(idColumn);
			long readID = Long.parseLong(mListeIdsAsString.get(position-mParentChildCount-1));
			//System.out.println("  readID:" + readID);

			markedAsRead.add(readID);
			markedAsUnread.remove(readID);
			//where = FeedData.EntryColumns.FEED_ID + " in (" + readID + ")";
			// feedid in (1,2) AND _id=42
			//String where = this.getSelectionFilter() + " AND _id=" + readID;
			//mActivity.getContentResolver().update(uri, RSSOverview.getReadContentValues(), where, null);
			//}
		}
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);

		markFreeAsRead(cursor.getPosition());

		String strTitle=cursor.getString(titleColumnPosition);
		textView.setText(strTitle);
		float fsize=15.0f;
		textView.setTextSize(fsize); // etwas kleiner!

		TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);
		
		final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
		TextView feedTextView = (TextView) view.findViewById(R.id.text3);

		if (Util.getTeaserPrefs(context)) {
			Struktur struktur = getTeaser(cursor, strTitle);
			feedTextView.setTextSize(fsize);
			if (!TextUtils.isEmpty(struktur.text)){
				feedTextView.setText(struktur.text);
				feedTextView.setVisibility(View.VISIBLE);
			}else{
				feedTextView.setVisibility(View.GONE);
			}
		}

		final long id = cursor.getLong(idColumn);
		
		String link=cursor.getString(linkColumn);
		view.setTag(link);
		
		final boolean favorite = !unfavorited.contains(id) && (cursor.getInt(favoriteColumn) == 1 || favorited.contains(id));
		
		imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
		imageView.setTag(favorite ? Strings.TRUE : Strings.FALSE);
		imageView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				boolean newFavorite = !Strings.TRUE.equals(view.getTag());
				
				if (newFavorite) {
					view.setTag(Strings.TRUE);
					imageView.setImageResource(android.R.drawable.star_on);
					favorited.add(id);
					unfavorited.remove(id);
				} else {
					view.setTag(Strings.FALSE);
					imageView.setImageResource(android.R.drawable.star_off);
					unfavorited.add(id);
					favorited.remove(id);
				}
				
				ContentValues values = new ContentValues();
				
				values.put(FeedData.EntryColumns.FAVORITE, newFavorite ? 1 : 0);
				view.getContext().getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns._ID).append(Strings.DB_ARG).toString(), new String[] {Long.toString(id)});
				context.getContentResolver().notifyChange(FeedData.EntryColumns.FAVORITES_CONTENT_URI, null);
				
			}
		});
		
		Date date = new Date(cursor.getLong(dateColumn));
		mDateFromLastShownUpHere=date.getTime();

		String feedName = cursor.getString(feedNameColumn);		
		if (showFeedInfo && feedIconColumn > -1 && feedNameColumn > -1) {
			byte[] iconBytes = cursor.getBlob(feedIconColumn);
			
			try {
				if (iconBytes != null && iconBytes.length > 0  && !link.contains(".feedburner.com") && !link.contains("//feedproxy.google.com")) {
					
					Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
					
					if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
						if(date.after(today)){
							dateTextView.setText(new StringBuilder().append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName)); // bad style
						}else{
							dateTextView.setText(new StringBuilder().append(' ').append(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName)); // bad style
						}
						bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
						bitmap = Util.getRoundedBitmap(bitmap);
						BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
						bitmapDrawable.setTargetDensity(densityDpi);	
						dateTextView.setCompoundDrawablesWithIntrinsicBounds(bitmapDrawable, null, null,  null);
					} else {
						dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName));
						TextDrawable textDrawable = Util.getRoundButtonImage(context, "", feedName);
						dateTextView.setCompoundDrawablesWithIntrinsicBounds(textDrawable, null, null, null);
					}
				} else {
					dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName));
					TextDrawable textDrawable = Util.getRoundButtonImage(context, "", feedName);
					dateTextView.setCompoundDrawablesWithIntrinsicBounds(textDrawable, null, null, null);
				}
			} catch (Exception e) {
				System.err.println("Catched Exception for createScaledBitmap in EntriesListAdapter");
				dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName));
				TextDrawable textDrawable = Util.getRoundButtonImage(context, "", feedName);
				dateTextView.setCompoundDrawablesWithIntrinsicBounds(textDrawable, null, null, null);
			}
			
		} else {
			// alles 1 feed - kein icon
			textView.setText(cursor.getString(titleColumnPosition));
			if(date.after(today)){
				dateTextView.setText(new StringBuilder(timeFormat.format(date)));
			}else{
				dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)));
			}
		}

		if (forcedState == STATE_ALLUNREAD && !markedAsRead.contains(id) || (forcedState != STATE_ALLREAD && cursor.isNull(readDateColumn) && !markedAsRead.contains(id)) || markedAsUnread.contains(id)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			dateTextView.setEnabled(true);
			textView.setTextColor(colorPrimary);
			feedTextView.setTextColor(colorPrimary);
			dateTextView.setTextColor(colorPrimary);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			textView.setTextColor(colorSecondary);
			feedTextView.setTextColor(colorSecondary);
			dateTextView.setTextColor(colorSecondary);
		}
	}

	private static String mSearchFilter=null;

	/**
	 * SearchFilter, umsetzung ?hnlich showRead(boolean)
	 */
	public void filter(String filter) {
		if(filter==null || "".equals(filter)){
			mSearchFilter=null;
		}else{
			mSearchFilter=filter.toLowerCase();
//			mSearchFilter=EntryColumns.TITLE + " REGEXP '.*" + filter.toLowerCase() + ".*' COLLATE NOCASE";  // +COLLATE NOCASE
			mSearchFilter=EntryColumns.TITLE + " REGEXP '(?i).*" + filter + ".*'";
		}
//		notifyDataSetChanged();
		mActivity.stopManagingCursor(getCursor());
		changeCursor(createManagedCursor(mActivity, uri, showRead, mFeedFilter, false));
	}
	
	public void showRead(boolean showRead) {
		if (showRead != this.showRead) {
			mActivity.stopManagingCursor(getCursor());
			changeCursor(createManagedCursor(mActivity, uri, showRead, mFeedFilter, false));
			this.showRead = showRead;
		}
	}
	
	public boolean isShowRead() {
		return showRead;
	}
	
	private static int mFeedFilter=1;
	protected static String mSelectionFilter=null;
	/**
	 * readdate is null AND _id in (1,2,3) 
	 */
	public String getSelectionFilter(){
		return mSelectionFilter;
	}
	
	private static Cursor createManagedCursor(Activity context, Uri uri, boolean showRead, int iFeedFilter, boolean bResetSearchFilter) {
		mActivity = context;
		mStrSortOrder=new StringBuilder(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_PRIORITIZE, false) ? SQLREAD : Strings.EMPTY).append(FeedData.EntryColumns.DATE).append(Strings.DB_DESC).toString();
		mFeedFilter=iFeedFilter;
		// READDATEISNULL = "readdate is null";
		String selection = null;
		if(!showRead) selection="readdate is null";

		String ret=getAllTopFeeds();
		if(ret!=null){
			if(selection ==null){
				
				selection=EntryColumns.FEED_ID + " in (" + ret + ")";
			}else{
				//selection += " AND _id in (" + ret + ")";
				selection += " AND " + EntryColumns.FEED_ID + " in (" + ret + ")";
			}
		}
		if(bResetSearchFilter){
			mSearchFilter=null;
		}
		if(mSearchFilter!=null){
			if(selection == null){
				selection=mSearchFilter;
			}else{
				selection= selection + " AND " + mSearchFilter;
			}
		}
		mSelectionFilter=selection;

		ermittleAlleIDs(uri);

		return context.managedQuery(uri, null, selection, null,mStrSortOrder);
	}
	
	public static String[]TOPFEED_PROJECTION={"_ID", FeedData.FeedColumns.TOPFEED, FeedData.FeedColumns.SYNC};
	
	public static String getAllTopFeeds(){
		String ret=null;
		String selectionWhere=null;
		if(mFeedFilter==EntriesListActivity.EXTRA_FILTER_TOP_FEEDS){
			selectionWhere="TOPFEED=1";
		}else if(mFeedFilter==EntriesListActivity.EXTRA_FILTER_OFFLINE){
			selectionWhere="SYNC=1";
		}
		String liste=null;
		Cursor cursor = mActivity.getContentResolver().query(FeedColumns.CONTENT_URI, TOPFEED_PROJECTION, selectionWhere,null, null);
		if (cursor.moveToFirst()) {
			while (cursor.isAfterLast() == false) {
				int i= (cursor.getInt(0));
				if(ret==null){
					ret=""+i;
				}else{
					ret=ret + "," +i;
				}
				cursor.moveToNext();
			}
		}
		cursor.close();		
		return ret;
	}

	
	
	public void markAsRead() {
		forcedState = STATE_ALLREAD;
		markedAsRead.clear();
		markedAsUnread.clear();
		notifyDataSetInvalidated();
	}
	
	public void markAsUnread() {
		forcedState = STATE_ALLUNREAD;
		markedAsRead.clear();
		markedAsUnread.clear();
		notifyDataSetInvalidated();
	}
	
	public void neutralizeReadState() {
		forcedState = STATE_NEUTRAL;
	}

	public void markAsRead(long id) {
		markedAsRead.add(id);
		markedAsUnread.remove(id);
		notifyDataSetInvalidated();
	}

	public void markAsUnread(long id) {
		markedAsUnread.add(id);
		markedAsRead.remove(id);
		notifyDataSetInvalidated();
	}

	public long getDateFromFirst(){
		Cursor cursor = getCursor();
		if(cursor.moveToFirst()){
			long l =cursor.getLong(dateColumn);
			return l;
		}
		return 0;
	}


	protected Struktur getTeaser(Cursor cursor, String strTitle){
		String strAbstract=cursor.getString(abstractColumnPosition);
		String linkGrafik=null;
		int cut=200;
		if(strAbstract==null){
			strAbstract=cursor.getString(fulltextColumnPosition);
		}
		if(strAbstract!=null){
			if(strAbstract.startsWith("<")){
				try {
					JResult result = new ArticleTextExtractor().extractContent(strAbstract, true);
					linkGrafik=result.getImageUrl();
					if(linkGrafik==null || "".equals(linkGrafik)){
						linkGrafik=Util.takeFirstSrc(strAbstract);
					}
					strAbstract=result.getText();
				} catch (Exception e) {
					System.err.println("Err Extracting " + strTitle + " " + e);
				}
			}
			if(!"".equals(strAbstract) && strAbstract.length()>cut){
				// next '.'
				int point=strAbstract.indexOf('.',cut);  // -1 || 200..
				if(point<0 || point > cut+100){
					// next ' '
					point=strAbstract.indexOf(' ',cut);
					if(point<0 || point > cut+100){
						point=cut;
					}
				}
				strAbstract=strAbstract.substring(0, point);
			}
		}
		Struktur struktur = new Struktur();
		struktur.text=strAbstract;
		struktur.linkGrafik=linkGrafik;
		return struktur;
	}

	/**
	 * Marker für letzten angesehenen Entry
	 */
	protected long mDateFromLastShownUpHere;

	public long getmDateFromLastShownUpHere() {
		return mDateFromLastShownUpHere;
	}
	public void setmDateFromLastShownUpHere(long mDateFromLastShownUpHere) {
		this.mDateFromLastShownUpHere = mDateFromLastShownUpHere;
	}

}
