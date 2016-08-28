/* com.ianhanniballake.localstorage.LocalStorageProvider が元。
 */

package jp.ddo.masm11.tailf;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

    /**
     * Check to see if we are missing the Storage permission group. In those cases, we cannot access local files and
     * must invalidate any root URIs currently available.
     *
     * @param context The current Context
     * @return whether the permission has been granted it is safe to proceed
     */
    static boolean isMissingPermission(@Nullable Context context) {
        if (context == null) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Make sure that our root is invalidated as apparently we lost permission
            context.getContentResolver().notifyChange(
                    DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY), null);
            return true;
        }
        return false;
    }

    @Override
    public Cursor queryRoots(final String[] projection) throws FileNotFoundException {
	Log.d("getContext()=%s", getContext().toString());
	Log.d("package=%s", getContext().getPackageName().toString());
	Log.d("granted=%d", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE));
        if (getContext() == null || ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
	    Log.d("queryRoots = null.");
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
	
        // Add Home directory
        File rootDir = new File("/");
	final MatrixCursor.RowBuilder row = result.newRow();
	// These columns are required
	row.add(Root.COLUMN_ROOT_ID, rootDir.getAbsolutePath());
	row.add(Root.COLUMN_DOCUMENT_ID, rootDir.getAbsolutePath());
	row.add(Root.COLUMN_TITLE, getContext().getString(R.string.filesystem));
	row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_IS_CHILD);
	row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);
	// These columns are optional
	// row.add(Root.COLUMN_SUMMARY, "File system");
	row.add(Root.COLUMN_AVAILABLE_BYTES, new StatFs(rootDir.getAbsolutePath()).getAvailableBytes());
	// Root.COLUMN_MIME_TYPE is another optional column and useful if you have multiple roots with different
	// types of mime types (roots that don't match the requested mime type are automatically hidden)
/*
        // Add SD card directory
        File sdCard = new File("/storage/extSdCard");
        String storageState = EnvironmentCompat.getStorageState(sdCard);
        if (TextUtils.equals(storageState, Environment.MEDIA_MOUNTED) ||
                TextUtils.equals(storageState, Environment.MEDIA_MOUNTED_READ_ONLY)) {
            final MatrixCursor.RowBuilder row = result.newRow();
            // These columns are required
            row.add(Root.COLUMN_ROOT_ID, sdCard.getAbsolutePath());
            row.add(Root.COLUMN_DOCUMENT_ID, sdCard.getAbsolutePath());
            row.add(Root.COLUMN_TITLE, getContext().getString(R.string.sd_card));
            // Always assume SD Card is read-only
            row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY);
            row.add(Root.COLUMN_ICON, R.drawable.ic_sd_card);
            row.add(Root.COLUMN_SUMMARY, sdCard.getAbsolutePath());
            row.add(Root.COLUMN_AVAILABLE_BYTES, new StatFs(sdCard.getAbsolutePath()).getAvailableBytes());
        }
*/
	Log.d("queryRoots = %s", result.toString());
        return result;
    }

/*
    @Override
    public AssetFileDescriptor openDocumentThumbnail(final String documentId, final Point sizeHint,
                                                     final CancellationSignal signal) throws FileNotFoundException {
        if (isMissingPermission(getContext())) {
            return null;
        }
        // Assume documentId points to an image file. Build a thumbnail no larger than twice the sizeHint
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(documentId, options);
        final int targetHeight = 2 * sizeHint.y;
        final int targetWidth = 2 * sizeHint.x;
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inSampleSize = 1;
        if (height > targetHeight || width > targetWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / options.inSampleSize) > targetHeight
                    || (halfWidth / options.inSampleSize) > targetWidth) {
                options.inSampleSize *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(documentId, options);
        // Write out the thumbnail to a temporary file
        File tempFile = null;
        FileOutputStream out = null;
        try {
            tempFile = File.createTempFile("thumbnail", null, getContext().getCacheDir());
            out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (IOException e) {
            Log.e(e, "Error writing thumbnail");
            return null;
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(e, "Error closing thumbnail");
                }
        }
        // It appears the Storage Framework UI caches these results quite aggressively so there is little reason to
        // write your own caching layer beyond what you need to return a single AssetFileDescriptor
        return new AssetFileDescriptor(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY), 0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }
*/

/*
    @Override
    public boolean isChildDocument(final String parentDocumentId, final String documentId) {
        return documentId.startsWith(parentDocumentId);
    }
*/

    @Override
    public Cursor queryChildDocuments(final String parentDocumentId, final String[] projection,
                                      final String sortOrder) throws FileNotFoundException {
        if (isMissingPermission(getContext())) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        final File parent = new File(parentDocumentId);
	File[] files = parent.listFiles();
	if (files == null)
	    return null;
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
        if (isMissingPermission(getContext())) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        includeFile(result, new File(documentId));
        return result;
    }

    private void includeFile(final MatrixCursor result, final File file) throws FileNotFoundException {
        final MatrixCursor.RowBuilder row = result.newRow();
        // These columns are required
        row.add(Document.COLUMN_DOCUMENT_ID, file.getAbsolutePath());
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        String mimeType = getDocumentType(file.getAbsolutePath());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        int flags = 0;
        // We only show thumbnails for image files - expect a call to openDocumentThumbnail for each file that has
        // this flag set
/*
        if (mimeType.startsWith("image/"))
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
*/
        row.add(Document.COLUMN_FLAGS, flags);
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
        if (isMissingPermission(getContext())) {
            return null;
        }
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
	return null;
    }

    @Override
    public boolean onCreate() {
	Log.d("");
        return true;
    }
}
