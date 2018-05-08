package com.reactlibrary.mailcompose;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RNMailComposeModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final ReactApplicationContext reactContext;
    private static final int ACTIVITY_SEND = 129382;
    public static String lastSelection = null;
    private Promise mPromise;

    public RNMailComposeModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNMailCompose";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("name", getName());
        return constants;
    }

    private void putExtra(Intent intent, String key, String value) {
        if (value != null && !value.isEmpty()) {
            intent.putExtra(key, value);
        }
    }

    private void putExtra(Intent intent, String key, Spanned value) {
        if (value != null) {
            intent.putExtra(key, value);
        }
    }

    private void putExtra(Intent intent, String key, String[] value) {
        if (value != null && value.length > 0) {
            intent.putExtra(key, value);
        }
    }

    private void putExtra(Intent intent, String key, ArrayList<String> value) {
        if (value != null && value.size() > 0) {
            intent.putExtra(key, value);
        }
    }

    private void addAttachments(Intent intent, ReadableArray attachments, String fileProviderUri) {
        if (attachments == null) return;

        ArrayList<Uri> uris = new ArrayList<>();
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.getType(i) == ReadableType.Map) {
                ReadableMap attachment = attachments.getMap(i);
                if (attachment != null) {
                    Uri contentUri = null;
                    byte[] blob = null;
                    if (attachment.hasKey("url") && attachment.getType("url") == ReadableType.String && fileProviderUri != null) {
                        contentUri = FileProvider.getUriForFile(this.reactContext, fileProviderUri, new File(attachment.getString("url")));
                    } else {
                        blob = getBlob(attachment, "data");
                    }

                    String text = getString(attachment, "text");
                    // String mimeType = getString(attachment, "mimeType");
                    String filename = getString(attachment, "filename");
                    if (filename == null) {
                        filename = UUID.randomUUID().toString();
                    }
                    String ext = getString(attachment, "ext");

                    File tempFile = null;

                    if (blob != null) {
                        createTempFile(filename, ext);
                        tempFile = writeBlob(tempFile, blob);
                    } else if (text != null) {
                        createTempFile(filename, ext);
                        tempFile = writeText(tempFile, text);
                    } else if (contentUri != null) {
                        uris.add(contentUri);
                    }

                    if (tempFile != null) {
                        uris.add(Uri.fromFile(tempFile));
                    }
                }
            }
        }

        if (uris.size() > 0) {
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private String getString(ReadableMap map, String key) {
        if (map.hasKey(key) && map.getType(key) == ReadableType.String) {
            return map.getString(key);
        }
        return null;
    }

    private String[] getStringArray(ReadableMap map, String key) {
        ReadableArray array = getArray(map, key);
        if (array == null) return null;

        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (array.getType(i) == ReadableType.String) {
                String str = array.getString(i);
                if (!isEmpty(str)) {
                    list.add(str);
                }
            }
        }

        String[] arr = new String[list.size()];
        return list.toArray(arr);
    }

    private ReadableArray getArray(ReadableMap map, String key) {
        if (map.hasKey(key) && map.getType(key) == ReadableType.Array) {
            return map.getArray(key);
        }
        return null;
    }

    private ReadableMap getMap(ReadableMap map, String key) {
        if (map.hasKey(key) && map.getType(key) == ReadableType.Map) {
            return map.getMap(key);
        }
        return null;
    }

    private byte[] getBlob(ReadableMap map, String key) {
        if (map.hasKey(key) && map.getType(key) == ReadableType.String) {
            String base64 = map.getString(key);
            if (base64 != null && !base64.isEmpty()) {
                return Base64.decode(base64, 0);
            }
        }
        return null;
    }

    public static byte[] byteArrayFromUrl(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;

        try {
            is = url.openStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.

            int n;
            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        return baos.toByteArray();
    }

    private byte[] getBlobFromUri(ReadableMap map, String key) {
        if (map.hasKey(key) && map.getType(key) == ReadableType.String) {
            String uri = map.getString(key);
            if (uri != null && !uri.isEmpty()) {
                return byteArrayFromUrl(uri);
            }
        }
        return null;
    }

    private File createTempFile(String filename, String ext) {
        if (filename != null && ext != null) {
            try {
                return File.createTempFile(filename, ext, getCurrentActivity().getBaseContext().getExternalCacheDir());
            } catch (IOException e1) {
            }
        }
        return null;
    }

    private File writeText(File file, String text) {
        if (file != null && text != null) {
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
                bw.write(text);
                bw.flush();
                bw.close();
                return file;
            } catch (Exception e) {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (Exception e1) {
                    }
                }
            }
        }
        return null;
    }

    private File writeBlob(File file, byte[] blob) {
        if (file != null && blob != null) {
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream(file);
                fo.write(blob);
                fo.flush();
                fo.close();
                return file;
            } catch (Exception e) {
                if (fo != null) {
                    try {
                        fo.close();
                    } catch (Exception e1) {
                    }
                }
            }
        }
        return null;
    }

    public static class ChooserBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = String.valueOf(intent.getExtras().get(Intent.EXTRA_CHOSEN_COMPONENT));
            String chosenComponent = result.substring(result.indexOf("{") + 1, result.indexOf("/"));

            RNMailComposeModule.lastSelection = chosenComponent;
        }
    }

    @ReactMethod
    public void hasMailApp(String appName, Promise promise) {

        // Get App infos
        Intent emailAppIntent = getEmailAppIntent();
        List<ResolveInfo> emailAppInfos = getCurrentActivity().getPackageManager().queryIntentActivities(emailAppIntent, PackageManager.MATCH_ALL);

        for (int i = 0; i < emailAppInfos.size(); i++) {
            String packageName = emailAppInfos.get(i).activityInfo.packageName;
            if (packageName.equals(appName)) {
                promise.resolve(true);
                return;
            }
        }
        promise.resolve(false);
    }

    @ReactMethod
    public void getMailAppData(Promise promise) {

        WritableArray emailAppArray = new WritableNativeArray();

        // Get App infos
        Intent emailAppIntent = getEmailAppIntent();
        PackageManager packageManager = getCurrentActivity().getPackageManager();
        List<ResolveInfo> emailAppInfos = packageManager.queryIntentActivities(emailAppIntent, PackageManager.MATCH_ALL);

        ArrayList<String> addedPackages = new ArrayList<>();
        for (int i = 0; i < emailAppInfos.size(); i++) {
            ActivityInfo activityInfo = emailAppInfos.get(i).activityInfo;
            // Prevent Duplicated
            if (addedPackages.indexOf(activityInfo.packageName) == -1) {
                addedPackages.add(activityInfo.packageName);

                // Build Map with app data
                WritableMap emailAppData = new WritableNativeMap();
                emailAppData.putString("name", activityInfo.packageName);
                emailAppData.putString("raw", packageManager.getApplicationLabel(activityInfo.applicationInfo).toString());

                Drawable icon = packageManager.getApplicationIcon(activityInfo.applicationInfo);
                emailAppData.putString("icon", getBase64(icon != null ? icon : packageManager.getDefaultActivityIcon()));

                // Add to array
                emailAppArray.pushMap(emailAppData);
            }
        }
        promise.resolve(emailAppArray);
    }

    private String getBase64(Drawable icon) {
        BitmapDrawable drawable = (BitmapDrawable) icon;
        Bitmap bitmap = drawable.getBitmap();
        return encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);
    }

    private String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    @ReactMethod
    public void getLastSelection(Promise promise) {
        String selected = lastSelection;
        lastSelection = null;
        promise.resolve(selected);
    }

    @ReactMethod
    public void send(ReadableMap data, Promise promise) {
        if (mPromise != null) {
            mPromise.reject("timeout", "Operation has timed out");
            mPromise = null;
        }

        try {
            // Check if Mail App exists
            ArrayList<Intent> mailIntents = getEmailAppLauncherIntentsWithData(data);
            if (mailIntents == null || mailIntents.size() == 0) {
                Toast.makeText(getCurrentActivity(), "No matching app found", Toast.LENGTH_LONG).show();
                return;
            }

            // Create chooser
            Intent chooserIntent = Intent.createChooser(new Intent(), "Select email app:", getIntentSender()); //TODO: handle <22 API devices
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, mailIntents.toArray( new Parcelable[mailIntents.size()] ));
            getCurrentActivity().startActivityForResult(chooserIntent, ACTIVITY_SEND);
            mPromise = promise;
        } catch (NullPointerException e) {
            promise.reject("failed", "StartActivityForResult failed");
        } catch (ActivityNotFoundException e) {
            promise.reject("failed", "Activity Not Found");
        } catch (RuntimeException e) {
            promise.reject("failed", "External App Probably Cannot Handle Parcelable");
        } catch (Exception e) {
            promise.reject("failed", "Unknown Error");
        }
    }

    // Create intent for data share
    private Intent getDataIntent (ReadableMap data) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        String text = getString(data, "body");
        String html = getString(data, "html");
        if (!isEmpty(html)) {
            intent.setType("text/html");
            putExtra(intent, Intent.EXTRA_TEXT, Html.fromHtml(html));
            putExtra(intent, Intent.EXTRA_HTML_TEXT, Html.fromHtml(html));
        } else {
            // intent.setType("text/plain");
            intent.setType("message/rfc822");

            if (!isEmpty(text)) {
                putExtra(intent, Intent.EXTRA_TEXT, text);
            }
        }
        putExtra(intent, Intent.EXTRA_SUBJECT, getString(data, "subject"));
        putExtra(intent, Intent.EXTRA_EMAIL, getStringArray(data, "toRecipients"));
        putExtra(intent, Intent.EXTRA_CC, getStringArray(data, "ccRecipients"));
        putExtra(intent, Intent.EXTRA_BCC, getStringArray(data, "bccRecipients"));
        addAttachments(intent, getArray(data, "attachments"), getString(data, "fileProviderUri"));
        intent.putExtra("exit_on_sent", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return intent;
    }

    // Intent that only email apps can handle
    private Intent getEmailAppIntent() {
        Intent emailAppIntent = new Intent(Intent.ACTION_SENDTO);
        emailAppIntent.setData(Uri.parse("mailto:"));
        emailAppIntent.putExtra(Intent.EXTRA_EMAIL, "");
        emailAppIntent.putExtra(Intent.EXTRA_SUBJECT, "");

        return emailAppIntent;
    }

    // Get E-Mail App intents only for share picker (filtered for duplicates)
    private ArrayList<Intent> getEmailAppLauncherIntentsWithData (ReadableMap data) {
        ArrayList<Intent> emailAppLauncherIntentsWithData = new ArrayList<>();
        Intent dataIntent = getDataIntent(data);
        Intent emailAppIntent = getEmailAppIntent();

        String selectedApp = getString(data, "selectedApp");

        if (selectedApp != null) {
            // Set selected mail app
            emailAppLauncherIntentsWithData.add(((Intent) dataIntent.clone()).setPackage(selectedApp));
        } else {
            // Get All installed apps that can handle email intent
            PackageManager packageManager = getCurrentActivity().getPackageManager();
            List<ResolveInfo> emailApps = packageManager.queryIntentActivities(emailAppIntent, PackageManager.GET_META_DATA);
            ArrayList<String> addedPackages = new ArrayList<>();
            for (int i = 0; i < emailApps.size(); i++) {
                String packageName = emailApps.get(i).activityInfo.packageName;
                if (addedPackages.indexOf(packageName) == -1) {
                    addedPackages.add(packageName);
                    emailAppLauncherIntentsWithData.add(((Intent) dataIntent.clone()).setPackage(packageName));
                }
            }
        }
        return emailAppLauncherIntentsWithData;
    }

    private IntentSender getIntentSender () {
        Intent receiverIntent = new Intent(reactContext, ChooserBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(reactContext, ACTIVITY_SEND, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_SEND) {

            if (mPromise != null) {
                mPromise.resolve("unknown");
                mPromise = null;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }
}
