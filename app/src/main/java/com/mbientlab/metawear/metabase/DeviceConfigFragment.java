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

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.mbientlab.metawear.IllegalRouteOperationException;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.impl.JseMetaWearBoard;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.Settings;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import bolts.Capture;
import bolts.Task;
import bolts.TaskCompletionSource;



import static com.mbientlab.metawear.metabase.Global.FIREBASE_PARAM_DEVICE_NAME;
import static com.mbientlab.metawear.metabase.Global.FIREBASE_PARAM_FIRMWARE;
import static com.mbientlab.metawear.metabase.Global.FIREBASE_PARAM_MAC;
import static com.mbientlab.metawear.metabase.Global.FIREBASE_PARAM_MODEL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceConfigFragment extends AppFragmentBase {
    private static final String FIREBASE_EVENT_RECORD = "start_log", FIREBASE_EVENT_STREAM = "start_stream";

    private SelectedGrouping parameter;
    private SensorConfig.Adapter sensorsAdapter;
    private Button configButton, backButton, saveSessionButton;
    private MetaBaseDevice.Adapter devicesAdapter;



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        parameter = (SelectedGrouping) activityBus.parameter();
        devicesAdapter.items.addAll(parameter.devices);
        sensorsAdapter.reset();

        final AlertDialog syncDialog = new AlertDialog.Builder(owner)
                .setTitle(R.string.title_config)
                .setView(R.layout.indeterminate_task)
                .create();
        syncDialog.show();
        ((TextView) syncDialog.findViewById(R.id.message)).setText(R.string.message_loading_sensors);

        Task<Void> result = Task.forResult(null);
        for(MetaBaseDevice d: parameter.devices) {
            final MetaWearBoard metawear = activityBus.getMetaWearBoard(d.btDevice);
            result = result.onSuccessTask(ignored -> {
                try {
                    ((JseMetaWearBoard) metawear).loadBoardAttributes();
                    sensorsAdapter.metawears.add(metawear);
                    return Task.forResult(null);
                } catch (IOException | ClassNotFoundException e) {
                    return metawear.connectAsync().onSuccessTask(ignored2 -> {
                        sensorsAdapter.metawears.add(metawear);
                        return metawear.disconnectAsync();
                    }).continueWithTask(task -> {
                        if (task.isFaulted()) {
                            throw new RuntimeException(String.format(Locale.US, "Unable to determine available sensors for '%s'", d.name));
                        }
                        return task;
                    });
                }
            });
        }
        result.continueWith(task -> {
            syncDialog.dismiss();
            if (task.isFaulted()) {
                new AlertDialog.Builder(owner)
                        .setTitle(R.string.title_error)
                        .setMessage(task.getError().getMessage())
                        .setPositiveButton(android.R.string.ok, ((dialog, which) -> activityBus.navigateBack()))
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                sensorsAdapter.populate();
            }

            //((RadioButton) getView().findViewById(R.id.logging_mode)).setChecked(parameter.devices.size() > 2);

            return null;
        }, Task.UI_THREAD_EXECUTOR);

        /*
        activityBus.scanner().start(arg1 -> {
            devicesAdapter.update(arg1);
            updateStartBtn();
        });

        updateStartBtn();

         */


    }

    boolean present = true;
    private void updateStartBtn() {
        boolean ifPresent = true;
        present = true;
        boolean enableDownload = false;

        for(MetaBaseDevice d: parameter.devices) {
            //enableDownload |= d.isRecording;

            if (!d.isDiscovered){
                configButton.setVisibility(View.GONE);
                backButton.setVisibility(View.GONE);
                saveSessionButton.setVisibility(View.GONE);
            }


            ifPresent &= d.isDiscovered;
        }

        if (ifPresent) {
            configButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            saveSessionButton.setVisibility(View.VISIBLE);
            present = true;


        } else {
            configButton.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            saveSessionButton.setVisibility(View.GONE);
            present = false;
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devicesAdapter = new MetaBaseDevice.Adapter();
        sensorsAdapter = new SensorConfig.Adapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView sensors = view.findViewById(R.id.sensors);
        sensors.setLayoutManager(new GridLayoutManager(getContext(), 2));


        sensorsAdapter.notifyDataSetChanged();
        sensors.setAdapter(sensorsAdapter);






        configButton = view.findViewById(R.id.configure);
        backButton = view.findViewById(R.id.back);
        saveSessionButton = view.findViewById(R.id.saved_session);


        ((RadioButton) view.findViewById(R.id.logging_mode)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            sensorsAdapter.setStreaming(!isChecked);
            if (isChecked){
                new AlertDialog.Builder(owner)
                        .setTitle("Logging Mode")
                        .setMessage(R.string.description_logging)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }
            else{
                new AlertDialog.Builder(owner)
                        .setTitle("Streaming Mode")
                        .setMessage(R.string.description_streaming)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }
            if (sensorsAdapter.isStreaming && !sensorsAdapter.checkTotalDataThroughput()) {
                new AlertDialog.Builder(owner)
                        .setTitle(R.string.title_error)
                        .setMessage(R.string.message_data_throughput)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        view.findViewById(R.id.configure).setOnClickListener(v -> {
            if (sensorsAdapter.isStreaming && !sensorsAdapter.checkTotalDataThroughput()) {
                new AlertDialog.Builder(owner)
                        .setTitle(R.string.title_error)
                        .setMessage(R.string.error_stream_throughput)
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            } else {
                boolean anyEnabled = false;
                for (SensorConfig c : sensorsAdapter.items) {
                    anyEnabled |= c.isEnabled;
                }

                if (!anyEnabled) {
                    new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_error)
                            .setMessage(R.string.message_select_sensor)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    final List<MetaBaseDevice> eraseable = new ArrayList<>();
                    final List<Pair<MetaBaseDevice, Map<SensorConfig, Route>>> activeDevices = new ArrayList<>();

                    // Values are valid, proceed with your logic
                    //configuration dialog
                    final AlertDialog configDialog = new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_config)
                            .setView(R.layout.indeterminate_task)
                            .setCancelable(false)
                            .create();

                    //checking dialog
                    final AlertDialog checkingDialog = new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_checking)
                            .setView(R.layout.indeterminate_task)
                            .setCancelable(false)
                            .create();

                    //check if all devices are connected

                    final boolean[] present = {false};

                    for(MetaBaseDevice d: parameter.devices) {
                        present[0] &= d.isDiscovered;
                        if (!d.isDiscovered){
                            Log.d("not discovered", d.name + "not discovered");
                        }
                    }
                    parameter.devicesRunSuccessful.clear();
                    //if (present) {
                    checkingDialog.show();
                    for (final MetaBaseDevice d : parameter.devices) {
                        parameter.devicesRunSuccessful.add(d);
                    }
                    Task.callInBackground(() -> {
                        Task<Void> task1 = Task.forResult(null);
                        for (final MetaBaseDevice d : parameter.devices) {
                            final MetaWearBoard m = activityBus.getMetaWearBoard(d.btDevice);
                            task1 = task1.onSuccessTask(ignored1-> {
                                Log.d("if success1", "if success1");
                                owner.runOnUiThread(() -> ((TextView) checkingDialog.findViewById(R.id.message)).setText(owner.getString(R.string.message_checking_board, d.name)));
                                return m.connectAsync();
                            }).continueWithTask(task3 -> {
                                if (task3.isFaulted()) {
                                    parameter.devicesRunSuccessful.remove(d);
                                    Log.d("if success2", "if success2");
                                    throw new RuntimeException(String.format("This session has been cancelled because the app failed to configure '%s'", d.name), task3.getError());
                                }
                                return task3;
                            });
                        }
                        task1.continueWith(task3 -> {
                            //configDialog.dismiss();

                            if (task3.isFaulted()) {
                                Log.d("if success6", "if success6");
                                present[0] = false;
                                DeviceInfoFragment.ifReset = true;
                                String errorMsg;
                                if (task3.getError() instanceof IllegalRouteOperationException) {
                                    StringBuilder builder = new StringBuilder();
                                    builder.append("Firmware v1.3.4 or new required.  Please update the firmware for: \n\n");


                                    errorMsg = builder.toString();
                                } else {
                                    errorMsg = task3.getError().getLocalizedMessage();
                                }

                                new AlertDialog.Builder(owner)
                                        .setTitle(R.string.title_error)
                                        .setMessage(errorMsg)
                                        .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                                            Task<Void> eraseTask = Task.forResult(null);


                                            for (MetaBaseDevice d : parameter.devicesRunSuccessful) {
                                                final MetaWearBoard m = activityBus.getMetaWearBoard(d.btDevice);
                                                m.getModule(Debug.class).disconnectAsync();
                                                m.disconnectAsync();
                                                //d.first.isDiscovered = true;
                                            }


                                            eraseTask.continueWith(ignored -> {
                                                checkingDialog.dismiss();
                                                return null;
                                            }, Task.UI_THREAD_EXECUTOR);
                                        }))
                                        .create()
                                        .show();

                            } else {
                                Log.d("if success7", "if success7");
                                present[0] = true;
                                DeviceInfoFragment.ifReset = false;
                                for (Pair<MetaBaseDevice, Map<SensorConfig, Route>> d : activeDevices) {
                                    final MetaWearBoard m = activityBus.getMetaWearBoard(d.first.btDevice);
                                    m.disconnectAsync();

                                }


                            }

                            checkingDialog.dismiss();
                            Task<Void> task = Task.forResult(null);
                            if (present[0]){
                                configDialog.show();
                                DeviceInfoFragment.ifReset = false;
                                for (final MetaBaseDevice d : parameter.devices) {
                                    final MetaWearBoard m = activityBus.getMetaWearBoard(d.btDevice);
                                    task = task.onSuccessTask(ignored -> {
                                        Log.d("if success11", "if success11");
                                        owner.runOnUiThread(() -> ((TextView) configDialog.findViewById(R.id.message)).setText(owner.getString(R.string.message_config_board, d.name)));
                                        return m.connectAsync();
                                    }).continueWithTask(task2 -> {
                                        if (task2.isFaulted()) {
                                            Log.d("if success21", "if success21");
                                            throw new RuntimeException(String.format("This session has been cancelled because the app failed to configure '%s'", d.name), task2.getError());
                                        }
                                        return task2;
                                    }).onSuccessTask(ignored -> {
                                        Log.d("if success31", "if success31");
                                        eraseable.add(d);
                                        Settings.BleConnectionParametersEditor editor = m.getModule(Settings.class).editBleConnParams();
                                        if (editor != null) {
                                            editor.maxConnectionInterval(Global.connInterval)
                                                    .commit();
                                            return Task.delay(1000L);
                                        }
                                        return Task.forResult(null);
                                    }).onSuccessTask(ignored -> {
                                        Log.d("if success41", "if success41");
                                        if (!sensorsAdapter.isStreaming) {
                                            m.getModule(Macro.class).startRecord();
                                        }

                                        Led led = m.getModule(Led.class);
                                        if (led != null) {
                                            led.editPattern(Led.Color.GREEN)
                                                    .highIntensity((byte) 31).lowIntensity((byte) 0)
                                                    .riseTime((short) 100).highTime((short) 200).fallTime((short) 100).pulseDuration((short) 800)
                                                    .repeatCount((byte) 3)
                                                    .commit();
                                            led.editPattern(Led.Color.RED)
                                                    .highIntensity((byte) 10).lowIntensity((byte) 0)
                                                    .riseTime((short) 100).highTime((short) 200).fallTime((short) 100).pulseDuration((short) 15000)
                                                    .delay((short) 2400)
                                                    .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                                                    .commit();
                                            led.play();
                                        }

                                        final Pair<MetaBaseDevice, Map<SensorConfig, Route>> active = new Pair<>(d, new HashMap<>());
                                        Task<Void> createRouteTask = Task.forResult(null);

                                        for (SensorConfig c : sensorsAdapter.items) {
                                            createRouteTask = createRouteTask.onSuccessTask(ignored2 -> {
                                                if (c.isEnabled && c.isValid((m))) {
                                                    return c.addRouteAsync(m).onSuccessTask(task2 -> {
                                                        active.second.put(c, task2.getResult());
                                                        return Task.forResult(null);
                                                    });
                                                }
                                                return Task.forResult(null);
                                            });
                                        }
                                        return createRouteTask.onSuccessTask(ignored2 -> Task.forResult(active));
                                    }).onSuccessTask(task2 -> {
                                        Log.d("if success51", "if success51");
                                        activeDevices.add(task2.getResult());
                                        if (!sensorsAdapter.isStreaming) {
                                            byte[] name = new byte[]{0x4d, 0x65, 0x74, 0x61, 0x57, 0x65, 0x61, 0x72};
                                            try {
                                                name = d.name.getBytes("ASCII");
                                            } catch (UnsupportedEncodingException ignored) {
                                                name = d.name.getBytes();
                                            } finally {
                                                int length = Math.min(name.length, Global.nameMaxChar);
                                                byte[] response = new byte[5 + length];
                                                response[0] = (byte) (response.length - 1);
                                                response[1] = (byte) 0xff;
                                                response[2] = Global.COMPANY_ID & 0xff;
                                                response[3] = (Global.COMPANY_ID >> 8) & 0xff;
                                                response[4] = Global.METABASE_SCAN_ID;
                                                System.arraycopy(name, 0, response, 5, length);

                                                m.getModule(Settings.class).editBleAdConfig()
                                                        .scanResponse(response)
                                                        .commit();
                                            }

                                            return m.getModule(Macro.class).endRecordAsync().continueWithTask(macroTask -> {
                                                if (macroTask.isFaulted()) {
                                                    throw macroTask.getError();
                                                }

                                                if (BuildConfig.LOG_EVENT) {
                                                    JseMetaWearBoard casted = (JseMetaWearBoard) m;
                                                    Bundle bundle = new Bundle();
                                                    bundle.putString(FIREBASE_PARAM_DEVICE_NAME, d.name);
                                                    bundle.putString(FIREBASE_PARAM_MAC, d.mac);
                                                    bundle.putString(FIREBASE_PARAM_MODEL, Global.getRealModel(m.getModelString(), casted.getModelNumber()));
                                                    bundle.putString(FIREBASE_PARAM_FIRMWARE, casted.getFirmware());

                                                }

                                                return Task.forResult(null);
                                            });
                                        }
                                        return Task.forResult(null);
                                    });
                                }
                            }
                            if(present[0]) {
                                task.continueWith(task2 -> {
                                            Log.d("if success61", "if success61");
                                            configDialog.dismiss();
                                            if (task2.isFaulted()) {
                                                String errorMsg;
                                                if (task2.getError() instanceof IllegalRouteOperationException) {
                                                    StringBuilder builder = new StringBuilder();
                                                    builder.append("Firmware v1.3.4 or new required.  Please update the firmware for: \n\n");

                                                    boolean first = true;
                                                    for (MetaBaseDevice d : eraseable) {
                                                        if (!first) builder.append(", ");
                                                        builder.append("'").append(d.name).append("'");

                                                        first = false;

                                                    }

                                                    errorMsg = builder.toString();
                                                } else {
                                                    errorMsg = task2.getError().getLocalizedMessage();
                                                }

                                                new AlertDialog.Builder(owner)
                                                        .setTitle(R.string.title_error)
                                                        .setMessage(errorMsg)
                                                        .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                                                            Task<Void> eraseTask = Task.forResult(null);

                                                            for (MetaBaseDevice d : eraseable) {
                                                                MetaWearBoard m = activityBus.getMetaWearBoard(d.btDevice);
                                                                eraseTask = eraseTask.continueWithTask(task4 -> m.connectAsync())
                                                                        .continueWithTask(connTask -> {
                                                                            if (!(connTask.isFaulted() || connTask.isCancelled())) {
                                                                                m.getModule(Macro.class).eraseAll();
                                                                                m.getModule(Debug.class).resetAfterGc();
                                                                                m.getModule(Debug.class).disconnectAsync();
                                                                            }
                                                                            return Task.forResult(null);
                                                                        });
                                                            }

                                                            for (Pair<MetaBaseDevice, Map<SensorConfig, Route>> d : activeDevices) {
                                                                final MetaWearBoard m = activityBus.getMetaWearBoard(d.first.btDevice);
                                                                m.disconnectAsync();

                                                            }


                                                            eraseTask.continueWith(ignored -> {
                                                                activityBus.navigateBack();
                                                                return null;
                                                            }, Task.UI_THREAD_EXECUTOR);
                                                        }))
                                                        .create()
                                                        .show();
                                            } else {
                                                new AlertDialog.Builder(owner)
                                                        .setTitle(R.string.title_success)
                                                        .setMessage(owner.getString(R.string.message_sensors_active, sensorsAdapter.isStreaming ? "streaming" : "logging"))
                                                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                                            if (!sensorsAdapter.isStreaming) {
                                                                for (Pair<MetaBaseDevice, Map<SensorConfig, Route>> d : activeDevices) {
                                                                    final MetaWearBoard m = activityBus.getMetaWearBoard(d.first.btDevice);

                                                                    m.getModule(Logging.class).start(false);
                                                                    for (SensorConfig c : d.second.keySet()) {
                                                                        c.start(m);
                                                                    }

                                                                    d.first.isDiscovered = false;
                                                                    m.getModule(Debug.class).disconnectAsync();
                                                                }
                                                                activityBus.navigateBack();
                                                            } else {
                                                                StreamMonitorFragment.Parameter streamParameters = new StreamMonitorFragment.Parameter();
                                                                streamParameters.devices = activeDevices;
                                                                streamParameters.sessions = parameter.sessions;
                                                                streamParameters.name = parameter.name;
                                                                streamParameters.mDevices = parameter.devices;
                                                                streamParameters.configSessions = parameter.configSessions;
                                                                //streamParameters.summaryItem = parameter.summaryItem;




                                                                activityBus.swapFragment(StreamMonitorFragment.class, streamParameters);
                                                            }
                                                        })
                                                        .setCancelable(false)
                                                        .create()
                                                        .show();
                                            }

                                            return null;
                                        }
                                        , Task.UI_THREAD_EXECUTOR);
                            }
                            return null;
                        }, Task.UI_THREAD_EXECUTOR);
                        return null;
                    });






                        //if not all devices are connected:
                   // }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityBus.navigateBack();
            }
        });

        saveSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean [] enabledSensors = new boolean[16];
                boolean [] enableGraphs = new boolean[16];
                for (int i = 0; i < enabledSensors.length; i++){
                    for (SensorConfig c : sensorsAdapter.items) {
                        if (c.identifier.equals("acceleration")) {
                            enabledSensors[0] = c.isEnabled;
                        }
                        else if (c.identifier.equals("acceleration-mag")) {
                            enabledSensors[1] = c.isEnabled;
                        }
                        else if (c.identifier.equals("angular-velocity")) {
                            enabledSensors[2] = c.isEnabled;
                        }
                        else if (c.identifier.equals("angular-velocity-peak")) {
                            enabledSensors[3] = c.isEnabled;
                        }
                        else if (c.identifier.equals("magnetic-field")) {
                            enabledSensors[4] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio0")) {
                            enabledSensors[5] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio1")) {
                            enabledSensors[6] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio2")) {
                            enabledSensors[7] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio3")) {
                            enabledSensors[8] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio4")) {
                            enabledSensors[9] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gpio5")) {
                            enabledSensors[10] = c.isEnabled;
                        }
                        else if (c.identifier.equals("linear-acceleration")) {
                            enabledSensors[11] = c.isEnabled;
                        }
                        else if (c.identifier.equals("euler-angles")) {
                            enabledSensors[12] = c.isEnabled;
                        }
                        else if (c.identifier.equals("gravity")) {
                            enabledSensors[13] = c.isEnabled;
                        }
                        else if (c.identifier.equals("pressure")) {
                            enabledSensors[14] = c.isEnabled;
                        }
                        else if (c.identifier.equals("orientation")) {
                            enabledSensors[15] = c.isEnabled;
                        }

                    }
                }

                Log.d("checkbox enabled acc", "checkbox enabled acc = " + enabledSensors[0]);
                Log.d("checkbox enabled acc-mag", "checkbox enabled acc-mag = " + enabledSensors[1]);
                Log.d("checkbox enabled gyro", "checkbox enabled gyro = " + enabledSensors[2]);
                Log.d("checkbox enabled mag", "checkbox enabled mag = " + enabledSensors[3]);
                Log.d("checkbox enabled gpio0", "checkbox enabled gpio0 = " + enabledSensors[4]);
                Log.d("checkbox enabled gpio1", "checkbox enabled gpio1 = " + enabledSensors[5]);
                Log.d("checkbox enabled gpio2", "checkbox enabled gpio2 = " + enabledSensors[6]);
                Log.d("checkbox enabled gpio3", "checkbox enabled gpio3 = " + enabledSensors[7]);
                Log.d("checkbox enabled gpio4", "checkbox enabled gpio4 = " + enabledSensors[8]);
                Log.d("checkbox enabled gpio5", "checkbox enabled gpio5 = " + enabledSensors[9]);
                Log.d("checkbox enabled lin-acc", "checkbox enabled lin-acc = " + enabledSensors[10]);
                Log.d("checkbox enabled eu-ang", "checkbox enabled eu-ang = " + enabledSensors[11]);
                Log.d("checkbox enabled gravity", "checkbox enabled gravity = " + enabledSensors[12]);
                Log.d("checkbox enabled gravity", "checkbox enabled gravity = " + enabledSensors[13]);

                for (int i = 0; i < enableGraphs.length; i++){
                    for (SensorConfig c : sensorsAdapter.items) {
                        if (c.identifier.equals("acceleration")) {
                            enableGraphs[0] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("acceleration-mag")) {
                            enableGraphs[1] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("angular-velocity")) {
                            enableGraphs[2] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("angular-velocity-peak")) {
                            enableGraphs[3] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("magnetic-field")) {
                            enableGraphs[4] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio0")) {
                            enableGraphs[5] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio1")) {
                            enableGraphs[6] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio2")) {
                            enableGraphs[7] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio3")) {
                            enableGraphs[8] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio4")) {
                            enableGraphs[9] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gpio5")) {
                            enableGraphs[10] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("linear-acceleration")) {
                            enableGraphs[11] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("euler-angles")) {
                            enableGraphs[12] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("gravity")) {
                            enableGraphs[13] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("pressure")) {
                            enableGraphs[14] = c.isEnabledGraph;
                        }
                        else if (c.identifier.equals("orientation")) {
                            enableGraphs[15] = c.isEnabledGraph;
                        }

                    }
                }

                Log.d("checkbox enabled acc", "checkbox enabled acc = " + enableGraphs[0]);
                Log.d("checkbox enabled acc-mag", "checkbox enabled acc-mag = " + enableGraphs[1]);
                Log.d("checkbox enabled gyro", "checkbox enabled gyro = " + enableGraphs[2]);
                Log.d("checkbox enabled mag", "checkbox enabled mag = " + enableGraphs[3]);
                Log.d("checkbox enabled gpio0", "checkbox enabled gpio0 = " + enableGraphs[4]);
                Log.d("checkbox enabled gpio1", "checkbox enabled gpio1 = " + enableGraphs[5]);
                Log.d("checkbox enabled gpio2", "checkbox enabled gpio2 = " + enableGraphs[6]);
                Log.d("checkbox enabled gpio3", "checkbox enabled gpio3 = " + enableGraphs[7]);
                Log.d("checkbox enabled gpio4", "checkbox enabled gpio4 = " + enableGraphs[8]);
                Log.d("checkbox enabled gpio5", "checkbox enabled gpio5 = " + enableGraphs[9]);
                Log.d("checkbox enabled lin-acc", "checkbox enabled lin-acc = " + enableGraphs[10]);
                Log.d("checkbox enabled eu-ang", "checkbox enabled eu-ang = " + enableGraphs[11]);
                Log.d("checkbox enabled gravity", "checkbox enabled gravity = " + enableGraphs[12]);
                Log.d("checkbox enabled gravity", "checkbox enabled gravity = " + enableGraphs[13]);
                Log.d("checkbox enabled pressure", "checkbox enabled pressure = " + enableGraphs[14]);

                Date currentTime = Calendar.getInstance().getTime();

                if (parameter.devices.size() <= 1){
                    final Capture<EditText> sessionName = new Capture<>();
                    final Capture<TextInputLayout> sessionNameTextWrapper = new Capture<>();
                    TaskCompletionSource<String> sessionNameTaskSource = new TaskCompletionSource<>();

                    final AlertDialog sessionDialog = new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_session_name)
                            .setView(R.layout.dialog_item_naming)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    sessionDialog.show();

                    ((TextView) sessionDialog.findViewById(R.id.instructions_text)).setText(R.string.instruction_configSessionName_session);
                    sessionName.set(sessionDialog.findViewById(R.id.item_name));
                    sessionNameTextWrapper.set(sessionDialog.findViewById(R.id.item_name_wrapper));
                    final int[] ifEmptyName = {0};
                    sessionDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                        String customName = sessionName.get().getText().toString();
                        ifEmptyName[0] = customName.length();
                        if (customName.contains("_")) {
                            sessionNameTextWrapper.get().setError(owner.getString(R.string.error_underscore));
                        } else {
                            sessionNameTextWrapper.get().setError(null);
                            sessionDialog.dismiss();
                            sessionNameTaskSource.setResult(customName);
                        }
                        Log.d("session name", "session name" +  ifEmptyName[0]);
                        for (MetaBaseDevice d: parameter.devices){
                            AppState.ConfigSession newSession;
                            if(sessionName.get().getText().toString().length() == 0){
                                newSession = new AppState.ConfigSession(d.name + " " + "Config Session"+ + (d.configSessions.size()),""+currentTime, enabledSensors, enableGraphs);
                            }else {
                                newSession = new AppState.ConfigSession(d.name + " " + sessionName.get().getText().toString(), "" + currentTime, enabledSensors, enableGraphs);
                            }

                            String name = newSession.name.replace(" ", "-");
                            String newTime = "" + currentTime;
                            newTime = newTime.replace(" ", "-");
                            String enableS = "";
                            String enableG = "";
                            for(boolean b : enabledSensors){
                                if(b == true){
                                    enableS += 1;
                                }
                                else{
                                    enableS += 0;
                                }
                            }
                            for(boolean b : enableGraphs){
                                if(b == true){
                                    enableG += 1;
                                }
                                else{
                                    enableG += 0;
                                }
                            }

                            for (File f : AppState.devicesPath.listFiles()){
                                if(d.getFileFriendlyMac().equals(f.getName())){
                                    File myFile = new File(f, "ConfigSession_" + name + "_" + newTime +"_" + enableS+"_" + enableG);

                                    try {
                                        FileOutputStream fStream = new FileOutputStream(myFile);
                                        DataOutputStream data0 = new DataOutputStream(fStream);
                                        fStream.close();
                                        data0.close();
                                    } catch (IOException e) {
                                        Log.d("add my file", "add my file error");
                                        e.printStackTrace();
                                    }




                                    for(File l: f.listFiles()){
                                        Log.d("add my file", "add my file " + l.getName());
                                    }


                                }
                            }


                            d.configSessions.add(newSession);
                        }

                    });



                }else{
                    final Capture<EditText> sessionName = new Capture<>();
                    final Capture<TextInputLayout> sessionNameTextWrapper = new Capture<>();
                    TaskCompletionSource<String> sessionNameTaskSource = new TaskCompletionSource<>();

                    final AlertDialog sessionDialog = new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_session_name)
                            .setView(R.layout.dialog_item_naming)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    sessionDialog.show();

                    ((TextView) sessionDialog.findViewById(R.id.instructions_text)).setText(R.string.instruction_configSessionName_session);
                    sessionName.set(sessionDialog.findViewById(R.id.item_name));
                    sessionNameTextWrapper.set(sessionDialog.findViewById(R.id.item_name_wrapper));
                    final int[] ifEmptyName = {0};
                    sessionDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                        String customName = sessionName.get().getText().toString();
                        ifEmptyName[0] = customName.length();
                        if (customName.contains("_")) {
                            sessionNameTextWrapper.get().setError(owner.getString(R.string.error_underscore));
                        } else {
                            sessionNameTextWrapper.get().setError(null);
                            sessionDialog.dismiss();
                            sessionNameTaskSource.setResult(customName);
                        }
                        Log.d("session name", "session name" +  ifEmptyName[0]);

                        AppState.ConfigSession newSession;
                        if(sessionName.get().getText().toString().length() == 0){
                            newSession = new AppState.ConfigSession(parameter.name + " " + "Config Session"+ + (parameter.configSessions.size()),""+currentTime, enabledSensors, enableGraphs);
                        }else {
                            newSession = new AppState.ConfigSession(parameter.name + " " + sessionName.get().getText().toString(), "" + currentTime, enabledSensors, enableGraphs);
                        }
                        String name = newSession.name.replace(" ", "-");
                        String newTime = "" + currentTime;
                        newTime = newTime.replace(" ", "-");
                        String enableS = "";
                        String enableG = "";
                        for(boolean b : enabledSensors){
                            if(b == true){
                                enableS += 1;
                            }
                            else{
                                enableS += 0;
                            }
                        }
                        for(boolean b : enableGraphs){
                            if(b == true){
                                enableG += 1;
                            }
                            else{
                                enableG += 0;
                            }
                        }

                       ArrayList<String> namesInGroup = new ArrayList<>();
                        for (File groupF: AppState.groupsPath.listFiles()){
                            if(parameter.name.equals(groupF.getName())){
                                for(File currentF: groupF.listFiles()){
                                    if(!(namesInGroup.contains(currentF.getName()))) {
                                        namesInGroup.add(currentF.getName());
                                    }
                                    Log.d("add my file3", "add my file3 "+ currentF.getName());
                                }
                            }
                        }

                        for (File f : AppState.devicesPath.listFiles()){
                                boolean ifFileInGroup = false;
                                for (String s: namesInGroup){
                                    Log.d("add my file4", "add my file4 "+ namesInGroup.size());
                                    if(s.equals(f.getName())){
                                        ifFileInGroup = true;
                                        break;
                                    }
                                }

                                if(ifFileInGroup) {
                                    File myFile = new File(f, "ConfigSession_" + name + "_" + newTime +"_" + enableS+"_" + enableG);
                                    try {
                                        FileOutputStream fStream = new FileOutputStream(myFile);
                                        DataOutputStream data0 = new DataOutputStream(fStream);
                                        fStream.close();
                                        data0.close();
                                    } catch (IOException e) {
                                        Log.d("add my file", "add my file error");
                                        e.printStackTrace();
                                    }
                                }




                                for(File l: f.listFiles()){
                                    Log.d("add my file", "add my file " + l.getName());
                                }



                        }
                        parameter.configSessions.add(newSession);


                    });

                }

                /*
                binder.session.files.clear();
                binder.session.files.addAll(renamed);
                binder.parameter.sessions.add(0, binder.session);


                 */

            }
        });

    }

    @Override
    Integer getMenuGroupResId() {
        return null;
    }
}
