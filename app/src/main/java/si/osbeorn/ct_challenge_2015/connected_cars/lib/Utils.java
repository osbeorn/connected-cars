package si.osbeorn.ct_challenge_2015.connected_cars.lib;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Benjamin on 28.3.2015.
 */
public class Utils
{
    private static final String TAG = "Utils";

    public static byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int byteArrayToInt(byte[] b)
    {
        return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
    }

    public static boolean digestMatch(byte[] data, byte[] digestData)
    {
        return Arrays.equals(getDigest(data), digestData);
    }

    public static byte[] getDigest(byte[] data)
    {
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            return messageDigest.digest(data);
        }
        catch (Exception ex)
        {
            Log.e("TAG", ex.toString());
            throw new UnsupportedOperationException("MD5 algorithm not available on this device.");
        }
    }

    public static <T> byte[] objectToByteArray(T requestObject)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] requestByteArray = null;
        try
        {
            out = new ObjectOutputStream(bos);
            out.writeObject(requestObject);
            requestByteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (out != null)
                    out.close();
            }
            catch (IOException ex) { }
            try
            {
                bos.close();
            }
            catch (IOException ex) { }
        }

        return requestByteArray;
    }

    public static <T> T byteArrayToObject(byte[] responseByteArray)
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(responseByteArray);
        ObjectInput in = null;
        T object = null;
        try {
            in = new ObjectInputStream(bis);
            object = (T) in.readObject();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (StreamCorruptedException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                bis.close();
            } catch (IOException ex) { }
            try
            {
                if (in != null)
                    in.close();
            } catch (IOException ex) { }
        }

        return object;
    }

    public static File createTempFile(String path, String suffix)
    {
        // Get the current timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());
        // Create an image name
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES +
                                                          File.separator +
                                                          "ConnectedCars" +
                                                          File.separator);

        File file = null;
        try
        {

            if (!storageDir.exists())
                storageDir.mkdirs();

            file = File.createTempFile(
                imageFileName,  /* prefix */
                suffix,         /* suffix */
                storageDir      /* directory */
            );

            return file;
        }
        catch (IOException ex)
        {
            Log.e(TAG, "Failed to create temp image file.", ex);
        }

        return file;
    }

    public static byte[] fileToByteArray(ContentResolver contentResolver, Uri uri)
    {
        try
        {
            InputStream iStream =  contentResolver.openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;

            while ((len = iStream.read(buffer)) != -1)
            {
                byteBuffer.write(buffer, 0, len);
            }

            return byteBuffer.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
