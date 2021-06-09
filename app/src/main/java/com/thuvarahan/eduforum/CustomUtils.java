package com.thuvarahan.eduforum;

import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CustomUtils {
    public static Drawable LoadImageFromUrl(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatTimestamp(Date timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp.getTime());
        return DateFormat.format("dd MMM yyyy hh:mm a", cal).toString();
    }
}
