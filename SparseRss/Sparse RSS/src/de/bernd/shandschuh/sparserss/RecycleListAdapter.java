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

import java.io.File;
import java.net.URL;
import java.util.Date;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.jetwick.snacktory.ArticleTextExtractor;
import de.jetwick.snacktory.JResult;

public class RecycleListAdapter extends EntriesListAdapter {

	
	
	public RecycleListAdapter(Activity context, Uri uri, boolean showFeedInfo, boolean autoreload, int layout, int iFeedFilter) {
		super(context, uri,showFeedInfo, autoreload,layout, iFeedFilter, true);
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		if (Util.isLightTheme(context)) {
			textView.setTextColor(Color.BLACK);
		}

		String strTitle=cursor.getString(titleColumnPosition);
		textView.setText(strTitle);
		float fsize=15.0f;
		textView.setTextSize(fsize); // etwas größer

		TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);
		
		final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
		
		
		TextView feedTextView = (TextView) view.findViewById(R.id.text3);
		String strAbstract=cursor.getString(abstractColumnPosition);
		int cut=200;
		String linkGrafik=null;
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
			if(strAbstract.length()>cut){
				strAbstract=strAbstract.substring(0, cut);
			}
			feedTextView.setText(strAbstract);
			
			feedTextView.setVisibility(View.GONE);
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
		String feedName = cursor.getString(feedNameColumn);		
		if (showFeedInfo && feedIconColumn > -1 && feedNameColumn > -1) {
			byte[] iconBytes = cursor.getBlob(feedIconColumn);
			
			try {
				if (iconBytes != null && iconBytes.length > 0  && !link.contains(".feedburner.com") && !link.contains("//feedproxy.google.com")) {
					
					Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
					
					if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
						dateTextView.setText(new StringBuilder().append(' ').append(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(feedName)); // bad style
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
			dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)));
		}
		
		if (forcedState == STATE_ALLUNREAD && !markedAsRead.contains(id) || (forcedState != STATE_ALLREAD && cursor.isNull(readDateColumn) && !markedAsRead.contains(id)) || markedAsUnread.contains(id)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			dateTextView.setEnabled(true);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			dateTextView.setEnabled(false);
		}
		
		final ImageView coverView = (ImageView) view.findViewById(R.id.coverimage);
		String mImageFolder = Util.getImageFolderFile(context).toString();
		String pathToImage = mImageFolder + "/" + id + "_cover.jpg";
//		Drawable d = Drawable.createFromPath(pathToImage);
		File imageFile = new File(pathToImage);
		if(imageFile.exists()){
			BitmapImageViewTarget roundedImageTarget = Util.getRoundedImageTarget(context, coverView, 30.0f);
			Glide.with(context).load(imageFile).asBitmap().centerCrop().into(roundedImageTarget);
//			Glide.with(context).load(imageFile).centerCrop().into(coverView);
		}else {
			if (linkGrafik==null){
				linkGrafik=cursor.getString(grafikLinkColumn);				
			}
			if (linkGrafik!=null){
				try {
					URL url = new URL(linkGrafik);
					BitmapImageViewTarget roundedImageTarget = Util.getRoundedImageTarget(context, coverView, 30.0f);
					Glide.with(context).load(url).asBitmap().centerCrop().into(roundedImageTarget);
				} catch (Exception e) {
					System.err.println("Err Loading dierect " + linkGrafik);
				}
			}
		}
	}
	
}
