package jp.ddo.masm11.tailf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

class TailfThread implements Runnable {
    interface LineListener {
	void onRead(String line, int remaining);
    }
    
    private final BufferedReader reader;
    private final EndlessFileInputStream baseStream;
    private final LineListener lineListener;
    
    TailfThread(BufferedReader reader, EndlessFileInputStream baseStream, LineListener lineListener) {
	this.baseStream = baseStream;
	this.reader = reader;
	this.lineListener = lineListener;
    }
    
    public void run() {
	Log.d("");
	try {
	    while (true) {
		Log.d("1");
		String line = reader.readLine();
		Log.d("2");
		int remaining = baseStream.available();
		Log.d("3");
		lineListener.onRead(line, remaining);
		Log.d("4");
		if (Thread.currentThread().interrupted())
		    break;
		Log.d("5");
	    }
	    Log.d("6");
	} catch (IOException e) {
	    Log.w(e, "ioexception");
	}
    }
}
