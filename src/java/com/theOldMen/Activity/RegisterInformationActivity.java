package com.theOldMen.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Created by jz on 2015/5/8.
 */
public class RegisterInformationActivity extends Activity {

    private EditText editNickName;
    private Spinner sexSpinner;
    private Button getInformationButton;
    private static String[] sexInfo={"男","女"};

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_information);
        findView();
    }

    public void findView(){
        this.editNickName = (EditText) findViewById(R.id.nickname);
        this.sexSpinner = (Spinner) findViewById(R.id.SpinnerSex);
        this.getInformationButton = (Button) findViewById(R.id.getNickInformation);
        this.getInformationButton.setOnClickListener(new OnClickListenerImpl());
    }

    private class OnClickListenerImpl implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String sexInfo = sexSpinner.getSelectedItem().toString();
            Toast.makeText(RegisterInformationActivity.this, "SEX:"+sexInfo+";NICKNAME:"+editNickName.getText().toString(), Toast.LENGTH_SHORT)
                    .show();
        }

    }

}


