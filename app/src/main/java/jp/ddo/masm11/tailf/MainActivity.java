/*
    tailf for android
    Copyright (C) 2016 Yuuki Harano

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package jp.ddo.masm11.tailf;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.view.ViewCompat;	// v13 にもあるが…?
import android.support.design.widget.AppBarLayout;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.app.AlertDialog;
import android.net.Uri;
import android.Manifest;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

public class MainActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int REQ_PERMISSION_ON_CREATE = 1;
    private static final int REQ_FILE_SELECTION = 2;
    
    private static final int N = 1000;
    private File file;
    private EndlessFileInputStream baseStream;
    private BufferedReader reader;
    private TailfThread tailfThread;
    private Thread thread;
    private Handler handler;
    private ArrayAdapter<CharSequence> adapter;
    
    /* ファイルが開いた後は、目的地まで自動で scroll する。
     * その処理中かどうか。
     */
    private boolean openingScroll;
    
    /* 現在の scroll 位置。画面の最下部の位置。
     * 画面を回転した時には、openingScroll において、ここまで scroll する。
     */
    private int currentScrollPos;
    
    /* 現在 autoScroll 中かどうか。
     * autoScroll 中に画面を回転した時には、currentScrollPos に関わらず、
     * 最後まで scroll する。
     */
    private boolean autoScroll;
    
    private boolean hasFromSavedInstance;
    private int currentScrollPosFromSavedInstance;
    private boolean autoScrollFromSavedInstance;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.init(getExternalCacheDir());
	setContentView(R.layout.activity_main);
	
	Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
	setSupportActionBar(bar);
	
	handler = new Handler();
	
	adapter = new ArrayAdapter<>(this, R.layout.line_layout);
	ListView listView = (ListView) findViewById(R.id.listview);
	assert listView != null;
	listView.setAdapter(adapter);
	ViewCompat.setNestedScrollingEnabled(listView, true);
	listView.setOnScrollListener(new AbsListView.OnScrollListener() {
	    @Override
	    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		currentScrollPos = firstVisibleItem + visibleItemCount - 1;
		autoScroll = (currentScrollPos >= totalItemCount - 1);
		Log.d("currentScrollPos=%d, autoScroll=%b", currentScrollPos, autoScroll);
	    }
	    
	    @Override
	    public void onScrollStateChanged(AbsListView view, int scrollState) {
		// 手で scroll したら終わりってことで。
		// あとは transcriptMode="normal" に任せる。
		if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
		    openingScroll = false;
	    }
	});
	
	if (savedInstanceState != null) {
	    Log.d("savedInstanceState exists.");
	    file = (File) savedInstanceState.getSerializable("file");
	    currentScrollPosFromSavedInstance = savedInstanceState.getInt("currentScrollPos");
	    autoScrollFromSavedInstance = savedInstanceState.getBoolean("autoScroll");
	    hasFromSavedInstance = true;
	} else {
	    Intent intent = getIntent();
	    if (intent != null) {
		Log.d("intent exists.");
		String action = intent.getAction();
		Log.d("action=%s", action == null ? "null" : action);
		java.util.Set<String> cats = intent.getCategories();
		if (cats != null) {
		    for (String s: cats) {
			Log.d("category=%s", s);
		    }
		}
		
		if (action != null && action.equals(Intent.ACTION_VIEW)) {
		    Log.d("is view.");
		    Uri uri = intent.getData();
		    if (uri != null) {
			Log.d("uri=%s", uri.toString());
			file = FileUtils.getFile(this, uri);
			if (file == null)
			    Log.i("Couldn't get file path.");
		    }
		}
	    }
	}
	
	if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
	    // permission がある => ファイルを開くなら開く。
	    if (file != null) {
		Log.d("file=%s", file.toString());
		openFile(file);
	    }
	} else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
	    // permission がない && 説明を表示すべき => 説明を表示した後、request。
	    AlertDialog dialog = new AlertDialog.Builder(this)
		    .setMessage(R.string.please_grant_permission)
		    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			    String[] permissions = new String[] {
				Manifest.permission.READ_EXTERNAL_STORAGE,
			    };
			    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQ_PERMISSION_ON_CREATE);
			}
		    })
		    .create();
	    dialog.show();
	} else {
	    // permission がない && 説明不要 => request。
	    String[] permissions = new String[] {
		Manifest.permission.READ_EXTERNAL_STORAGE,
	    };
	    ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ON_CREATE);
	}
	Log.d("currentScrollPosFromSavedInstance=%d", currentScrollPosFromSavedInstance);
	Log.d("autoScrollFromSavedInstance=%b", autoScrollFromSavedInstance);
	Log.d("hasFromSavedInstance=%b", hasFromSavedInstance);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
	Log.d("");
	super.onSaveInstanceState(outState);
	outState.putSerializable("file", file);
	outState.putInt("currentScrollPos", currentScrollPos);
	outState.putBoolean("autoScroll", autoScroll);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main, menu);
	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.action_open:
	    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
	    intent.addCategory(Intent.CATEGORY_OPENABLE);
	    intent.setType("*/*");
	    startActivityForResult(intent, REQ_FILE_SELECTION);
	    return true;
	    
	default:
	    return super.onOptionsItemSelected(item);
	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == REQ_FILE_SELECTION) {
	    if (resultCode == RESULT_OK) {
		Uri uri = data.getData();
		Log.d("uri=%s", uri.toString());
		file = FileUtils.getFile(this, uri);
		Log.d("file=%s", file == null ? "null" : file.toString());
		if (file == null)
		    Log.i("Couldn't get file path.");
		else
		    open();
	    }
	}
	
	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    protected void onResume() {
	super.onResume();
	
	startThread();
    }
    
    @Override
    protected void onPause() {
	stopThread();
	
	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
	Log.d("");
	closeFile();
	
	super.onDestroy();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
	    @NonNull String[] permissions,
	    @NonNull int[] grantResults) {
	if (requestCode == REQ_PERMISSION_ON_CREATE) {
	    /* 結果に関わらず、アクセスはする。
	     * で、エラーならエラー dialog を表示する。
	     */
	    if (file != null) {
		Log.d("file=%s", file.toString());
		openFile(file);
	    }
	}
    }
    
    private void open() {
	hasFromSavedInstance = false;
	
	if (thread != null) {
	    stopThread();
	    closeFile();
	    openFile(file);
	    startThread();
	} else {
	    closeFile();
	    openFile(file);
	}
    }
    
    private void openFile(File file) {
	adapter.clear();
	openingScroll = true;
	currentScrollPos = 0;
	autoScroll = true;
	
	try {
	    baseStream = new EndlessFileInputStream(file);
	    baseStream.seekLast(N);
	    reader = new BufferedReader(new InputStreamReader(baseStream));
	} catch (IOException e) {
	    Log.e(e, "ioexception");
	    
	    closeFile();
	    
	    AlertDialog dialog = new AlertDialog.Builder(this)
		    .setMessage(R.string.couldnt_open_file)
		    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			    // NOP.
			}
		    })
		    .create();
	    dialog.show();
	}
    }
    
    private void closeFile() {
	if (reader != null) {
	    try {
		reader.close();
	    } catch (IOException e) {
		Log.e(e, "ioexception");
	    }
	    reader = null;
	    baseStream =  null;
	}
    }
    
    private void startThread() {
	if (reader == null)
	    return;
	
	if (thread != null) {
	    Log.w("Thread is already running.");
	    return;
	}
	
	tailfThread = new TailfThread(reader, baseStream, new TailfThread.LineListener() {
	    @Override
	    public void onRead(final String line, int remaining) {
		// Log.d("line=%s", line);
		// Log.d("remaining=%d", remaining);
		handler.post(new Runnable() {
		    @Override
		    public void run() {
			adapter.add(line);
			if (adapter.getCount() > N) {
			    // remove は first occurrence を削除するらしいので、
			    // これでいいか。
			    adapter.remove(adapter.getItem(0));
			}
			if (openingScroll) {
			    ListView listView = (ListView) findViewById(R.id.listview);
			    assert listView != null;
			    
			    int newpos;
			    char r;
			    if (hasFromSavedInstance) {
				if (autoScrollFromSavedInstance) {
				    newpos = adapter.getCount() - 1;
				    r = 'a';
				} else {
				    if (currentScrollPosFromSavedInstance > adapter.getCount() - 1) {
					newpos = adapter.getCount() - 1;
					r = 'A';
				    } else {
					newpos = currentScrollPosFromSavedInstance;
					r = 'c';
				    }
				}
			    } else {
				newpos = adapter.getCount() - 1;
				r = 'n';
			    }
			    Log.d("newpos=%d/%c", newpos, r);
			    listView.setSelection(newpos);
			    // fixme: transcriptMode をうまく調整してみる?
			    
			    AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
			    assert appBarLayout != null;
			    appBarLayout.setExpanded(false);
			}
		    }
		});
	    }
	    @Override
	    public void onEOF() {
		handler.post(new Runnable() {
		    @Override
		    public void run() {
			AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
				.setMessage(R.string.file_was_truncated)
				.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int id) {
				    }
				})
				.create();
			dialog.show();
		    }
		});
	    }
	});
	thread = new Thread(tailfThread);
	thread.start();
    }
    
    private void stopThread() {
	if (thread != null) {
	    thread.interrupt();
	    try {
		thread.join();
	    } catch (InterruptedException e) {
		Log.w(e, "ioexception");
	    }
	    
	    thread = null;
	    tailfThread = null;
	}
    }
}
