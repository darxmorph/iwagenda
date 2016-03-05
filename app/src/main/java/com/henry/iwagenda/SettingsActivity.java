package com.henry.iwagenda;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private UserResources ur = new UserResources(this);
    private BroadcastReceiver themeChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        paintUI();
    }
    @Override
    protected void onDestroy() {
        unregisterReceiver(themeChangeReceiver);
        super.onDestroy();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        if (header.id == R.id.credits) {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.pref_credits)
                    .setMessage(R.string.credits)
                    .setIcon(R.drawable.ic_account)
                    .show();
        }
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferencesFragment.class.getName().equals(fragmentName)
                || ThemePreferencesFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName("general");
            prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            Preference agendas = findPreference("changeAgendas");
            agendas.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.are_you_sure)
                            .setMessage(R.string.change_agendas_description)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    SharedPreferences agendaPref = getActivity().getSharedPreferences("agendas", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor agendaEdit = agendaPref.edit();
                                    if (agendaPref.contains("selectedAgendas")) {
                                        agendaEdit.remove("selectedAgendas");
                                        agendaEdit.commit();
                                    }
                                    Intent i = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                    return true;
                }
            });

            Preference logout = findPreference("logout");
            logout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.are_you_sure)
                            .setMessage(R.string.log_out_reset)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String[] sharedPreferencesToRemove = {"auth", "general", "offline", "agendas"};
                                    for (String sharedPrefName : sharedPreferencesToRemove) {
                                        SharedPreferences sharedPref = getActivity().getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
                                        SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                                        sharedPrefEdit.clear();
                                        sharedPrefEdit.commit();
                                    }

                                    Intent i = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                    return true;
                }
            });

            Preference calendarMonths = findPreference("calendarMonths");
            Preference.OnPreferenceChangeListener onCalendarMonthsChange = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent intent = new Intent();
                    intent.setAction("CALENDAR_MONTHS_CHANGE");
                    getActivity().sendBroadcast(intent);
                    return true;
                }
            };
            calendarMonths.setOnPreferenceChangeListener(onCalendarMonthsChange);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ThemePreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName("theme");
            prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
            addPreferencesFromResource(R.xml.pref_theme);
            setHasOptionsMenu(true);

            Preference colorPrimary = findPreference("colorPrimary");
            Preference colorAccent = findPreference("colorAccent");
            Preference colorFuture = findPreference("colorFuture");
            Preference colorPast = findPreference("colorPast");

            Preference.OnPreferenceChangeListener onThemeChange = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent intent = new Intent();
                    intent.setAction("THEME_CHANGE");
                    getActivity().sendBroadcast(intent);
                    return true;
                }
            };

            Preference.OnPreferenceChangeListener onCalendarColorChange = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent intent = new Intent();
                    intent.setAction("CALENDAR_COLOR_CHANGE");
                    getActivity().sendBroadcast(intent);
                    return true;
                }
            };

            colorPrimary.setOnPreferenceChangeListener(onThemeChange);
            colorAccent.setOnPreferenceChangeListener(onThemeChange);
            colorFuture.setOnPreferenceChangeListener(onCalendarColorChange);
            colorPast.setOnPreferenceChangeListener(onCalendarColorChange);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    private void paintUI() {
        tintUI();

        // Receive theme change events
        IntentFilter filter = new IntentFilter();
        filter.addAction("THEME_CHANGE");

        themeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tintUI();
            }
        };

        registerReceiver(themeChangeReceiver, filter);
    }
    private void tintUI() {
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(ur.getColorPrimary()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ur.getColorPrimaryDark());
            // window.setNavigationBarColor(ur.getColorPrimaryDark());
        }
    }
}
