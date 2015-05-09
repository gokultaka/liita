package in.stormlight.liita;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jcodec.common.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
                File image=null;
                Log.v("LOG_TAG", "Selected Images"+ mArrayUri.size());
                File[] mFile = new File[mArrayUri.size()];
                for(int i=0;i<mArrayUri.size();i++){
                    Uri temp = mArrayUri.get(i);
                    String directPath = getPath(CreateVideoActivity.this, temp);
                    File tempFile = new File(directPath);
                    mFile[i]= tempFile;
                    image = tempFile;
                }
//                File file = image;
                //mFile has all image files . send to ramo method
                File file = MediaUtil.getMP4VideoFile(mFile,null,"Ramo Rocks");
//                File file = new File(Environment.getExternalStorageDirectory()+"/Liita/test.mp4");
                if(file.exists()){
//                    Log.d("PATH IS","PATH"+file.getPath());
//                    Log.d("EDHO IRUKU","TAKA HERE");
//                    HttpClient client = new DefaultHttpClient();
//                    HttpPost post = new HttpPost(LiitaConstants.videoUploadPath);
//                    MultipartEntity postEntity = new MultipartEntity();
//                    postEntity.addPart("image", new FileBody(file, "image/jpeg"));
//                    post.setEntity(postEntity);
//                    try {
//                       HttpResponse response = client.execute(post);
//                        Log.d("TAAKKAKAKAKKAKA","Response"+response.toString());
//                    } catch (IOException e) {
//                        e.printStackTrace();

                    HttpClient httpclient = new DefaultHttpClient();
                    InputStream ip= null;
                    try {
                       ip = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    byte[] dataIo = null;
                    try {
                       dataIo = IOUtils.toByteArray(ip);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //post request to send the video
                    HttpPost httppost = new HttpPost(LiitaConstants.videoUploadPath);
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy( policy);
                    FileBody video_file1 = new FileBody(new File(file.getPath()));
                    InputStreamBody isb = new InputStreamBody(new ByteArrayInputStream(dataIo),"apop.mp4");
                    MultipartEntity reqEntity = new MultipartEntity();
                    reqEntity.addPart("file", isb);
                    httppost.setEntity(reqEntity);

                    // DEBUG
                    Log.d("TALA", "executing request " + httppost.getRequestLine( ) );
                    HttpResponse response = null;
                    try {
                        response = httpclient.execute( httppost );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    HttpEntity resEntity = response.getEntity( );

                    // DEBUG
                    System.out.println( response.getStatusLine( ) );
                    if (resEntity != null) {
                        try {
                            System.out.println( EntityUtils.toString(resEntity) );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } // end if
                    if (resEntity != null) {
                        try {
                            resEntity.consumeContent( );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } // end if

                    httpclient.getConnectionManager( ).shutdown( );

                }else {
                    Log.d("POCHE","POCHE");
                }

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
