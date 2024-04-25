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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DataProducer;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBma255;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBmi270;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.BarometerBme280;
import com.mbientlab.metawear.module.BarometerBmp280;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi270;
import com.mbientlab.metawear.module.HumidityBme280;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import bolts.Task;

abstract class SensorConfig {
    static final String KEY_IMU = "imu", KEY_FUSED = "fused_imu";
    static final Map<String, String> DISJOINT_KEY;

    static {
        DISJOINT_KEY = new HashMap<>();
        DISJOINT_KEY.put(KEY_IMU, KEY_FUSED);
        DISJOINT_KEY.put(KEY_FUSED, KEY_IMU);
    }

    static SensorConfig[] All() {
        return new SensorConfig[]{
                new AccelerometerConfig(),
                new AccelerometerMagConfig(),
               // new AmbientLightConfig(),
                new GyroConfig(),
                new GyroConfigPeak(),
                new Orientation(),
                new Resample(),
                new MagnetometerConfig(),
                //GPIOs
                new GpioConfig0(),
                new GpioConfig1(),
                new GpioConfig2(),
                new GpioConfig3(),
                new GpioConfig4(),
                new GpioConfig5(),
                new LinearAccelerationConfig(),
                new EulerAnglesConfig(),
                new GravityConfig(),
                //new HumidityConfig(),
                new PressureConfig(),
                new QuaternionConfig(),
                //new TemperatureConfig(),

        };
    }

    //TODO: SensorConfig attributes
    final String key, identifier;
    final int imageResId, nameResId, orderKey;
    String outputControl = "";
    //float max, min;
    boolean ifShowFreq, ifShowRange, ifShowPull;
    boolean isStreaming, isEnabled, isEnabledGraph;

    int freqIndex, initialFreqIndex, rangeIndex, readModeIndex, pullModeIndex;




    String readMode;

    //TODO: SensorConfig constructors

    SensorConfig(int imageResId, int nameResId, String identifier, int initialFreqIndex, int orderKey) {
        this(imageResId, nameResId, identifier, null, initialFreqIndex, orderKey);
    }

    SensorConfig(int imageResId, int nameResId, String identifier, String key, int initialFreqIndex, int orderKey) {
        this.imageResId = imageResId;
        this.nameResId = nameResId;
        this.identifier = identifier;
        this.key = key;
        this.initialFreqIndex = initialFreqIndex;
        isStreaming = true;
        this.orderKey = orderKey;
        this.isEnabled = false;


    }

    //TODO: select appropriate frequency
    void setStreaming(boolean isStreaming) {
        this.isStreaming = isStreaming;

        float[] available = frequencies();
        if (available != null && freqIndex >= available.length) {
            freqIndex = available.length - 1;
        }
    }

    String selectedFreqText() {
        return String.format(Locale.US, "%.3fHz", frequencies()[freqIndex]);
    }

    String selectedRangeText() {
        return "";
    }

    String selectedReadModeText() {
        return "";
    }

    String selectedPullModeText() {
        return "";
    }


    RouteComponent setupStream(RouteComponent source) {
        return source.stream(null);
    }

    Task<Route> addRouteAsync(MetaWearBoard metawear) {
        configure(metawear);
        return getProducer(metawear).addRouteAsync(source -> {
            if (isStreaming) {
                //Streams the input data to the local device
                setupStream(source);
            } else {
                //Records the input data to the on-board logger, retrieved when a log download is started
                source.log(null);
            }
        });
    }

    abstract float[] frequencies();

    float[] ranges() {
        return new float[]{};
    }

    String readMode() {
        return "";
    }

    String outputControl() {
        return "";
    }

    String[] pullMode() {
        return new String[]{};
    }


    void setFreqIndex() {
        float[] values = frequencies();
        if (values.length > 1) {
            freqIndex = initialFreqIndex;
        }
    }


    void setInitialRangeIndex() {
        float[] values = ranges();
        if (values.length > 0) {
            rangeIndex = values.length - 1;
        }
    }

    //TODO: set initial readMode Index
    void setInitialReadModeIndex() {
        readModeIndex = 0;
    }

    //TODO: set initial pullMode Index
    void setInitialPullModeIndex() {
        String[] values = pullMode();
        if (values.length > 0) {
            pullModeIndex = 1;
        }
    }


    abstract boolean isValid(MetaWearBoard metawear);

    abstract void configure(MetaWearBoard metawear);

    abstract DataProducer getProducer(MetaWearBoard metawear);

    abstract void start(MetaWearBoard metawear);

    abstract void stop(MetaWearBoard metawear);

    float dataThroughputSum(MetaWearBoard metawear) {
        return frequencies()[freqIndex];
    }

    float frequency(MetaWearBoard metawear) {
        return frequencies()[freqIndex];
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        final SortedMap<Integer, SensorConfig> hidden;
        final List<MetaWearBoard> metawears;
        final List<SensorConfig> items;
        boolean isStreaming = true;
        int changeNum = 0;


        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }

        // Provide a suitable constructor (depends on the kind of data set)
        Adapter() {
            items = new ArrayList<>();
            metawears = new ArrayList<>();
            hidden = new TreeMap<>();
        }

        //check if frequency exceeds maximum frequency
        boolean checkTotalDataThroughput() {
            float sum = 0;

            for (SensorConfig c : items) {
                if (c.isEnabled) {

                    for (MetaWearBoard metawear : metawears) {
                        if (c.isValid(metawear)) {
                            //get frequency
                            sum += c.dataThroughputSum(metawear);
                        }
                    }
                }
            }

            return !(sum > 300f * metawears.size());
        }

        //reset devices
        void reset() {
            metawears.clear();
            hidden.clear();
            items.clear();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.setting, parent, false));
        }

        static boolean isEnableButtonClicked[] = {false};
        static boolean isGraphButtonClicked[] = {false} ;
        static boolean setTrue [] = new boolean[16];
        static boolean setGraphTrue [] = new boolean[16];
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            //access current sensor config
            SensorConfig current = items.get(position);

            //initialize attributes of the current config
            ((ImageView) holder.itemView.findViewById(R.id.sensor_image)).setImageResource(current.imageResId);
            ((TextView) holder.itemView.findViewById(R.id.text_sensor)).setText(current.nameResId);

            //TODO: initialize frequency index
            int freqIndex = current.freqIndex;
            //TextView readModeTxt = (TextView) holder.itemView.findViewById(R.id.read_mode_text);
            Spinner frequencySelector = (Spinner)holder.itemView.findViewById(R.id.frequency_selector);
            TextView frequencyText = (TextView) holder.itemView.findViewById(R.id.freq_text);
            ArrayAdapter<CharSequence> spinnerAdapterFreq;
            /*
            if(current.identifier.equals("angular-velocity-peak") || current.identifier.equals("linear-acceleration")
                || current.identifier.equals("euler-angles") || current.identifier.equals("gravity")){
                ((CheckBox) holder.itemView.findViewById(R.id.enableGraph)).setVisibility(View.GONE);
            }


             */



            if (current.identifier.equals("linear-acceleration") || current.identifier.equals("euler-angles") || current.identifier.equals("gravity")){
                frequencySelector.setVisibility(View.GONE);
            }
            //set frequency selector text for each Sensor
            if (current.identifier.equals("acceleration")){
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.acc_frequency, android.R.layout.simple_spinner_item);
            }
            else if (current.identifier.equals("acceleration-mag")){
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.acc_mag_frequency, android.R.layout.simple_spinner_item);
            }
            else if(current.identifier.equals("magnetic-field")) {
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.mag_frequency, android.R.layout.simple_spinner_item);
            }
            else if (current.identifier.equals("angular-velocity")){
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.gyro_frequency, android.R.layout.simple_spinner_item);
            }
            else if (current.identifier.equals("gpio0")){
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.gpio0_frequency, android.R.layout.simple_spinner_item);
            }
            else if (current.identifier.equals("pressure")){
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.pressure_frequency, android.R.layout.simple_spinner_item);
            }
            else{
                spinnerAdapterFreq = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.gyro_frequency, android.R.layout.simple_spinner_item);
            }

            spinnerAdapterFreq.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            frequencySelector.setAdapter(spinnerAdapterFreq);
            frequencySelector.setSelection(freqIndex);


            //set range to visible if range is available
            if (current.ifShowFreq) {
                frequencySelector.setVisibility(View.VISIBLE);
                frequencyText.setVisibility(View.VISIBLE);
                Log.d("testFreq", "pass");

                frequencySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        current.freqIndex = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
            else {
                //readModeTxt.setVisibility(View.GONE);
                frequencySelector.setVisibility(View.GONE);
                frequencyText.setVisibility(View.GONE);
            }



            //TODO: initialize range index
            int rangeIndex = current.rangeIndex;
            //TextView readModeTxt = (TextView) holder.itemView.findViewById(R.id.read_mode_text);
            Spinner rangeSelector = (Spinner)holder.itemView.findViewById(R.id.range_selector);
            TextView rangeText = (TextView) holder.itemView.findViewById(R.id.range_text);
            ArrayAdapter<CharSequence> spinnerAdapterRange;

            if (current.identifier.equals("acceleration") || current.identifier.equals("acceleration-mag") || current.identifier.equals("angular-velocity") || current.identifier.equals("angular-velocity-peak")){
                rangeSelector.setVisibility(View.VISIBLE);
                rangeText.setVisibility(View.VISIBLE);
                if (current.identifier.equals("acceleration") || current.identifier.equals("acceleration-mag")){
                    spinnerAdapterRange = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.acc_range, android.R.layout.simple_spinner_item);
                    rangeText.setText("g");
                }
                else{
                    spinnerAdapterRange = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.gyro_range, android.R.layout.simple_spinner_item);
                    rangeText.setText("\u00B0/s");
                }
            }
            else{
                rangeSelector.setVisibility(View.GONE);
                rangeText.setVisibility(View.GONE);
                spinnerAdapterRange = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.empty_array, android.R.layout.simple_spinner_item);

            }

            spinnerAdapterRange.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            rangeSelector.setAdapter(spinnerAdapterRange);
            rangeSelector.setSelection(rangeIndex);


            //set range to visible if range is available
            if (current.ifShowRange) {
                rangeSelector.setVisibility(View.VISIBLE);
                rangeText.setVisibility(View.VISIBLE);
                Log.d("testRange", "pass");

                rangeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        current.rangeIndex = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
            else {
                //readModeTxt.setVisibility(View.GONE);
                rangeSelector.setVisibility(View.GONE);
                rangeText.setVisibility(View.GONE);
            }

            //initialize range options


            float[] ranges = current.ranges();





            //TODO: initialize readMode index
            int readModeIn = current.readModeIndex;
            //TextView readModeTxt = (TextView) holder.itemView.findViewById(R.id.read_mode_text);
            Spinner readModeSelector = (Spinner)holder.itemView.findViewById(R.id.read_mode_selector);
            ArrayAdapter<CharSequence> spinnerAdapterRead = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.values_gpio_read_mode, android.R.layout.simple_spinner_item);
            spinnerAdapterRead .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            readModeSelector.setAdapter(spinnerAdapterRead );
            readModeSelector.setSelection(readModeIn);



            //set range to visible if range is available
            if (current.outputControl.equals("On")) {
                // readModeTxt.setVisibility(View.VISIBLE);
                //readModeTxt.setText("Read Mode");
                readModeSelector.setVisibility(View.VISIBLE);
                Log.d("test", "pass");
                //readModeTxt.setText(current.selectedReadModeText());
                //select the range
                readModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        switch (i) {
                            case 0:
                                current.readMode = "adc";
                                current.readModeIndex = 0;
                                break;
                            case 1:
                                current.readMode = "abs";
                                current.readModeIndex = 1;
                                break;

                            default:
                                break;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
            else {
                //readModeTxt.setVisibility(View.GONE);
                readModeSelector.setVisibility(View.GONE);
            }

            //TODO: initialize pull index
            int pullIndex = current.pullModeIndex;
            //TextView readModeTxt = (TextView) holder.itemView.findViewById(R.id.read_mode_text);
            Spinner pullSelector = (Spinner)holder.itemView.findViewById(R.id.pull_selector);
            ArrayAdapter<CharSequence> spinnerAdapterPull;

            if (current.identifier.equals("gpio0") || current.identifier.equals("gpio1") || current.identifier.equals("gpio2")
                    ||  current.identifier.equals("gpio3")|| current.identifier.equals("gpio4")|| current.identifier.equals("gpio5")) {
                pullSelector.setVisibility(View.VISIBLE);
                spinnerAdapterPull = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.gpio_pull, android.R.layout.simple_spinner_item);
            }
            else{
                pullSelector.setVisibility(View.GONE);
                spinnerAdapterPull = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.empty_array, android.R.layout.simple_spinner_item);

            }

            spinnerAdapterPull.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pullSelector.setAdapter(spinnerAdapterPull);
            pullSelector.setSelection(pullIndex);


            //set range to visible if range is available
            if (current.ifShowPull) {
                pullSelector.setVisibility(View.VISIBLE);
                Log.d("testPull", "pass");

                pullSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        current.pullModeIndex = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
            else {
                //readModeTxt.setVisibility(View.GONE);
                pullSelector.setVisibility(View.GONE);
                pullSelector.setVisibility(View.GONE);
            }


            //TODO: initialize pullMode

            String[] pullMode = current.pullMode();




            //TODO: control which part should be shown


            final CheckBox enable = holder.itemView.findViewById(R.id.enable);
            final CheckBox enableGraph = holder.itemView.findViewById(R.id.enableGraph);

            /*
            if(current.isEnabled){
                enable.setChecked(true);
            }

             */


            enable.setOnCheckedChangeListener(null);
            enable.setChecked(current.isEnabled);




            if(setTrue[0] && current.identifier.equals("acceleration")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[0] = false;
            }

            if(setTrue[1] && current.identifier.equals("acceleration-mag")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[1] = false;
            }
            if(setTrue[2] && current.identifier.equals("angular-velocity")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[2] = false;
            }
            if(setTrue[3] && current.identifier.equals("angular-velocity-peak")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[3] = false;
            }
            if(setTrue[4] && current.identifier.equals("magnetic-field")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[4] = false;
            }
            if(setTrue[5] && current.identifier.equals("gpio0")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[5] = false;
            }
            if(setTrue[6] && current.identifier.equals("gpio1")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[6] = false;
            }
            if(setTrue[7] && current.identifier.equals("gpio2")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[7] = false;
            }
            if(setTrue[8] && current.identifier.equals("gpio3")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[8] = false;
            }
            if(setTrue[9] && current.identifier.equals("gpio4")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[9] = false;
            }
            if(setTrue[10] && current.identifier.equals("gpio5")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[10] = false;
            }
            if(setTrue[11] && current.identifier.equals("linear-acceleration")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[11] = false;
            }
            if(setTrue[12] && current.identifier.equals("euler-angles")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[12] = false;
            }
            if(setTrue[13] && current.identifier.equals("gravity")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[13] = false;
            }
            if(setTrue[14] && current.identifier.equals("pressure")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[14] = false;
            }
            if(setTrue[15] && current.identifier.equals("orientation")){
                current.isEnabled = true;
                enable.setChecked(true);
                setTrue[15] = false;
            }


            if(setGraphTrue[0] && current.identifier.equals("acceleration")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[0] = false;
            }

            if(setGraphTrue[1] && current.identifier.equals("acceleration-mag")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[1] = false;
            }
            if(setGraphTrue[2] && current.identifier.equals("angular-velocity")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[2] = false;
            }
            if(setGraphTrue[3] && current.identifier.equals("angular-velocity-peak")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[3] = false;
            }
            if(setGraphTrue[4] && current.identifier.equals("magnetic-field")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[4] = false;
            }
            if(setGraphTrue[5] && current.identifier.equals("gpio0")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[5] = false;
            }
            if(setGraphTrue[6] && current.identifier.equals("gpio1")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[6] = false;
            }
            if(setGraphTrue[7] && current.identifier.equals("gpio2")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[7] = false;
            }
            if(setGraphTrue[8] && current.identifier.equals("gpio3")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[8] = false;
            }
            if(setGraphTrue[9] && current.identifier.equals("gpio4")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[9] = false;
            }
            if(setGraphTrue[10] && current.identifier.equals("gpio5")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[10] = false;
            }
            if(setGraphTrue[11] && current.identifier.equals("linear-acceleration")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[11] = false;
            }
            if(setGraphTrue[12] && current.identifier.equals("euler-angles")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[12] = false;
            }
            if(setGraphTrue[13] && current.identifier.equals("gravity")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[13] = false;
            }
            if(setGraphTrue[14] && current.identifier.equals("pressure")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[14] = false;
            }
            if(setGraphTrue[15] && current.identifier.equals("orientation")){
                current.isEnabledGraph = true;
                //enableGraph.setChecked(true);
                setGraphTrue[15] = false;
            }

            enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                current.isEnabled = isChecked;

                //if streaming, check Total data, else remove
                if (current.isEnabled) {
                    boolean throughputOk = true;
                    if (isStreaming) {
                        throughputOk = checkTotalDataThroughput();
                    }

                    if (throughputOk) {
                        List<SensorConfig> remove = new ArrayList<>();
                        int i = 0;


                        //remove unsuitable sensors
                        for (SensorConfig it : items) {
                            if (DISJOINT_KEY.containsKey(current.key) && DISJOINT_KEY.get(current.key).equals(it.key)) {
                                hidden.put(i, it);
                                remove.add(it);
                            }
                            i++;
                        }

                        for (SensorConfig it : remove) {
                            items.remove(it);
                        }

                        //ensure the frequency bound is not passed
                        holder.itemView.post(this::notifyDataSetChanged);
                    } else {
                        new AlertDialog.Builder(holder.itemView.getContext())
                                .setTitle(R.string.title_error)
                                .setMessage(R.string.message_data_throughput)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    current.isEnabled = false;
                                    holder.itemView.post(() -> notifyItemChanged(holder.getAdapterPosition()));
                                })
                                .show();
                    }

                    //if current is not enabled
                } else {
                    isEnableButtonClicked[0] = true;
                    boolean remaining = false;
                    for (SensorConfig it : items) {
                        remaining |= it.isEnabled && it.key.equals(current.key);
                    }

                    if (!remaining) {
                        for (SortedMap.Entry<Integer, SensorConfig> it : hidden.entrySet()) {
                            items.add(it.getKey(), it.getValue());
                        }
                        hidden.clear();

                        holder.itemView.post(this::notifyDataSetChanged);
                    }
                }
            });



            //TODO: control which part should be shown


            enableGraph.setOnCheckedChangeListener(null);
            enableGraph.setChecked(current.isEnabledGraph);

            //enable all graphs automatically
            /*
            if(current.key.equals(KEY_IMU) && !isGraphButtonClicked[0]){
                enableGraph.setChecked(true);
                current.isEnabledGraph = true;
            }


             */

            enableGraph.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (enableGraph.isChecked()){
                        current.isEnabledGraph = true;
                    }
                    else {
                        current.isEnabledGraph = false;
                        isGraphButtonClicked[0] = true;
                    }
                }
            });


        }



        @Override
        public int getItemCount() {
            return items.size();
        }

        //add valid SensorConfigs
        void populate() {
            for (SensorConfig c : SensorConfig.All()) {
                boolean valid = false;
                for (MetaWearBoard m : metawears) {
                    valid |= c.isValid(m);
                }

                if (valid) {
                    items.add(c);
                }
            }
            for (SensorConfig c : items) {
                c.setFreqIndex();
                c.setInitialRangeIndex();
                c.setInitialReadModeIndex();
                c.setInitialPullModeIndex();
            }

            notifyDataSetChanged();
        }

        //set the streaming status for the (context of) Adapter
        void setStreaming(boolean isStreaming) {
            this.isStreaming = isStreaming;

            for (SensorConfig c : items) {
                c.setStreaming(isStreaming);
            }

            notifyDataSetChanged();
        }
    }

    abstract static class ActiveSensorConfig extends SensorConfig {
        ActiveSensorConfig(int imageResId, int nameResId, String identifier, int initialFreqIndex, int orderKey) {
            super(imageResId, nameResId, identifier, initialFreqIndex, orderKey);
        }

        ActiveSensorConfig(int imageResId, int nameResId, String identifier, String key, int initialFreqIndex, int orderKey) {
            super(imageResId, nameResId, identifier, key, initialFreqIndex, orderKey);
        }

        @Override
        void start(MetaWearBoard metawear) {
            //Starts data creation
            ((AsyncDataProducer) getProducer(metawear)).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            ((AsyncDataProducer) getProducer(metawear)).stop();
        }
    }

    static class AccelerometerConfig extends ActiveSensorConfig {
        private static final float[] BMI160 = new float[]{12.5f, 25f, 50f, 100f, 200f, 400f, 800f},
                BMI270 = new float[]{12.5f, 25f, 50f, 100f, 200f, 400f, 800f},
                MMA8452Q = new float[]{1.56f, 6.25f, 12.5f, 50f, 100f, 200f, 400f, 800f},
                BMA255 = new float[]{15.62f, 31.26f, 62.5f, 125f, 250f, 500f},
                BOSCH_RANGE = new float[]{2f, 4f, 8f, 16f},
                MMA_RANGE = new float[]{2f, 4f, 8f, 16f};

        private float[] frequencies, ranges;




        AccelerometerConfig() {
            super(R.mipmap.ic_accelerometer, R.string.sensor_accelerometer, "acceleration", KEY_IMU, 2, 0);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = true;
            ifShowPull = false;
            //max = ranges[rangeIndex];
            //min = - ranges[rangeIndex];
        }


        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0fg", ranges()[rangeIndex]);
        }




        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }


        float getRange (){
            return ranges()[rangeIndex];
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            //check the module type of AccelerometerBmi160
            Accelerometer acc = metawear.getModule(Accelerometer.class);
            if (acc instanceof AccelerometerBmi160) {
                frequencies = BMI160;
                ranges = BOSCH_RANGE;

            } else if (acc instanceof AccelerometerBmi270) {
                frequencies = BMI270;
                ranges = BOSCH_RANGE;
            } else if (acc instanceof AccelerometerBma255) {
                frequencies = frequencies == null || frequencies == BMA255 ? BMA255 : BMI160;
                ranges = BOSCH_RANGE;
            } else if (acc instanceof AccelerometerMma8452q) {
                frequencies = frequencies == null || frequencies == MMA8452Q ? MMA8452Q : BMI160;
                ranges = ranges == null || frequencies == MMA_RANGE ? MMA_RANGE : BOSCH_RANGE;
            } else {
                return false;
            }
            return true;
        }



        @Override
            //start to configure
        void configure(MetaWearBoard metawear) {

            metawear.getModule(Accelerometer.class).configure()
                    .odr(frequencies()[freqIndex])
                    .range(ranges[rangeIndex])
                    .commit();
        }

        @Override
            //Get an implementation of the Accelerometer.AccelerationDataProducer interface
            // Returns: AccelerationDataProducer object
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(Accelerometer.class).acceleration();
        }

        @Override
            //Get the output data rate. The returned value is only meaningful if the API has configured the sensor
            //Returns: Selected output data rate
        float dataThroughputSum(MetaWearBoard metawear) {
            //frequency should not exceed twice as the number of devices
            return metawear.getModule(Accelerometer.class).getOdr() / 2f;
        }

        @Override
        float frequency(MetaWearBoard metawear) {
            return metawear.getModule(Accelerometer.class).getOdr();
        }

        @Override
            //Switch the accelerometer into active mode
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(Accelerometer.class).start();
        }

        @Override
            //Switch the accelerometer into standby mode
        void stop(MetaWearBoard metawear) {
            metawear.getModule(Accelerometer.class).stop();
            super.stop(metawear);
        }

        @Override
            //Streams the input data to the local device
            //Params: subscriber – Subscriber to handle the received data
            //Returns: Calling object
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }

    static class AccelerometerMagConfig extends ActiveSensorConfig {
        private static final float[] BMI160 = new float[]{12.5f, 25f, 50f, 100f, 200f, 400f, 800f},
                BMI270 = new float[]{12.5f, 25f, 50f, 100f, 200f, 400f, 800f},
                MMA8452Q = new float[]{1.56f, 6.25f, 12.5f, 50f, 100f, 200f, 400f, 800f},
                BMA255 = new float[]{15.62f, 31.26f, 62.5f, 125f, 250f, 500f},
                BOSCH_RANGE = new float[]{2f, 4f, 8f, 16f},
                MMA_RANGE = new float[]{2f, 4f, 8f, 16f};

        private float[] frequencies, ranges;




        AccelerometerMagConfig() {
            super(R.mipmap.ic_accelerometer, R.string.sensor_acc_mag, "acceleration-mag", KEY_IMU, 2, 1);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = true;
            ifShowPull = false;

            //max = ranges[rangeIndex];
            //min = - ranges[rangeIndex];
        }


        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0fg", ranges()[rangeIndex]);
        }




        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }


        float getRange (){
            return ranges()[rangeIndex];
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            //check the module type of AccelerometerBmi160
            Accelerometer acc = metawear.getModule(Accelerometer.class);
            if (acc instanceof AccelerometerBmi160) {
                frequencies = BMI160;
                ranges = BOSCH_RANGE;

            } else if (acc instanceof AccelerometerBmi270) {
                frequencies = BMI270;
                ranges = BOSCH_RANGE;
            } else if (acc instanceof AccelerometerBma255) {
                frequencies = frequencies == null || frequencies == BMA255 ? BMA255 : BMI160;
                ranges = BOSCH_RANGE;
            } else if (acc instanceof AccelerometerMma8452q) {
                frequencies = frequencies == null || frequencies == MMA8452Q ? MMA8452Q : BMI160;
                ranges = ranges == null || frequencies == MMA_RANGE ? MMA_RANGE : BOSCH_RANGE;
            } else {
                return false;
            }
            return true;
        }



        @Override
            //start to configure
        void configure(MetaWearBoard metawear) {

            metawear.getModule(Accelerometer.class).configure()
                    .odr(frequencies()[freqIndex])
                    .range(ranges[rangeIndex])
                    .commit();
        }

        @Override
            //Get an implementation of the Accelerometer.AccelerationDataProducer interface
            // Returns: AccelerationDataProducer object
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(Accelerometer.class).acceleration();
        }

        @Override
            //Get the output data rate. The returned value is only meaningful if the API has configured the sensor
            //Returns: Selected output data rate
        float dataThroughputSum(MetaWearBoard metawear) {
            //frequency should not exceed twice as the number of devices
            return metawear.getModule(Accelerometer.class).getOdr() / 2f;
        }

        @Override
        float frequency(MetaWearBoard metawear) {
            return metawear.getModule(Accelerometer.class).getOdr();
        }

        @Override
            //Switch the accelerometer into active mode
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(Accelerometer.class).start();
        }

        @Override
            //Switch the accelerometer into standby mode
        void stop(MetaWearBoard metawear) {
            metawear.getModule(Accelerometer.class).stop();
            super.stop(metawear);
        }

        @Override
            //Streams the input data to the local device
            //Params: subscriber – Subscriber to handle the received data
            //Returns: Calling object
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }

    static class GyroConfig extends ActiveSensorConfig {
        private float[] frequencies, ranges;

        GyroConfig() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_gyroscope, "angular-velocity", KEY_IMU, 1, 2);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = true;
            ifShowPull = false;


            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            ranges = new float[]{125f, 250f, 500f, 1000f, 2000f};
        }

        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0f\u00B0/s", ranges()[rangeIndex]);
        }

        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }

        float getRange (){
            return ranges()[rangeIndex];
        }


        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(Gyro.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            gyro.configure()
                    .odr(Gyro.OutputDataRate.values()[freqIndex])
                    .range(Gyro.Range.values()[ranges.length - rangeIndex - 1])
                    .commit();
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            if (gyro instanceof GyroBmi270) {
                return metawear.getModule(GyroBmi270.class).angularVelocity();
            } else {
                return metawear.getModule(GyroBmi160.class).angularVelocity();
            }
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(Gyro.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            metawear.getModule(Gyro.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }


    static class GyroConfigPeak extends ActiveSensorConfig {
        private float[] frequencies, ranges;



        GyroConfigPeak() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_gyro_peak, "angular-velocity-peak", KEY_IMU, 1, 3);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = true;
            ifShowPull = false;


            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            ranges = new float[]{125f, 250f, 500f, 1000f, 2000f};
        }

        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0f\u00B0/s", ranges()[rangeIndex]);
        }

        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }

        float getRange (){
            return ranges()[rangeIndex];
        }


        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(Gyro.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            gyro.configure()
                    .odr(Gyro.OutputDataRate.values()[freqIndex])
                    .range(Gyro.Range.values()[ranges.length - rangeIndex - 1])
                    .commit();
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            if (gyro instanceof GyroBmi270) {
                return metawear.getModule(GyroBmi270.class).angularVelocity();
            } else {
                return metawear.getModule(GyroBmi160.class).angularVelocity();
            }
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(Gyro.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            metawear.getModule(Gyro.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }


    static class Orientation extends ActiveSensorConfig {
        private float[] frequencies, ranges;

        Orientation() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_orientation, "orientation", KEY_IMU, 1, 11);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = false;
            ifShowRange = false;
            ifShowPull = false;


            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            ranges = new float[]{125f, 250f, 500f, 1000f, 2000f};
        }

        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0f\u00B0/s", ranges()[rangeIndex]);
        }

        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }

        float getRange (){
            return ranges()[1];
        }


        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(Gyro.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            gyro.configure()
                    .odr(Gyro.OutputDataRate.values()[freqIndex])
                    .range(Gyro.Range.values()[ranges.length - rangeIndex - 1])
                    .commit();
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            if (gyro instanceof GyroBmi270) {
                return metawear.getModule(GyroBmi270.class).angularVelocity();
            } else {
                return metawear.getModule(GyroBmi160.class).angularVelocity();
            }
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            //metawear.getModule(Gyro.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            //metawear.getModule(Gyro.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }

    static class Resample extends ActiveSensorConfig {
        private float[] frequencies, ranges;

        Resample() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_resample, "resample", KEY_IMU, 1, 2);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = false;
            ifShowRange = false;
            ifShowPull = false;


            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            ranges = new float[]{125f, 250f, 500f, 1000f, 2000f};
        }

        @Override
        String selectedRangeText() {
            return String.format(Locale.US, "%.0f\u00B0/s", ranges()[rangeIndex]);
        }

        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        @Override
        float[] ranges() {
            return ranges;
        }

        float getRange (){
            return ranges()[rangeIndex];
        }


        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(Gyro.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            gyro.configure()
                    .odr(Gyro.OutputDataRate.values()[freqIndex])
                    .range(Gyro.Range.values()[ranges.length - rangeIndex - 1])
                    .commit();
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gyro gyro = metawear.getModule(Gyro.class);
            if (gyro instanceof GyroBmi270) {
                return metawear.getModule(GyroBmi270.class).angularVelocity();
            } else {
                return metawear.getModule(GyroBmi160.class).angularVelocity();
            }
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            //metawear.getModule(Gyro.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            //metawear.getModule(Gyro.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }



    static class MagnetometerConfig extends ActiveSensorConfig {
        //initialization
        MagnetometerConfig() {
            super(R.mipmap.ic_magnetometer, R.string.sensor_magnetometer, "magnetic-field", KEY_IMU, 3, 4);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = false;

        }

        @Override
        float[] frequencies() {
            return new float[]{10f, 15f, 20f, 25f, 30f};
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(MagnetometerBmm150.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            MagnetometerBmm150 mag = metawear.getModule(MagnetometerBmm150.class);
            MagnetometerBmm150.OutputDataRate odr;

            switch (freqIndex) {
                case 0:
                    odr = MagnetometerBmm150.OutputDataRate.ODR_10_HZ;
                    break;
                case 1:
                    odr = MagnetometerBmm150.OutputDataRate.ODR_15_HZ;
                    break;
                case 2:
                    odr = MagnetometerBmm150.OutputDataRate.ODR_20_HZ;
                    break;
                case 3:
                    odr = MagnetometerBmm150.OutputDataRate.ODR_25_HZ;
                    break;
                case 4:
                    odr = MagnetometerBmm150.OutputDataRate.ODR_30_HZ;
                    break;

                default:
                    odr = null;
                    break;
            }

            mag.configure()
                    .outputDataRate(odr)
                    .commit();

        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(MagnetometerBmm150.class).magneticField();
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies()[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(MagnetometerBmm150.class).start();
        }




        @Override
        void stop(MetaWearBoard metawear) {
            metawear.getModule(MagnetometerBmm150.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.pack((byte) 2).account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }

    static abstract class SensorFusionConfig extends ActiveSensorConfig {
        SensorFusionConfig(int imageResId, int nameResId, String identifier, int initialFreqIndex) {
            super(imageResId, nameResId, identifier, KEY_FUSED, initialFreqIndex, -1);
        }

        @Override
        float[] frequencies() {
            return new float[]{100f};
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(SensorFusionBosch.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            metawear.getModule(SensorFusionBosch.class).configure()
                    .accRange(SensorFusionBosch.AccRange.AR_16G)
                    .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                    .mode(SensorFusionBosch.Mode.NDOF)
                    .commit();
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);
            metawear.getModule(SensorFusionBosch.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            metawear.getModule(SensorFusionBosch.class).stop();
            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.account(RouteComponent.AccountType.COUNT).stream(null);
        }
    }

    static class QuaternionConfig extends SensorFusionConfig {
        QuaternionConfig() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_quaternion, "quaternion", 0);
            outputControl = "Off";
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(SensorFusionBosch.class).quaternion();
        }
    }

    static class EulerAnglesConfig extends SensorFusionConfig {
        EulerAnglesConfig() {
            super(R.mipmap.ic_gyroscope, R.string.sensor_euler_angles, "euler-angles", 0);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = false;
            ifShowRange = false;
            ifShowPull = false;

        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(SensorFusionBosch.class).eulerAngles();
        }
    }


    static class LinearAccelerationConfig extends SensorFusionConfig {
        LinearAccelerationConfig() {
            super(R.mipmap.ic_accelerometer, R.string.sensor_linear_acc, "linear-acceleration", 11);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = false;
            ifShowRange = false;
            ifShowPull = false;

        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(SensorFusionBosch.class).linearAcceleration();
        }
    }

    static class GravityConfig extends SensorFusionConfig {
        GravityConfig() {
            super(R.mipmap.ic_accelerometer, R.string.sensor_gravity, "gravity", 0);
            outputControl = "Off";
            isEnabledGraph = false;
            ifShowFreq = false;
            ifShowRange = false;
            ifShowPull = false;

        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(SensorFusionBosch.class).gravity();
        }
    }

    static class AmbientLightConfig extends ActiveSensorConfig {
        AmbientLightConfig() {
            super(R.mipmap.ic_ambientlight, R.string.sensor_ambient_light, "illuminance", 0, -1);
            outputControl = "Off";

        }

        @Override
        float[] frequencies() {
            return new float[]{0.5f, 1f, 2f, 5f, 10f};
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(AmbientLightLtr329.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            AmbientLightLtr329.MeasurementRate[] rates = AmbientLightLtr329.MeasurementRate.values();

            metawear.getModule(AmbientLightLtr329.class).configure()
                    .gain(AmbientLightLtr329.Gain.LTR329_1X)
                    .integrationTime(AmbientLightLtr329.IntegrationTime.LTR329_TIME_100MS)
                    .measurementRate(rates[AmbientLightLtr329.MeasurementRate.LTR329_RATE_2000MS.ordinal() - freqIndex])
                    .commit();
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(AmbientLightLtr329.class).illuminance();
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.account().stream(null);
        }
    }

    static class PressureConfig extends ActiveSensorConfig {
        private static final float[] BMP280 = new float[]{0.25f, 0.50f, 0.99f, 1.96f, 3.82f, 7.33f, 13.51f, 83.33f},
                BME280 = new float[]{0.99f, 1.96f, 3.82f, 7.33f, 13.51f, 31.75f, 46.51f, 83.33f},
                COMBINED = new float[]{0.99f, 1.96f, 3.82f, 7.33f, 13.51f, 83.33f};

        private float[] frequencies;

        PressureConfig() {
            super(R.mipmap.ic_pressure, R.string.sensor_pressure, "pressure", 0, 15);
            outputControl = "Off";
            ifShowFreq = true;
        }

        @Override
        float[] frequencies() {
            return frequencies;
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            BarometerBosch barometer = metawear.getModule(BarometerBosch.class);
            if (barometer instanceof BarometerBmp280) {
                frequencies = (frequencies == null || frequencies == BMP280) ? BMP280 : COMBINED;
            } else if (barometer instanceof BarometerBme280) {
                frequencies = (frequencies == null || frequencies == BME280) ? BME280 : COMBINED;
            } else {
                return false;
            }
            return true;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            BarometerBosch barometer = metawear.getModule(BarometerBosch.class);

            if (barometer instanceof BarometerBmp280) {
                BarometerBmp280 bmp280 = (BarometerBmp280) barometer;
                BarometerBmp280.StandbyTime offset = frequencies == COMBINED ? BarometerBmp280.StandbyTime.TIME_1000 : BarometerBmp280.StandbyTime.TIME_4000;

                bmp280.configure()
                        .filterCoeff(BarometerBosch.FilterCoeff.OFF)
                        .pressureOversampling(BarometerBosch.OversamplingMode.LOW_POWER)
                        .standbyTime(BarometerBmp280.StandbyTime.values()[offset.ordinal() - freqIndex])
                        .commit();
            } else {
                BarometerBme280 bme280 = (BarometerBme280) barometer;
                BarometerBme280.ConfigEditor editor = bme280.configure()
                        .filterCoeff(BarometerBosch.FilterCoeff.OFF)
                        .pressureOversampling(BarometerBosch.OversamplingMode.LOW_POWER);

                if (frequencies == COMBINED) {
                    switch (freqIndex) {
                        case 0:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_1000);
                            break;
                        case 1:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_500);
                            break;
                        case 2:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_250);
                            break;
                        case 3:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_125);
                            break;
                        case 4:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_62_5);
                            break;
                        case 5:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_0_5);
                            break;
                    }
                } else {
                    switch (freqIndex) {
                        case 0:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_1000);
                            break;
                        case 1:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_500);
                            break;
                        case 2:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_250);
                            break;
                        case 3:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_125);
                            break;
                        case 4:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_62_5);
                            break;
                        case 5:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_20);
                            break;
                        case 6:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_10);
                            break;
                        case 7:
                            editor.standbyTime(BarometerBme280.StandbyTime.TIME_0_5);
                            break;
                    }
                }

                editor.commit();
            }
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(BarometerBosch.class).pressure();
        }

        @Override
        void start(MetaWearBoard metawear) {
            super.start(metawear);

            metawear.getModule(BarometerBosch.class).start();
        }

        @Override
        void stop(MetaWearBoard metawear) {
            metawear.getModule(BarometerBosch.class).stop();

            super.stop(metawear);
        }

        @Override
        RouteComponent setupStream(RouteComponent source) {
            return source.account().stream(null);
        }
    }

    abstract static class ForcedSensorConfig extends SensorConfig {
        private final Map<MetaWearBoard, Timer.ScheduledTask> scheduledTasks = new HashMap<>();


        ForcedSensorConfig(int imageResId, int nameResId, String identifier, int initialFreqIndex) {
            super(imageResId, nameResId, identifier, initialFreqIndex, -1);
        }

        ForcedSensorConfig(int imageResId, int nameResId, String identifier, String key, int initialFreqIndex, int orderKey) {
            super(imageResId, nameResId, identifier, key, initialFreqIndex, orderKey);
        }

        @Override
        Task<Route> addRouteAsync(MetaWearBoard metawear) {
            Task<Route> result = super.addRouteAsync(metawear);
            return result.onSuccessTask(ignored ->
                    metawear.getModule(Timer.class).scheduleAsync(
                            (int) (frequencies()[freqIndex] * 1000),
                            false,
                            () -> ((ForcedDataProducer) getProducer(metawear)).read()
                    )
            ).onSuccessTask(task -> {
                scheduledTasks.put(metawear, task.getResult());
                return result;
            });
        }

        @Override
        void start(MetaWearBoard metawear) {
            if (scheduledTasks.containsKey(metawear)) {
                scheduledTasks.get(metawear).start();
            }
        }

        @Override
        void stop(MetaWearBoard metawear) {
            if (scheduledTasks.containsKey(metawear)) {
                scheduledTasks.get(metawear).stop();
            }
        }
    }


    abstract static class SlowSensorConfig extends ForcedSensorConfig {

        SlowSensorConfig(int imageResId, int nameResId, String identifier, int initialFreqIndex) {
            super(imageResId, nameResId, identifier, initialFreqIndex);
        }

        @Override
        String selectedFreqText() {
            float[] available = frequencies();
            if (available[freqIndex] >= 3600) {
                return String.format(Locale.US, "%dhr", (int) (available[freqIndex] / 3600));
            }
            if (available[freqIndex] >= 60) {
                return String.format(Locale.US, "%dm", (int) (available[freqIndex] / 60));
            }
            return String.format(Locale.US, "%ds", (int) available[freqIndex]);
        }

        @Override
        float[] frequencies() {
            return new float[]{3600, 1800, 900, 60, 30, 15, 1};
        }

        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return 1f / frequencies()[freqIndex];
        }
    }

    static class TemperatureConfig extends SlowSensorConfig {
        TemperatureConfig() {
            super(R.mipmap.ic_temperature, R.string.sensor_temperature, "temperature", 0);
            outputControl = "Off";
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            Temperature temperature = metawear.getModule(Temperature.class);
            return temperature != null && temperature.findSensors(Temperature.SensorType.PRESET_THERMISTOR) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {

        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(Temperature.class).findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
        }
    }


    static class HumidityConfig extends SlowSensorConfig {
        HumidityConfig() {
            super(R.mipmap.ic_humidity, R.string.sensor_humidity, "relative-humidity", 0);
            outputControl = "Off";
        }

        @Override
        boolean isValid(MetaWearBoard metawear) {
            return metawear.getModule(HumidityBme280.class) != null;
        }

        @Override
        void configure(MetaWearBoard metawear) {
            metawear.getModule(HumidityBme280.class).setOversampling(HumidityBme280.OversamplingMode.SETTING_1X);
        }

        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            return metawear.getModule(HumidityBme280.class).value();
        }
    }


    //TODO: implement Gpio


    static class GpioConfig0 extends ForcedSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 0;



        //Constructor
        GpioConfig0() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio0, "gpio0", KEY_IMU, 1, 5);

            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, NONE by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc-data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }


    static class GpioConfig1 extends ActiveSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 1;



        //Constructor
        GpioConfig1() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio1, "gpio1", KEY_IMU, 1, 6);

            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, "NONE" by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                    //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }

    static class GpioConfig2 extends ActiveSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 2;



        //Constructor
        GpioConfig2() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio2, "gpio2", KEY_IMU, 1, 7);

            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, "NONE" by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                    //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }

    static class GpioConfig3 extends ActiveSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 3;



        //Constructor
        GpioConfig3() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio3, "gpio3", KEY_IMU, 1, 8);

            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, NONE by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc-data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                    //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }


    static class GpioConfig4 extends ActiveSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 4;



        //Constructor
        GpioConfig4() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio4, "gpio4", KEY_IMU, 1, 9);

            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, NONE by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc-data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }


    static class GpioConfig5 extends ActiveSensorConfig {
        private float[] frequencies;
        private String[] pullMode;
        protected float min, max;
        private byte gpioPin = 5;



        //Constructor
        GpioConfig5() {
            super(R.mipmap.ic_proximity, R.string.sensor_gpio5, "gpio5", KEY_IMU, 1, 10);
            frequencies = new float[]{25f, 50f, 100f, 200f, 400f, 800f};
            pullMode = new String[]{"UP", "NONE", "DOWN"};
            outputControl = "On";
            isEnabledGraph = false;
            ifShowFreq = true;
            ifShowRange = false;
            ifShowPull = true;


        }


        String selectedPullModeText() {
            return String.format(Locale.US, "%s", pullMode()[pullModeIndex]);
        }


        @Override
        float[] frequencies() {
            if (isStreaming) {
                List<Float> filtered = new ArrayList<>();
                for (float f : frequencies) {
                    if (f < 300f) {
                        filtered.add(f);
                    }
                }

                float[] result = new float[filtered.size()];
                int i = 0;
                for (float f : filtered) {
                    result[i] = f;
                    i++;
                }

                return result;
            }
            return frequencies;
        }

        String readMode() {
            return this.readMode;
        }

        String[] pullMode() {
            return pullMode;
        }

        String outputControl() {
            return outputControl;
        }

        @Override
            //check if Gpio is valid and set read mode
        boolean isValid(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            String read_mode = readMode();
            String pull_mode = pullMode[pullModeIndex];

            switch (this.readModeIndex) {
                case 0:
                    max = 1023;
                    break;
                case 1:
                    max = 3.f;
                    break;
                //return false for every other cases
                default:
                    return false;
            }

            //set pullMode, NONE by default
            switch (pull_mode) {
                case "UP":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_UP);
                    break;
                case "DOWN":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.PULL_DOWN);
                    break;
                case "NONE":
                    gpio.pin((byte) gpioPin).setPullMode(Gpio.PullMode.NO_PULL);
                    break;
                default:
                    return false;

            }

            return true;
        }

        private long startTime = -1;

        private final Subscriber gpioSubscriber = (data, env) -> {
            final Short gpioValue = data.value(Short.class);

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

            }
        };

        void configure(MetaWearBoard metawear) {

            String read_mode = this.readMode();
            switch (this.readModeIndex) {
                //configure adc data
                case 0:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "adc = " + data.value(Short.class));
                                            Log.d("MainActivity", "adc index" + readModeIndex );

                                        }
                                    });
                                }
                            });

                    break;
                    //configure abs-ref data
                case 1:
                    metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef()
                            .addRouteAsync(new RouteBuilder() {
                                @Override
                                public void configure(RouteComponent source) {
                                    source.stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env) {
                                            Log.i("MainActivity", "abs = " + data.value(Float.class));
                                            Log.d("MainActivity", "abs index" + readModeIndex );
                                        }
                                    });
                                }
                            });
                    break;
                //return false for every other cases
                default:
                    break;
            }
            Timer timer;
            timer = metawear.getModule(Timer.class);
            Timer.ScheduledTask scheduledTask;
            timer.scheduleAsync((int) ((1.0f / frequencies()[freqIndex]) * 1000), false, () -> {
                switch (readModeIndex) {
                    case 0:

                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc().read();
                        break;
                    case 1:
                        metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef().read();
                        break;
                    default:
                        Log.d("wow", "wow");
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }).continueWith(task -> {
                task.getResult().start();

                return null;
            });
        }


        @Override
        DataProducer getProducer(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            if (readModeIndex == 0) {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAdc();

            } else {
                return metawear.getModule(Gpio.class).pin((byte) gpioPin).analogAbsRef();
            }
        }


        @Override
        float dataThroughputSum(MetaWearBoard metawear) {
            return frequencies[freqIndex] / 2f;
        }

        @Override
        void start(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().start();

        }

        @Override
        void stop(MetaWearBoard metawear) {
            Gpio gpio = metawear.getModule(Gpio.class);
            gpio.pin((byte) gpioPin).monitor().stop();
        }


    }

}