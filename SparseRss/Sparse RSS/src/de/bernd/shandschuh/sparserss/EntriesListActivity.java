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

public class EntriesListActivity extends ParentActivity {

	public void mySetContentView() {
		setContentView(R.layout.entries);
	}
	
	protected void createAdapter(){
		mAdapter = new EntriesListAdapter(this, uri, getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false), getIntent().getBooleanExtra(EXTRA_AUTORELOAD, false), R.layout.entrylistitem);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		
//		// Menues in die Toolbar, SHOW_AS_ACTION_ALWAYS zieht nur hier
//        MenuItem markAsRead = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread); 
//        MenuItemCompat.setShowAsAction(markAsRead, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//        markAsRead.setIcon(android.R.drawable.ic_menu_revert);
//		
//		getMenuInflater().inflate(R.menu.entrylist, menu);
//		return true;
//	}

}
