package com.koenidv.camtools;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.michaelbel.bottomsheet.BottomSheet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = "Settings";
    private Snackbar mTimerSnackbar;
    private TextView mSnackBarText;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent); // or whatever method used to update your GUI fields
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = SettingsActivity.this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();


        final LinearLayout mDarkmodeLayout = findViewById(R.id.darkmodeLayout);
        final Button mDarkmodeButton = findViewById(R.id.settingsDarkmodeButton);
        final Switch mDarkmodeSystemSwitch = findViewById(R.id.followSystemSwitch);
        final Button mCamerasButton = findViewById(R.id.settingsCamerasButton);
        final TextView mApertureTextView = findViewById(R.id.settingsUnitsApertureTitle);
        final Button mApertureButton = findViewById(R.id.settingsUnitsApertureButton);
        final TextView mDistancesTextView = findViewById(R.id.settingsUnitsDistanceTitle);
        final Button mDistancesButton = findViewById(R.id.settingsUnitsDistanceButton);
        final TextView mNdTextView = findViewById(R.id.settingsUnitsNdTitle);
        final Button mNdButton = findViewById(R.id.settingsUnitsNdButton);
        final Button mFeedbackButton = findViewById(R.id.settingsFeedbackButton);

        (new ModuleManager()).checkDarkmode(prefs);


        if (prefs.getBoolean("darkmode", false)) {
            mDarkmodeButton.setText(getString(R.string.setting_disable));
        }

        if (!prefs.getBoolean("used_darkmode", false)) {
            mDarkmodeLayout.setBackgroundColor(getResources().getColor(R.color.settings_darkmodeBackground));
            ((TextView) findViewById(R.id.settingsDarkmodeTitle)).setTextColor(getResources().getColor(R.color.background_light_lighter));
            ((TextView) findViewById(R.id.settingsDarkmodeDescription)).setTextColor(getResources().getColor(R.color.background_light_normal));
            mDarkmodeSystemSwitch.setTextColor(Color.WHITE);
        }

        if (prefs.getBoolean("darkmode_follow_system", false)) {
            mDarkmodeSystemSwitch.setChecked(true);
            mDarkmodeButton.setEnabled(false);
            mDarkmodeButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.gray), PorterDuff.Mode.MULTIPLY);
        }

        String apertureText = getString(R.string.setting_units_aperture_short) + ": ";
        switch (prefs.getInt("aperture_stops", 2)) {
            case 2:
                apertureText += getString(R.string.setting_units_aperture_full_short);
                break;
            case 4:
                apertureText += getString(R.string.setting_units_aperture_half_short);
                break;
            case 6:
                apertureText += getString(R.string.setting_units_aperture_third_short);
        }
        mApertureTextView.setText(apertureText);

        String distanceText = getString(R.string.setting_units_distance) + ": ";
        if (prefs.getBoolean("empirical", false)) {
            distanceText += getString(R.string.setting_units_distance_empirical);
        } else {
            distanceText += getString(R.string.setting_units_distance_metrical);
        }
        mDistancesTextView.setText(distanceText);

        String ndText = getString(R.string.setting_units_nd) + ": ";
        if (prefs.getBoolean("ndstops", false)) {
            ndText += getString(R.string.setting_units_nd_stops);
        } else {
            ndText += getString(R.string.setting_units_nd_times);
        }
        mNdTextView.setText(ndText);

        /*
         * Listeners
         */

        mDarkmodeButton.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                prefsEdit.putBoolean("darkmode", false).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                prefsEdit.putBoolean("darkmode", true).apply();
                if (!prefs.getBoolean("used_darkmode", false)) {
                    prefsEdit.putBoolean("used_darkmode", true).apply();
                }
            }
            prefsEdit.putBoolean("theme_changed", true).apply();
            SettingsActivity.this.recreate();
        });

        mDarkmodeSystemSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsEdit.putBoolean("darkmode_follow_system", isChecked).apply();
            mDarkmodeButton.setEnabled(!isChecked);
            if (isChecked) {
                mDarkmodeButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.gray), PorterDuff.Mode.MULTIPLY);
            } else {
                mDarkmodeButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), PorterDuff.Mode.MULTIPLY);
            }
        });

        View.OnClickListener camerasClickListener = v -> startActivity(new Intent(SettingsActivity.this, EditCamerasActivity.class));
        mCamerasButton.setOnClickListener(camerasClickListener);
        findViewById(R.id.settingsCamerasCard).setOnClickListener(camerasClickListener);

        mApertureButton.setOnClickListener(v -> {
            int[] items = new int[]{
                    R.string.setting_units_aperture_full,
                    R.string.setting_units_aperture_half,
                    R.string.setting_units_aperture_third
            };
            BottomSheet.Builder mBuilder = new BottomSheet.Builder(SettingsActivity.this);
            mBuilder.setTitle(getString(R.string.setting_units_aperture))
                    .setItems(items, (dialog, which) -> {
                        //Full (0) -> 2, Half (1) -> 4, Third (2) -> 6
                        prefsEdit.putInt("aperture_stops", (which + 1) * 2).apply();
                        String apertureText1 = getString(R.string.setting_units_aperture_short) + ": ";
                        switch (which) {
                            case 0:
                                apertureText1 += getString(R.string.setting_units_aperture_full_short);
                                break;
                            case 1:
                                apertureText1 += getString(R.string.setting_units_aperture_half_short);
                                break;
                            case 2:
                                apertureText1 += getString(R.string.setting_units_aperture_third_short);
                        }
                        mApertureTextView.setText(apertureText1);
                    })
                    .setDarkTheme(getResources().getBoolean(R.bool.darkmode))
                    .show();
        });

        mDistancesButton.setOnClickListener(v -> {
            int[] items = new int[]{
                    R.string.setting_units_distance_metrical,
                    R.string.setting_units_distance_empirical
            };
            BottomSheet.Builder mBuilder = new BottomSheet.Builder(SettingsActivity.this);
            mBuilder.setTitle(getString(R.string.setting_units_distance))
                    .setItems(items, (dialog, which) -> {
                        prefsEdit.putBoolean("empirical", which != 0).apply();
                        String distanceText1 = getString(R.string.setting_units_distance) + ": ";
                        if (which == 1) {
                            distanceText1 += getString(R.string.setting_units_distance_empirical);
                        } else {
                            distanceText1 += getString(R.string.setting_units_distance_metrical);
                        }
                        mDistancesTextView.setText(distanceText1);
                    })
                    .setDarkTheme(getResources().getBoolean(R.bool.darkmode))
                    .show();
        });

        mNdButton.setOnClickListener(v -> {
            int[] items = new int[]{
                    R.string.setting_units_nd_stops,
                    R.string.setting_units_nd_times
            };
            BottomSheet.Builder mBuilder = new BottomSheet.Builder(SettingsActivity.this);
            mBuilder.setTitle(getString(R.string.setting_units_nd))
                    .setItems(items, (dialog, which) -> {
                        prefsEdit.putBoolean("ndstops", which == 0).apply();
                        String ndText1 = getString(R.string.setting_units_nd) + ": ";
                        if (which == 1) {
                            ndText1 += getString(R.string.setting_units_nd_times);
                        } else {
                            ndText1 += getString(R.string.setting_units_nd_stops);
                        }
                        mNdTextView.setText(ndText1);
                    })
                    .setDarkTheme(getResources().getBoolean(R.bool.darkmode))
                    .show();
        });

        View.OnClickListener feedbackClickListener = v -> {
            final String appPackageName = SettingsActivity.this.getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        };
        mFeedbackButton.setOnClickListener(feedbackClickListener);
        findViewById(R.id.settingsFeedbackCard).setOnClickListener(feedbackClickListener);

    }

    @Override
    public void onStop() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            // Receiver was probably already stopped in onPause()
        }
        super.onStop();
    }

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            ModuleManager mModuleManager = new ModuleManager();
            long millisUntilFinished = intent.getLongExtra("remaining", 0);
            int maxTime = intent.getIntExtra("max", 0);
            String name = intent.getStringExtra("name");

            if (intent.getBooleanExtra("dismiss", false)) {
                mTimerSnackbar.dismiss();
            } else if (millisUntilFinished == 0) {
                mTimerSnackbar.dismiss();
                Snackbar.make(findViewById(R.id.rootView), String.format(getString(R.string.timer_finished), name), Snackbar.LENGTH_LONG)
                        .setAction(R.string.okay, v -> mTimerSnackbar.dismiss())
                        .show();
            } else {
                if (mTimerSnackbar == null || !mTimerSnackbar.isShown()) {
                    mTimerSnackbar = Snackbar.make(findViewById(R.id.rootView),
                            String.format(getString(R.string.timer_text), name, mModuleManager.convertMilliseconds(millisUntilFinished)),
                            Snackbar.LENGTH_INDEFINITE);
                    mTimerSnackbar
                            .setAction(R.string.show, v -> {
                                TimerSheet sheet = new TimerSheet();
                                sheet.startTime = maxTime / 1000;
                                sheet.tagName = name;
                                sheet.show(getSupportFragmentManager(), "timer");
                            })
                            .setBehavior(new BaseTransientBottomBar.Behavior() {
                                @Override
                                public boolean canSwipeDismissView(View child) {
                                    return false;
                                }
                            })
                            .show();
                    mSnackBarText = mTimerSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                } else {
                    mSnackBarText.setText(String.format(getString(R.string.timer_text), name, mModuleManager.convertMilliseconds(millisUntilFinished)));
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = SettingsActivity.this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        if (prefs.getBoolean("theme_changed", false)) {
            prefs.edit().putBoolean("theme_changed", false).apply();
            startActivity(new Intent(SettingsActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.i(TAG, "Unregistered broacast receiver");
    }

    @Override
    public void onResume() {
        super.onResume();
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = SettingsActivity.this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        ((TextView) findViewById(R.id.settingsCamerasDescription)).setText(getString(R.string.setting_cameras_description).replace("%s",
                getResources().getQuantityString(R.plurals.cameras, prefs.getInt("cameras_amount", -1) + 1, prefs.getInt("cameras_amount", -1) + 1)));

        registerReceiver(receiver, new IntentFilter(TimerService.ACTION));
        Log.i(TAG, "Registered broacast receiver");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = SettingsActivity.this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        switch (id) {
            case R.id.action_delete_history:
                prefsEdit.remove("history").apply();
                break;
            case R.id.action_reset:
                prefsEdit.clear().apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case R.id.action_help:
                //startActivity(new Intent(MainActivity.this, FragmentActivity.class));
                break;
            case R.id.action_libraries:
                new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .start(this);
        }

        return super.onOptionsItemSelected(item);
    }
}
