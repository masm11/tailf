package jp.ddo.masm11.tailf;

import android.support.v4.widget.NestedScrollView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.view.ViewCompat;	// v13 にもあるが…?
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.net.Uri;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.Manifest;
import android.text.Spanned;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import com.ipaulpro.afilechooser.utils.FileUtils;

public class MainActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int N = 1000;
    private File file;
    private EndlessFileInputStream baseStream;
    private BufferedReader reader;
    private TailfThread tailfThread;
    private Thread thread;
/*
    private final StringBuilder buffer = new StringBuilder();
*/
    private int lineCount;	// synchronized (buffer) {} 内で。
    private Handler handler;
    
    private ArrayAdapter<CharSequence> adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.init(getExternalCacheDir());
	setContentView(R.layout.activity_main);
	
	Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
	setSupportActionBar(bar);
	
	handler = new Handler();
	
//	adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	adapter = new ArrayAdapter<CharSequence>(this, R.xml.line_layout);
	ListView listView = (ListView) findViewById(R.id.listview);
	listView.setAdapter(adapter);
	ViewCompat.setNestedScrollingEnabled(listView, true);
	
	if (savedInstanceState != null) {
	    Log.d("savedInstanceState exists.");
	    file = (File) savedInstanceState.getSerializable("file");
	    if (file != null) {
		Log.d("savedInstanceState: file=%s", file.toString());
		openFile(file);
	    }
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
	    Intent intent = FileUtils.createGetContentIntent();
	    intent.setType(FileUtils.MIME_TYPE_TEXT);
	    Intent i = Intent.createChooser(intent, "Select a file");
	    startActivityForResult(i, 0);
	    return true;
	    
	default:
	    return super.onOptionsItemSelected(item);
	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == 0) {
	    if (data != null) {
		Uri uri = data.getData();
		file = FileUtils.getFile(this, uri);
		Log.d("file=%s", file.toString());
		
		if (FileUtils.isExternalStorageDocument(uri)) {
		    Log.d("is external storage document.");
		    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			Log.d("permission not granted.");
			String[] permissions = new String[] {
			    Manifest.permission.READ_EXTERNAL_STORAGE,
			};
			ActivityCompat.requestPermissions(this, permissions, 0);
		    } else {
			Log.d("permission already granted.");
			open();
		    }
		} else {
		    Log.d("is not external storage document.");
		    open();
		}
	    } else
		Log.w("data=null");
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
	closeFile();
	
	super.onDestroy();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
	    String[] permissions,
	    int[] grantResults) {
	if (requestCode == 0) {
	    if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
		open();
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
	// buffer.setLength(0);
	adapter.clear();
	lineCount = 0;
	updateTextView();
	
	try {
	    baseStream = new EndlessFileInputStream(file);
	    baseStream.seekLast(N);
	    reader = new BufferedReader(new InputStreamReader(baseStream));
	} catch (IOException e) {
	    Log.e(e, "ioexception");
	    
	    closeFile();
	    
	    AlertDialog dialog = new AlertDialog.Builder(this)
		    .setMessage("Couldn't open the file.")
		    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
    
    private void updateTextView() {
	handler.post(new Runnable() {
	    @Override
	    public void run() {
/*
		final TextView textView = (TextView) findViewById(R.id.textview);
		assert textView != null;
*/
/*
		synchronized (buffer) {
		    textView.setText(buffer);
		}
*/
/*
		Log.d("txt.height=%d", textView.getHeight());
*/
/*
		final NestedScrollView nestedScrollView = (NestedScrollView) findViewById(R.id.scrollview);
		nestedScrollView.post(new Runnable() {
		    @Override
		    public void run() {
			Log.d("scr.amount=%d", nestedScrollView.getMaxScrollAmount());
			Log.d("scr.scrollY=%d", nestedScrollView.getScrollY());
			Log.d("txt.height=%d", textView.getHeight());
			// nestedScrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
			nestedScrollView.scrollTo(0, textView.getHeight());
		    }
		});
*/
	    }
	});
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
			    // fixme: おかしい。
			    adapter.remove(adapter.getItem(0));
			}
		    }
		});
/*
		synchronized (buffer) {
		    buffer.append(line);
		    buffer.append('\n');
		    
		    if (++lineCount > N) {
			int max = buffer.length();
			for (int i = 0; i < max; i++) {
			    if (buffer.charAt(i) == '\n') {
				buffer.delete(0, i + 1);
				lineCount--;
				break;
			    }
			}
		    }
		}
*/
		
		if (remaining == 0)
		    updateTextView();
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
