package jp.ddo.masm11.tailf;

import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private class TailfReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();
	    if (action.equals("jp.ddo.masm11.tailf.LINE")) {
		String line = intent.getStringExtra("jp.ddo.masm11.tailf.LINE");
		ArrayList<String> lines = intent.getStringArrayListExtra("jp.ddo.masm11.tailf.LINES");
		// Log.d("MainActivity", line);
		
		if (line != null) {
		    buffer.append(line);
		    buffer.append('\n');
		} else {
		    for (String str: lines) {
			buffer.append(str);
			buffer.append('\n');
		    }
		}
		
		TextView textView = (TextView)findViewById(R.id.textview);
		assert textView != null;
		textView.setText(buffer);
		
		final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollview);
		// scrollView.fullScroll(View.FOCUS_DOWN);
		// scrollView.scrollTo(0, 10000);
		// scrollView.scrollTo(0, scrollView.getBottom());
		scrollView.post(new Runnable() {
		    @Override
		    public void run() {
			scrollView.fullScroll(ScrollView.FOCUS_DOWN);
		    }
		});
	    }
	}
    }
    
    private StringBuilder buffer;
    private TailfReceiver receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	
	TextView textView = (TextView)findViewById(R.id.textview);
	assert textView != null;
	
	buffer = new StringBuilder();
	
	textView.setText(buffer);
	
	receiver = new TailfReceiver();
	IntentFilter filter = new IntentFilter();
	filter.addAction("jp.ddo.masm11.tailf.LINE");
	registerReceiver(receiver, filter);
	
	Intent intent = new Intent(this, TailfService.class);
	intent.setAction("jp.ddo.masm11.tailf.START");
	intent.putExtra("jp.ddo.masm11.tailf.FILENAME", new File(getExternalCacheDir(), "test.txt").toString());
	startService(intent);
    }
}
