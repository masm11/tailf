package jp.ddo.masm11.tailf;

import android.content.Intent;
import android.app.Service;
import android.os.IBinder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TailfService extends Service {
    private Thread thread;
    private TailfThread tailfThread;
    private ArrayList<String> buf100;
    private boolean starting;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	if (intent != null) {
	    String action = intent.getAction();
	    if (action == "jp.ddo.masm11.tailf.START") {
		String filename = intent.getStringExtra("jp.ddo.masm11.tailf.FILENAME");
		if (filename != null)
		    start(filename);
	    }
	}
	return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
    
    @Override
    public void onDestroy() {
	try {
	    thread.interrupt();
	    thread.join();
	} catch (InterruptedException e) {
	    android.util.Log.e("TailfService", "onDestroy", e);
	}
    }
    
    private void start(String filename) {
	try {
	    starting = true;
	    buf100 = new ArrayList<String>();
	    
	    tailfThread = new TailfThread(new File(filename), new TailfThread.TailfReceiver() {
		@Override
		public void onRead(String line, int remaining) {
		    android.util.Log.d("TailfService", line);
		    if (starting) {
			buf100.add(line);
			if (buf100.size() > 100)
			    buf100.remove(0);
			if (remaining <= 0) {
			    sendLines(buf100);
			    buf100 = null;
			    starting = false;
			}
		    } else
			sendLine(line);
		}
	    });
	    thread = new Thread(tailfThread);
	    thread.start();
	} catch (IOException e) {
	    android.util.Log.e("TailfService", "start", e);
	}
    }
    
    private void sendLine(String line) {
	Intent intent = new Intent("jp.ddo.masm11.tailf.LINE");
	intent.putExtra("jp.ddo.masm11.tailf.LINE", line);
	sendBroadcast(intent);
    }
    
    private void sendLines(ArrayList<String> lines) {
	Intent intent = new Intent("jp.ddo.masm11.tailf.LINE");
	intent.putExtra("jp.ddo.masm11.tailf.LINES", lines);
	sendBroadcast(intent);
    }
}
