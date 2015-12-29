package com.hcireader.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.*;

public class CopyAssets {
    Context app_context;

    public CopyAssets(Context context) {
        app_context = context;
    }

    public void copyAsset(String path) {
        AssetManager manager = app_context.getAssets();

        // If we have a directory, we make it and recurse. If a file, we copy its
        // contents.
        try {
            String[] contents = manager.list(path);

            // The documentation suggests that list throws an IOException, but doesn't
            // say under what conditions. It'd be nice if it did so when the path was
            // to a file. That doesn't appear to be the case. If the returned array is
            // null or has 0 length, we assume the path is to a file. This means empty
            // directories will get turned into files.
            if (contents == null || contents.length == 0)
                throw new IOException();

            // Make the directory.
            File dir = new File(app_context.getExternalFilesDir(null), path);
            dir.mkdirs();

            // Recurse on the contents.
            for (String entry : contents) {
                copyAsset(path + "/" + entry);
            }
        } catch (IOException e) {
            copyFileAsset(path);
        }
    }

    private void copyFileAsset(String path) {
        File file = new File(app_context.getExternalFilesDir(null), path);
        try {
            InputStream in = app_context.getAssets().open(path);
            OutputStream out = new FileOutputStream(file);
            Log.i("CopyAssets", file.getAbsolutePath());
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Log.e("Foo", e.toString());
        }
    }
}
