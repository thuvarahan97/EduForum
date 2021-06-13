package com.thuvarahan.eduforum;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.thuvarahan.eduforum.interfaces.IAlertDialogTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

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

    public static void saveLocalUserData(Context context, String userID, String displayName, String username, String dateCreated) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userID", userID);
        editor.putString("displayName", displayName);
        editor.putString("username", username);
        editor.putString("dateCreated", dateCreated);
        editor.apply();
    }

    public static HashMap<String, Object> getLocalUserData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", MODE_PRIVATE);
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("userID", sharedPreferences.getString("userID", ""));
        userData.put("displayName", sharedPreferences.getString("displayName", ""));
        userData.put("username", sharedPreferences.getString("username", ""));
        userData.put("dateCreated", sharedPreferences.getString("dateCreated", ""));
        return userData;
    }

    public static void clearLocalUserData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    public static void showAlertDialog(Context context, String title, String message, String yesOption, String noOption, IAlertDialogTask alertDialogTask) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        alertDialogTask.onPressedYes(dialog);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        alertDialogTask.onPressedNo(dialog);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialog);
        builder
//        .setTitle(title)
        .setMessage(message).setPositiveButton(yesOption, dialogClickListener)
        .setNegativeButton(noOption, dialogClickListener)
        .setCancelable(false)
        .show();

        /*AlertDialog dialog = builder.create();
        Button btnNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        btnNegative.setTextColor(Color.BLACK);
        dialog.show();*/
    }

    /*public static Uri getAppLink() {
        AppLinking.Builder builder = AppLinking.newBuilder()
            .setUriPrefix("https://eduforum.dra.agconnect.link")
            .setDeepLink(Uri.parse("https://eduforum-app.firebaseapp.com/question"))
            .setAndroidLinkInfo(AppLinking.AndroidLinkInfo.newBuilder()
                    .setAndroidDeepLink("agckit://eduforum-app.firebaseapp.com/question")
                    .build())
            .setSocialCardInfo(AppLinking.SocialCardInfo.newBuilder()
                    .setTitle("Title")
                    .setImageUrl("https://example.com/1.png")
                    .setDescription("Description").build())
            .setCampaignInfo(AppLinking.CampaignInfo.newBuilder()
                    .setName("name")
                    .setSource("AGC")
                    .setMedium("App")
                    .build())
            .setPreviewType(AppLinking.LinkingPreviewType.AppInfo);
        *//*builder.buildShortAppLinking().addOnSuccessListener(shortAppLinking -> {
            Uri shortLinkUri = shortAppLinking.getShortUrl();
        }).addOnFailureListener(e -> {
            //AppLinkingException
        });*//*
        return builder.buildAppLinking().getUri();
    }*/

    public static void copyTextToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    public static void saveLocalTokenData(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("push_token", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }

    public static String getLocalTokenData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("push_token", MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }

}
