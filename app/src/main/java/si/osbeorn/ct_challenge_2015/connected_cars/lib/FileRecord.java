package si.osbeorn.ct_challenge_2015.connected_cars.lib;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created by Benjamin on 6.4.2015.
 */
public class FileRecord
{
    private String filePath;
    private Date createdOn;
    private Bitmap fileBitmap;

    public FileRecord(String filePath, Date dateTaken)
    {
        this.filePath = filePath;
        this.createdOn = dateTaken;
    }

    public FileRecord() {}

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Date getDateTaken()
    {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn)
    {
        this.createdOn = createdOn;
    }

    public Bitmap getFileBitmap()
    {
        return fileBitmap;
    }

    public void setFileBitmap(Bitmap fileBitmap)
    {
        this.fileBitmap = fileBitmap;
    }

    @Override
    public String toString()
    {
        return "Created on: " + createdOn.toString();
    }
}
