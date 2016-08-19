package jp.ddo.masm11.tailf;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.content.ActivityNotFoundException;
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
import android.util.Log;
import android.app.ActionBar;
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
	setContentView(R.layout.activity_main);
	
	Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
	setSupportActionBar(bar);
	
	handler = new Handler();
	
	buffer.setLength(0);
	lineCount = 0;
	
	try {
	    baseStream = new EndlessFileInputStream(new File(getExternalCacheDir(), "test.txt"));
	    reader = new BufferedReader(new InputStreamReader(baseStream));
	} catch (IOException e) {
	    Log.e("MainActivity", "onCreate", e);
	}
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
		Log.d("Main", "file=" + file.toString());
	    } else
		Log.w("Main", "data=null");
	}
	
	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    protected void onResume() {
	super.onResume();
	
	tailfThread = new TailfThread(reader, baseStream, new TailfThread.LineListener() {
	    @Override
	    public void onRead(String line, int remaining) {
		Log.d("MainActivity", line);
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
		
		handler.post(new Runnable() {
		    @Override
		    public void run() {
			final TextView textView = (TextView) findViewById(R.id.textview);
			assert textView != null;
			synchronized (buffer) {
			    textView.setText(buffer);
			}
			Log.d("Main", "txt.height=" + textView.getHeight());
			
			final NestedScrollView nestedScrollView = (NestedScrollView) findViewById(R.id.scrollview);
			nestedScrollView.post(new Runnable() {
			    @Override
			    public void run() {
				Log.d("Main", "scr.amount=" + nestedScrollView.getMaxScrollAmount());
				Log.d("Main", "scr.scrollY=" + nestedScrollView.getScrollY());
				Log.d("Main", "txt.height=" + textView.getHeight());
				// nestedScrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
				nestedScrollView.scrollTo(0, textView.getHeight());
			    }
			});
		    }
		});
	    }
	});
	thread = new Thread(tailfThread);
	thread.start();
    }
    
    @Override
    protected void onPause() {
	if (thread != null) {
	    thread.interrupt();
	    try {
		thread.join();
	    } catch (InterruptedException e) {
	    }
	    
	    thread = null;
	}
	
	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
	try {
	    reader.close();
	} catch (IOException e) {
	    Log.e("MainActivity", "onDestroy", e);
	}
	
	super.onDestroy();
    }
}
