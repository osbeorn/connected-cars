package si.osbeorn.ct_challenge_2015.connected_cars.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import si.osbeorn.ct_challenge_2015.connected_cars.R;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.FileRecord;

/**
 * Created by Benjamin on 6.4.2015.
 */
public class FileListViewAdapter extends BaseAdapter
{
    private ArrayList<FileRecord> list = new ArrayList<>();
    private static LayoutInflater inflater = null;
    private Context mContext;

    public FileListViewAdapter(Context context)
    {
        mContext = context;
        inflater = LayoutInflater.from(mContext);
    }

    public int getCount()
    {
        return list.size();
    }

    public Object getItem(int position)
    {
        return list.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {

        View newView = convertView;
        ViewHolder holder;

        FileRecord curr = list.get(position);

        if (null == convertView)
        {
            holder = new ViewHolder();
            newView = inflater.inflate(R.layout.view_file_record, null);
            holder.fileBitmap = (ImageView) newView.findViewById(R.id.fileBitmap);
            holder.createdOn = (TextView) newView.findViewById(R.id.createdOn);
            newView.setTag(holder);

        }
        else
        {
            holder = (ViewHolder) newView.getTag();
        }

        holder.fileBitmap.setImageBitmap(curr.getFileBitmap());
        holder.createdOn.setText("Created on: " + curr.getDateTaken().toString());

        return newView;
    }

    static class ViewHolder
    {
        ImageView fileBitmap;
        TextView createdOn;
    }

    public void add(FileRecord listItem)
    {
        list.add(listItem);
        notifyDataSetChanged();
    }

    public ArrayList<FileRecord> getList(){
        return list;
    }

    public void removeAllViews(){
        list.clear();
        this.notifyDataSetChanged();
    }
}
