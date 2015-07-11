//package com.way.Activity;
//
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentManager;
//import android.os.Bundle;
//import android.support.v7.app.ActionBarActivity;
//import android.view.Menu;
//import android.view.MenuItem;
//
//import com.way.contactListView.ExpandableListFragment;
//
//
//public class MainActivity extends ActionBarActivity {
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_expandablelistview);
//        FragmentManager fm = getSupportFragmentManager();
//        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
//        if(fragment == null) {
//            fragment = new ExpandableListFragment();
//            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
//        }
//
//    }
//
//
//
//
//
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//
//}
//
//

package com.theOldMen.Activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.theOldMen.service.IConnectionStatusCallback;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.T;

import org.jivesoftware.smackx.packet.VCard;


public class MainActivity2 extends Activity implements IConnectionStatusCallback {
    public static final int FILE_SELECT_CODE = 1;
    public static String thePath = "";
    private Button mButton;

    private XXService mXxService;

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(MainActivity2.this);
            // 开始连接xmpp服务器
            if (!mXxService.isAuthenticated()) {

                String usr = PreferenceUtils.getPrefString(MainActivity2.this,
                        PreferenceConstants.ACCOUNT, "");

                String password = PreferenceUtils.getPrefString(
                        MainActivity2.this, PreferenceConstants.PASSWORD, "");

                mXxService.Login(usr, password);
            } else {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }

    };
    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            //L.i(LoginActivity.class, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            //L.e(LoginActivity.class, "Service wasn't bound!");
        }
    }

    private void bindXMPPService() {
        //L.i(LoginActivity.class, "[SERVICE] Unbind");
        bindService(new Intent(MainActivity2.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(MainActivity2.this, XXService.class));

        setContentView(R.layout.activity_main);
//        showFileChooser();

        init();

    }

    public void init(){


        mButton = (Button) findViewById(R.id.test_login);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                VCard myvcard = mXxService.getVCard();
//                myvcard.setField("Sex","Man");
//                myvcard.setNickName("i'm A");
//                mXxService.saveCard(myvcard);
                String nickName = myvcard.getNickName();
                String sex = myvcard.getField("Sex");
                if(nickName == null)
                    T.showShort(MainActivity2.this,myvcard.toString());
                if(sex == null)
                    T.showShort(MainActivity2.this,"SEX Fail");
                else
                    T.showShort(MainActivity2.this,"Sex,NickName: "+sex+" "+myvcard.getNickName());
                //L.i(myvcard.toString());
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                    thePath = path;

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {

    }

    public static class FileUtils {
        public static String getPath(Context context, Uri uri) {

            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {"_data"};
                Cursor cursor = null;

                try {
                    cursor = context.getContentResolver().query(uri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    // Eat it
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            return null;
        }
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


    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindXMPPService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}