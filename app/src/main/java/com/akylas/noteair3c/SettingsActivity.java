package com.akylas.noteair3c;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.akylas.noteair3c.lsposed.R;

public class SettingsActivity extends AppCompatActivity {
    public static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }


//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        switch (id) {
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @SuppressLint("WorldReadableFiles")
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.prefs);

        }

    }
}
