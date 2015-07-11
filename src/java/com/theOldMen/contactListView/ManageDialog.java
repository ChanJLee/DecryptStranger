package com.theOldMen.contactListView;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import com.theOldMen.Activity.R;

/**
 * Created by cheng on 2015/4/21.
 */
public class ManageDialog extends AlertDialog {

    public ManageDialog(Context context, int theme) {
        super(context, theme);
    }

    public ManageDialog(Context context) {
        super(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_managedialog);
    }
}
