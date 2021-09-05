package com.thuvarahan.eduforum.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.network.file.api.GlobalRequestConfig;
import com.huawei.hms.network.file.api.Progress;
import com.huawei.hms.network.file.api.Response;
import com.huawei.hms.network.file.api.Result;
import com.huawei.hms.network.file.api.exception.InterruptedException;
import com.huawei.hms.network.file.api.exception.NetworkException;
import com.huawei.hms.network.file.download.api.DownloadManager;
import com.huawei.hms.network.file.download.api.FileRequestCallback;
import com.huawei.hms.network.file.download.api.GetRequest;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.interfaces.IAlertDialogTask;
import com.thuvarahan.eduforum.interfaces.IDownloadTask;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.checkSelfPermission;

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

    public static void saveLocalUserData(Context context, String userID, String displayName, String username, int userType, String dateCreated) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userID", userID);
        editor.putString("displayName", displayName);
        editor.putString("username", username);
        editor.putString("userType", String.valueOf(userType));
        editor.putString("dateCreated", dateCreated);
        editor.apply();
    }

    public static HashMap<String, Object> getLocalUserData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", MODE_PRIVATE);
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("userID", sharedPreferences.getString("userID", ""));
        userData.put("displayName", sharedPreferences.getString("displayName", ""));
        userData.put("username", sharedPreferences.getString("username", ""));
        userData.put("userType", sharedPreferences.getString("userType", "0"));
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
                switch (which) {
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

    public static void makeEllipsizedTextView(final TextView textView, int maxLine) {
        if (maxLine > 0 && textView.getLineCount() >= maxLine) {
            int lineEndIndex = textView.getLayout().getLineEnd(maxLine - 1);
            String text = textView.getText().subSequence(0, lineEndIndex) + "...";
            textView.invalidate();
            textView.setText(text);
        }
    }

    public static void showFullImage(Context context, Drawable drawable, URL imgUrl) {
        if (drawable != null && imgUrl != null && !(imgUrl.toString().trim().isEmpty())) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            final View alertView = layoutInflater.inflate(R.layout.content_imagedialog, null);
            final ImageView postImage = alertView.findViewById(R.id.post_image);
            postImage.setImageDrawable(drawable);
            dialog.setView(alertView);
            dialog.setCancelable(false);
            dialog.setNegativeButton("Close", null);
            dialog.setPositiveButton("Download", null);
            AlertDialog alertDialog = dialog.create();
            alertDialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            alertDialog.show();
            Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String fileName = imgUrl.getPath().substring(imgUrl.getPath().lastIndexOf('/') + 1);
                        downloadFile(context, imgUrl.toString(), fileName, new IDownloadTask() {
                            @Override
                            public void onFinished(boolean isDownloaded, String filePath) {
                                try {
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isDownloaded) {
                                                Toast.makeText(context.getApplicationContext(), "Download completed!" + ((filePath!=null)?" at "+filePath:""), Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(context.getApplicationContext(), "Download failed!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        alertDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                        alertDialog.dismiss();
                    }
                }
            });
        }
    }

    public static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static void downloadFile(Context context, String fileUrl, String fileName, IDownloadTask downloadTask) {
        if (checkStoragePermission(context)) {
            GlobalRequestConfig commonConfig = DownloadManager.newGlobalRequestConfigBuilder()
                    .retryTimes(1)
                    .build();
            DownloadManager downloadManager = new DownloadManager.Builder("downloadManager")
                    .commonConfig(commonConfig)
                    .build(context);
            String downloadFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "EduForum";
            File path = new File(downloadFilePath);
            if (!path.exists()) {
                path.mkdir();
            }
            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
            downloadFilePath += File.separator + "eduforum_" + System.currentTimeMillis() + "." + fileExtension;
            GetRequest getRequest = DownloadManager.newGetRequestBuilder()
                    .filePath(downloadFilePath)
                    .url(fileUrl)
                    .build();
            String TAG = "File Download";
            FileRequestCallback callback = new FileRequestCallback() {
                @Override
                public GetRequest onStart(GetRequest request) {
                    // Set the method to be called when file download starts.
                    Log.i(TAG, "activity new onStart:" + request);
                    return request;
                }

                @Override
                public void onProgress(GetRequest request, Progress progress) {
                    // Set the method to be called when the file download progress changes.
                    Log.i(TAG, "onProgress:" + progress);
                }

                @Override
                public void onSuccess(Response<GetRequest, File, Closeable> response) {
                    // Set the method to be called when file download is completed successfully.
                    String filePath = "";
                    if (response.getContent() != null) {
                        filePath = response.getContent().getAbsolutePath();
                    }
                    Log.i(TAG, "onSuccess:" + filePath);
                    downloadTask.onFinished(true, filePath);
                }

                @Override
                public void onException(GetRequest request, NetworkException exception, Response<GetRequest, File, Closeable> response) {
                    // Set the method to be called when a network exception occurs during file download or when the request is paused or canceled.
                    if (exception instanceof InterruptedException) {
                        String errorMsg = "onException for paused or canceled";
                        Log.w(TAG, errorMsg);
                    } else {
                        String errorMsg = "onException for:" + request.getId() + " " + Log.getStackTraceString(exception);
                        Log.e(TAG, errorMsg);
                    }
                    downloadTask.onFinished(false, null);
                }
            };
            Result result = downloadManager.start(getRequest, callback);
            if (result.getCode() != Result.SUCCESS) {
                // If the result is Result.SUCCESS, file download starts successfully. Otherwise, file download fails to be started.
                Log.e(TAG, "start download task failed:" + result.getMessage());
                downloadTask.onFinished(false, null);
            }
        }
    }

    public static Dialog createProgressDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.ProgressDialogSpinnerOnly);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View alertView = layoutInflater.inflate(R.layout.content_progressbar_alertdialog, null);
        dialogBuilder.setView(alertView);
        dialogBuilder.setCancelable(false);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        return dialog;
    }

    public static void toggleWindowInteraction(Activity activity, boolean canInteract) {
        if (canInteract) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
}
