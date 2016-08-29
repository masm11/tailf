/* com.ipaulpro.afilechooser.utils.FileUtils を参考にした。
 */

package jp.ddo.masm11.tailf;

import android.os.Environment;
import android.content.Context;
import android.content.ContentUris;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.net.Uri;
import android.database.Cursor;
import java.io.File;

class FileUtils {
    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = {
                column
        };
	
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    static File getFile(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
	    String authority = uri.getAuthority();
	    String docid = DocumentsContract.getDocumentId(uri);
	    Log.d("authority=%s", authority);
	    Log.d("docid=%s", docid);
	    
	    if (authority.equals(BuildConfig.DOCUMENTS_AUTHORITY)) {
		Log.d("is filesystem.");
		return new File(docid);
	    }
	    
	    if (authority.equals("com.android.externalstorage.documents")) {
		Log.d("is external storage.");
		String[] split = docid.split(":", 2);
		if (split.length != 2) {
		    Log.w("bad docid.");
		    return null;
		}
		String type = split[0];
		Log.d("type=%s", type);
		if (type.equalsIgnoreCase("primary")) {
		    return new File(Environment.getExternalStorageDirectory(), split[1]);
		}
		if (type.matches("[0-9A-F]{4}-[0-9A-F]{4}")) {	// SD card
		    return new File("/storage/" + type + "/" + split[1]);
		}
		return null;
	    }
	    
	    if (authority.equals("com.android.providers.downloads.documents")) {
		Log.d("is downloads.");
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docid));
		String path = getDataColumn(context, contentUri, null, null);
		if (path == null)
		    return null;
                return new File(path);
	    }
	    
	    if (authority.equals("com.android.providers.media.documents")) {
		Log.d("is media.");
                String[] split = docid.split(":", 2);
		if (split.length != 2) {
		    Log.w("bad docid.");
		    return null;
		}
                String type = split[0];
                Uri contentUri;
		switch (type) {
		case "image":
		    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		    break;
                case "video":
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		    break;
                case "audio":
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		    break;
		default:
		    return null;
		}
		String path = getDataColumn(context, contentUri, "_id=?", new String[] { split[1] });
		if (path == null)
		    return null;
		return new File(path);
	    }
	    
	    Log.i("Unknown authority: %s", authority);
	    return null;
	}
	
	if (uri.getScheme().equalsIgnoreCase("file")) {
	    return new File(uri.getPath());
	}
	
	return null;
    }
}
