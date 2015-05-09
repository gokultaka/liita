package in.stormlight.liita;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class PlayBackActivity extends ActionBarActivity {

    private static VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_back);
        Intent intent = getIntent();
        String content = intent.getExtras().getString(LiitaConstants.contents);
        Toast.makeText(getApplicationContext(),"Received content "+content, Toast.LENGTH_LONG).show();
        mVideoView = (VideoView)findViewById(R.id.videoView);
        PlayBackDownload PB = new PlayBackDownload(this,content);
        PB.execute("");
    }
    class PlayBackDownload extends AsyncTask<String,Boolean,Boolean>{
        ProgressDialog PD;
        private Context mContext;
        private String mVideoFileName;
        public PlayBackDownload(Context context,String videoFileName){
            mContext = context;
            mVideoFileName = videoFileName;
        }
        @Override
        protected void onPreExecute() {
            PD= ProgressDialog.show(PlayBackActivity.this,null, "Please Wait ...Downloading", true);
            PD.setCancelable(true);
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            String videoFilePath = "http://52.7.71.133/uploads/"+mVideoFileName+".mp4";
         return DownloadFile(videoFilePath,mVideoFileName+".mp4");
        }
        protected void onPostExecute(Boolean result) {
            PD.dismiss();

            mVideoView.setVideoPath(Environment.getExternalStorageDirectory() + "/Liita/" + mVideoFileName + ".mp4");
            mVideoView.setMediaController(new android.widget.MediaController(mVideoView.getContext()));
            mVideoView.requestFocus();
            mVideoView.start();

        }

    }
    public Boolean DownloadFile(String fileURL, String fileName) {
        try {

            String RootDir = Environment.getExternalStorageDirectory()+"/Liita";

            File rootFile = new File(RootDir);
            boolean exists = rootFile.exists();

            if(!exists){
                rootFile.mkdir();
            }
            File file = new File(RootDir,fileName);
            if(!file.exists()) {
                URL u = new URL(fileURL);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(true);
                c.connect();
                FileOutputStream f = new FileOutputStream(file);
                InputStream in = c.getInputStream();
                byte[] buffer = new byte[1024];
                int len1 = 0;

                while ((len1 = in.read(buffer)) > 0) {
                    f.write(buffer, 0, len1);
                }
                f.close();
            }
        return true;

        } catch (Exception e) {

            Log.d("Error....", e.toString());
            return false;
        }
    }
}
