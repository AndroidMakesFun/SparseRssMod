package de.bernd.shandschuh.sparserss.util;

import java.util.ArrayList;

import com.amulyakhare.textdrawable.TextDrawable;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

	private static int iTextColor= Color.BLACK;

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

		if(Util.isLightTheme(context)){
			iTextColor=Color.BLACK;
		}else{
			iTextColor=Util.colGrey;
		}

		mInflater = LayoutInflater.from(context);
		mListeNavDrawerEntries = new ArrayList<NavDrawerLineEntry>();
		
		int buttonSize = Util.getButtonSizeInPixel(context);
		int densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
		
		Drawable drawable;
		String titel;
		if(Util.isLightTheme(context)){
			drawable = context.getResources().getDrawable( R.drawable.ic_arrow_left_light);
		}else{
			drawable = context.getResources().getDrawable( R.drawable.ic_arrow_left_dark);
		}
		titel = "Sparse rss Mod";
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_overview)); //  R.id.cancel_action));
		
//		drawable = context.getResources().getDrawable( R.drawable.ic_statusbar_rss );
//		titel = context.getResources().getString(R.string.overview);
//		mListeNavDrawerEntries.add(new NavDrawerLineEntry(drawable, titel, R.id.menu_overview));
		
//		drawable = context.getResources().getDrawable( R.drawable.icon );
		Bitmap bitmap = BitmapFactory.decodeResource (context.getResources(), R.drawable.icon);
		bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);		
		BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
		bitmapDrawable.setTargetDensity(densityDpi);	
		titel = context.getResources().getString(R.string.all);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(bitmapDrawable, titel, R.id.menu_alle));
		
		bitmap = BitmapFactory.decodeResource (context.getResources(), R.drawable.ic_terrain_grey600_48dp);
		bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);		
		bitmapDrawable = new BitmapDrawable(bitmap);
		bitmapDrawable.setTargetDensity(densityDpi);
		titel = context.getResources().getString(R.string.topfeeds);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(bitmapDrawable, titel, R.id.menu_alle_top_feeds));
		
		bitmap = BitmapFactory.decodeResource (context.getResources(), R.drawable.ic_save_grey600_48dp);
		bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);		
		bitmapDrawable = new BitmapDrawable(bitmap);
		bitmapDrawable.setTargetDensity(densityDpi);		
		titel = context.getResources().getString(R.string.offline);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(bitmapDrawable, titel, R.id.menu_alle_offline));
		
		bitmap = BitmapFactory.decodeResource (context.getResources(), android.R.drawable.star_big_off);
		bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);		
		bitmapDrawable = new BitmapDrawable(bitmap);
		bitmapDrawable.setTargetDensity(densityDpi);
		titel = context.getResources().getString(R.string.favorites);
		mListeNavDrawerEntries.add(new NavDrawerLineEntry(bitmapDrawable, titel, R.id.menu_favorites));
		
//		mListeNavDrawerEntries.add(new NavDrawerLineEntry(null, "", R.id.menu_overview));
		
		Cursor cursor;
		cursor = context.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, null, null, null);
		
		int nameColumnPosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		int iconPosition = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
		
		int linkPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			String name = cursor.getString(nameColumnPosition);
			int id=Integer.parseInt(cursor.getString(idPosition));
			byte[] iconBytes = cursor.getBlob(iconPosition);
			String link = cursor.getString(linkPosition);
			
			try {
				if(!link.contains(".feedburner.com")){
					if (iconBytes != null && iconBytes.length > 0) {
						bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
						bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
						bitmap = Util.getRoundedBitmap(bitmap);
						bitmapDrawable = new BitmapDrawable(bitmap);
						bitmapDrawable.setTargetDensity(densityDpi);
						mListeNavDrawerEntries.add(new NavDrawerLineEntry(bitmapDrawable, name, id));
					}else{
						TextDrawable textDrawable = Util.getRoundButtonImage(context, Long.valueOf(id), name);
						mListeNavDrawerEntries.add(new NavDrawerLineEntry(textDrawable, name, id));
					}
				}else{
					TextDrawable textDrawable = Util.getRoundButtonImage(context, Long.valueOf(id), name);
					mListeNavDrawerEntries.add(new NavDrawerLineEntry(textDrawable, name, id));
				}
			} catch (Exception e) {
				System.err.println("Catched Exception for createScaledBitmap in NavigationDrawerAdapter");
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
			holder.text.setTextColor(iTextColor);
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
