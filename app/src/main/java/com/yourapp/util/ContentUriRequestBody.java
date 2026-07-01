package com.yourapp.util;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ContentUriRequestBody extends RequestBody {

    private final ContentResolver contentResolver;
    private final Uri uri;
    private final MediaType mediaType;

    public ContentUriRequestBody(ContentResolver contentResolver, Uri uri, String mimeType) {
        this.contentResolver = contentResolver;
        this.uri = uri;
        this.mediaType = MediaType.parse(mimeType != null ? mimeType : "application/octet-stream");
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Cannot open selected media");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
            }
        }
    }
}
