package com.sunshinetpu.demochatandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

public class RealPathUtil {
	@SuppressLint("NewApi")
	public static String getRealPathFromURI_API19(Context context, Uri uri) {

		String filePath = "";
		if (DocumentsContract.isDocumentUri(context, uri)) {
			String wholeID = DocumentsContract.getDocumentId(uri);

			// Split at colon, use second item in the array
			String[] splits = wholeID.split(":");
			if (splits.length == 2) {
				String id = splits[1];
				return id;

			}
		} else {
			filePath = uri.getPath();
		}
		return filePath;
	}

}