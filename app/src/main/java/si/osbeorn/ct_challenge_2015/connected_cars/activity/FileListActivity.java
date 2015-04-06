package si.osbeorn.ct_challenge_2015.connected_cars.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import si.osbeorn.ct_challenge_2015.connected_cars.R;
import si.osbeorn.ct_challenge_2015.connected_cars.adapter.FileListViewAdapter;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.FileRecord;


public class FileListActivity extends ListActivity
{

    private FileListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        adapter = new FileListViewAdapter(FileListActivity.this);
        populateAdapterWithExistingFiles();

        setListAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_list, menu);
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

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);


        FileRecord fileRecord = (FileRecord) getListAdapter().getItem(position);
        showPostActivity(fileRecord);
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight)
    {
        //First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize, Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        int inSampleSize = 1;

        if (height > reqHeight)
        {
            inSampleSize = Math.round((float)height / (float)reqHeight);
        }
        int expectedWidth = width / inSampleSize;

        if (expectedWidth > reqWidth)
        {
            //if(Math.round((float)width / (float)reqWidth) > inSampleSize) // If bigger SampSize..
            inSampleSize = Math.round((float)width / (float)reqWidth);
        }

        options.inSampleSize = inSampleSize;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }

    private Date decodeDateFromFile(String path)
    {
        String fileNameFormat = "JPEG_([\\w_]{15})_(.*)";
        Matcher m = Pattern.compile(fileNameFormat).matcher(path);
        if(m.matches()) {
            String stringDate = m.group(1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

            try {
                Date parsedDate = sdf.parse(stringDate);
                return parsedDate;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void populateAdapterWithExistingFiles()
    {
        File storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES +
                                                              File.separator +
                                                              "ConnectedCars" +
                                                              File.separator);

        if (!storageDir.exists())
            return;

        File[] files = storageDir.listFiles();
        for (File file : files)
        {
            Bitmap bitmap = decodeSampledBitmapFromFile(file.getAbsolutePath(), 200, 200);
            Date date = decodeDateFromFile(file.getName());

            FileRecord fileRecord = new FileRecord();
            fileRecord.setFilePath(file.getAbsolutePath());
            fileRecord.setFileBitmap(bitmap);
            fileRecord.setCreatedOn(date);

            adapter.add(fileRecord);
        }
    }

    private void showPostActivity(FileRecord fileRecord)
    {
        Intent intent = new Intent(FileListActivity.this, PostActivity.class);
        intent.putExtra(PostActivity.IMAGE_FILE_PATH_EXTRA, fileRecord.getFilePath());
        startActivity(intent);
    }
}
