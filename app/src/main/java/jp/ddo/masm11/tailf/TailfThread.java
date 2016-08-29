package jp.ddo.masm11.tailf;

import java.io.BufferedReader;
import java.io.IOException;

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
		String line = reader.readLine();
		int remaining = baseStream.available();
		lineListener.onRead(line, remaining);
		if (Thread.interrupted())
		    break;
	    }
	} catch (IOException e) {
	    Log.w(e, "ioexception");
	}
    }
}
