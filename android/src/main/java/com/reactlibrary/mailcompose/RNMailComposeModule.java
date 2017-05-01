package com.reactlibrary.mailcompose;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

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
import java.util.Map;
import java.util.UUID;


public class RNMailComposeModule extends ReactContextBaseJavaModule {
    private static final int ACTIVITY_SEND = 129382;

    private Promise mPromise;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == ACTIVITY_SEND) {
                if (mPromise != null) {
                    if (resultCode == Activity.RESULT_CANCELED) {
                        mPromise.reject("cancelled", "Operation has been cancelled");
                    } else {
                        mPromise.resolve("sent");
                    }
                    mPromise = null;
                }
            }
        }
    };

    public RNMailComposeModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
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

    private void addAttachments(Intent intent, ReadableArray attachments) {
        if (attachments == null) return;

        ArrayList<Uri> uris = new ArrayList<>();
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.getType(i) == ReadableType.Map) {
                ReadableMap attachment = attachments.getMap(i);
                if (attachment != null) {
                    byte[] blob = getBlob(attachment, "data");
                    String text = getString(attachment, "text");
                    // String mimeType = getString(attachment, "mimeType");
                    String filename = getString(attachment, "filename");
                    if (filename == null) {
                        filename = UUID.randomUUID().toString();
                    }
                    String ext = getString(attachment, "ext");

                    File tempFile = createTempFile(filename, ext);

                    if (blob != null) {
                        tempFile = writeBlob(tempFile, blob);
                    } else if (text != null) {
                        tempFile = writeText(tempFile, text);
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

    @ReactMethod
    public void send(ReadableMap data, Promise promise) throws IOException {
        if (mPromise != null) {
            mPromise.reject("timeout", "Operation has timed out");
            mPromise = null;
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        String text = getString(data, "body");
        String html = getString(data, "html");
        if (!isEmpty(html)) {
            intent.setType("text/html");
            putExtra(intent, Intent.EXTRA_TEXT, Html.fromHtml(html));
            putExtra(intent, Intent.EXTRA_HTML_TEXT, Html.fromHtml(html));
        } else {
            intent.setType("text/plain");
            if (!isEmpty(text)) {
                putExtra(intent, Intent.EXTRA_TEXT, text);
            }
        }

        putExtra(intent, Intent.EXTRA_SUBJECT, getString(data, "subject"));
        putExtra(intent, Intent.EXTRA_EMAIL, getStringArray(data, "toRecipients"));
        putExtra(intent, Intent.EXTRA_CC, getStringArray(data, "ccRecipients"));
        putExtra(intent, Intent.EXTRA_BCC, getStringArray(data, "bccRecipients"));
        addAttachments(intent, getArray(data, "attachments"));

        intent.putExtra("exit_on_sent", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            getCurrentActivity().startActivityForResult(Intent.createChooser(intent, "Send Mail"), ACTIVITY_SEND);
            mPromise = promise;
        } catch (ActivityNotFoundException e) {
            promise.reject("failed", "Activity Not Found");
        } catch (Exception e) {
            promise.reject("failed", "Unknown Error");
        }
    }
}

