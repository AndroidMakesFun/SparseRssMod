package de.bernd.shandschuh.sparserss.util;

import java.util.ArrayList;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.request.animation.DrawableCrossFadeFactory;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.R;
import de.bernd.shandschuh.sparserss.Util;
import de.bernd.shandschuh.sparserss.provider.FeedData;

public class NavigationDrawerAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private static ArrayList<NavDrawerLineEntry> mListeNavDrawerEntries = null;

	public class NavDrawerLineEntry {
		public Drawable res ;
		public String titel = "";
		public int ID = 0;

		public NavDrawerLineEntry(Drawable res, String strTitel, int targetid) {
			this.res = res;
			this.titel = strTitel;
			ID = targetid;
		}
	}

	public NavigationDrawerAdapter(Context context) {

		mInflater = LayoutInflater.from(context);
		mListeNavDrawerEntries = new ArrayList<NavDrawerLineEntry>();
		
		Drawable drawable;
		String titel;
		drawable = context.getResources().getDrawable( R.drawable.ic_arrow_back_grey600_36dp);
		titel = "Sparse rss Mod";
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_overview)); //  R.id.cancel_action));
		
//		drawable = context.getResources().getDrawable( R.drawable.ic_statusbar_rss );
//		titel = context.getResources().getString(R.string.overview);
//		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_overview));
		
		drawable = context.getResources().getDrawable( R.drawable.icon );
		titel = context.getResources().getString(R.string.all);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_alle));
		
		drawable = context.getResources().getDrawable( android.R.drawable.star_big_off );
		titel = context.getResources().getString(R.string.favorites);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_favorites));
		
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(null, "", R.id.menu_overview));
		
		Cursor cursor;
		cursor = context.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, null, null, null);
		
		int nameColumnPosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		int iconPosition = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
		
		int linkPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		int buttonSize = Util.getButtonSizeInPixel(context);
		
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			String name = cursor.getString(nameColumnPosition);
			int id=Integer.parseInt(cursor.getString(idPosition));
			byte[] iconBytes = cursor.getBlob(iconPosition);
			String link = cursor.getString(linkPosition);
			
			if(!link.contains(".feedburner.com")){
				if (iconBytes != null && iconBytes.length > 0) {
					Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
					bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
					mListeNavDrawerEntries.add(new NavDrawerLineEntry(new BitmapDrawable(bitmap), name, id));
				}else{
					TextDrawable textDrawable = Util.getRoundButtonImage(context, Long.valueOf(id), name);
					mListeNavDrawerEntries.add(new NavDrawerLineEntry(textDrawable, name, id));
				}
			}else{
				TextDrawable textDrawable = Util.getRoundButtonImage(context, Long.valueOf(id), name);
				mListeNavDrawerEntries.add(new NavDrawerLineEntry(textDrawable, name, id));
			}
			
			cursor.moveToNext();
		}
		cursor.close();
	}

	public int getCount() {
		return mListeNavDrawerEntries.size();
	}

	public Object getItem(int position) {
		return mListeNavDrawerEntries.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;

		if (convertView == null) {

			convertView = mInflater.inflate(R.layout.navigation_drawer_list_item, null);

			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.text43);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon43);
			
			convertView.setTag(holder);
			
		} else {

			holder = (ViewHolder) convertView.getTag();
		}

		holder.text.setText(mListeNavDrawerEntries.get(position).titel);
		holder.icon.setBackground(mListeNavDrawerEntries.get(position).res);
		return convertView;
	}

	static class ViewHolder {
		TextView text;
		ImageView icon;
	}

}
