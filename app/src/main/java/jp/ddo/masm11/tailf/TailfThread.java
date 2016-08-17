package jp.ddo.masm11.tailf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

class TailfThread implements Runnable {
    interface TailfReceiver {
	void onRead(String line, int remaining);
    }
    
    private final EndlessFileInputStream stream;
    private final BufferedReader reader;
    private final TailfReceiver receiver;
    
    TailfThread(File file, TailfReceiver receiver)
	    throws IOException {
	stream = new EndlessFileInputStream(file);
	reader = new BufferedReader(new InputStreamReader(stream));
	
	this.receiver = receiver;
    }
    
    public void run() {
	try {
	    while (true) {
		String line = reader.readLine();
		int remaining = stream.available();
		receiver.onRead(line, remaining);
	    }
	} catch (IOException e) {
	}
    }
}
