package com.own.alessio.p2pandon;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class UserSettingActivity extends PreferenceActivity {
    private static int prefs=R.xml.preferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getClass().getMethod("getFragmentManager");
            AddResourceApi11AndGreater();
        } catch (NoSuchMethodException e) {
            AddResourceApiLessThan11();
        }
    }

    // preferences for older versions
    @SuppressWarnings("deprecation")
    protected void AddResourceApiLessThan11() {
        addPreferencesFromResource(R.xml.preferences);
    }

    // preferences for AP11 or newer versions
    @TargetApi(11)
    protected void AddResourceApi11AndGreater() {
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PF()).commit();
    }

    @TargetApi(11)
    public static class PF extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(UserSettingActivity.prefs);
        }
    }

}
