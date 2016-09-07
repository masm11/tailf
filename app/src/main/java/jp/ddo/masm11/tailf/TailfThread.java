/*
    tailf for android
    Copyright (C) 2016 Yuuki Harano

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package jp.ddo.masm11.tailf;

import java.io.BufferedReader;
import java.io.IOException;

class TailfThread implements Runnable {
    interface LineListener {
	void onRead(String line, int remaining);
	void onEOF();
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
		if (line == null) {
		    lineListener.onEOF();
		    break;
		}
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
