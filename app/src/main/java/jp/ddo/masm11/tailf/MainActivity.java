package jp.ddo.masm11.tailf;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private class Test implements Runnable {
	public void run() {
	    try {
		Log.i("MainActivity", new File(getExternalCacheDir(), "test.txt").toString());
		new File(getExternalCacheDir(), "test.txt").createNewFile();
		
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new EndlessFileInputStream(
//					new File("/sdcard/Test/test.txt")
					new File(getExternalCacheDir(), "test.txt")
				    )));
		while (true) {
		    String line = br.readLine();
		    Log.i("Test", line);
		}
	    } catch (IOException e) {
		Log.e("MainActivity", "Error", e);
	    }
	}
    }
/*
    private class AdditionReader extends AsyncTask<Void, Void, Void> {
	@Override
	protected void onPreExecute() {
	}
	
	@Override
	protected void doInBackground() {
	}
	
	@Override
	protected void onProgressUpdate() {
	}
	
	@Override
	protected void onCancelled() {
	}
    }
*/
    
    private StringBuilder buffer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	
	TextView textView = (TextView)findViewById(R.id.textview);
	assert textView != null;
	
	buffer = new StringBuilder();
	
	try {
	    BufferedReader reader = new BufferedReader(new FileReader("/ueventd.rc"));
	    while (true) {
		String str = reader.readLine();
		if (str == null)
		    break;
		buffer.append(str);
		buffer.append('\n');
	    }
	} catch (IOException e) {
	    Log.e("MainActivity", "IOException", e);
	}
	
	textView.setText(buffer);
	
	ScrollView scrollView = (ScrollView) findViewById(R.id.scrollview);
	// scrollView.fullScroll(View.FOCUS_DOWN);
	scrollView.scrollTo(0, 1000);
	
	Thread thr = new Thread(new Test());
	thr.start();
    }
}
