package com.hcireader.app;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;

public class MainActivity extends ActionBarActivity implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    CopyAssets assetCopier;
    String DATA_PATH = "/storage/emulated/0/Android/data/com.hcireader.app/files/";
    TessBaseAPI tesseract;
    String lang = "eng+tur";
    ArrayList<String> result_list = new ArrayList<String>();
    ArrayAdapter<String> result_adapter;

    Bitmap currentPicture;
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            currentPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
            //String result_text = runOCR(currentPicture);
            Callable<String> photo_thread = new NewThread(tesseract, currentPicture);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future_text = executor.submit(photo_thread);
            try {
                String result = future_text.get(3, TimeUnit.SECONDS);
                if(result.contains("\n")) {
                    Collections.addAll(result_list, result.split("\n"));
                }
                else {
                    result_list.add(result);
                }
                result_adapter.notifyDataSetChanged();
            } catch (TimeoutException e) {
                future_text.cancel(true);
                Toast.makeText(getApplicationContext(), "No text found", Toast.LENGTH_SHORT).show();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            executor.shutdownNow();
            refreshCamera();

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assetCopier = new CopyAssets(getApplicationContext());
        assetCopier.copyAsset("tessdata");

        setContentView(R.layout.activity_main);
        tesseract = new TessBaseAPI();
        tesseract.init(DATA_PATH, lang);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(this);
        result_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, result_list);
        ListView listView = (ListView) findViewById(R.id.result_list);
        listView.setAdapter(result_adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Label", result_list.get(position));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Item copied", Toast.LENGTH_SHORT).show();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                intent.putExtra(SearchManager.QUERY, result_list.get(position));
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshCamera(){
        if(surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            Camera.Parameters params = camera.getParameters();
            params.setPictureSize(800, 600);
            camera.setParameters(params);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return;
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    public void takePicture(View view) {
        camera.takePicture(null, null, jpegCallback);
    }
}

class NewThread implements Callable<String> {
    private TessBaseAPI tesseract;
    private Bitmap picture;
    public NewThread(TessBaseAPI tesseract, Bitmap picture){
        this.tesseract = tesseract;
        this.picture = picture;
    }
    @Override
    public String call() throws Exception {
        tesseract.setImage(picture);
        String recognized = tesseract.getUTF8Text();
        recognized = recognized.replaceAll("\\p{Punct}+", "");
        recognized = recognized.trim();
        Log.i("OCR", "Recognized Text: " + recognized);
        return recognized;
    }
}