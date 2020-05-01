package com.modosa.apkinstaller.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.drakeet.about.AbsAboutActivity;
import com.drakeet.about.Card;
import com.drakeet.about.Category;
import com.drakeet.about.Contributor;
import com.drakeet.about.License;
import com.modosa.apkinstaller.BuildConfig;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.OpUtil;

import java.util.List;

/**
 * @author dadaewq
 */
public class AboutActivity extends AbsAboutActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.rate) {
            OpUtil.launchCustomTabsUrl(this, "https://www.coolapk.com/apk/com.modosa.apkinstaller");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreateHeader(@NonNull ImageView icon, @NonNull TextView slogan, @NonNull TextView version) {
        icon.setImageResource(R.mipmap.ic_launcher);
        slogan.setText(getString(R.string.app_name));
        version.setText("v" + BuildConfig.VERSION_NAME + "（" + BuildConfig.VERSION_CODE + "）");
    }

    @Override
    protected void onItemsCreated(@NonNull List<Object> items) {
        items.add(new Category(getString(R.string.title_Introduction)));
        items.add(new Card(getString(R.string.desc_Introduction)));

        items.add(new Category(getString(R.string.title_developer)));
        items.add(new Contributor(R.drawable.avatar_dadaewq_circle_middle, "dadaewq", getString(R.string.desc_developer), "http://www.coolapk.com/u/460110"));

        items.add(new Category(getString(R.string.title_other_works)));
        items.add(new Contributor(R.mipmap.ic_dark_mode_switch, getString(R.string.name_dark_mode_switch), getString(R.string.desc_dark_mode_switch), "https://www.coolapk.com/apk/com.modosa.switchnightui"));


        items.add(new Category(getString(R.string.title_licenses)));
        items.add(new License("Install-Lion", "dadaewq", "", "https://github.com/dadaewq/Install-Lion"));
        items.add(new License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"));
        items.add(new License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"));
        items.add(new License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"));
        items.add(new License("PokeInstaller", "bavelee", "", "https://github.com/bavelee/PokeInstaller"));
        items.add(new License("RootInstaller", "Bave", License.GPL_V3, "https://gitee.com/Bave/RootInstaller"));
        items.add(new License("IceBox-SDK", "heruoxin", "", "https://github.com/heruoxin/IceBox-SDK"));
        items.add(new License("Delegated-Scopes-Manager", "heruoxin", "WTFPL", "https://github.com/heruoxin/Delegated-Scopes-Manager"));
        items.add(new License("Shizuku", "Rikka", "", "https://github.com/RikkaApps/Shizuku"));
        items.add(new License("FreezeYou", "Playhi", License.APACHE_2, "https://github.com/Playhi/FreezeYou"));
        items.add(new License("SAI", "Aefyr", License.GPL_V3, "https://github.com/Aefyr/SAI"));
        items.add(new License("AndroidFilePicker", "rosuH", License.MIT, "https://github.com/rosuH/AndroidFilePicker"));
        items.add(new License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"));
        items.add(new License("RxJava", "ReactiveX", License.APACHE_2, "https://github.com/ReactiveX/RxJava"));
    }
}
