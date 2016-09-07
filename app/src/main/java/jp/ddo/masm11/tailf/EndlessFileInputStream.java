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

import android.support.annotation.NonNull;
import android.os.FileObserver;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

class EndlessFileInputStream extends InputStream {
/*
    private class Watcher extends FileObserver {
	Watcher(File file) {
	    super(file.toString(), MODIFY);
	    Log.d("file=%s", file.toString());
	}
	
	@Override
	public void onEvent(int event, String path) {
	    Log.d("called.");
	    
	    synchronized (EndlessFileInputStream.this) {
		int pos = buf.position();
		try {
		    buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		    System.gc();
		} catch (IOException e) {
		    Log.e(e, "ioexception");
		}
		buf.position(pos);
		
		EndlessFileInputStream.this.notify();
	    }
	}
    }
*/
    
    /*
      Marshmallow では FileObserver がうまく動作しないようなので、
      polling することにした。
      なんか反応悪いけど…
    */
    private class Watcher implements Runnable {
	private final File file;
	Watcher(File file) {
	    this.file = file;
	    Log.d("file=%s", file.toString());
	}
	public void run() {
	    try {
		long cur_size = -1;
		while (true) {
		    synchronized (EndlessFileInputStream.this) {
			if (cur_size > channel.size()) {
			    Log.d("truncated.");
			    truncated = true;
			    EndlessFileInputStream.this.notifyAll();
			    break;
			}
			if (cur_size != channel.size()) {
			    Log.d("size changed.");
			    cur_size = channel.size();
			    
			    int pos = buf.position();
			    try {
				buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
				System.gc();
			    } catch (IOException e) {
				Log.e(e, "ioexception");
			    }
			    buf.position(pos);
			    
			    EndlessFileInputStream.this.notify();
			}
		    }
		    
		    Thread.sleep(1000);
		}
	    } catch (InterruptedException e) {
		Log.e(e, "interrupted.");
	    } catch (IOException e) {
		Log.e(e, "ioexception.");
	    }
	}
    }
    
    private Watcher watcher;
    private Thread thread;
    private final File file;
    private long mark;
    private FileChannel channel;
    private MappedByteBuffer buf;
    private boolean truncated;
    
    EndlessFileInputStream(File file)
	    throws IOException {
	super();
	Log.d("file=%s", file.toString());
	
	this.file = file;
	FileInputStream fis = new FileInputStream(file.toString());
	channel = fis.getChannel();
	long size = channel.size();
	buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
	
	watcher = new Watcher(file);
	thread = new Thread(watcher);
	// watcher.startWatching();
	thread.start();
    }
    
    public void seekLast(int lines) {
	synchronized (this) {
	    int pos = buf.limit();
	    int lfc = 0;	// line-feed count
	    while (--pos >= 0) {
		byte b = buf.get(pos);
		if ((char) b == '\n') {
		    if (++lfc > lines) {
			pos++;
			break;
		    }
		}
	    }
	    if (pos < 0)
		pos = 0;
	    buf.position(pos);
	}
    }
    
    @Override
    public int available() {
	synchronized (this) {
	    return buf.remaining();
	}
    }
    
    @Override
    public void close()
	    throws IOException {
	Log.d("file=%s", file.toString());
	// watcher.stopWatching();
	thread.interrupt();
	try {
	    thread.join();
	} catch (InterruptedException e) {
	    Log.e(e, "interrupted.");
	}
	channel.close();
	buf = null;
	System.gc();
    }
    
    @Override
    public void mark(int readlimit) {
	synchronized (this) {
	    mark = buf.position();
	}
    }
    
    @Override
    public boolean markSupported() {
	return true;
    }
    
    @Override
    public int read()
	    throws IOException {
	synchronized (this) {
	    while (buf.remaining() <= 0) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    throw new IOException(e);
		}
		if (truncated)
		    return -1;
	    }
	    return buf.get();
	}
    }
    
    @Override
    public int read(@NonNull byte[] b, int off, int len)
	    throws IOException {
	synchronized (this) {
	    while (buf.remaining() <= 0) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    throw new IOException(e);
		}
		if (truncated)
		    return -1;
	    }
	    if (len > buf.remaining())
		len = buf.remaining();
	    buf.get(b, off, len);
	    return len;
	}
    }
    
    @Override
    public void reset() {
	synchronized (this) {
	    buf.position((int) mark);
	}
    }
    
    @Override
    public long skip(long n) {
	if (n < 0)
	    return 0;
	synchronized (this) {
	    if (n > buf.remaining())
		n = buf.remaining();
	    buf.position((int) (buf.position() + n));
	    return n;
	}
    }
}
