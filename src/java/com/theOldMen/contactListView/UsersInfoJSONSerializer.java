package com.theOldMen.contactListView;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by pandora-delusion on 2015/4/18.
 */

/**
 * 该类尚未进行测试，慎用
 * */
public class UsersInfoJSONSerializer {

    private Context m_context;
    private String m_filename;

    public UsersInfoJSONSerializer(Context c, String f) {
        m_context = c;
        m_filename = f;
    }
    /*
    * 以分组的形式存储用户信息*/
    public void saveUsers(ArrayList<UsersGroup> usersGroups)
            throws JSONException, IOException{
        JSONArray jsonArray = new JSONArray();
        for (UsersGroup usersGroup : usersGroups) {
            jsonArray.put(usersGroup.toJSON());
        }

        Writer writer = null;

        // 异常处理交由调用该模块的代码处理
        try {
            OutputStream outputStream = m_context.openFileOutput(m_filename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(outputStream);
            writer.write(jsonArray.toString());
            Log.d("JSON", jsonArray.toString());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public ArrayList<UsersGroup> loadUsers() throws JSONException, IOException {
        BufferedReader reader = null;
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList<UsersGroup> usersGroups = new ArrayList<UsersGroup>();
        try {
            InputStream inputStream = m_context.openFileInput(m_filename);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            JSONArray jsonArray = (JSONArray)new JSONTokener(stringBuffer.toString()).nextValue();
            for (int i = 0; i < jsonArray.length(); ++ i) {
                usersGroups.add(new UsersGroup(jsonArray.getJSONObject(i)));
            }
        } finally {
            if (reader != null)
                reader.close();
        }
        return usersGroups;
    }
}
