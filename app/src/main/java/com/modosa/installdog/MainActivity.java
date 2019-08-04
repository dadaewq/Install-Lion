package com.modosa.installdog;

import android.Manifest;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.catchingnow.delegatedscopeclient.DSMClient;

import java.util.List;

public class MainActivity extends AppCompatActivity {
//    private static Activity sInstance = null;
    private int permission = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        sInstance = this;
        findViewById(R.id.request).setOnClickListener(view -> DSMClient.requestScopes(this, "dsm-delegation-install-uninstall-app"));
        requestPermission();
    }

    @Override
    protected void onResume() {
        printStatus();
        updatePermissionState();
        super.onResume();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                0x233);
    }

    private void updatePermissionState() {
        permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission == -1) {
            requestPermission();
        } else if (permission == 0) {
            printStatus();
        }
    }

    private void printStatus() {
        TextView textView = findViewById(R.id.status);
        StringBuilder stringBuilder = new StringBuilder();
        String str = "\n";
        stringBuilder.append(str);
        stringBuilder.append("已安装的托管主应用包名:\n");
        stringBuilder.append(DSMClient.getOwnerPackageName(this));
        stringBuilder.append(str);
        stringBuilder.append("托管应用SDK版本:\n");
        stringBuilder.append(DSMClient.getOwnerSDKVersion(this));
        stringBuilder.append(str);
        stringBuilder.append("本应用所有已授权的 scopes:\n");
        List<String> scopes = DSMClient.getDelegatedScopes(this);
        for (String scope : scopes) {
            stringBuilder.append(scope);
            stringBuilder.append(str);
        }
        if (!scopes.isEmpty() && permission == 0) {
            stringBuilder.append(str);
            stringBuilder.append("现在可以静默安装啦(*^__^*)");
            stringBuilder.append(str);
        }
        stringBuilder.append(str);
        textView.setText(stringBuilder.toString());
    }

}
