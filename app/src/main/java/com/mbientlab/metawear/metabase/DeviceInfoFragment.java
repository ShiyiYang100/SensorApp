/*
 * Copyright 2014-2018 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.metabase;

import android.app.Activity;;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mbientlab.function.Action;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.impl.platform.TimedTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import bolts.TaskCompletionSource;

public class DeviceInfoFragment extends AppFragmentBase {
    private static final String FIREBASE_EVENT_CLOUD_SYNC_SUCCESS = "sync_metacloud";

    interface MetaCloudOptions {
        boolean showAbout();
        void disableAbout();
    }

    private static final int LOGIN_REQUEST = 0;

    private TaskCompletionSource<Void> loginTaskSource;
    static boolean inNewSession = false;
    private MetaBaseDevice.Adapter2 devicesAdapter;
    private AppState.Session.Adapter sessionsAdapter;
    private AppState.ConfigSession.Adapter configSessionsAdapter;
    private Button startBtn, viewMySessions;
    private TextView title, sessionsBar;
    private RecyclerView devicesView, sessionsView;
    private SelectedGrouping parameter;
    private MetaCloudOptions options;
    BluetoothDevice btDevice;
    MetaWearBoard m;
    boolean ifSuccess = true;
    boolean ifSuccess2 = true;
    static boolean ifReset = false;


    public DeviceInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case LOGIN_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    loginTaskSource.setResult(null);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    loginTaskSource.setCancelled();
                } else {
                    loginTaskSource.setError(new RuntimeException("MetaCloud login failed"));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateStartBtn() {
        boolean present = true;
        boolean enableDownload = false;

        for(MetaBaseDevice d: parameter.devices) {
            enableDownload |= d.isRecording;
            present &= d.isDiscovered;
        }

        if (present && !inNewSession) {
            startBtn.setVisibility(View.VISIBLE);
            viewMySessions.setVisibility(View.VISIBLE);
            if (enableDownload) {
                startBtn.setText(R.string.title_download);
                startBtn.setOnClickListener(v -> activityBus.swapFragment(DataDownloadFragment.class, parameter));
            } else {
                startBtn.setText(R.string.label_new_session);

                //automatically enable the sensors and their graphs
                SensorConfig.Adapter.isEnableButtonClicked[0] = false;
                SensorConfig.Adapter.isGraphButtonClicked[0] = false;

                //create a new session
                //create a new session
                startBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        /*
                        title.setVisibility(View.GONE);
                        sessionsBar.setVisibility(View.GONE);
                        devicesView.setVisibility(View.GONE);
                        sessionsView.setVisibility(View.GONE);

                         */
                        SavedSessionsFragment.ifAccessedSavedConfigSessions = false;
                        for (int i = 0; i < 16; i ++){
                            SensorConfig.Adapter.setTrue[i] = false;
                        }
                        for (int i = 0; i < 16; i ++){
                            SensorConfig.Adapter.setGraphTrue[i] = false;
                        }
                        for(MetaBaseDevice md : parameter.devices) {
                            LayoutInflater inflater = LayoutInflater.from(owner);
                            View dialogView = inflater.inflate(R.layout.knee_height_layout, null);
                            final EditText firstValueEditText = dialogView.findViewById(R.id.knee_height);
                            final EditText secondValueEditText = dialogView.findViewById(R.id.knee_radius);
                            AlertDialog kneeDialog = new AlertDialog.Builder(owner)
                                    .setTitle("Enter knee height and radius for device: " + md.name+ " " + md.mac)
                                    .setView(dialogView)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int w) {
                                            try {
                                                float firstValue = Float.parseFloat(firstValueEditText.getText().toString());
                                                float secondValue = Float.parseFloat(secondValueEditText.getText().toString());
                                                if (firstValue > 0 && secondValue > 0) {
                                                    md.m.setKneeHeight(firstValue);
                                                    md.m.setKneeRadius( secondValue);
                                                }
                                            } catch (NumberFormatException e) {
                                                StreamMonitorFragment.leftKneeHeight = 0f;
                                                StreamMonitorFragment.rightKneeHeight = 0f;
                                            }
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create();

                            kneeDialog.show();

                        }





                        activityBus.swapFragment(DeviceConfigFragment.class, parameter);
                    }
                });

                //view saved sessions
                viewMySessions.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SavedSessionsFragment.ifAccessedSavedConfigSessions = true;
                        activityBus.swapFragment(SavedSessionsFragment.class, parameter);
                    }
                });



                //view saved sessions
            }
        } else {
            startBtn.setVisibility(View.GONE);
            viewMySessions.setVisibility(View.GONE);
            startBtn.setOnClickListener(null);
            viewMySessions.setOnClickListener(null);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicesAdapter = new MetaBaseDevice.Adapter2();
        devicesAdapter.itemClicked = arg1 -> activityBus.swapFragment(DiagnosticFragment.class, arg1);
        sessionsAdapter = new AppState.Session.Adapter();
        configSessionsAdapter = new AppState.ConfigSession.Adapter();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!(getActivity() instanceof MetaCloudOptions)) {
            throw new ClassCastException("Owning activity does not derive from MetaCloudOptions");
        }
        options = (MetaCloudOptions) getActivity();

        parameter = (SelectedGrouping) activityBus.parameter();

        if (parameter.devices.size() == 1) {
            title.setText(R.string.label_device);
        } else {
            title.setText(parameter.name);
        }

        activityBus.onBackPressed(new Action<Void>() {

            public void apply(Void arg1) {
                if (inNewSession){
                    inNewSession = true;
                    title.setVisibility(View.VISIBLE);
                    sessionsBar.setVisibility(View.VISIBLE);
                    devicesView.setVisibility(View.VISIBLE);
                    sessionsView.setVisibility(View.VISIBLE);
                    startBtn.setVisibility(View.VISIBLE);
                    viewMySessions.setVisibility(View.VISIBLE);
                    inNewSession = false;
                }
            }
        });

        devicesAdapter.items.clear();
        devicesAdapter.items.addAll(parameter.devices);
        devicesAdapter.notifyDataSetChanged();

        configSessionsAdapter.configItems.clear();
        configSessionsAdapter.configItems.addAll(parameter.configSessions);
        configSessionsAdapter.notifyDataSetChanged();

        sessionsAdapter.items.clear();
        sessionsAdapter.items.addAll(parameter.sessions);
        sessionsAdapter.notifyDataSetChanged();

        sessionsAdapter.shareSession = arg1 -> {
            final StringBuilder devices = new StringBuilder();
            boolean first = true;
            for(MetaBaseDevice d: parameter.devices) {
                if (!first) {
                    devices.append(", ");
                }
                devices.append(d.mac);
                first = false;
            }

            final Intent intentShareFile = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intentShareFile.setType("text/plain");
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, String.format(Locale.US, "MetaBase Session: %s", arg1.name));
            ArrayList<Uri> uris = new ArrayList<>();
            for (File it : arg1.files) {
                uris.add(FileProvider.getUriForFile(owner, "com.mbientlab.metawear.metabase.fileprovider", it));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    intentShareFile.setData(FileProvider.getUriForFile(owner, "com.mbientlab.metawear.metabase.fileprovider", it));
                    intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }
            intentShareFile.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intentShareFile.putExtra(Intent.EXTRA_TEXT, String.format(Locale.US, "Boards: [%s]", devices.toString()));
            //owner.startActivity(Intent.createChooser(intent, "Saving Data..."));

            Intent chooser = Intent.createChooser(intentShareFile, "Share File");
            List<ResolveInfo> resInfoList = owner.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                for (Uri uri : uris) {
                    owner.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            startActivity(chooser);
        };



        if(ifReset) {
            Log.d("I need to reset", "I need to reset");
            TimedTask<BluetoothDevice> deviceFinder = new TimedTask<>();
        }
        activityBus.scanner().start(arg1 -> {
            Log.d("I need to reset1", "I need to reset1");
            devicesAdapter.update(arg1);
            updateStartBtn();
        });
        updateStartBtn();
    }

    @Override
    public void onResume() {
        super.onResume();

        owner.invalidateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    Integer getMenuGroupResId() {
        return R.id.group_dev_info;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_device_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView devices = view.findViewById(R.id.known_devices);
        devices.setAdapter(devicesAdapter);
        devicesAdapter.notifyDataSetChanged();

        inNewSession = false;

        RecyclerView sessions = view.findViewById(R.id.config_sessions);
        sessions.setAdapter(sessionsAdapter);

        startBtn = view.findViewById(R.id.new_session);
        viewMySessions = view.findViewById(R.id.exsisting_sessions);
        title = view.findViewById(R.id.title_devices);
        sessionsBar = view.findViewById(R.id.title_sessions);
        devicesView = view.findViewById(R.id.known_devices);
        devicesView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        sessionsView = view.findViewById(R.id.config_sessions);

        startBtn.setVisibility(View.VISIBLE);
        viewMySessions.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);
        sessionsBar.setVisibility(View.VISIBLE);
        devicesView.setVisibility(View.VISIBLE);
        sessionsView.setVisibility(View.VISIBLE);


    }
}
