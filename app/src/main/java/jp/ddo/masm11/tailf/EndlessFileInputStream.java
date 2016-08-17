package jp.ddo.masm11.tailf;

import android.os.FileObserver;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

class EndlessFileInputStream extends InputStream {
    private class Watcher extends FileObserver {
	Watcher(File file) {
	    super(file.toString(), MODIFY);
	}
	
	@Override
	public void onEvent(int event, String path) {
	    android.util.Log.d("Watcher", "onEvent called.");
	    
	    synchronized (EndlessFileInputStream.this) {
		int pos = buf.position();
		try {
		    buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		    System.gc();
		} catch (IOException e) {
		    android.util.Log.e("EndlessFileInputStream", "onEvent", e);
		}
		buf.position(pos);
		
		EndlessFileInputStream.this.notify();
	    }
	}
    }
    
    private Watcher watcher;
    private File file;
    private long mark;
    private FileChannel channel;
    private MappedByteBuffer buf;
    
    EndlessFileInputStream(File file)
	    throws IOException {
	super();
	
	this.file = file;
	FileInputStream fis = new FileInputStream(file.toString());
	channel = fis.getChannel();
	long size = channel.size();
	buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
	
	watcher = new Watcher(file);
	watcher.startWatching();
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
	watcher.stopWatching();
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
	    }
	    return buf.get();
	}
    }
    
    @Override
    public int read(byte[] b, int off, int len)
	    throws IOException {
	synchronized (this) {
	    while (buf.remaining() <= 0) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    throw new IOException(e);
		}
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
