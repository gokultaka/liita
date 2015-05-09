package in.stormlight.liita;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;


public class CreateVideoActivity extends ActionBarActivity {

    private static TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_video);
        Log.d("TAKACREATE", "COMES");

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("COMES", "TAKACREATE COMES");
        String text = "";
        if(data.getData()!=null){

            Uri mImageUri=data.getData();
            Log.d("ONLYONE","SELECTED"+mImageUri.toString()+" Path is "+getPath(CreateVideoActivity.this,mImageUri));
            text += getPath(CreateVideoActivity.this,mImageUri);
        }else{
            if(data.getClipData()!=null){
                ClipData mClipData=data.getClipData();
                ArrayList<Uri> mArrayUri=new ArrayList<Uri>();
                for(int i=0;i<mClipData.getItemCount();i++){

                    ClipData.Item item = mClipData.getItemAt(i);
                    Uri uri = item.getUri();
                    mArrayUri.add(uri);
                    text+= getPath(CreateVideoActivity.this,uri);
                }
                Log.v("LOG_TAG", "Selected Images"+ mArrayUri.size());
            }
        }
        mTextView = (TextView)findViewById(R.id.selectedImages);
        mTextView.setText(text);
        super.onActivityResult(requestCode, resultCode, data);
    }
    public static String getPath(Context context, Uri uri){
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }


}
