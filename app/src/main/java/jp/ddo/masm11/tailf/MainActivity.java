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
    private boolean openingScroll;	// ファイルを開いた後の scroll 中?
    
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
	    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQ_PERMISSION_ON_CREATE);
	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
	Log.d("");
	super.onSaveInstanceState(outState);
	outState.putSerializable("file", file);
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
	
	try {
	    baseStream = new EndlessFileInputStream(file);
	    baseStream.seekLast(N);
	    reader = new BufferedReader(new InputStreamReader(baseStream));
	} catch (IOException e) {
	    Log.e(e, "ioexception");
	    
	    closeFile();
	    
	    AlertDialog dialog = new AlertDialog.Builder(this)
		    .setMessage("Couldn't open the file.")
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
		Log.d("line=%s", line);
		Log.d("remaining=%d", remaining);
		handler.post(new Runnable() {
		    @Override
		    public void run() {
			adapter.add(line);
			if (adapter.getCount() > N) {
			    // remove は first occurence を削除するらしいので、
			    // これでいいか。
			    adapter.remove(adapter.getItem(0));
			}
			if (openingScroll) {
			    ListView listView = (ListView) findViewById(R.id.listview);
			    assert listView != null;
			    listView.setSelection(adapter.getCount() - 1);

			    AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
			    assert appBarLayout != null;
			    appBarLayout.setExpanded(false);
			}
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
