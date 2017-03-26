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
		boolean bShowFeedInfo = getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false);
		boolean bAutoreload = getIntent().getBooleanExtra(EXTRA_AUTORELOAD, false);
		int iFeedFilter=getIntent().getIntExtra(EntriesListActivity.EXTRA_SHOWFEEDFILTER, EntriesListActivity.EXTRA_FILTER_ALL);
		mAdapter = new EntriesListAdapter(this, uri, bShowFeedInfo, bAutoreload, R.layout.entrylistitem, iFeedFilter, true);
	}

}
