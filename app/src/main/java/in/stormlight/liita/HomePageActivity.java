package in.stormlight.liita;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class HomePageActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        VideoFragment.OnFragmentInteractionListener{

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private static JSONObject mJsonResponse;
    private CharSequence mTitle;
    private String mVideoURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(position == 0){
            fragmentManager.beginTransaction().replace(R.id.container, new VideoFragment()).commit();

        }else {
            if(position == 1){
//                String contentTemp = "2382438911431181020";
//                GetVideoURL getVideoURL = new GetVideoURL(contentTemp,HomePageActivity.this);
//                getVideoURL.execute();
//
//                //
//                Log.d("TAKAVIDEOURL ","URL IS "+mVideoURL);

                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, 0);

            }else {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                        .commit();
            }
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Toast.makeText(getApplicationContext(),"You just scanned "+contents,Toast.LENGTH_LONG).show();
                //Use this token to get the accompanying mp4 file and send to Play back activity
                GetVideoURL getVideoURL = new GetVideoURL(contents,HomePageActivity.this);
                getVideoURL.execute();

                //


            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"POCHE POCEH",Toast.LENGTH_LONG).show();
                Log.i("xZing", "Cancelled");
            }
        }
    }

    public class GetVideoURL extends AsyncTask<Object ,Void, JSONObject> {
        ProgressDialog PD;
        private String mToken;
        private Context mContext;
        public GetVideoURL(String token,Context context){
            mToken = token;
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            PD= ProgressDialog.show(HomePageActivity.this,null, "Please Wait ...Downloading", true);
            PD.setCancelable(true);
        }

        @Override
        protected JSONObject doInBackground(Object[] params) {
            JSONObject jsonResponse = null;
            int responseCode = -1;
            try {
                String urlPath = LiitaConstants.urlPath+mToken+LiitaConstants.sourceLoc;
                Log.d("TAKAAAA","PATH "+urlPath);
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                responseCode = connection.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK){
                    InputStream inputStream = connection.getInputStream();

                    Reader reader = new InputStreamReader(inputStream);
                    int contentLen = connection.getContentLength();
                    char[] content = new char[contentLen];
                    reader.read(content);

                    String responseData = new String(content);

                    jsonResponse = new JSONObject(responseData);

                }else {
                    Log.i("TAKA", "Response code is " + responseCode);
                }

            } catch (MalformedURLException e) {
                Log.e("TAKA", "Exception thrown here", e);
            } catch (IOException e) {
                Log.e("TAKA", "Exception thrown here", e);
            } catch (Exception e) {
                Log.e("TAKA", "Generic Exception thrown here", e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            mJsonResponse = result;
            handleJSONResponse();
            PD.dismiss();
            Intent playBackIntent = new Intent(HomePageActivity.this,PlayBackActivity.class);
            Log.d("TRYING TO CALL","TAKAKA");
            playBackIntent.putExtra(LiitaConstants.contents,mVideoURL);
            startActivity(playBackIntent);

        }
    }

    private void handleJSONResponse(){
        if(mJsonResponse != null){
            try {
                JSONObject mJsonResponseJSONObject = mJsonResponse.getJSONObject("data");
                String url = mJsonResponseJSONObject.getString("video_url");
                mVideoURL = url;
                Log.d("URLPATH","URL IS "+url);
                String path = url.substring(url.lastIndexOf("/")+1,url.length()-4);
                mVideoURL = path;

                Log.d("VIDEOFILENAME","IS THIS"+path);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else {
            Log.d("ONNUM ILLA","OLLLLA");
        }

    }
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.home_page, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_home_page, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((HomePageActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
