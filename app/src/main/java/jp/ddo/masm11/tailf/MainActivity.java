package jp.ddo.masm11.tailf;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.net.Uri;
import android.database.Cursor;
import android.provider.DocumentsContract;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import com.ipaulpro.afilechooser.utils.FileUtils;

public class MainActivity extends AppCompatActivity {
    private EndlessFileInputStream baseStream;
    private BufferedReader reader;
    private TailfThread tailfThread;
    private Thread thread;
    private final StringBuilder buffer = new StringBuilder();
    private int lineCount;	// synchronized (buffer) {} 内で。
    private Handler handler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.init(getExternalCacheDir());
	setContentView(R.layout.activity_main);
	
	Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
	setSupportActionBar(bar);
	
	handler = new Handler();
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
		File file = FileUtils.getFile(this, uri);
		Log.d("file=%s", file.toString());
		
		if (thread != null) {
		    Log.d("stopThread.");
		    stopThread();
		    Log.d("closeFile.");
		    closeFile();
		    Log.d("openFile.");
		    openFile(file);
		    Log.d("startThread.");
		    startThread();
		} else {
		    Log.d("closeFile.");
		    closeFile();
		    Log.d("openFile.");
		    openFile(file);
		}
		Log.d("end.");
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
	Log.d("stopThread.");
	stopThread();
	
	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
	closeFile();
	
	super.onDestroy();
    }
    
    private void openFile(File file) {
	buffer.setLength(0);
	lineCount = 0;
	updateTextView();
	
	try {
	    baseStream = new EndlessFileInputStream(file);
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
		final TextView textView = (TextView) findViewById(R.id.textview);
		assert textView != null;
		synchronized (buffer) {
		    textView.setText(buffer);
		}
		Log.d("txt.height=%d", textView.getHeight());
		
		final NestedScrollView nestedScrollView = (NestedScrollView) findViewById(R.id.scrollview);
		nestedScrollView.post(new Runnable() {
		    @Override
		    public void run() {
/*
			Log.d("scr.amount=%d", nestedScrollView.getMaxScrollAmount());
			Log.d("scr.scrollY=%d", nestedScrollView.getScrollY());
			Log.d("txt.height=%d", textView.getHeight());
*/
			// nestedScrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
			nestedScrollView.scrollTo(0, textView.getHeight());
		    }
		});
	    }
	});
    }
    
    private void startThread() {
	Log.d("0");
	if (reader == null)
	    return;
	Log.d("1");
	
	if (thread != null) {
	    Log.w("Thread is already running.");
	    return;
	}
	
	tailfThread = new TailfThread(reader, baseStream, new TailfThread.LineListener() {
	    @Override
	    public void onRead(String line, int remaining) {
		Log.d("line=%s", line);
		synchronized (buffer) {
		    buffer.append(line);
		    buffer.append('\n');
		    
		    if (++lineCount > 100) {
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
		
		updateTextView();
	    }
	});
	Log.d("2");
	thread = new Thread(tailfThread);
	Log.d("3");
	thread.start();
	Log.d("4");
    }
    
    private void stopThread() {
	Log.d("0");
	if (thread != null) {
	    Log.d("1");
	    thread.interrupt();
	    Log.d("2");
	    try {
		Log.d("3");
		thread.join();
		Log.d("4");
	    } catch (InterruptedException e) {
		Log.d("5");
		Log.w(e, "ioexception");
	    }
	    Log.d("6");
	    
	    thread = null;
	    tailfThread = null;
	}
	Log.d("7");
    }
}
