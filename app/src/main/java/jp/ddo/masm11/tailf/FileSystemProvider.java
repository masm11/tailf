/* com.ianhanniballake.localstorage.LocalStorageProvider が元。
 */

package jp.ddo.masm11.tailf;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

public class FileSystemProvider extends DocumentsProvider {
    /**
     * Default root projection: everything but Root.COLUMN_MIME_TYPES
     */
    private final static String[] DEFAULT_ROOT_PROJECTION = new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS, Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_ICON,
            Root.COLUMN_AVAILABLE_BYTES};
    /**
     * Default document projection: everything but Document.COLUMN_ICON and Document.COLUMN_SUMMARY
     */
    private final static String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_FLAGS, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED};
    
    @Override
    public Cursor queryRoots(final String[] projection) throws FileNotFoundException {
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
	
        // Add Home directory
        File rootDir = new File("/");
	final MatrixCursor.RowBuilder row = result.newRow();
	// These columns are required
	row.add(Root.COLUMN_ROOT_ID, rootDir.getAbsolutePath());
	row.add(Root.COLUMN_DOCUMENT_ID, rootDir.getAbsolutePath());
	row.add(Root.COLUMN_TITLE, getContext().getString(R.string.filesystem));
	row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY);
	row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);
	// These columns are optional
	// row.add(Root.COLUMN_SUMMARY, "File system");
	row.add(Root.COLUMN_AVAILABLE_BYTES, new StatFs(rootDir.getAbsolutePath()).getAvailableBytes());
	// Root.COLUMN_MIME_TYPE is another optional column and useful if you have multiple roots with different
	// types of mime types (roots that don't match the requested mime type are automatically hidden)
	
	Log.d("queryRoots = %s", result.toString());
        return result;
    }
    
    @Override
    public Cursor queryChildDocuments(final String parentDocumentId, final String[] projection,
                                      final String sortOrder) throws FileNotFoundException {
	Log.d("");
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        final File parent = new File(parentDocumentId);
	File[] files = parent.listFiles();
	if (files == null) {
	    Log.w("failed to list files.");
	    return null;
	}
        for (File file : files) {
	    if (file.getName().equals("."))
		continue;
	    if (file.getName().equals(".."))
		continue;
	    // Adds the file's display name, MIME type, size, and so on.
	    includeFile(result, file);
        }
        return result;
    }

    @Override
    public Cursor queryDocument(final String documentId, final String[] projection) throws FileNotFoundException {
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        includeFile(result, new File(documentId));
        return result;
    }

    private void includeFile(final MatrixCursor result, final File file) throws FileNotFoundException {
	Log.d("");
        final MatrixCursor.RowBuilder row = result.newRow();
        // These columns are required
        row.add(Document.COLUMN_DOCUMENT_ID, file.getAbsolutePath());
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        String mimeType = getDocumentType(file.getAbsolutePath());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_FLAGS, 0);
        // COLUMN_SIZE is required, but can be null
        row.add(Document.COLUMN_SIZE, file.length());
        // These columns are optional
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        // Document.COLUMN_ICON can be a resource id identifying a custom icon. The system provides default icons
        // based on mime type
        // Document.COLUMN_SUMMARY is optional additional information about the file
    }

    @Override
    public String getDocumentType(final String documentId) throws FileNotFoundException {
	Log.d("");
        File file = new File(documentId);
        if (file.isDirectory())
            return Document.MIME_TYPE_DIR;
        // From FileProvider.getType(Uri)
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId, final String mode,
                                             final CancellationSignal signal) throws FileNotFoundException {
	Log.d("");
	return null;
    }

    @Override
    public boolean onCreate() {
	Log.d("");
        return true;
    }
}
