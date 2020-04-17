package com.modosa.apkinstaller.activity;


import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.fragment.SettingsFragment;


/**
 * @author dadaewq
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG_SETTINGS = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_ui);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction().replace(R.id.framelayout, new SettingsFragment(), TAG_SETTINGS).commit();

    }

    private void showMyToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    private void showMyToast0(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
