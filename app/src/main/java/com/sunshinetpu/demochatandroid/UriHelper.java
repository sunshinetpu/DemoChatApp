package com.sunshinetpu.demochatandroid;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

public class UriHelper {
	public static final String KEY_EXTRA_SUCCESS = "success";
	public static final String KEY_EXTRA_PRIMARY = "primary";
	public static final String KEY_EXTRA_PATH = "path";
	public static final String UNKNOW_PATH = "unknown";

	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri
				.getAuthority());
	}

	public static Boolean hasExternalSDcard(Context context) {
		File[] externalDirs = context
				.getExternalFilesDirs(Environment.DIRECTORY_MOVIES);

		if (externalDirs != null && externalDirs.length > 1) {
			if (externalDirs[0] != null && externalDirs[1] != null) {
				return true;
			}

		}

		return false;
	}

	public static Uri getUriWithFileProvider(Context context, File file){
		return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".provider", file);
	}



	public static String getSdcardPath(Context context) {
		String sdCardPath = null;
		File[] externalDirs = context
				.getExternalFilesDirs(Environment.DIRECTORY_MOVIES);

		if (externalDirs != null && externalDirs.length > 1
				&& externalDirs[1] != null) {
			String pathAzData = externalDirs[1].getAbsolutePath();
			int index = pathAzData.indexOf("Android/data");
			if (index > 1) {
				sdCardPath = pathAzData.substring(0, index - 1);
			}

		}
		return sdCardPath;
	}



	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 * @author paulburke
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 * @author paulburke
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri
				.getAuthority());
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @param selection
	 *            (Optional) Filter used in the query.
	 * @param selectionArgs
	 *            (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 * @author paulburke
	 */
	public static String getDataColumn(Context context, Uri uri,
									   String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {

				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	public static Bundle getPath(final Context context, final Uri uri) {
		Boolean resultOK = false;
		Boolean isPrimary = false;
		String finalPath = null;
		// DocumentProvider
		if (DocumentsContract.isDocumentUri(context, uri)) {

			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				if ("primary".equalsIgnoreCase(type)) {
					resultOK = true;
					isPrimary = true;
					if (split.length > 1) {
						finalPath = Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/" + split[1];
					} else {
						finalPath = Environment.getExternalStorageDirectory()
								.getAbsolutePath();
					}

				} else {
					finalPath = docId;
					String sdcardPath = getSdcardPath(context);
					if (sdcardPath != null) {
						if (split.length > 1) {
							finalPath = sdcardPath + "/" + split[1];
						} else {
							finalPath = sdcardPath;
						}

						File outputDir = new File(finalPath);
						if (outputDir.exists()) {
							resultOK = true;
							isPrimary = false;
						}
					}

				}
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				finalPath = getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				finalPath = getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}
		Log.i("test", "finalPath is " + finalPath);
		Bundle result = new Bundle();
		
		if(finalPath != null){
			resultOK = true;
		}else{
			finalPath = UNKNOW_PATH;
		}
		
		result.putBoolean(KEY_EXTRA_SUCCESS, resultOK);
		result.putBoolean(KEY_EXTRA_PRIMARY, isPrimary);
		result.putString(KEY_EXTRA_PATH, finalPath);
		return result;
		
	}

	public static boolean isImageFile(String path){
		return path.endsWith("png") || path.endsWith("jpg") || path.endsWith("gif");
	}

	

}
