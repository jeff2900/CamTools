package com.koenidv.camtools;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import org.michaelbel.bottomsheet.BottomSheet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.recyclerview.widget.RecyclerView;

//  Created by koenidv on 30.11.2018.
class ModuleManager {

    /*
     *
     *  UI
     *
     */

    /**
     * Display a bottom sheet to let the user select a camera
     *
     * @param mContext        The context to run in.
     * @param mCameraTextView The TextView which displays the currently selected camera.
     * @param mValue          The list in which either the current cicle of confusion or pixel pitch is saved.
     * @param mType           Defines what should be saved to {@param mValue}.
     *                        Should be "coc" for circle of confusion or "pixelpitch" for pixel pitch.
     * @param mDefault        The default value to save to {@param mValue} in case nothing is stored in {@param prefs}.
     *                        Required by {@link SharedPreferences}.
     */
    void selectCamera(final Context mContext, final TextView mCameraTextView, final float[] mValue, final String mType, final float mDefault) {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        if (prefs.getInt("cameras_amount", 0) == 0) {
            mContext.startActivity(new Intent(mContext, EditCamerasActivity.class));
        } else {
            final List<String> cameras = new ArrayList<>();
            for (int camera = 0; camera <= prefs.getInt("cameras_amount", 0); camera++) {
                cameras.add(prefs.getString("camera_" + camera + "_name", mContext.getString(R.string.camera_default_name)));
            }
            cameras.add(mContext.getString(R.string.calculate_camera_manage));
            ArrayList<Integer> icons = new ArrayList<>();
            for (int icon = 0; icon < cameras.size() - 1; icon++) {
                icons.add(R.drawable.ic_camera);
            }
            icons.add(R.drawable.ic_settings);
            int[] iconlist = new int[icons.size()];
            Iterator<Integer> iterator = icons.iterator();
            for (int i = 0; i < iconlist.length; i++) {
                iconlist[i] = iterator.next();
            }

            BottomSheet.Builder mBuilder = new BottomSheet.Builder(mContext);
            mBuilder.setTitle(mContext.getString(R.string.calculate_camera_choose))
                    .setItems(cameras.toArray(new String[0]), iconlist, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == cameras.size() - 1) {
                                mContext.startActivity(new Intent(mContext, EditCamerasActivity.class));
                            } else {
                                prefsEdit.putInt("cameras_last", which).apply();
                                mValue[0] = prefs.getFloat("camera_" + which + "_" + mType, mDefault);
                                mCameraTextView.setText(mContext.getString(R.string.calculate_camera).replace("%s", prefs.getString("camera_" + which + "_name", mContext.getString(R.string.camera_default_name))));
                            }
                        }
                    })
                    .setDarkTheme(prefs.getBoolean("system_darkmode", false))
                    .show();
        }
    }

    /**
     * Show a bottom sheet with equations
     *
     * @param mContext The context to run in.
     * @param mName    The name of the equation as saved in strings.xml
     */
    void showEquations(Context mContext, String mName) {
        final BottomSheetDialog mDialog = new BottomSheetDialog(mContext);
        mDialog.setContentView(R.layout.sheet_equation);

        int mathId = mContext.getResources().getIdentifier("equation_" + mName, "string", mContext.getPackageName());
        int descrId = mContext.getResources().getIdentifier("equation_" + mName + "_description", "string", mContext.getPackageName());

        io.github.kexanie.library.MathView mMathView = mDialog.findViewById(R.id.math_view);
        TextView mDescriptionTextView = mDialog.findViewById(R.id.descriptionTextView);
        mMathView.setText(mContext.getString(mathId));
        mDescriptionTextView.setText(descrId);

        mDialog.show();
    }

    /**
     * Show a bottom sheet which lets the user manipulate a camera
     *
     * @param mContext  The context to run in.
     * @param mIndex    The position in {@param mCardList}.
     *                  Should be one larger than the size of {@param mCardList} to create a new camera
     *                  or withing its size to edit an existing camera.
     * @param mCardList A list in which all {@link cameraCard} are stored to display them in the RecyclerView.
     * @param mAdapter  The RecyclerView's adapter.
     * @see #editCameraCheckAllFilled(Context, Dialog)
     * @see EditCamerasActivity
     */
    void editCamera(final Context mContext, final int mIndex, final List<cameraCard> mCardList, final RecyclerView.Adapter mAdapter) {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        final BottomSheetDialog mDialog = new BottomSheetDialog(mContext);
        mDialog.setContentView(R.layout.dialog_camera_add_custom);
        Objects.requireNonNull(mDialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        FrameLayout bottomSheet = mDialog.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);

        TextView mTitleTextView = mDialog.findViewById(R.id.titleTextView);
        final EditText mNameEditText = mDialog.findViewById(R.id.nameEditText);
        TextView mResolutionPresetTextView = mDialog.findViewById(R.id.resolutionPresetTextView);
        final EditText mResolutionXEditText = mDialog.findViewById(R.id.resolutionXEditText);
        final EditText mResolutionYEditText = mDialog.findViewById(R.id.resolutionYEditText);
        TextView mSizePresetTextView = mDialog.findViewById(R.id.sensorsizePresetTextView);
        final EditText mSizeXEditText = mDialog.findViewById(R.id.sensorsizeXEditText);
        final EditText mSizeYEditText = mDialog.findViewById(R.id.sensorsizeYEditText);
        final EditText mConfusionEditText = mDialog.findViewById(R.id.cocEditText);
        final Button mCancelButton = mDialog.findViewById(R.id.cancelButton);
        final Button mSaveButton = mDialog.findViewById(R.id.saveButton);

        final String prefix = "camera_" + String.valueOf(mIndex) + "_";

        if (!prefs.getString(prefix + "name", "").equals("")) {
            mTitleTextView.setText(R.string.setting_cameras_edit_custom_title);
            mNameEditText.setText(prefs.getString(prefix + "name", ""));
            mResolutionXEditText.setText(String.valueOf(prefs.getInt(prefix + "resolution_x", 0)));
            mResolutionYEditText.setText(String.valueOf(prefs.getInt(prefix + "resolution_y", 0)));
            mSizeXEditText.setText(String.valueOf(prefs.getFloat(prefix + "sensorsize_x", 0)));
            mSizeYEditText.setText(String.valueOf(prefs.getFloat(prefix + "sensorsize_y", 0)));
            mConfusionEditText.setText(String.valueOf(prefs.getFloat(prefix + "coc", 0)));
        } else {
            mSaveButton.setEnabled(false);
            mSaveButton.getBackground().setAlpha(0);
            mSaveButton.setTextColor(mContext.getResources().getColor(R.color.gray));
        }


        TextWatcher checkOnTextChanged = new TextWatcher() {
            //f:off
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            //f:on
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editCameraCheckAllFilled(mContext, mDialog);
            }
        };

        mNameEditText.addTextChangedListener(checkOnTextChanged);
        mSizeXEditText.addTextChangedListener(checkOnTextChanged);
        mSizeYEditText.addTextChangedListener(checkOnTextChanged);
        mResolutionXEditText.addTextChangedListener(checkOnTextChanged);
        mResolutionYEditText.addTextChangedListener(checkOnTextChanged);
        mConfusionEditText.addTextChangedListener(checkOnTextChanged);

        mResolutionPresetTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> presets = new ArrayList<>();
                for (int preset = 0; preset <= mContext.getResources().getInteger(R.integer.preset_res_amount); preset++) {
                    int resId = mContext.getResources().getIdentifier("preset_res_" + String.valueOf(preset), "string", mContext.getPackageName());
                    presets.add(mContext.getString(resId));
                }

                BottomSheet.Builder mBuilder = new BottomSheet.Builder(mContext);
                mBuilder.setTitle(mContext.getString(R.string.preset_choose))
                        .setItems(presets.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int resXid = mContext.getResources().getIdentifier("preset_res_" + String.valueOf(which) + "_x", "string", mContext.getPackageName());
                                int resYid = mContext.getResources().getIdentifier("preset_res_" + String.valueOf(which) + "_y", "string", mContext.getPackageName());

                                mResolutionXEditText.setText(mContext.getString(resXid));
                                mResolutionYEditText.setText(mContext.getString(resYid));
                            }
                        })
                        .setDarkTheme(prefs.getBoolean("system_darkmode", false))
                        .show();
            }
        });

        mSizePresetTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> presets = new ArrayList<>();
                for (int preset = 0; preset <= mContext.getResources().getInteger(R.integer.preset_size_amount); preset++) {
                    int resId = mContext.getResources().getIdentifier("preset_size_" + String.valueOf(preset), "string", mContext.getPackageName());
                    presets.add(mContext.getString(resId));
                }

                BottomSheet.Builder mBuilder = new BottomSheet.Builder(mContext);
                mBuilder.setTitle(mContext.getString(R.string.preset_choose))
                        .setItems(presets.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int sizeXid = mContext.getResources().getIdentifier("preset_size_" + String.valueOf(which) + "_x", "string", mContext.getPackageName());
                                int sizeYid = mContext.getResources().getIdentifier("preset_size_" + String.valueOf(which) + "_y", "string", mContext.getPackageName());
                                int cocid = mContext.getResources().getIdentifier("preset_size_" + String.valueOf(which) + "_coc", "string", mContext.getPackageName());

                                mSizeXEditText.setText(mContext.getString(sizeXid));
                                mSizeYEditText.setText(mContext.getString(sizeYid));
                                mConfusionEditText.setText(mContext.getString(cocid));
                            }
                        })
                        .setDarkTheme(prefs.getBoolean("system_darkmode", false))
                        .show();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float pixelpitch = Float.valueOf(mSizeXEditText.getText().toString()) / Float.valueOf(mResolutionXEditText.getText().toString()) * 1000;
                prefsEdit.putString(prefix + "name", mNameEditText.getText().toString())
                        .putInt(prefix + "resolution_x", Integer.valueOf(mResolutionXEditText.getText().toString()))
                        .putInt(prefix + "resolution_y", Integer.valueOf(mResolutionYEditText.getText().toString()))
                        .putFloat(prefix + "sensorsize_x", Float.valueOf(mSizeXEditText.getText().toString()))
                        .putFloat(prefix + "sensorsize_y", Float.valueOf(mSizeYEditText.getText().toString()))
                        .putFloat(prefix + "coc", Float.valueOf(mConfusionEditText.getText().toString()))
                        .putFloat(prefix + "pixelpitch", pixelpitch)
                        .apply();

                cameraCard mCameraCard = new cameraCard(
                        mNameEditText.getText().toString(),
                        mContext.getString(R.string.sensorsize_indicator) + mSizeXEditText.getText().toString() + "x" + mSizeYEditText.getText().toString() + mContext.getString(R.string.millimeter),
                        mContext.getString(R.string.resolution_indicator) + String.valueOf(Integer.valueOf(mResolutionXEditText.getText().toString()) * Integer.valueOf(mResolutionYEditText.getText().toString()) / 1000000) + mContext.getString(R.string.megapixel),
                        mContext.getString(R.string.pixelpitch_indicator) + String.valueOf(pixelpitch) + String.valueOf(R.string.micrometer),
                        mContext.getString(R.string.coc_indicator) + mConfusionEditText.getText().toString() + mContext.getString(R.string.millimeter),
                        (prefs.getInt("cameras_last", -1) == mIndex));

                if (mIndex > prefs.getInt("cameras_amount", 0)) {
                    mCardList.add(mCameraCard);
                    prefsEdit.putInt("cameras_amount", mIndex).apply();
                } else {
                    mCardList.set(mIndex, mCameraCard);
                }
                mAdapter.notifyDataSetChanged();

                mDialog.dismiss();
            }
        });

        mDialog.show();
    }

    /**
     * Delete a camera
     * Show a bottom sheet which lets the user manipulate a camera
     *
     * @param mContext  The context to run in.
     * @param mIndex    The position in {@param mCardList}.
     * @param mCardList A list in which all {@link cameraCard} are stored to display them in the RecyclerView.
     * @param mAdapter  The RecyclerView's adapter.
     * @see EditCamerasActivity
     */
    void deleteCamera(final Context mContext, final int mIndex, final List<cameraCard> mCardList, final RecyclerView.Adapter mAdapter) {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        for (int move = mIndex + 1; move <= prefs.getInt("cameras_amount", 0); move++) {
            String prefixLast = "camera_" + String.valueOf(move - 1) + "_";
            String prefixThis = "camera_" + move + "_";

            prefsEdit.putString(prefixLast + "name", prefs.getString(prefixThis + "name", ""))
                    .putInt(prefixLast + "resolution_x", prefs.getInt(prefixThis + "resolution_x", 0))
                    .putInt(prefixLast + "resolution_y", prefs.getInt(prefixThis + "resolution_y", 0))
                    .putFloat(prefixLast + "sensorsize_x", prefs.getFloat(prefixThis + "sensorsize_x", 0))
                    .putFloat(prefixLast + "sensorsize_y", prefs.getFloat(prefixThis + "sensorsize_y", 0))
                    .putFloat(prefixLast + "coc", prefs.getFloat(prefixThis + "coc", 0))
                    .putFloat(prefixLast + "pixelpitch", prefs.getFloat(prefixThis + "pixelpitch", 0))
                    .apply();
        }

        String prefix = "camera_" + prefs.getInt("cameras_amount", 0) + "_";
        prefsEdit.remove(prefix + "name")
                .remove(prefix + "resolution_x")
                .remove(prefix + "resolution_y")
                .remove(prefix + "sensorsize_x")
                .remove(prefix + "sensorsize_y")
                .remove(prefix + "coc")
                .remove(prefix + "pixelpitch")
                .apply();

        prefsEdit.putInt("cameras_amount", prefs.getInt("cameras_amount", 0) - 1).apply();
        mCardList.remove(mIndex);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Switches to position of two cameras
     *
     * @param mContext   The context to run in.
     * @param mFromIndex The position of the camera to move.
     * @param mToIndex   The position to move the camera to.
     * @param mCardList  A list in which all {@link cameraCard} are stored to display them in the RecyclerView.
     * @param mAdapter   The RecyclerView's adapter.
     * @see EditCamerasActivity
     */
    void moveCamera(final Context mContext, final int mFromIndex, final int mToIndex, final List<cameraCard> mCardList, final RecyclerView.Adapter mAdapter) {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") final SharedPreferences.Editor prefsEdit = prefs.edit();

        String prefixFrom = "camera_" + mFromIndex + "_";
        String prefixTo = "camera_" + mToIndex + "_";

        prefsEdit.putString(prefixFrom + "name", prefs.getString(prefixTo + "name", ""))
                .putInt(prefixFrom + "resolution_x", prefs.getInt(prefixTo + "resolution_x", 0))
                .putInt(prefixFrom + "resolution_y", prefs.getInt(prefixTo + "resolution_y", 0))
                .putFloat(prefixFrom + "sensorsize_x", prefs.getFloat(prefixTo + "sensorsize_x", 0))
                .putFloat(prefixFrom + "sensorsize_y", prefs.getFloat(prefixTo + "sensorsize_y", 0))
                .putFloat(prefixFrom + "coc", prefs.getFloat(prefixTo + "coc", 0))
                .putFloat(prefixFrom + "pixelpitch", prefs.getFloat(prefixFrom + "pixelpitch", 0))
                .putString(prefixTo + "name", prefs.getString(prefixFrom + "name", ""))
                .putInt(prefixTo + "resolution_x", prefs.getInt(prefixFrom + "resolution_x", 0))
                .putInt(prefixTo + "resolution_y", prefs.getInt(prefixFrom + "resolution_y", 0))
                .putFloat(prefixTo + "sensorsize_x", prefs.getFloat(prefixFrom + "sensorsize_x", 0))
                .putFloat(prefixTo + "sensorsize_y", prefs.getFloat(prefixFrom + "sensorsize_y", 0))
                .putFloat(prefixTo + "coc", prefs.getFloat(prefixFrom + "coc", 0))
                .putFloat(prefixTo + "pixelpitch", prefs.getFloat(prefixFrom + "pixelpitch", 0))
                .apply();

        cameraCard toCard = mCardList.get(mToIndex);
        mCardList.set(mToIndex, mCardList.get(mFromIndex));
        mCardList.set(mFromIndex, toCard);
        mAdapter.notifyDataSetChanged();
    }

    void showHistory(Context mContext) {
        if (Build.VERSION.SDK_INT >= 24) {
            @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
            Gson gson = new Gson();

            ArrayList<String> historyClasses = gson.fromJson(prefs.getString("history", gson.toJson(new ArrayList<String>())), ArrayList.class);
            ArrayList<String> historyNames = new ArrayList<>();
            for (int i = 0; i < historyClasses.size(); i++) {
                // Get activity label for each activity
                String label = historyClasses.get(i);
                try {
                    label = mContext.getResources().getString(
                            mContext.getPackageManager().getActivityInfo(
                                    new ComponentName(mContext.getPackageName(), mContext.getPackageName() + "." + historyClasses.get(i)), 0
                            ).labelRes);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                historyNames.add(label);
            }

            BottomSheet.Builder mBuilder = new BottomSheet.Builder(mContext);
            mBuilder.setItems(historyNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Class<?> c = Class.forName(mContext.getPackageName() + "." + historyClasses.get(which));
                        Intent intent = new Intent(mContext, c);
                        mContext.startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (historyClasses.size() > 0) {
                mBuilder.show();
            }
        }
    }

    /*
     *
     *  Tools
     *
     */

    /**
     * Checks whether all EditTexts in {@param mDialog} are filled
     * and enables / disables its save button
     *
     * @param mContext The context to run in.
     * @param mDialog  The dialog in which to check the views.
     */
    private void editCameraCheckAllFilled(Context mContext, Dialog mDialog) {
        EditText mNameEditText = mDialog.findViewById(R.id.nameEditText);
        EditText mSizeXEditText = mDialog.findViewById(R.id.sensorsizeXEditText);
        EditText mSizeYEditText = mDialog.findViewById(R.id.sensorsizeYEditText);
        EditText mResolutionXEditText = mDialog.findViewById(R.id.resolutionXEditText);
        EditText mResolutionYEditText = mDialog.findViewById(R.id.resolutionYEditText);
        EditText mConfusionEditText = mDialog.findViewById(R.id.cocEditText);
        Button mSaveButton = mDialog.findViewById(R.id.saveButton);

        if (!mNameEditText.getText().toString().isEmpty()
                && !mSizeXEditText.getText().toString().isEmpty()
                && !mSizeYEditText.getText().toString().isEmpty()
                && !mResolutionXEditText.getText().toString().isEmpty()
                && !mResolutionYEditText.getText().toString().isEmpty()
                && !mConfusionEditText.getText().toString().isEmpty()
                && !mConfusionEditText.getText().toString().equals(".")) {
            mSaveButton.setEnabled(true);
            mSaveButton.getBackground().setAlpha(255);
            mSaveButton.setTextColor(Color.WHITE);
        } else {
            mSaveButton.setEnabled(false);
            mSaveButton.getBackground().setAlpha(0);
            mSaveButton.setTextColor(mContext.getResources().getColor(R.color.gray));
        }
    }

    void addToHistory(Context mContext, final String mName) {
        if (Build.VERSION.SDK_INT >= 24) {
            @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
            final SharedPreferences.Editor prefsEdit = prefs.edit();
            Gson gson = new Gson();

            ArrayList<String> currentHistory = new ArrayList<>();
            currentHistory.add("");
            currentHistory.addAll(1, gson.fromJson(prefs.getString("history", gson.toJson(new ArrayList<String>())), ArrayList.class));
            currentHistory.removeIf(element -> element.equals(mName));
            if (currentHistory.size() == 5) {
                currentHistory.remove(4);
            }
            currentHistory.set(0, mName);

            prefsEdit.putString("history", gson.toJson(currentHistory)).apply();
        }
    }

    /**
     * Returns the same number as String with a maximum of two decimals
     *
     * @param mNumber The number to truncate.
     * @return {@param mNumber} as String.
     * @see DecimalFormat
     */
    static String truncateNumber(Float mNumber) {
        return new DecimalFormat("#.##").format(mNumber);
    }

    /**
     * Converts a number of seconds to a Spanned containing hours, minutes, seconds
     *
     * @param mContext    The context to run in.
     * @param mCalculated The number of seconds to calculate with.
     * @return A Spanned formatted as "DD days, HH hours, MM minutes, SS seconds.
     */
    Spanned convertTime(Context mContext, float mCalculated) {
        int days = 0;
        int hours = 0;
        int minutes = 0;

        while (mCalculated > 86400) {
            days++;
            mCalculated -= 86400;
        }
        while (mCalculated > 3600) {
            hours++;
            mCalculated -= 3600;
        }
        while (mCalculated > 60) {
            minutes++;
            mCalculated -= 60;
        }

        StringBuilder mResult = new StringBuilder();
        if (days != 0) {
            mResult.append(String.valueOf(days)).append("<small>").append(mContext.getString(R.string.time_days)).append("</small>").append(" ");
        }
        if (hours != 0) {
            mResult.append(String.valueOf(hours)).append("<small>").append(mContext.getString(R.string.time_hours)).append("</small>").append(" ");
        }
        if (minutes != 0) {
            mResult.append(String.valueOf(minutes)).append("<small>").append(mContext.getString(R.string.time_minutes)).append("</small>").append(" ");
        }
        if (mCalculated != 0 || mResult.length() == 0) {
            mResult.append(new DecimalFormat(mCalculated > 10 ? "#" : "#.#").format(mCalculated)).append("<small>").append(mContext.getString(R.string.time_seconds)).append("</small>");
        }

        return Html.fromHtml(mResult.toString());
    }

    /**
     * Truncates the input to a number of decimals depending on its size
     * and converts to the empirical system if neccessary
     *
     * @param mContext    The context to run in.
     * @param mCalculated The number of meters to calculate with.
     * @return A Spanned as # if over 20 meters, else #.#, converted to feet if needed so
     */
    Spanned convertDistance(Context mContext, float mCalculated) {
        @SuppressWarnings("ConstantConditions") final SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);

        if (prefs.getBoolean("empirical", false)) {
            mCalculated *= 3.281;
            return Html.fromHtml(new DecimalFormat(mCalculated > 20 ? "#" : "#.#").format(mCalculated) + "<small>" + mContext.getString(R.string.feet) + "</small>");
        } else {
            return Html.fromHtml(new DecimalFormat(mCalculated > 20 ? "#" : "#.#").format(mCalculated) + "<small>" + mContext.getString(R.string.meter) + "</small>");
        }
    }
}