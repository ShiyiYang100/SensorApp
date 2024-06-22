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

import static com.mbientlab.metawear.metabase.Global.FIREBASE_PARAM_LOG_DURATION;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.textfield.TextInputLayout;
import com.mbientlab.function.Action3;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.impl.JseMetaWearBoard;
import com.mbientlab.metawear.module.Debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import bolts.Capture;
import bolts.Task;
import bolts.TaskCompletionSource;

public class StreamMonitorFragment extends AppFragmentBase implements ServiceConnection {
    private final static String FIREBASE_EVENT_STREAM_STOP = "stop_stream";


    static class Parameter {
        List<Pair<MetaBaseDevice, Map<SensorConfig, Route>>> devices;
        List<AppState.Session> sessions;
        String name;
        List<MetaBaseDevice> devicesRunSuccessful;
        AppState.SummaryItem summaryItem =   this.summaryItem = new AppState.SummaryItem(new TemperalParameters(),
                new SpatialParameters(),
                new AngularParameters());
        List<AppState.ConfigSession> configSessions;
        List<MetaBaseDevice> mDevices;
    }

    public static class Service extends android.app.Service implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        private final LocalBinder localBinder = new LocalBinder();
        class LocalBinder extends Binder {
            boolean active;
            Parameter parameter;

            List<MetaWearBoard> metawears;
            List<DataHandler> dataHandlers;

            List <DataHandler> csvWriters;
            List<DataHandler.CsvDataHandler> accConfigHandlers;
            List<DataHandler.CsvDataHandler> accMagConfigHandlers;
            List<DataHandler.CsvDataHandler> magConfigHandlers;
            List<DataHandler.CsvDataHandler> gyroConfigHandlers;
            List<DataHandler.CsvDataHandler> gyroPeakConfigHandlers;
            List<DataHandler.CsvDataHandler> orientationConfigHandlers;
            List<DataHandler.CsvDataHandler> gpioAdcConfigHandlers;
            List<DataHandler.CsvDataHandler> gpioAbsConfigHandlers;
            List<DataHandler.CsvDataHandler> gaitConfigHandlers;
            AppState.Session session;
            long start;
            List<DataHandler.SampleCountDataHandler> streamMetrics;
            Map<MetaBaseDevice, List<Pair<SensorConfig, DataHandler.SampleCountDataHandler>>> samples;
            List<Pair<MetaBaseDevice,MetaBaseDeviceData>> metaBaseDeviceDataList;



            void start(Parameter parameter) {
                this.parameter = parameter;
                metawears = new ArrayList<>();
                dataHandlers = new ArrayList<>();
                accConfigHandlers = new ArrayList<>();
                accMagConfigHandlers = new ArrayList<>();
                magConfigHandlers = new ArrayList<>();
                gyroConfigHandlers = new ArrayList<>();
                gyroPeakConfigHandlers = new ArrayList<>();
                orientationConfigHandlers = new ArrayList<>();
                gpioAdcConfigHandlers = new ArrayList<>();
                gpioAbsConfigHandlers = new ArrayList<>();
                gaitConfigHandlers = new ArrayList<>();
                streamMetrics = new ArrayList<>();
                samples = new LinkedHashMap<>();
                csvWriters = new ArrayList<>();
                metaBaseDeviceDataList = new ArrayList<>();

                getApplicationContext().bindService(new Intent(Service.this, BtleService.class), Service.this, Context.BIND_AUTO_CREATE);
                active = true;
            }
        }

        @Override
        public void onDestroy() {
            localBinder.active = false;
            getApplicationContext().unbindService(this);

            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return localBinder;
        }
    }

    private Parameter parameter;
    private SelectedGrouping p;
    private Service.LocalBinder binder;
    private Intent streamServiceIntent;
    private static ArrayList<Intent> services;
    private TextView elapsedTimeText;
    private TextView graphButton, gaitButton;
    private ScrollView graphs, gaitParameters;
    int sensorSelectionIndex;
    private boolean ifAccConfig, ifAccMagConfig, ifMagConfig, ifGyroConfig, ifGyroPeakConfig,
            ifOrientationConfig, ifGpioAdcConfig, ifGpioAbsConfig;
    public ArrayList<Thread> allThreads = new ArrayList<>();
    long prevUpdate = -1;

    //TODO: use multiple handlers
    private Handler uiScheduler= new Handler();
    private Handler accHandler = new Handler();
    private Handler accMagHandler = new Handler();
    private Handler magHandler = new Handler();
    private Handler gyroHandler = new Handler();
    private Handler gyroPeakHandler = new Handler();
    private Handler orientationHandler = new Handler();
    private Handler gpioAdcHandler = new Handler();
    private Handler gpioAbsHandler = new Handler();
    private Handler gaitHandler = new Handler();

    public static float leftKneeHeight;
    public static float rightKneeHeight;


    TextView realTimeStrideTime, realTimeSwingTime, realTimeStanceTime, realTimeToeOff,
            realTimeHeelStrike, realTimeDST, realTimeSST, realTimeStepTime, realTimeStrideLen,
            realTimeWalkingSpeed, realTimeMFC, realTimeStepNum, realTimeOXMAX, realTimeOXMIN,
            realTimeOYMAX, realTimeOYMIN, realTimeOZMAX, realTimeOZMIN;
    RecyclerView summarySesssions;
    private AppState.SummaryItem.Adapter summaryItemsAdapter = new AppState.SummaryItem.Adapter();


    private final Runnable updateValues= new Runnable() {
        @Override
        public void run() {
            for(DataHandler.SampleCountDataHandler it: binder.streamMetrics) {
                //TODO: change time text continuously
                it.sampleCountView.setText(String.format(Locale.US, "%d", it.samples));
            }


            long elapsed= System.nanoTime() - binder.start;
            elapsedTimeText.setText(String.format(Locale.US, "%02d:%02d:%02d", (elapsed / 3600000000000L) % 24, (elapsed / 60000000000L) % 60, (elapsed / 1000000000L) % 60));

            uiScheduler.postDelayed(updateValues, 1000L);
        }
    };

    //update and add the data to accelerometer graph
    private final Runnable updateAccConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.accConfigHandlers) {
                //TODO: change time text continuously
                //Thread t = new Thread(new Runnable() {

                //public void run() {
                LineData lineData =it.accelerometerChart.getData();
                lineData.addEntry(new Entry(it.accSampleNum, it.accXValue), 0);
                lineData.addEntry(new Entry(it.accSampleNum, it.accYValue), 1);
                lineData.addEntry(new Entry(it.accSampleNum, it.accZValue), 2);

                it.accelerometerChart.getData().notifyDataChanged();
                it.accelerometerChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.accelerometerChart.setVisibleXRangeMinimum(120);
                it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.accelerometerChart.moveViewToX(Math.max(0f, it.accSampleNum - 1));
                Log.d("if getting data", "if getting data");

                // }
                // });
                // allThreads.add(t);
                //t.start();




                //it.accXView.setText(String.format(Locale.US, "x-axis: %.3f", it.accXValue));
                //it.accYView.setText(String.format(Locale.US, "y-axis: %.3f", it.accYValue));
                // it.accZView.setText(String.format(Locale.US, "z-axis: %.3f", it.accZValue));
            }
            accHandler.postDelayed(updateAccConfigValues, 0L);

        }
    };
    //update and add the data to accelerometer graph
    private final Runnable updateAccMagConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.accMagConfigHandlers) {
                //TODO: change time text continuously
                //Thread t = new Thread(new Runnable() {

                //public void run() {
                LineData lineData =it.accMagChart.getData();
                lineData.addEntry(new Entry(it.accMagSampleNum, it.accMag), 0);


                it.accMagChart.getData().notifyDataChanged();
                it.accMagChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.accMagChart.setVisibleXRangeMinimum(120);
                it.accMagChart.setVisibleXRangeMaximum(120);
                it.accMagChart.moveViewToX(Math.max(0f, it.accMagSampleNum - 1));
                Log.d("if getting data", "if getting data");

                // }
                // });
                // allThreads.add(t);
                //t.start();




                //it.accXView.setText(String.format(Locale.US, "x-axis: %.3f", it.accXValue));
                //it.accYView.setText(String.format(Locale.US, "y-axis: %.3f", it.accYValue));
                // it.accZView.setText(String.format(Locale.US, "z-axis: %.3f", it.accZValue));
            }
            accMagHandler.postDelayed(updateAccMagConfigValues, 0L);

        }
    };
    //update and add the data to magnetometer graph
    private final Runnable updateMagConfigValues = new Runnable() {
        @Override
        public void run() {
            for (DataHandler.CsvDataHandler it : binder.magConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData = it.magnetometerChart.getData();
                lineData.addEntry(new Entry(it.magSampleNum, it.magXValue), 0);
                lineData.addEntry(new Entry(it.magSampleNum, it.magYValue), 1);
                lineData.addEntry(new Entry(it.magSampleNum, it.magZValue), 2);
                it.magnetometerChart.getData().notifyDataChanged();
                it.magnetometerChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.magnetometerChart.setVisibleXRangeMinimum(120);
                it.magnetometerChart.setVisibleXRangeMaximum(120);
                it.magnetometerChart.moveViewToX(Math.max(0f, it.magSampleNum - 1));
                Log.d("if getting data", "if getting data");


            }
            magHandler.postDelayed(updateMagConfigValues, 0L);
        }


    };

    //update and add the data to gyroscope graph
    private final Runnable updateGyroConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.gyroConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData =it.gyroscopeChart.getData();
                //lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroXValue), 0);
                lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroXValue), 0);
                lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroYValue), 1);
                lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroZValue), 2);

                //lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroZValue), 2);
                //lineData.addEntry(new Entry(it.gyroSampleNum, it.filteredY), 3);
                //lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroZValue), 2);
                it.gyroscopeChart.getData().notifyDataChanged();
                it.gyroscopeChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.gyroscopeChart.setVisibleXRangeMinimum(120);
                it.gyroscopeChart.setVisibleXRangeMaximum(120);
                it.gyroscopeChart.moveViewToX(Math.max(0f, it.gyroSampleNum - 1));
                Log.d("if getting data", "if getting data");
            }
            gyroHandler.postDelayed(updateGyroConfigValues, 0L);

        }
    };

    //update and add the data to gyroscope graph
    private final Runnable updateGyroPeakConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.gyroPeakConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData =it.gyroPeakChart.getData();
                //lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroXValue), 0);
                lineData.addEntry(new Entry(it.gyroPeakSampleNum, it.gyroPeakYValue), 0);
                lineData.addEntry(new Entry(it.gyroPeakSampleNum, it.v2), 1);
                //lineData.addEntry(new Entry(it.gyroPeakSampleNum, it.special_value), 2);

                if (it.special_value != 0f){
                    lineData.addEntry(new Entry(it.gyroPeakSampleNum, it.special_value), 2);
                }


                //lineData.getDataSets().get(2).setVisible(false);
                it.gyroPeakChart.getData().notifyDataChanged();
                it.gyroPeakChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.gyroPeakChart.setVisibleXRangeMinimum(120);
                it.gyroPeakChart.setVisibleXRangeMaximum(120);
                it.gyroPeakChart.moveViewToX(Math.max(0f, it.gyroPeakSampleNum - 1));
                Log.d("if getting data", "if getting data");
            }
            gyroPeakHandler.postDelayed(updateGyroPeakConfigValues, 0L);

        }
    };
    private final Runnable updateOrientationConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.orientationConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData = it.orientationChart.getData();
                //lineData.addEntry(new Entry(it.gyroSampleNum, it.gyroXValue), 0);
                lineData.addEntry(new Entry(it.orientationSampleNum, it.orientationX), 0);
                lineData.addEntry(new Entry(it.orientationSampleNum, it.orientationY), 1);
                lineData.addEntry(new Entry(it.orientationSampleNum, it.orientationZ), 2);

                //lineData.getDataSets().get(2).setVisible(false);
                it.orientationChart.getData().notifyDataChanged();
                it.orientationChart.notifyDataSetChanged();
                //it.accelerometerChart.setVisibleXRangeMinimum(120);
                //it.accelerometerChart.setVisibleXRangeMaximum(120);
                it.orientationChart.setVisibleXRangeMinimum(120);
                it.orientationChart.setVisibleXRangeMaximum(120);
                it.orientationChart.moveViewToX(Math.max(0f, it.orientationSampleNum - 1));
                Log.d("if getting data", "if getting data");
            }
            orientationHandler.postDelayed(updateOrientationConfigValues, 0L);

        }
    };

    //update and add teh data to Gpio Adc graph(s)
    private final Runnable updateGpioAdcConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.gpioAdcConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData =it.gpioAdcChart.getData();
                lineData.addEntry(new Entry(it.gpioAdcSampleNum, it.gpioAdcValue), 0);

                it.gpioAdcChart.getData().notifyDataChanged();
                it.gpioAdcChart.notifyDataSetChanged();
                it.gpioAdcChart.setVisibleXRangeMinimum(120);
                it.gpioAdcChart.setVisibleXRangeMaximum(120);
                it.gpioAdcChart.moveViewToX(Math.max(0f, it.gpioAdcSampleNum - 1));
                Log.d("if getting data", "if getting data");
            }
            gpioAdcHandler.postDelayed(updateGpioAdcConfigValues, 0L);

        }
    };

    //update and add teh data to Gpio Abs graph(s)
    private final Runnable updateGpioAbsConfigValues = new Runnable() {
        @Override
        public void run() {
            for(DataHandler.CsvDataHandler it: binder.gpioAbsConfigHandlers) {
                //TODO: change time text continuously

                LineData lineData =it.gpioAbsChart.getData();
                lineData.addEntry(new Entry(it.gpioAbsSampleNum, it.gpioAbsValue), 0);

                it.gpioAbsChart.getData().notifyDataChanged();
                it.gpioAbsChart.notifyDataSetChanged();
                it.gpioAbsChart.setVisibleXRangeMinimum(120);
                it.gpioAbsChart.setVisibleXRangeMaximum(120);
                it.gpioAbsChart.moveViewToX(Math.max(0f, it.gpioAbsSampleNum - 1));
                Log.d("if getting data", "if getting data");
            }
            gpioAbsHandler.postDelayed(updateGpioAbsConfigValues, 0L);

        }
    };

    private final Runnable updateGaitParameters = new Runnable() {
        @Override
        public void run() {
            //calculate gait parameters
            List<AppState.SummaryItem> summaryItems = summaryItemsAdapter.summaryItems;
            float stride = 0f;
            int device_num = 0;
            float stance = 0f;
            float swing = 0f;
            float totalTime = 0f;
            float heelStrike = 0f;
            float toeOff = 0f;
            int numStep = 0;
            float strideNum, stepTime;
            float strideLen = 0f, walkingSpeed = 0f, MFC = 0f;
            float oXMax = 0f, oXMin = 0f, oYMax = 0f, oYMin = 0f, oZMax = 0f, oZMin = 0f;

            //get the list in the summary item adapter


            for(int i = 0; i < binder.metaBaseDeviceDataList.size(); i ++){
                Pair<MetaBaseDevice, MetaBaseDeviceData> m = binder.metaBaseDeviceDataList.get(i);
                stride+= m.second.getRealStrideTime();
                stance += m.second.getRealStanceTime();
                swing += m.second.getRealSwingTime();
                //totalTime += m.second.getTotalTime();
                heelStrike += m.second.getRealHeelStrike();
                toeOff += m.second.getRealToeOff();
                numStep += (int) m.second.getTotalStride() / 2 ;
                strideLen += m.second.getRealStrideLen();
                walkingSpeed += m.second.getRealWalkingSpeed();
                MFC += m.second.getRealMFC();
                oXMax = m.second.getOXMax();
                oXMin = m.second.getOXMin();
                oYMax = m.second.getOYMax();
                oYMin = m.second.getOYMin();
                oZMax = m.second.getOZMax();
                oZMin = m.second.getOZMin();


                TemperalParameters temperalParameters =  new TemperalParameters(m.second.getStride(),
                        m.second.getAvgSwing(), m.second.getAvgStance(), toeOff,
                        heelStrike, 0f, swing,(m.second.getStride() /  2));
                SpatialParameters spatialParameters = new SpatialParameters(
                        strideLen,walkingSpeed,MFC,(int)m.second.getTotalStride() / 2);
                AngularParameters angularParameters = new AngularParameters(oXMax, oXMin, oYMax, oYMin,oZMax, oZMin);
                AppState.SummaryItem summaryItem = new AppState.SummaryItem(temperalParameters,
                                spatialParameters,angularParameters);
                summaryItems.set(i, summaryItem);

                device_num++;
            }


            if(device_num != 0) {
                stride /= device_num;
                stance /= device_num;
                swing /= device_num;
                totalTime /= device_num;
                heelStrike /= device_num;
                toeOff /= device_num;
                numStep /= device_num;
                strideLen /= device_num;
                walkingSpeed /= device_num;
                MFC /= device_num;
                oXMax /= device_num;
                oXMin /= device_num;
                oYMax /= device_num;
                oYMin /= device_num;
                oZMax /= device_num;
                oZMin /= device_num;
            }

            stepTime = stride /  2 ;

            if(totalTime != 0){
                strideNum = totalTime / stride;
            }

            realTimeStrideTime.setText(stride + "");
            realTimeSwingTime.setText(swing + "");
            realTimeStanceTime.setText(stance +"");
            realTimeToeOff.setText(toeOff+"");
            realTimeHeelStrike.setText(heelStrike+"");
            float dsst = (stride / 2) - swing;
            realTimeDST.setText(dsst+"");
            realTimeSST.setText(swing + "");
            realTimeStepTime.setText(stepTime+"");
            realTimeStrideLen.setText(strideLen+"");
            realTimeWalkingSpeed.setText(walkingSpeed+"");
            realTimeMFC.setText(MFC+"");
            realTimeStepNum.setText(numStep+"");
            realTimeOXMAX.setText(oXMax+"");
            realTimeOXMIN.setText(oXMin+"");
            realTimeOYMAX.setText(oYMax+"");
            realTimeOYMIN.setText(oYMin+"");
            realTimeOZMAX.setText(oZMax+"");
            realTimeOZMIN.setText(oZMin+"");
            summaryItemsAdapter.notifyDataSetChanged();


            gaitHandler.postDelayed(updateGaitParameters, 0L);

        }
    };




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("wowow1", "wowow1");
        return inflater.inflate(R.layout.fragment_stream_monitor, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d("wowow", "wowow");

        super.onViewCreated(view, savedInstanceState);
        //elapsedTime is the time after starting configuring
        elapsedTimeText= view.findViewById(R.id.elapsed_time);
        graphButton = view.findViewById(R.id.stream_monitor_graph);
        gaitButton = view.findViewById(R.id.stream_monitor_parameter);
        graphs =  view.findViewById(R.id.graphs);
        gaitParameters = view.findViewById(R.id.stream_monitor_gait_view);
        realTimeStrideTime =  view.findViewById(R.id.realtime_stride_time);
        realTimeSwingTime = view.findViewById(R.id.realtime_swing_time);
        realTimeStanceTime = view.findViewById(R.id.summary_stance_time);
        realTimeToeOff = view.findViewById(R.id.summary_toe_off);
        realTimeHeelStrike = view.findViewById(R.id.summary_heel_strike);
        realTimeDST = view.findViewById(R.id.summary_dst);
        realTimeSST = view.findViewById(R.id.summary_sst);
        realTimeStepTime = view.findViewById(R.id.summary_step_time);
        realTimeStrideLen = view.findViewById(R.id.summary_stride_length);
        realTimeWalkingSpeed = view.findViewById(R.id.summary_walking_speed);
        realTimeMFC = view.findViewById(R.id.summary_mfc);
        realTimeStepNum = view.findViewById(R.id.summary_num_of_step);
        realTimeOXMAX = view.findViewById(R.id.summary_OXMax);
        realTimeOXMIN = view.findViewById(R.id.summary_OXMin);
        realTimeOYMAX =  view.findViewById(R.id.summary_OYMax);
        realTimeOYMIN =  view.findViewById(R.id.summary_OYMin);
        realTimeOZMAX =  view.findViewById(R.id.summary_OZMax);
        realTimeOZMIN =  view.findViewById(R.id.summay_OZMin);
        summarySesssions = view.findViewById(R.id.realtime_device_info);
        summarySesssions.setLayoutManager(new LinearLayoutManager(getContext()));

        summarySesssions.setAdapter(summaryItemsAdapter);
        summaryItemsAdapter.notifyDataSetChanged();

        graphs.setVisibility(View.VISIBLE);
        gaitParameters.setVisibility(View.INVISIBLE);
        elapsedTimeText.setText("00:00:00");
        sensorSelectionIndex = 0;

        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graphs.setVisibility(View.VISIBLE);
                gaitParameters.setVisibility(View.INVISIBLE);
            }
        });

        gaitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graphs.setVisibility(View.INVISIBLE);
                gaitParameters.setVisibility(View.VISIBLE);
            }
        });

        view.findViewById(R.id.stream_stop).setOnClickListener(v -> {
            DataHandler.CsvDataHandler.ifStop = true;

            for(DataHandler csvWriter: binder.csvWriters){
                csvWriter.rewrite();
            }
            owner.stopService(streamServiceIntent);

            final long stop = System.nanoTime();
            //remove the handlers if they exist
            uiScheduler.removeCallbacks(updateValues);
            if (ifAccConfig) {
                accHandler.removeCallbacks(updateAccConfigValues);
            }
            if (ifAccMagConfig) {
                accMagHandler.removeCallbacks(updateAccMagConfigValues);
            }
            if (ifMagConfig ) {
                magHandler.removeCallbacks(updateMagConfigValues);
            }
            if (ifGyroConfig) {
                gyroHandler.removeCallbacks(updateGyroConfigValues);
            }
            if (ifGyroPeakConfig) {
                gyroPeakHandler.removeCallbacks(updateGyroPeakConfigValues);
            }
            if (ifOrientationConfig) {
                orientationHandler.removeCallbacks(updateOrientationConfigValues);
            }
            if (ifGpioAdcConfig) {
                gpioAdcHandler.removeCallbacks(updateGpioAdcConfigValues);
            }
            if (ifGpioAbsConfig){
                gpioAbsHandler.removeCallbacks(updateGpioAbsConfigValues);
            }
            if(ifGyroPeakConfig && ifAccConfig && ifGyroConfig && ifOrientationConfig){
                gaitHandler.removeCallbacks(updateGaitParameters);
            }
            //show the dialog after clicking the stop button at the bottom
            final AlertDialog resetDialog = new AlertDialog.Builder(owner)
                    .setTitle(R.string.title_cleanup)
                    .setView(R.layout.indeterminate_task)
                    .create();
            resetDialog.show();
            ((TextView) resetDialog.findViewById(R.id.message)).setText(R.string.message_reset_devices);
            //TODO: reset the accelerometer fragment in a new task here
            List<Task<Void>> tasks = new ArrayList<>();
            for(MetaWearBoard m: binder.metawears) {
                if (m.isConnected()) {
                    tasks.add(m.getModule(Debug.class).resetAsync());
                }
            }
            for (Thread t: allThreads){
                t.interrupt();
            }
            Task.whenAll(tasks).continueWithTask(ignored -> {
                for(DataHandler it: binder.dataHandlers) {
                    if(!it.getIdentifier().equals("orientation") &&!it.getIdentifier().equals("resample")
                            || it.getIdentifier().equals("raw-mag")){
                        it.stop();
                    }
                }



                for(Pair<MetaBaseDevice, Map<SensorConfig, Route>> it: binder.parameter.devices) {
                    it.first.isDiscovered = false;
                    activityBus.removeMetaWearBoard(it.first.btDevice);
                }
                for(DataHandler it: binder.dataHandlers) {
                    if(it.getIdentifier().equals("orientation") || it.getIdentifier().equals("resample")){
                        it.stop();
                    }
                }

                if (BuildConfig.LOG_EVENT) {
                    Bundle bundle = new Bundle();
                    bundle.putLong(FIREBASE_PARAM_LOG_DURATION, (stop - binder.start) / 1000L);
                    for (Map.Entry<MetaBaseDevice, List<Pair<SensorConfig, DataHandler.SampleCountDataHandler>>> it : binder.samples.entrySet()) {
                        for (Pair<SensorConfig, DataHandler.SampleCountDataHandler> it2 : it.getValue()) {
                            //set sensor name and samples read
                            bundle.putInt(getString(it2.first.nameResId).toLowerCase().replaceAll(" ", "_"), it2.second.samples);
                        }
                    }
                }

                //ask the users to give a name to the session
                final Capture<EditText> sessionName = new Capture<>();
                final Capture<TextInputLayout> sessionNameTextWrapper = new Capture<>();
                TaskCompletionSource<String> sessionNameTaskSource = new TaskCompletionSource<>();
                owner.runOnUiThread(() -> {
                    resetDialog.dismiss();
                    final AlertDialog sessionDialog = new AlertDialog.Builder(owner)
                            .setTitle(R.string.title_session_name)
                            .setView(R.layout.dialog_item_naming)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    sessionDialog.show();

                    ((TextView) sessionDialog.findViewById(R.id.instructions_text)).setText(R.string.instruction_name_session);
                    sessionName.set(sessionDialog.findViewById(R.id.item_name));
                    sessionNameTextWrapper.set(sessionDialog.findViewById(R.id.item_name_wrapper));

                    sessionDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                        String customName = sessionName.get().getText().toString();
                        if (customName.contains("_")) {
                            sessionNameTextWrapper.get().setError(owner.getString(R.string.error_underscore));
                        } else {
                            sessionNameTextWrapper.get().setError(null);
                            sessionDialog.dismiss();

                            sessionNameTaskSource.setResult(customName);
                        }
                    });
                });
                return sessionNameTaskSource.getTask();
            }).continueWith(task -> {
                String name = task.getResult();
                binder.session.name = name.length() == 0 ?
                        String.format(Locale.US, "%sSession %d", binder.parameter.devices.size() > 1 ? binder.parameter.name + " ": "", binder.parameter.sessions.size() + 1) :
                        name;

                String[] paths = new String[binder.session.files.size()];
                int i = 0;
                List<File> renamed = new ArrayList<>();
                for(File it: binder.session.files) {
                    File newName = new File(it.getParent(), String.format(Locale.US, "%s_%s", binder.session.name, it.getName()));
                    if (it.renameTo(newName)) {
                        renamed.add(newName);
                        paths[i] = newName.getAbsolutePath();
                    } else {
                        renamed.add(it);
                        paths[i] = it.getAbsolutePath();
                    }
                    i++;
                }

                MediaScannerConnection.scanFile(owner, paths, null, null);


                //calculate gait parameters
                float stride = 0f;
                int device_num = 0;
                float stance = 0f;
                float swing = 0f;
                float totalTime = 0f;
                float heelStrike = 0f;
                float toeOff = 0f;
                int numStep = 0;
                float doubleSupportTime = 0f;
                float strideNum = 0f;
                float strideLen = 0f, walkingSpeed = 0f, MFC = 0f;
                float oXMax = 0f, oXMin = 0f, oYMax = 0f, oYMin = 0f, oZMax = 0f, oZMin = 0f;


                for(Pair<MetaBaseDevice, MetaBaseDeviceData> m: binder.metaBaseDeviceDataList){
                    stride+= m.second.getStride();
                    stance += m.second.getAvgStance();
                    swing += m.second.getAvgSwing();
                    totalTime += m.second.getTotalTime();
                    heelStrike += m.second.getAvgHeelStrike();
                    toeOff += m.second.getAvgToeOff();
                    numStep += (int) m.second.getTotalStride() / 2 ;
                    float dsst = (m.second.getStride() / 2) - m.second.getAvgSwing();
                    doubleSupportTime += dsst;
                    strideLen += m.second.getAvgStrideLen();
                    walkingSpeed += m.second.getAvgWalkingSpeed();
                    MFC += m.second.getAvgMFC();
                    oXMax += m.second.getOXMax();
                    oXMin += m.second.getOXMin();
                    oYMax += m.second.getOYMax();
                    oYMin += m.second.getOYMin();
                    oZMax += m.second.getOZMax();
                    oZMin += m.second.getOZMin();

                    TemperalParameters temperalParameters =  new TemperalParameters(m.second.getStride(),
                            m.second.getAvgSwing(), m.second.getAvgStance(), toeOff,
                            heelStrike, dsst, swing,(m.second.getStride() /  2));
                    SpatialParameters spatialParameters = new SpatialParameters(
                            m.second.getAvgStrideLen(),m.second.getAvgWalkingSpeed(),m.second.getAvgMFC(),(int)m.second.getTotalStride() / 2);
                    AngularParameters angularParameters = new AngularParameters(oXMax, oXMin, oYMax, oYMin,oZMax, oZMin);

                    m.first.summaryItem.temperalParameters = temperalParameters;
                    m.first.summaryItem.spatialParameters = spatialParameters;
                    m.first.summaryItem.angularParameters = angularParameters;
                    m.first.summaryItem.deviceName = m.first.name + " " + m.first.mac;

                    device_num++;
                }
                if(device_num != 0) {
                    stride /= device_num;
                    stance /= device_num;
                    swing /= device_num;
                    totalTime /= device_num;
                    heelStrike /= device_num;
                    toeOff /= device_num;
                    numStep /= device_num;
                    doubleSupportTime /= device_num;
                    strideLen /= device_num;
                    walkingSpeed /= device_num;
                    MFC /= device_num;
                    oXMax /= device_num;
                    oXMin /= device_num;
                    oYMax /= device_num;
                    oYMin /= device_num;
                    oZMax /= device_num;
                    oZMin /= device_num;
                }

                if(totalTime != 0){
                    strideNum = totalTime / stride;
                }
                //cadence = (strideNum/totalTime_Minutes)
                //also display individual (for later)

                binder.session.files.clear();
                binder.session.files.addAll(renamed);
                binder.parameter.sessions.add(0, binder.session);

                p = new SelectedGrouping(binder.parameter.sessions,  binder.parameter.configSessions,binder.parameter.name,
                        binder.parameter.summaryItem) ;

                p.devices = parameter.mDevices;
                p.devicesRunSuccessful = parameter.devicesRunSuccessful;



                TemperalParameters temperalParameters =  new TemperalParameters(stride, swing, stance, toeOff,
                        heelStrike,doubleSupportTime,swing,stride / 2);
                p.summaryItem.temperalParameters = temperalParameters;
                SpatialParameters spatialParameters =  new SpatialParameters(strideLen,walkingSpeed,MFC,numStep);
                p.summaryItem.spatialParameters = spatialParameters;
                AngularParameters angularParameters = new AngularParameters(oXMax, oXMin, oYMax, oYMin,oZMax, oZMin);
                p.summaryItem.angularParameters = angularParameters;

                activityBus.swapFragment(SummaryFragment.class, p);
                return null;
            });
        });
    }

    public ServiceConnection getService(){
        return this;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("start activity", "start activity");

        parameter = (Parameter) activityBus.parameter();
        streamServiceIntent = new Intent(owner, Service.class);
        owner.startService(streamServiceIntent);
        owner.getApplicationContext().bindService(streamServiceIntent, this, Context.BIND_AUTO_CREATE);
        parameter = (Parameter) activityBus.parameter();

        activityBus.popBackstack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        owner.getApplicationContext().unbindService(this);
    }

    @Override
    Integer getMenuGroupResId() {
        return null;
    }


    boolean hasGraphs = false;
    boolean hasGpioGraphs = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (Service.LocalBinder) service;


        Action3<MetaWearBoard, TextView, ImageView> dcHandler = (m, deviceName, alert) -> {
            owner.runOnUiThread(() -> {
                deviceName.setTextColor(Color.argb(0xff, 0xcc, 0x00, 0x00));
                alert.setVisibility(View.VISIBLE);
            });
            final Capture<Integer> delay = new Capture<>(5000);
            Task.forResult(null).continueWhile(() -> !m.isConnected(), ignored -> m.connectAsync().continueWithTask(connTask -> {
                if (connTask.isFaulted()) {
                    return Task.delay(delay.get()).continueWithTask(ignored2 -> {
                        delay.set(Math.min((int) (delay.get() * 1.5), 30000));
                        return m.connectAsync();
                    });
                }
                return connTask;
            })).onSuccess(ignored3 -> {
                deviceName.setTextColor(Color.argb(0xff, 0x66, 0x99, 0x6e));
                alert.setVisibility(View.INVISIBLE);
                return null;
            }, Task.UI_THREAD_EXECUTOR);
        };

        if (!binder.active) {
            Log.d("executed wow1", "executed wow1");
            binder.start(parameter);

            Calendar now = Calendar.getInstance();

            binder.session = new AppState.Session("", String.format(Locale.US, DataHandler.CsvDataHandler.TIMESTAMP_FORMAT, now));

            LinearLayout metrics = getView().findViewById(R.id.metrics);
            LinearLayout graphMetrics = getView().findViewById(R.id.each_device_graphs);


            int count = 0;
            List<AppState.SummaryItem> summaryItemList = new ArrayList<>();
            for (Pair<MetaBaseDevice, Map<SensorConfig, Route>> it : parameter.devices) {
                ifAccConfig = false;
                ifAccMagConfig = false;
                ifMagConfig = false;
                ifGyroConfig = false;
                ifGyroPeakConfig = false;
                ifOrientationConfig = false;
                ifGpioAdcConfig = false;
                ifGpioAbsConfig = false;
                final DataHandler.SampleCountDataHandler sampleCounter = new DataHandler.SampleCountDataHandler();

                //initialize a MetaBaseDeviceData object for the MetaBaseDevice
                MetaBaseDeviceData metaBaseDeviceData = it.first.m;
                metaBaseDeviceData.init();

                sampleCounter.init();
                binder.dataHandlers.add(sampleCounter);
                binder.streamMetrics.add(sampleCounter);
                binder.metaBaseDeviceDataList.add(new Pair<>(it.first, metaBaseDeviceData));

                ConstraintLayout status = (ConstraintLayout) getLayoutInflater().inflate(R.layout.board_status, null);
                ConstraintLayout generalGraphStatus = (ConstraintLayout) getLayoutInflater().inflate(R.layout.general_graph_status, null);
                ConstraintLayout gpioGraphStatus = (ConstraintLayout) getLayoutInflater().inflate(R.layout.general_graph_status, null);
                ConstraintLayout twoGraphsStatus = (ConstraintLayout) getLayoutInflater().inflate(R.layout.two_graphs_status, null);
                ConstraintLayout twoGraphsStatusSecond = (ConstraintLayout) getLayoutInflater().inflate(R.layout.two_graphs_status_second, null);
                //generalGraphStatus.setVisibility(View.GONE);
                LinearLayout eachDeviceAllGraphMetrics = generalGraphStatus.findViewById(R.id.device_graphs);
                LinearLayout eachDeviceGpioGraphMetrics = gpioGraphStatus.findViewById(R.id.device_graphs);



                final TextView deviceName = status.findViewById(R.id.device_name);
                final TextView graphDeviceName = generalGraphStatus.findViewById(R.id.devices_name);
                final TextView gpioGraphDeviceName = gpioGraphStatus.findViewById(R.id.devices_name);
                Button graphs = status.findViewById(R.id.graphsButton);
                Button gpioGraphs = status.findViewById(R.id.gpio_graphs);

                //get list of device name
                ArrayList <String> sensorConfigName = new ArrayList<String>();
                sensorConfigName.add("");

                //check which buttons need to be enabled
                for (SensorConfig sensorConfig: it.second.keySet() ){
                    Log.d("find nameID", "nameID = " +sensorConfig.nameResId);
                    String prefix = sensorConfig.identifier.split("[:|\\[]")[0];
                    if (prefix.equals("acceleration") && sensorConfig.isEnabledGraph) {
                        ifAccConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }
                    else if (prefix.equals("acceleration-mag")&& sensorConfig.isEnabledGraph){
                        ifAccMagConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }
                    else if (prefix.equals("angular-velocity")&& sensorConfig.isEnabledGraph){
                        ifGyroConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }
                    else if (prefix.equals("angular-velocity-peak")&& sensorConfig.isEnabledGraph){
                        ifGyroPeakConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }
                    else if (prefix.equals("orientation")&& sensorConfig.isEnabledGraph){
                        ifOrientationConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }
                    else if (prefix.equals("magnetic-field")&& sensorConfig.isEnabledGraph) {
                        ifMagConfig = true;
                        graphs.setEnabled(true);
                        hasGraphs = true;

                    }

                    else if (prefix.equals("gpio0")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig0 gpioConfig0 = (SensorConfig.GpioConfig0)sensorConfig;
                        if (gpioConfig0.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig0.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }

                    else if (prefix.equals("gpio1")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig1 gpioConfig1 = (SensorConfig.GpioConfig1)sensorConfig;
                        if (gpioConfig1.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig1.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }

                    else if (prefix.equals("gpio2")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig2 gpioConfig2 = (SensorConfig.GpioConfig2)sensorConfig;
                        if (gpioConfig2.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig2.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }

                    else if (prefix.equals("gpio3")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig3 gpioConfig3 = (SensorConfig.GpioConfig3)sensorConfig;
                        if (gpioConfig3.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig3.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }

                    else if (prefix.equals("gpio4")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig4 gpioConfig4 = (SensorConfig.GpioConfig4)sensorConfig;
                        if (gpioConfig4.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig4.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }

                    else if (prefix.equals("gpio5")&& sensorConfig.isEnabledGraph){
                        SensorConfig.GpioConfig5 gpioConfig5 = (SensorConfig.GpioConfig5)sensorConfig;
                        if (gpioConfig5.readMode().equals("adc")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAdcConfig = true;
                        }
                        else if (gpioConfig5.readMode().equals("abs")) {
                            gpioGraphs.setEnabled(true);
                            hasGpioGraphs = true;
                            ifGpioAbsConfig = true;
                        }
                    }







                }




                deviceName.setText(it.first.name);
                //set the general headers of the graphs
                graphDeviceName.setText(it.first.name + " General Graphs");
                gpioGraphDeviceName.setText(it.first.name + " Gpio(s)");


                final ImageView alert = status.findViewById(R.id.alert_reconnecting);

                sampleCounter.sampleCountView = status.findViewById(R.id.sample_count);
                sampleCounter.sampleCountView.setText("0");





                final MetaWearBoard m = activityBus.getMetaWearBoard(it.first.btDevice);
                m.onUnexpectedDisconnect(code -> dcHandler.apply(m, deviceName, alert));
                binder.metawears.add(m);

                List<Pair<SensorConfig, DataHandler.SampleCountDataHandler>> sensorSamples = new ArrayList<>();
                JseMetaWearBoard casted = (JseMetaWearBoard) m;
                File csvDest = new File(AppState.devicesPath, it.first.getFileFriendlyMac());

                boolean ifNeedSecond = false;
                //TODO: define the map to sort Views
                Map <Integer, Pair<Integer, View>> sortingMapGeneral = new HashMap<>();
                Map <Integer, Pair<Integer, View>> sortingMapGpio = new HashMap<>();
                final int[] testCount = {0};
                /*
                if( DataHandler.CsvDataHandler.ifAcc && DataHandler.CsvDataHandler.ifGyro){
                    String filename = String.format(Locale.US, "%s_%s_" + DataHandler.CsvDataHandler.TIMESTAMP_FORMAT + "_%s_%s_%s.csv",
                            it.first.name,
                            "Orientation",
                            now,
                            it.first.getFileFriendlyMac(),
                            "",
                            casted.getFirmware()
                    );
                    File output = new File(csvDest, filename);
                }

                 */
                for (Map.Entry<SensorConfig, Route> it2 : it.second.entrySet()) {

                    String filename = String.format(Locale.US, "%s_%s_" + DataHandler.CsvDataHandler.TIMESTAMP_FORMAT + "_%s_%s_%s.csv",
                            it.first.name,
                            owner.getString(it2.getKey().nameResId),
                            now,
                            it.first.getFileFriendlyMac(),
                            it2.getKey().selectedFreqText(),
                            casted.getFirmware()
                    );
                    File output = new File(csvDest, filename);

                    binder.session.files.add(output);
                    //define handlers for each sensorConfig in each Device
                    final DataHandler.CsvDataHandler csvWriter;



                    ConstraintLayout eachThreeAxisGraphStatus = (ConstraintLayout) getLayoutInflater().inflate(R.layout.each_three_axis_graph, null);
                    try {
                        //ConstraintLayout eachThreeAxisGraphStatus = (ConstraintLayout) getLayoutInflater().inflate(R.layout.each_three_axis_graph, null);
                        DataHandler.SampleCountDataHandler sensorCount = new DataHandler.SampleCountDataHandler();
                        sensorSamples.add(new Pair<>(it2.getKey(), sensorCount));
                        //use DataHandler to read the file
                        /*
                        csvWriter = new DataHandler.CsvDataHandler(new FileOutputStream(output), it2.getValue().generateIdentifier(0), it2.getKey().frequency(m), true, it2.getKey().identifier);

                         */
                        // if (it2.getKey().identifier.equals("orientation")){
                        //csvWriter = new DataHandler.CsvDataHandler(new FileOutputStream(output), it2.getKey().identifier, 50f, true, it2.getKey().identifier);
                        // }
                        //else{
                        csvWriter = new DataHandler.CsvDataHandler(new FileOutputStream(output),  it2.getValue().generateIdentifier(0), it2.getKey().frequency(m), true, it2.getKey().identifier, metaBaseDeviceData);
                        // }
                        //BufferedReader csvReader = new BufferedReader(new FileReader(csvDest));
                        binder.csvWriters.add(csvWriter);
                        csvWriter. init();
                        Log.d("I am init", "I am init");
                        //sampleCounter.init();
                        binder.dataHandlers.add(csvWriter);
                        Log.d("data handler num", "num = " + binder.dataHandlers.size());

                        String prefix = it2.getKey().identifier.split("[:|\\[]")[0];
                        //float max  =  it2.getKey().max;
                        //float min  =  it2.getKey().min;



                        //initialize the corresponding graphs of current SensorConfig

                        //TODO: Accelerometer
                        if (prefix.equals("acceleration") && it2.getKey().isEnabledGraph) {

                            csvWriter.accName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.accName.setText("Accelerometer");
                            csvWriter.accelerometerChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);
                            SensorConfig.AccelerometerConfig accelerometerConfig = (SensorConfig.AccelerometerConfig)it2.getKey();
                            float range = accelerometerConfig.getRange();
                            csvWriter.accelerometerChart.setDragEnabled(true);
                            csvWriter.accelerometerChart.setScaleEnabled(true);
                            csvWriter.accelerometerChart.setDragXEnabled(true);
                            csvWriter.accelerometerChart.setDragYEnabled(true);
                            csvWriter.accelerometerChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "x-axis");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setLabel("x-axis");
                            setX.setDrawValues(false);


                            //Add y-axis values
                            ArrayList<Entry> yAxisValues = new ArrayList<>();
                            yAxisValues.add(new Entry(0, 0f));
                            LineDataSet setY = new LineDataSet(yAxisValues, "y-axis");
                            dataSets.add(setY);
                            setY.setDrawCircles(false);
                            setY.setLineWidth(2.0f);
                            setY.setColor(Color.RED);
                            setY.setLabel("y-axis");
                            setY.setDrawValues(false);

                            //Add z-axis values
                            ArrayList<Entry> zAxisValues = new ArrayList<>();
                            zAxisValues.add(new Entry(0, 0f));
                            LineDataSet setZ = new LineDataSet(zAxisValues, "z-axis");
                            dataSets.add(setZ);
                            setZ.setDrawCircles(false);
                            setZ.setLineWidth(2.0f);
                            setZ.setColor(Color.GREEN);
                            //setZ.setLabel("z-axis");
                            setZ.setDrawValues(false);

                            LineData data = new LineData();
                            data.addDataSet(setX);
                            data.addDataSet(setY);
                            data.addDataSet(setZ);

                            //data.setDrawValues(true);
                            csvWriter.accelerometerChart.setData(data);


                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.accelerometerChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.accelerometerChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            csvWriter.accelerometerChart.getDescription().setEnabled(false);

                            XAxis xAxis = csvWriter.accelerometerChart.getXAxis();


                            YAxis leftAxis = csvWriter.accelerometerChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();

                            leftAxis.setAxisMaximum(range);
                            leftAxis.setAxisMinimum(-range);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            leftAxis.setDrawLabels(true);
                            csvWriter.accelerometerChart.getAxisRight().setEnabled(false);



                            csvWriter.accChartData =  csvWriter.accelerometerChart.getData();
                            csvWriter.accelerometerChart.setVisibleXRangeMinimum(1);



                            csvWriter.accelerometerChart.notifyDataSetChanged();
                            binder.accConfigHandlers.add(csvWriter);
                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));


                        }

                        //TODO: Accelerometer-Mag
                        else if (prefix.equals("acceleration-mag")&& it2.getKey().isEnabledGraph) {

                            csvWriter.accMagName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.accMagName.setText("Acc-Mag");
                            csvWriter.accMagChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);
                            SensorConfig.AccelerometerMagConfig accelerometerMagConfig = (SensorConfig.AccelerometerMagConfig)it2.getKey();
                            float range = accelerometerMagConfig.getRange();
                            csvWriter.accMagChart.setDragEnabled(true);
                            csvWriter.accMagChart.setScaleEnabled(true);
                            csvWriter.accMagChart.setDragXEnabled(true);
                            csvWriter.accMagChart.setDragYEnabled(true);
                            csvWriter.accMagChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "acc-mag value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);




                            LineData data = new LineData();
                            data.addDataSet(setX);


                            //data.setDrawValues(true);
                            csvWriter.accMagChart.setData(data);


                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.accMagChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.accMagChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            csvWriter.accMagChart.getDescription().setEnabled(false);

                            XAxis xAxis = csvWriter.accMagChart.getXAxis();


                            YAxis leftAxis = csvWriter.accMagChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();

                            leftAxis.setAxisMaximum(range);
                            leftAxis.setAxisMinimum(-range);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            leftAxis.setDrawLabels(true);
                            csvWriter.accMagChart.getAxisRight().setEnabled(false);



                            csvWriter.accMagChartData =  csvWriter.accMagChart.getData();
                            csvWriter.accMagChart.setVisibleXRangeMinimum(1);



                            csvWriter.accMagChart.notifyDataSetChanged();
                            binder.accMagConfigHandlers.add(csvWriter);
                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));


                        }

                        //TODO: Magnetometer
                        else if (prefix.equals("magnetic-field")&& it2.getKey().isEnabledGraph) {

                            csvWriter.magName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.magName.setText("Magnetometer");
                            csvWriter.magnetometerChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.magnetometerChart.setDragEnabled(true);
                            csvWriter.magnetometerChart.setScaleEnabled(true);
                            csvWriter.magnetometerChart.setDragXEnabled(true);
                            csvWriter.magnetometerChart.setDragYEnabled(true);
                            csvWriter.magnetometerChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "x-axis");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            //Add y-axis values
                            ArrayList<Entry> yAxisValues = new ArrayList<>();
                            yAxisValues.add(new Entry(0, 0f));
                            LineDataSet setY = new LineDataSet(yAxisValues, "y-axis");
                            dataSets.add(setY);
                            setY.setDrawCircles(false);
                            setY.setLineWidth(2.0f);
                            setY.setColor(Color.RED);
                            setY.setDrawValues(false);

                            //Add z-axis values
                            ArrayList<Entry> zAxisValues = new ArrayList<>();
                            zAxisValues.add(new Entry(0, 0f));
                            LineDataSet setZ = new LineDataSet(zAxisValues, "z-axis");
                            dataSets.add(setZ);
                            setZ.setDrawCircles(false);
                            setZ.setLineWidth(2.0f);
                            setZ.setColor(Color.GREEN);
                            setZ.setDrawValues(false);



                            LineData data = new LineData();
                            data.addDataSet(setX);
                            data.addDataSet(setY);
                            data.addDataSet(setZ);
                            //data.setDrawValues(true);
                            csvWriter.magnetometerChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.magnetometerChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.magnetometerChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);

                            YAxis leftAxis = csvWriter.magnetometerChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(2500f);
                            leftAxis.setAxisMinimum(-2500f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.magnetometerChart.getAxisRight().setEnabled(false);


                            csvWriter.magChartData =  csvWriter.magnetometerChart.getData();
                            csvWriter.magnetometerChart.setVisibleXRangeMinimum(1);



                            // if(currentSensorConfigNum == 0 || currentSensorConfigNum == 1){

                            //}
                            //else{
                            //twoGraphsMetricsSecond.addView(eachThreeAxisGraphStatus);
                            //ifNeedSecond = true;
                            // }

                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.magnetometerChart.notifyDataSetChanged();


                            binder.magConfigHandlers.add(csvWriter);
                            //add to sortingMapGeneral
                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }


                        //TODO: Gyroscope
                        else if (prefix.equals("angular-velocity")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gyroName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gyroName.setText("Gyroscope");
                            csvWriter.gyroscopeChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gyroscopeChart.setDragEnabled(true);
                            csvWriter.gyroscopeChart.setScaleEnabled(true);
                            csvWriter.gyroscopeChart.setDragXEnabled(true);
                            csvWriter.gyroscopeChart.setDragYEnabled(true);
                            csvWriter.gyroscopeChart.setSaveEnabled(true);

                            SensorConfig.GyroConfig gyroConfig = (SensorConfig.GyroConfig)it2.getKey();
                            float range = gyroConfig.getRange();
                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "x-axis");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(1.5f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            //Add y-axis values
                            ArrayList<Entry> yAxisValues = new ArrayList<>();
                            yAxisValues.add(new Entry(0, 0f));
                            LineDataSet setY = new LineDataSet(yAxisValues, "y-axis");
                            dataSets.add(setY);
                            setY.setDrawCircles(false);
                            setY.setLineWidth(1.5f);
                            setY.setColor(Color.RED);
                            setY.setDrawValues(false);

                            //Add z-axis values
                            ArrayList<Entry> zAxisValues = new ArrayList<>();
                            zAxisValues.add(new Entry(0, 0f));
                            LineDataSet setZ = new LineDataSet(zAxisValues, "z-axis");
                            dataSets.add(setZ);
                            setZ.setDrawCircles(false);
                            setZ.setLineWidth(1.5f);
                            setZ.setDrawValues(false);
                            setZ.setColor(Color.GREEN);
                            setZ.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);
                            data.addDataSet(setY);
                            data.addDataSet(setZ);
                            //data.setDrawValues(true);
                            csvWriter.gyroscopeChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gyroscopeChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gyroscopeChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gyroscopeChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(range);
                            leftAxis.setAxisMinimum(-range);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gyroscopeChart.getAxisRight().setEnabled(false);


                            csvWriter.gyroChartData =  csvWriter.gyroscopeChart.getData();
                            csvWriter.gyroscopeChart.setVisibleXRangeMinimum(1);



                            // if(currentSensorConfigNum == 0 || currentSensorConfigNum == 1){

                            //}
                            //else{
                            //twoGraphsMetricsSecond.addView(eachThreeAxisGraphStatus);
                            //ifNeedSecond = true;
                            // }

                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gyroscopeChart.notifyDataSetChanged();
                            binder.gyroConfigHandlers.add(csvWriter);

                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));


                        }
                        //TODO: Accelerometer-Mag
                        else if (prefix.equals("angular-velocity-peak")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gyroPeakName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gyroPeakName.setText("Gyro-Peak");
                            csvWriter.gyroPeakChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);
                            SensorConfig.GyroConfigPeak gyroConfigPeak = (SensorConfig.GyroConfigPeak)it2.getKey();
                            float range = gyroConfigPeak.getRange();
                            csvWriter.gyroPeakChart.setDragEnabled(true);
                            csvWriter.gyroPeakChart.setScaleEnabled(true);
                            csvWriter.gyroPeakChart.setDragXEnabled(true);
                            csvWriter.gyroPeakChart.setDragYEnabled(true);
                            csvWriter.gyroPeakChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "y-axis");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(1.5f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            //Add y-axis values
                            ArrayList<Entry> yAxisValues = new ArrayList<>();
                            yAxisValues.add(new Entry(0, 0f));
                            LineDataSet setY = new LineDataSet(yAxisValues, "y-axis filtered data");
                            dataSets.add(setY);
                            setY.setDrawCircles(false);
                            setY.setLineWidth(1.5f);
                            setY.setColor(Color.RED);
                            setY.setDrawValues(false);

                            ArrayList<Entry> zAxisValues = new ArrayList<>();
                            zAxisValues.add(new Entry(0, 0f));
                            LineDataSet setZ = new LineDataSet(zAxisValues, "special value");
                            dataSets.add(setZ);
                            setZ.setDrawCircles(true);
                            setZ.setLineWidth(0.0f);
                            setZ.setFormLineWidth(0.0f);
                            setZ.setColor(Color.WHITE);
                            setZ.setDrawValues(false);
                            setZ.setDrawCircles(true);

                            LineData data = new LineData();
                            data.addDataSet(setX);
                            data.addDataSet(setY);
                            data.addDataSet(setZ);
                            //data.setDrawValues(true);
                            csvWriter.gyroPeakChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gyroPeakChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gyroPeakChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gyroPeakChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(range);
                            leftAxis.setAxisMinimum(-range);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gyroPeakChart.getAxisRight().setEnabled(false);


                            csvWriter.gyroPeakChartData =  csvWriter.gyroPeakChart.getData();
                            csvWriter.gyroPeakChart.setVisibleXRangeMinimum(1);



                            // if(currentSensorConfigNum == 0 || currentSensorConfigNum == 1){

                            //}
                            //else{
                            //twoGraphsMetricsSecond.addView(eachThreeAxisGraphStatus);
                            //ifNeedSecond = true;
                            // }

                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gyroPeakChart.notifyDataSetChanged();
                            binder.gyroPeakConfigHandlers.add(csvWriter);

                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));



                        }
                        else if (prefix.equals("orientation") && it2.getKey().isEnabledGraph) {

                            csvWriter.orientationName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.orientationName.setText("Orientation");
                            csvWriter.orientationChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);
                            SensorConfig.Orientation orientation = (SensorConfig.Orientation)it2.getKey();
                            float range = orientation.getRange();
                            csvWriter.orientationChart.setDragEnabled(true);
                            csvWriter.orientationChart.setScaleEnabled(true);
                            csvWriter.orientationChart.setDragXEnabled(true);
                            csvWriter.orientationChart.setDragYEnabled(true);
                            csvWriter.orientationChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "x-axis");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setLabel("x-axis");
                            setX.setDrawValues(false);


                            //Add y-axis values
                            ArrayList<Entry> yAxisValues = new ArrayList<>();
                            yAxisValues.add(new Entry(0, 0f));
                            LineDataSet setY = new LineDataSet(yAxisValues, "y-axis");
                            dataSets.add(setY);
                            setY.setDrawCircles(false);
                            setY.setLineWidth(2.0f);
                            setY.setColor(Color.RED);
                            setY.setLabel("y-axis");
                            setY.setDrawValues(false);

                            //Add z-axis values
                            ArrayList<Entry> zAxisValues = new ArrayList<>();
                            zAxisValues.add(new Entry(0, 0f));
                            LineDataSet setZ = new LineDataSet(zAxisValues, "z-axis");
                            dataSets.add(setZ);
                            setZ.setDrawCircles(false);
                            setZ.setLineWidth(2.0f);
                            setZ.setColor(Color.GREEN);
                            //setZ.setLabel("z-axis");
                            setZ.setDrawValues(false);

                            LineData data = new LineData();
                            data.addDataSet(setX);
                            data.addDataSet(setY);
                            data.addDataSet(setZ);

                            //data.setDrawValues(true);
                            csvWriter.orientationChart.setData(data);


                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.orientationChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.orientationChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            csvWriter.orientationChart.getDescription().setEnabled(false);

                            XAxis xAxis = csvWriter.orientationChart.getXAxis();


                            YAxis leftAxis = csvWriter.orientationChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();

                            leftAxis.setAxisMaximum(range);
                            leftAxis.setAxisMinimum(-range);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            leftAxis.setDrawLabels(true);
                            csvWriter.orientationChart.getAxisRight().setEnabled(false);



                            csvWriter.orientationChartData =  csvWriter.orientationChart.getData();
                            csvWriter.orientationChart.setVisibleXRangeMinimum(1);



                            csvWriter.orientationChart.notifyDataSetChanged();
                            binder.orientationConfigHandlers.add(csvWriter);
                            sortingMapGeneral.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));


                        }

                        //TODO: Gpio0
                        else if (prefix.equals("gpio0") &&  it2.getKey().readMode().equals("adc") && it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio0-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);



                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);



                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio0") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio0-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);



                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);



                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }


                        //TODO: Gpio1
                        else if (prefix.equals("gpio1") &&  it2.getKey().readMode().equals("adc")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio1-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);



                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);


                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio1") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {
                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio1-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */


                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        //TODO: Gpio2
                        else if (prefix.equals("gpio2") &&  it2.getKey().readMode().equals("adc")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio2-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */


                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio2") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio2-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);



                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */


                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }



                        //TODO: Gpio3
                        else if (prefix.equals("gpio3") &&  it2.getKey().readMode().equals("adc")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio3-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);


                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio3") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio3-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */


                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        //TODO: Gpio4
                        else if (prefix.equals("gpio4") &&  it2.getKey().readMode().equals("adc")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio4-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);


                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio4") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio4-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */


                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        //TODO: Gpio5
                        else if (prefix.equals("gpio5") &&  it2.getKey().readMode().equals("adc")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio5-adc");
                            csvWriter.gpioAdcChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAdcChart.setDragEnabled(true);
                            csvWriter.gpioAdcChart.setScaleEnabled(true);
                            csvWriter.gpioAdcChart.setDragXEnabled(true);
                            csvWriter.gpioAdcChart.setDragYEnabled(true);
                            csvWriter.gpioAdcChart.setSaveEnabled(true);


                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "adc-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAdcChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAdcChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAdcChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAdcChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(1024);
                            leftAxis.setAxisMinimum(-1024);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAdcChart.getAxisRight().setEnabled(false);


                            csvWriter.gpioAdcChartData =  csvWriter.gpioAdcChart.getData();
                            csvWriter.gpioAdcChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAdcChart.notifyDataSetChanged();
                            binder.gpioAdcConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }

                        else if (prefix.equals("gpio5") &&  it2.getKey().readMode().equals("abs")&& it2.getKey().isEnabledGraph) {

                            csvWriter.gpioName = eachThreeAxisGraphStatus.findViewById(R.id.sensor_config_name);
                            csvWriter.gpioName.setText("Gpio5-abs");
                            csvWriter.gpioAbsChart = (LineChart) eachThreeAxisGraphStatus.findViewById(R.id.data_chart);

                            csvWriter.gpioAbsChart.setDragEnabled(true);
                            csvWriter.gpioAbsChart.setScaleEnabled(true);
                            csvWriter.gpioAbsChart.setDragXEnabled(true);
                            csvWriter.gpioAbsChart.setDragYEnabled(true);
                            csvWriter.gpioAbsChart.setSaveEnabled(true);

                            //Add x-axis values
                            ArrayList<Entry> xAxisValues = new ArrayList<>();
                            xAxisValues.add(new Entry(0, 0f));
                            //yValues.add(new Entry(2, 1.822f));
                            LineDataSet setX = new LineDataSet(xAxisValues, "abs-value");
                            //set1.setFillAlpha(110);
                            ArrayList<LineDataSet> dataSets = new ArrayList<>();
                            dataSets.add(setX);
                            setX.setDrawCircles(false);
                            setX.setLineWidth(2.0f);
                            setX.setColor(Color.BLUE);
                            setX.setDrawValues(false);


                            LineData data = new LineData();
                            data.addDataSet(setX);

                            //data.setDrawValues(true);
                            csvWriter.gpioAbsChart.setData(data);
                            //accSensorConfigHandler.accelerometerChart.setPinchZoom(true);
                            csvWriter.gpioAbsChart.setBackgroundColor(Color.WHITE);
                            //XAxis xAxis = accSensorConfigHandler.accelerometerChart.getXAxis();
                            //xAxis.setAvoidFirstLastClipping(true);
                            Legend l = csvWriter.gpioAbsChart.getLegend();
                            l.setForm(Legend.LegendForm.LINE);
                            l.setTextColor(Color.BLACK);
                            l.setEnabled(true);
                            YAxis leftAxis = csvWriter.gpioAbsChart.getAxisLeft();
                            leftAxis.removeAllLimitLines();
                            leftAxis.setAxisMaximum(3.0f);
                            leftAxis.setAxisMinimum(-3.0f);
                            leftAxis.setDrawLimitLinesBehindData(false);
                            csvWriter.gpioAbsChart.getAxisRight().setEnabled(false);

                            /*
                            data.addEntry(new Entry(2, 2.0f), 0);
                            data.addEntry(new Entry(3, 1.0f), 0);
                            data.addEntry(new Entry(4, 0.7f), 0);
                            data.addEntry(new Entry(5, 0f), 0);
                            data.addEntry(new Entry(6, -1.0f), 0);

                             */



                            csvWriter.gpioAbsChartData =  csvWriter.gpioAbsChart.getData();
                            csvWriter.gpioAbsChart.setVisibleXRangeMinimum(1);


                            //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                            csvWriter.gpioAbsChart.notifyDataSetChanged();
                            binder.gpioAbsConfigHandlers.add(csvWriter);
                            sortingMapGpio.put(it2.getKey().orderKey, Pair.create(it2.getKey().orderKey, eachThreeAxisGraphStatus));

                        }



                        //process the data, either add to file or add to graph
                        it2.getValue().resubscribe(0, (data, env) -> {
                            sampleCounter.process(data);
                            csvWriter.process(data);


                            Log.d("I am executed", "I am executed" + testCount[0]);
                            sensorCount.process(data);
                        });

                    } catch (FileNotFoundException e) {
                        Log.w("metabase", "Failed to create CSV file for sensor [" + owner.getString(it2.getKey().nameResId) + ", " + m.getMacAddress() + "]");
                    }

                    //eachDeviceAllGraphMetrics.addView(eachThreeAxisGraphStatus);
                }




                //sorting algorithms, the order of the graphs is
                //Accelerometer, Magnetometer, Gyroscope, Gpio0 - Gpio5
                ArrayList<Integer> intofKeys = new ArrayList<>();

                ArrayList<Pair<Integer, View>>  viewPairList = new ArrayList<>();

                Collection setofViews = sortingMapGeneral.values();
                intofKeys.addAll(sortingMapGeneral.keySet());
                viewPairList.addAll(setofViews);
                Collections.sort(intofKeys);
                for (int i = 0; i < intofKeys.size(); i ++){
                    if (intofKeys.get(i) == viewPairList.get(i).first){
                        eachDeviceAllGraphMetrics.addView(viewPairList.get(i).second);
                    }
                }

                ArrayList<Integer> intofGpioKeys = new ArrayList<>();

                ArrayList<Pair<Integer, View>>  viewGpioPairList = new ArrayList<>();

                Collection setofGpioViews = sortingMapGpio.values();
                intofGpioKeys.addAll(sortingMapGpio.keySet());
                viewGpioPairList.addAll(setofGpioViews);
                Collections.sort(intofGpioKeys);
                for (int i = 0; i < intofGpioKeys.size(); i ++){
                    if (intofGpioKeys.get(i) == viewGpioPairList.get(i).first){
                        eachDeviceGpioGraphMetrics.addView(viewGpioPairList.get(i).second);
                    }
                }


                binder.samples.put(it.first, sensorSamples);
                //TODO: set up the view for each device (does not affect reading data)
                //add the view to the corresponding View object on the graph page
                if(intofKeys.size() > 0) {
                    graphMetrics.addView(generalGraphStatus);
                }
                if (intofGpioKeys.size() > 0) {
                    graphMetrics.addView(gpioGraphStatus);
                }

                metrics.addView(status);


                //TODO: swap fragment 1th
                //when clicked on the button, move the selected view to the first view,
                // set the selected view to visible, and others to invisible
                graphs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        graphMetrics.removeView(generalGraphStatus);
                        graphMetrics.addView(generalGraphStatus, 0);

                        for (int i = 0; i < graphMetrics.getChildCount(); i++) {
                            graphMetrics.getChildAt(i).setVisibility(View.GONE);
                        }


                        graphMetrics.getChildAt(0).setVisibility(View.VISIBLE);

                    }
                });

                gpioGraphs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        graphMetrics.removeView(gpioGraphStatus);
                        graphMetrics.addView(gpioGraphStatus, 0);

                        for (int i = 0; i < graphMetrics.getChildCount(); i++) {
                            graphMetrics.getChildAt(i).setVisibility(View.GONE);
                        }


                        graphMetrics.getChildAt(0).setVisibility(View.VISIBLE);

                    }
                });

                TemperalParameters temperalParameters = new TemperalParameters();
                SpatialParameters spatialParameters = new SpatialParameters();
                AngularParameters angularParameters = new AngularParameters();
                AppState.SummaryItem summaryItem = new AppState.SummaryItem(temperalParameters, spatialParameters,
                                                    angularParameters);
                summaryItemList.add(summaryItem);
            }

            summaryItemsAdapter.summaryItems.addAll(summaryItemList);
            summaryItemsAdapter.notifyDataSetChanged();



            for (int i = 0; i < parameter.devices.size(); i++) {
                for (Map.Entry<SensorConfig, Route> it2 : parameter.devices.get(i).second.entrySet()) {
                    it2.getKey().start(binder.metawears.get(i));
                }
            }

            binder.start = System.nanoTime();
            uiScheduler.postDelayed(updateValues, 1000L);
            if (ifAccConfig) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        accHandler.postDelayed(updateAccConfigValues, 0L);
                    }
                });
                t.start();
            }
            if (ifAccMagConfig) {
                Thread t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        accMagHandler.postDelayed(updateAccMagConfigValues, 0L);
                    }
                });
                t1.start();
            }
            if (ifMagConfig) {
                Thread t2 = new Thread(new Runnable() {

                    public void run() {
                        magHandler.postDelayed(updateMagConfigValues, 0L);
                    }
                });
                t2.start();

            }
            if (ifGyroConfig) {
                Thread t3 = new Thread(new Runnable() {

                    public void run() {
                        gyroHandler.postDelayed(updateGyroConfigValues, 0L);
                    }
                });
                t3.start();

            }
            if (ifGyroPeakConfig) {
                Thread t3 = new Thread(new Runnable() {

                    public void run() {
                        gyroPeakHandler.postDelayed(updateGyroPeakConfigValues, 0L);
                    }
                });
                t3.start();

            }
            if (ifOrientationConfig) {
                Thread t6 = new Thread(new Runnable() {

                    public void run() {
                        orientationHandler.postDelayed(updateOrientationConfigValues, 0L);
                    }
                });
                t6.start();

            }
            if (ifGpioAdcConfig) {
                Thread t4 = new Thread(new Runnable() {

                    public void run() {
                        gpioAdcHandler.postDelayed(updateGpioAdcConfigValues, 0L);;
                    }
                });
                t4.start();

            }
            if (ifGpioAbsConfig) {
                Thread t5 = new Thread(new Runnable() {

                    public void run() {
                        gpioAbsHandler.postDelayed(updateGpioAbsConfigValues, 0L);
                    }
                });
                t5.start();

            }
            if(ifAccConfig && ifGyroConfig && ifGyroPeakConfig && ifOrientationConfig){
                Thread t6 = new Thread(new Runnable() {

                    public void run() {
                        gaitHandler.postDelayed(updateGaitParameters, 0L);
                    }
                });
                t6.start();
            }
        }
        //if the service needs to be reconnected, execute codes in the "else" statement
        else {
            LinearLayout metrics = getView().findViewById(R.id.metrics);
            int i = 0;
            for (MetaBaseDevice d: binder.samples.keySet()) {
                ConstraintLayout status = (ConstraintLayout) getLayoutInflater().inflate(R.layout.board_status, null);
                final TextView deviceName = status.findViewById(R.id.device_name);
                final ImageView alert = status.findViewById(R.id.alert_reconnecting);

                MetaWearBoard m = activityBus.getMetaWearBoard(d.btDevice);
                m.onUnexpectedDisconnect(code -> dcHandler.apply(m, deviceName, alert));

                deviceName.setText(d.name);
                long elapsed= System.nanoTime() - binder.start;
                elapsedTimeText.setText(String.format(Locale.US, "%02d:%02d:%02d", (elapsed / 3600000000000L) % 24, (elapsed / 60000000000L) % 60, (elapsed / 1000000000L) % 60));

                uiScheduler.postDelayed(updateValues, 1000L);
                if(ifAccConfig) {
                    accHandler.postDelayed(updateAccConfigValues, 0L);
                }
                if(ifAccMagConfig) {
                    accMagHandler.postDelayed(updateAccMagConfigValues, 0L);
                }
                if (ifMagConfig) {
                    magHandler.postDelayed(updateMagConfigValues, 0L);
                }
                if (ifGyroConfig) {
                    gyroHandler.postDelayed(updateGyroConfigValues, 0L);
                }
                if (ifGyroPeakConfig) {
                    gyroPeakHandler.postDelayed(updateGyroPeakConfigValues, 0L);
                }
                if (ifGpioAdcConfig) {
                    gpioAdcHandler.postDelayed(updateGpioAdcConfigValues, 0L);
                }
                if (ifGpioAbsConfig){
                    gpioAbsHandler.postDelayed(updateGpioAbsConfigValues, 0L);
                }
                if(ifAccConfig && ifGyroConfig && ifGyroPeakConfig && ifOrientationConfig){
                    gaitHandler.postDelayed(updateGaitParameters, 0L);
                }

                binder.streamMetrics.get(i).sampleCountView = status.findViewById(R.id.sample_count);
                binder.streamMetrics.get(i).sampleCountView.setText(String.format(Locale.US, "%d", binder.streamMetrics.get(i).samples));
                Button graphs = status.findViewById(R.id.graphsButton);
                Button gpioGraphs = status.findViewById(R.id.gpio_graphs);
                if (hasGraphs){
                    graphs.setEnabled(true);
                }
                if(hasGpioGraphs){
                    gpioGraphs.setEnabled(true);
                }

                metrics.addView(status);


                i++;
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
