package si.osbeorn.ct_challenge_2015.connected_cars;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends ActionBarActivity implements SurfaceHolder.Callback
{
    // region Members
    private static final String TAG = "CameraActivity";

    private int cameraId;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private Camera camera;
    private Camera.PictureCallback rawCallback;
    private Camera.ShutterCallback shutterCallback;
    private Camera.PictureCallback jpegCallback;

    private boolean inPreview = false;
    private boolean cameraConfigured=false;

    private Display display;

    private Button start, stop, capture;

    private byte[] pictureData;
    public static final String PICTURE_BYTE_DATA = "PICTURE_BYTE_DATA";


    //endregion

    //region Overrides

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /*
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        */

        /*
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        */

//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        //| View.SYSTEM_UI_FLAG_IMMERSIVE
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        surfaceView = (SurfaceView)findViewById(R.id.camera_surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        rawCallback = new Camera.PictureCallback()
        {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                Log.d(TAG, "onPictureTaken - raw");
            }
        };

        /** Handles data for jpeg picture */
        shutterCallback = new Camera.ShutterCallback()
        {
            public void onShutter()
            {
                Log.i(TAG, "onShutter'd");
            }
        };

        jpegCallback = new Camera.PictureCallback()
        {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                Log.d(TAG, "onPictureTaken - jpeg");
                setActivityResult(data);
            }
        };

        //start_camera();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        camera = Camera.open(getRequestedCameraId(Camera.CameraInfo.CAMERA_FACING_BACK));
        startPreview();
    }

    @Override
    protected void onPause()
    {
        if (inPreview)
        {
            camera.stopPreview();
        }

        camera.release();
        camera = null;
        inPreview = false;

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    private void setActivityResult(byte[] pictureByteArray)
    {
        FileOutputStream outStream;
        ExifInterface exif;

        Intent intent = getIntent();
        Uri uri = (Uri)intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        String filePath = uri.getPath();

        try
        {
            outStream = new FileOutputStream(filePath);
            outStream.write(/*flippedPictureByteArray*/pictureByteArray);
            outStream.close();

            exif = new ExifInterface(filePath);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                              String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            exif.saveAttributes();

            Log.d(TAG, "onPictureTaken - wrote bytes: " + /*flippedPictureByteArray*/pictureByteArray.length);

            Intent resultIntent = getIntent();
            resultIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            setResult(RESULT_OK, resultIntent);
            finish();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally {}
    }

    public void takePictureOrRecoding()
    {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public void takePictureOrRecording(View view)
    {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private int getRequestedCameraId(int cameraFacing)
    {
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; i++)
        {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);

            if (info.facing == cameraFacing)
            {
                cameraId = i;
                break;
            }
        }

        return cameraId;
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters)
    {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes())
        {
            if (size.width <= width && size.height <= height)
            {
                if (result==null)
                {
                    result=size;
                }
                else
                {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea)
                    {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }

    private Camera.Parameters setAdditionalParameters(Camera.Parameters parameters)
    {
        return parameters;
    }

    private int getCorrectOrientation()
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int rotation = display.getRotation();
        int degrees = 0;

        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {   // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private void initPreview(int width, int height)
    {
        if (camera != null && surfaceHolder.getSurface() != null)
        {
            try
            {
                camera.setPreviewDisplay(surfaceHolder);
            }
            catch (Throwable t)
            {
                Log.e(TAG, "Ex. in setPreviewDisplay()", t);
                Toast.makeText(CameraActivity.this, t.getMessage(), Toast.LENGTH_LONG)
                     .show();
            }

            if (!cameraConfigured)
            {
                Camera.Parameters parameters = camera.getParameters();
                parameters = setAdditionalParameters(parameters);

                Camera.Size size = getBestPreviewSize(width, height, parameters);

                if (size != null)
                {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setDisplayOrientation(getCorrectOrientation());
                    camera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }

    private void startPreview()
    {
        if (cameraConfigured && camera != null)
        {
            camera.setPreviewCallback(new Camera.PreviewCallback()
            {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    takePictureOrRecoding();
                }
            });

            camera.startPreview();
            inPreview=true;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        //int orientation = getResources().getConfiguration().orientation;

        initPreview(width, height);
        startPreview();
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        // TODO Auto-generated method stub
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // TODO Auto-generated method stub
    }
}