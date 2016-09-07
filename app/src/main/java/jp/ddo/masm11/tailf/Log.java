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

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedList;

class Log {
    private static class Item {
	final int priority;
	final Throwable e;
	final String msg;
	final String klass;
	final String method;
	final Date stamp;
	Item(int priority, Throwable e, String msg,
		String klass, String method, Date stamp) {
	    this.priority = priority;
	    this.e = e;
	    this.msg = msg;
	    this.klass = klass;
	    this.method = method;
	    this.stamp = stamp;
	}
    }
    
    private static final LinkedList<Item> queue = new LinkedList<>();
    
    private static class Logger implements Runnable {
	public void run() {
	    try {
		while (true) {
		    Item item;
		    
		    synchronized (queue) {
			while (queue.size() == 0)
			    queue.wait();
			item = queue.remove();
		    }
		    
		    StringBuilder buf = new StringBuilder();
		    buf.append(item.method);
		    buf.append("(): ");
		    buf.append(item.msg);
		    if (item.e != null) {
			buf.append('\n');
			buf.append(android.util.Log.getStackTraceString(item.e));
		    }
		    String msg = buf.toString();
		    android.util.Log.println(item.priority, item.klass, msg);
		    
		    if (writer != null) {
			String time = formatter.format(item.stamp);
			writer.println(time + " " + item.klass + ": " + msg);
			writer.flush();
		    }
		}
	    } catch (InterruptedException e) {
		android.util.Log.d("Logger", "interrupted", e);
	    }
	}
    }
    
    static void init(File dir) {
	File logFile = new File(dir, "log.txt");
	
	try {
	    if (logFile.exists()) {
		writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
		writer.println("================");
		writer.flush();
	    }
	} catch (NullPointerException e) {
	    android.util.Log.e("Log", "nullpointerexception", e);
	} catch (IOException e) {
	    android.util.Log.e("Log", "ioexception", e);
	}
	
	Thread thread = new Thread(new Logger());
	thread.start();
    }
    
    static void d(String fmt, Object... args) {
	common(android.util.Log.DEBUG, null, fmt, args);
    }
    
    static void d(Throwable e, String fmt, Object... args) {
	common(android.util.Log.DEBUG, e, fmt, args);
    }
    
    static void i(String fmt, Object... args) {
	common(android.util.Log.INFO, null, fmt, args);
    }
    
    static void i(Throwable e, String fmt, Object... args) {
	common(android.util.Log.INFO, e, fmt, args);
    }
    
    static void w(String fmt, Object... args) {
	common(android.util.Log.WARN, null, fmt, args);
    }
    
    static void w(Throwable e, String fmt, Object... args) {
	common(android.util.Log.WARN, e, fmt, args);
    }
    
    static void e(String fmt, Object... args) {
	common(android.util.Log.ERROR, null, fmt, args);
    }
    
    static void e(Throwable e, String fmt, Object... args) {
	common(android.util.Log.ERROR, e, fmt, args);
    }
    
    private static void common(int priority, Throwable e, String fmt, Object... args) {
	String[] stkinf = getStackInfo();
	String msg = String.format(fmt, args);
	Item item = new Item(priority, e, msg, stkinf[0], stkinf[1], new Date());
	synchronized (queue) {
	    queue.addLast(item);
	    queue.notify();
	}
    }
    
    private static PrintWriter writer = null;
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    private static String[] getStackInfo() {
	StackTraceElement[] elems = Thread.currentThread().getStackTrace();
	
	return new String[] { elems[5].getClassName().replace("jp.ddo.masm11.tailf.", ""),
			      elems[5].getMethodName() };
    }
}
