package com.modosa.apkinstaller.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.OpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ManageAllowSourceActivity extends AppCompatActivity {

    public static final String SP_KEY_ALLOWSOURCE = "allowsource";
    private SharedPreferences spAllowSource;
    private ListView uaamListView;
    private AlertDialog alertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_allowsource);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        spAllowSource = getSharedPreferences(SP_KEY_ALLOWSOURCE, Context.MODE_PRIVATE);
        uaamListView = findViewById(R.id.uaam_listView);
        refreshList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_clear_allowource, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        refreshList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.clearAllowedList) {
            showDialogClearAllowedList();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    private void refreshList() {

        final String allowsourceString = spAllowSource.getString(SP_KEY_ALLOWSOURCE, "");

        if ("".equals(allowsourceString)) {
            List<StringHashMap> pkgList = new ArrayList<>();
            StringHashMap hashMap = new StringHashMap();
            hashMap.put("Lable", getString(R.string.empty));
            hashMap.put("PkgName", "");
            pkgList.add(hashMap);
            final SimpleAdapter adapter = new SimpleAdapter(this, pkgList,
                    R.layout.manage_allowsource_view, new String[]{"Lable", "PkgName"}, new int[]{R.id.uaaml_name, R.id.uaaml_pkgName});
            uaamListView.setAdapter(adapter);
            uaamListView.setOnItemClickListener(null);
        } else {
            List<StringHashMap> pkgList = new ArrayList<>();
            final String[] strings = allowsourceString.split(",");

            for (String aString : strings) {

                if (!"".equals(aString)) {
                    StringHashMap hashMap = new StringHashMap();
                    hashMap.put("Lable",
                            AppInfoUtil.getApplicationLabel(
                                    this, aString));
                    hashMap.put("PkgName", aString);
                    pkgList.add(hashMap);
                }
            }
            final SimpleAdapter adapter = new SimpleAdapter(this, pkgList,
                    R.layout.manage_allowsource_view, new String[]{"Lable", "PkgName"}, new int[]{R.id.uaaml_name, R.id.uaaml_pkgName});
            uaamListView.setAdapter(adapter);
            Context context = this;
            uaamListView.setOnItemClickListener((parent, view, position, id) -> {
                StringHashMap hashMap = (StringHashMap) adapter.getItem(position);
                final String pkgName = hashMap.get("PkgName");
                if (pkgName != null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                            .setTitle(R.string.message_confirm_remove_list)
                            .setMessage(hashMap.get("Lable") + System.getProperty("line.separator") + pkgName)
                            .setNeutralButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                List<String> ls = OpUtil.convertToList(strings);
                                ls.remove(pkgName);
                                spAllowSource.edit().putString(SP_KEY_ALLOWSOURCE, OpUtil.listToString(ls, ",")).apply();
                                refreshList();
                            });

                    alertDialog = builder.create();
                    OpUtil.showAlertDialog(context, alertDialog);

                }
            });
        }
    }

    private void showDialogClearAllowedList() {

        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);

        checkBox.setText(R.string.checkbox_clearAllowedList);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_clearAllowedList)
                .setMessage(R.string.message_clearAllowedList)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    getSharedPreferences(SP_KEY_ALLOWSOURCE, Context.MODE_PRIVATE).edit().clear().apply();
                    refreshList();
                });


        alertDialog = builder.create();
        OpUtil.showAlertDialog(this, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private static class StringHashMap extends HashMap<String, String> {
    }
}

