package com.akylas.noteair3c;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;

import com.akylas.noteair3c.lsposed.R;

public class SettingsActivity extends AppCompatActivity {
    public static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @SuppressLint("WorldReadableFiles")
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.prefs);

        }

    }
}
