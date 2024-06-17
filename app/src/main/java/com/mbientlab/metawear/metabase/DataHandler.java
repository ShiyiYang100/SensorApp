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

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.Data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import uk.me.berndporr.iirj.Butterworth;

interface DataHandler {
    void init();

    void process(Data data);

    void stop();
    void rewrite();

    String getIdentifier();

    float ALPHA = 0.2f; // if ALPHA = 1 OR 0, no filter applies.
    static float[] lowPass(float[] input, float[] output)
    {
        if ( output == null )
            return input;
        for ( int ix=0; ix<input.length; ix++ )
        {
            output[ix] = output[ix] + ALPHA * (input[ix] - output[ix]);
        }
        return output;
    }

    static int findMin(float OATime, float OGTime, float OMTime){
        if (OATime < OGTime && OATime < OMTime){
            return 0;
        } else if (OGTime < OATime && OGTime < OMTime) {
            return 1;
        }else{
            return 2;
        }
    }



    class CsvDataHandler implements DataHandler {
        static final String TIMESTAMP_FORMAT = "%tY-%<tm-%<tdT%<tH.%<tM.%<tS.%<tL", TZ_OFFSET;
        private static final Map<String, String> CSV_HEADERS;

        static boolean ifStop;
        static boolean ifAcc;
        static boolean ifMag;
        static boolean ifGyro;


        static {
            CSV_HEADERS = new HashMap<>();
            CSV_HEADERS.put("acceleration", "x-axis (g),x-axis filtered (g),y-axis (g),y-axis filtered (g),z-axis (g),z-axis filtered (g)");
            CSV_HEADERS.put("acceleration-mag", "x-axis (g),y-axis (g),z-axis (g), acc-mag (g)");
            CSV_HEADERS.put("angular-velocity", "x-axis (deg/s),x-axis filtered (deg/s),y-axis (deg/s),y-axis filtered (deg/s),z-axis (deg/s),z-axis filtered (deg/s)");
            CSV_HEADERS.put("angular-velocity-peak", "y-axis filtered (deg/s),peak index,peak,valley index,valley,mid-swing-index,mid-swing,toe-off-index,toe-off,heel-strike-index," +
                    "heel-strike,frequency,start,end,swing-phase,stance-phase, order,stride time,swing time,stance time,toe off,heel strike,double support time," +
                    "single support time,stride length, walking speed, MFC, step num");
            CSV_HEADERS.put("magnetic-field", "x-axis (T),y-axis (T),z-axis (T)");
            CSV_HEADERS.put("linear-acceleration", "x-axis (g),y-axis (g),z-axis (g)");
            CSV_HEADERS.put("gravity", "x-axis (g),y-axis (g),z-axis (g)");
            CSV_HEADERS.put("euler-angles", "pitch (deg),roll (deg),yaw (deg), heading (deg)");
            CSV_HEADERS.put("quaternion", "w (number),x (number),y (number), z (number)");
            CSV_HEADERS.put("illuminance", "illuminance (lx)");
            CSV_HEADERS.put("relative-humidity", "relative humidity (%)");
            CSV_HEADERS.put("pressure", "pressure (Pa),y-value");
            CSV_HEADERS.put("temperature", "temperature (C)");
            CSV_HEADERS.put("adc", "adc");
            CSV_HEADERS.put("abs-ref", "abs");
            CSV_HEADERS.put("digital", "digital");
            CSV_HEADERS.put("orientation", "Ax,Ay,Az,Gx,Gy,Gz,O-Roll,O-Pitch,O-Yawl,OXMAX,OXMIN,OYMAX,OYMIN,OZMAX,OZMIN");
            CSV_HEADERS.put("resample", "Ax,Ay,Az,Gx,Gy,Gz,Mx,My,Mz");

            SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.US);
            TimeZone defaultTz = TimeZone.getDefault();
            int offset = defaultTz.useDaylightTime() ? defaultTz.getRawOffset() - defaultTz.getDSTSavings() : defaultTz.getRawOffset();
            TZ_OFFSET = offset < 0 ? "-" + df.format(offset) : df.format(offset);
        }

        static String formatTimestamp(Calendar datetime) {
            return String.format(Locale.US, "%tY-%<tm-%<tdT%<tH:%<tM:%<tS.%<tL", datetime);
        }

        final boolean isStreaming;
        final String identifier, prefix, sensorConfigType;
        final private FileOutputStream fos;
        final private Float period, frequency;
        float accXValue;
        TextView accXView;
        float accYValue, accZValue, accFilteredX, accFilteredY, accFilteredZ;

        float gyroXValue, gyroYValue, gyroZValue, gyroFilteredX, gyroFilteredY, gyroFilteredZ;
        float gyroPeakXValue, gyroPeakYValue, gyroPeakZValue, gyroPeakFilteredY, gyroPeakFilteredX;
        float thetaY, prevThetaY, thetaYFilt,thetaY_rad, thetaY_cos, thetaY_sin;
        float thetaX, prevThetaX, thetaXFilt,thetaX_rad, thetaX_cos, thetaX_sin;
        float knee_h, v_ay_gyro_hor, v_ay_hor, knee_r, v_ax_gyro_hor, v_ax_hor,
                v_hor, prevStrideLen, strideLen, walkingSpeed, v_ay_gyro_ver,
                v_ay_ver;
        float fc, prev_fc, prev_prev_fc, fc_min, prev_fc_min, prev_prev_fc_min,
            fc_min_time, prev_fc_min_time, prev_prev_fc_min_time,MFC;
        int gyroPeakOrder;
        //peak-detection data
        float threshold_min ;
        float threshold_max ;
        int count_peak, count_valley;
        float v1, v2, v3;
        float peak, prev_peak,valley;
        int peak_index;
        long data_num, prev_data_num;
        ArrayList<Float> peak_values;
        int valley_index;
        ArrayList<Float> valley_values;

        int mid_swing_index;
        float mid_swing;

        int orientationCount;


        int toe_off_index;
        float toe_off;
        int heel_strike_index;
        float heel_strike;
        boolean ifMidSwing;
        boolean ifToeOff;
        boolean ifHealStrike;
        float last_mid_swing;
        float last_toe_off;
        float last_heal_strike;
        float swing_phase;
        float stance_phase;
        float stride_phase;

        //FFT filter
        ArrayList<Float> fftDataAccX;
        ArrayList<Float> fftDataAccY;
        ArrayList<Float> fftDataAccZ;
        ArrayList<Float> samplingFreqsAX;
        ArrayList<Float> samplingFreqsAY;
        ArrayList<Float> samplingFreqsAZ;
        ArrayList<Float> fftData;
        ArrayList<Float> fftDataGyroX;
        ArrayList<Float> fftDataGyroY;
        ArrayList<Float> fftDataGyroZ;
        ArrayList<Float> samplingFreqs;
        ArrayList<Float> samplingFreqsGX;
        ArrayList<Float> samplingFreqsGY;
        ArrayList<Float> samplingFreqsGZ;

        ArrayList<Float> fftDataMagX;
        ArrayList<Float> fftDataMagY;
        ArrayList<Float> fftDataMagZ;
        ArrayList<Float> samplingFreqsMX;
        ArrayList<Float> samplingFreqsMY;
        ArrayList<Float> samplingFreqsMZ;
        ArrayList<Float> intervalData;
        float currFreqGyroPeak;
        float startTimeGyroPeak;
        float lastStartTimeGyroPeak;
        float endTimeGyroPeak;

        float prevFreqGyroPeak;
        float initFreqGyroPeak;

        //Acc X
        float currFreqAccX;
        float startTimeAccX;
        float lastStartTimeAccX;
        float endTimeAccX;

        float prevFreqAccX;
        float initFreqAccX;

        //Acc Y
        float currFreqAccY;
        float startTimeAccY;
        float lastStartTimeAccY;
        float endTimeAccY;

        float prevFreqAccY;
        float initFreqAccY;


        //Acc Z
        float currFreqAccZ;
        float startTimeAccZ;
        float lastStartTimeAccZ;
        float endTimeAccZ;

        float prevFreqAccZ;
        float initFreqAccZ;

        float currFreqMagX;
        float startTimeMagX;
        float lastStartTimeMagX;
        float endTimeMagX;

        float prevFreqMagX;
        float initFreqMagX;

        //Acc Y
        float currFreqMagY;
        float startTimeMagY;
        float lastStartTimeMagY;
        float endTimeMagY;

        float prevFreqMagY;
        float initFreqMagY;


        //Acc Z
        float currFreqMagZ;
        float startTimeMagZ;
        float lastStartTimeMagZ;
        float endTimeMagZ;

        float prevFreqMagZ;
        float initFreqMagZ;

        float OMTime;
        float prevOrientationTime, orientationX, orientationY, orientationZ,
        OXMAX,OXMIN,OYMAX,OYMIN,OZMAX,OZMIN;


        float previous_MX;
        float previous_MY;
        float previous_MZ;
        float previous_MTime;
        // The coefficient for the fusedOrientation... 0.5 = means it is averaging the two
        // transfer functions (rotations from the gyroscope and
        // acceleration/magnetic, respectively).
        public static float timeConstant;


        protected static final float EPSILON = 0.000000001f;


        //GyroScope X
        float currFreqGyroX;
        float startTimeGyroX;
        float lastStartTimeGyroX;
        float endTimeGyroX;

        float prevFreqGyroX;
        float initFreqGyroX;

        //GyroScope Y
        float currFreqGyroY;
        float startTimeGyroY;
        float lastStartTimeGyroY;
        float endTimeGyroY;

        float prevFreqGyroY;
        float initFreqGyroY;

        //GyroScope Z
        float currFreqGyroZ;
        float startTimeGyroZ;
        float lastStartTimeGyroZ;
        float endTimeGyroZ;

        float prevFreqGyroZ;
        float initFreqGyroZ;

        //DoFFt
        double startTimeGyro;
        double endTimeGyro;

        double startTimeAcc;
        double endTimeAcc;

        double startTimeMag;
        double endTimeMag;

        //complementary filter without magnetometer
        //Initialize variables


        //Define dynamic alpha parameters
        static float alphaMin;
        static float alphaMax;

        static List<Double> gyroMagnitude;


        //define variables for butter worth filter
        Butterworth butterworthAX, butterworthAY, butterworthAZ,butterworthGX, butterworthGY, butterworthGZ,
                butterworthMX, butterworthMY, butterworthMZ, butterworthGyroPeakX,butterworthGyroPeakY, butterworthThetaY,butterworthThetaX ;
        float gyroXPrev, gyroYPrev, gyroZPrev;
        float [] currentV =  new float[3];
        float [] prevV =  new float[3];
        int accSampleNum, gyroSampleNum, gyroPeakSampleNum, orientationSampleNum;

        //DoFFt
        double startTimePeak;
        double endTimePeak;

        static double[] rotationVector;

        static boolean ifRotationVectorSet;

        //NS2S: nano second to second
        protected static final float NS2S = 1.0f / 1000000000.0f;

        Calendar first, last;
        private Long start, next, prev;


        MetaBaseDeviceData m;
        float OAx;
        float OAy;
        float OAz;
        float OGx;
        float OGy;
        float OGz;
        float t;

        float[] out;

        //graphs
        LineChart accelerometerChart, accMagChart;
        LineData accChartData, accMagChartData;
        TextView accName, accMagName;
        LineChart magnetometerChart;
        LineData magChartData;
        TextView magName;
        LineChart gyroscopeChart;
        LineData gyroChartData;
        TextView gyroName;
        LineData gyroPeakChartData;
        LineChart gyroPeakChart;
        TextView gyroPeakName;

        LineData orientationChartData;
        LineChart orientationChart;
        TextView orientationName;
        LineChart gpioAdcChart;
        LineData gpioAdcChartData;
        TextView gpioName;
        LineChart gpioAbsChart;
        LineData gpioAbsChartData;
        float accMagSampleNum, accMag, magSampleNum, magXValue, magYValue, magZValue,
            special_value, gpioAdcSampleNum, gpioAdcValue, gpioAbsSampleNum, gpioAbsValue;

        //orientation buffer objects
        OrientationBuffer oXMax, oXMin, oYMax, oYMin, oZMax, oZMin;

        //gait parameter objects
        TextView realTimeStrideTime, realTimeSwingTime, realTimeStanceTime, realTimeToeOff,
            realTimeHeelStrike, realTimeDST, realTimeSST, realTimeStepTime, realTimeStrideLen,
            realTimeWalkingSpeed, realTimeMFC, realTimeStepNum, realTimeOXMAX, realTimeOXMIN,
            realTimeOYMAX, realTimeOYMIN, realTimeOZMAX, realTimeOZMIN;


        private static final DecimalFormat df = new DecimalFormat("0.00");

        CsvDataHandler(FileOutputStream fos, String identifier, Float frequency, boolean isStreaming, String sensorConfigType,
                        MetaBaseDeviceData m) {
            this.identifier = identifier;
            this.prefix = identifier.split("[:|\\[]")[0];
            this.fos = fos;
            this.period = (1f / (frequency)) * 1000;
            this.frequency = frequency;
            this.isStreaming = isStreaming;
            this.sensorConfigType = sensorConfigType;
            this.m = m;


            start = null;
            prev = null;
        }



        void setFirst(long epoch) {
            start = epoch;

            first = Calendar.getInstance();
            first.setTimeInMillis(epoch);
        }

        @Override
        public void init() {

            ifStop = false;
            ifAcc = false;
            ifMag = false;
            ifGyro = false;

            accXValue = 0f;
            accYValue  =0f;
            accZValue = 0f;
            accSampleNum = 0;


            gyroSampleNum = 0;
            gyroPeakSampleNum = 0;
            orientationSampleNum = 0;
            gyroXValue = 0.0f;
            gyroYValue = 0.0f;
            gyroZValue = 0.0f;
            gyroPeakXValue = 0.0f;
            gyroPeakYValue = 0.0f;
            gyroPeakZValue = 0.0f;
            gyroPeakOrder = 0;
            gyroPeakFilteredX = 0.0f;
            gyroPeakFilteredY = 0.0f;
            accMag = 0f;
            magXValue = 0f;
            magYValue = 0f;
            magZValue = 0f;
            special_value = 0f;
            gpioAdcValue = 0f;
            gpioAbsValue = 0f;

            thetaY = 0f;
            prevThetaY = 0f;
            thetaYFilt = 0f;
            thetaY_rad = 0f;
            thetaY_cos = 0f;
            thetaY_sin = 0f;

            thetaX = 0f;
            prevThetaX = 0f;
            thetaXFilt = 0f;
            thetaX_rad = 0f;
            thetaX_cos = 0f;
            thetaX_sin = 0f;

            knee_h = m.getKneeHeight();
            v_ay_gyro_hor = 0f;
            v_ay_hor = 0f;
            knee_r = 0f;
            v_ax_gyro_hor = 0f;
            v_ax_hor = 0f;
            v_hor = 0f;
            prevStrideLen = 0f;
            strideLen = 0f;
            walkingSpeed = 0f;
            v_ay_gyro_ver = 0f;
            v_ay_ver = 0f;
            fc = Integer.MAX_VALUE;
            prev_fc = 0f;
            prev_prev_fc = Integer.MAX_VALUE;
            fc_min = Integer.MAX_VALUE;
            prev_fc_min = 0f;
            prev_prev_fc_min = Integer.MAX_VALUE;
            fc_min_time =  Integer.MAX_VALUE;
            prev_fc_min_time = 0f;
            MFC = 0F;
            prev_prev_fc_min_time = Integer.MAX_VALUE;


            //peak-detection data
            threshold_min = -20f;
            threshold_max = 50f;
            count_peak = 0;
            count_valley = 0;
            v1 = 0f;
            v2 = 0f;
            v3 = 0f;
            peak = 0f;
            valley = 0f;
            peak_index = 0;
            peak_values = new ArrayList<>();
            valley_index = 0;
            valley_values = new ArrayList<>();
            mid_swing_index = 0;
            mid_swing = 0f;
            toe_off_index = 0;
            toe_off = 0f;
            heel_strike_index = 0;
            heel_strike = 0f;
            data_num = 0;
            prev_data_num = 0;
            orientationCount = 0;

            OAx  =0 ;
            OAy = 0;
            OAz = 0;
            OGx = 0;
            OGy = 0;
            OGz = 0;
            t = 0f;
            out = new float[3];

            startTimeAcc = 0;
            endTimeAcc = 0;
            startTimeGyro = 0;
            endTimeGyro = 0;
            startTimeMag = 0;
            endTimeMag = 0;
            startTimePeak = 0;
            endTimePeak = 0;

            ifMidSwing = false;
            ifToeOff = false;
            ifHealStrike = false;
            last_mid_swing = 2001f;
            last_toe_off = 2001f;
            last_heal_strike = 0f;
            swing_phase = 0f;
            stance_phase = 0f;
            stride_phase = 0f;

            rotationVector = new double[4];
            ifRotationVectorSet = false;


            alphaMin = 0.9f;
            alphaMax = 1.0f;
            gyroMagnitude = new ArrayList<Double>();


            fftDataAccX = new ArrayList<Float>();
            fftDataAccY = new ArrayList<Float>();
            fftDataAccZ = new ArrayList<Float>();
            samplingFreqsAX = new ArrayList<Float>();
            samplingFreqsAY = new ArrayList<Float>();
            samplingFreqsAZ = new ArrayList<Float>();
            fftData = new ArrayList<Float>();
            fftDataGyroX = new ArrayList<Float>();
            fftDataGyroY = new ArrayList<Float>();
            fftDataGyroZ = new ArrayList<Float>();
            samplingFreqs = new ArrayList<Float>();
            samplingFreqsGX = new ArrayList<Float>();
            samplingFreqsGY = new ArrayList<Float>();
            samplingFreqsGZ = new ArrayList<Float>();

            fftDataMagX = new ArrayList<Float>();
            fftDataMagY = new ArrayList<Float>();
            fftDataMagZ = new ArrayList<Float>();
            samplingFreqsMX = new ArrayList<Float>();
            samplingFreqsMY = new ArrayList<Float>();
            samplingFreqsMZ = new ArrayList<Float>();

            intervalData  = new ArrayList<Float>();

            currFreqGyroPeak = 0;
            startTimeGyroPeak = 0;
            lastStartTimeGyroPeak = 0;
            endTimeGyroPeak = 0;
            initFreqGyroPeak = 5f;
            prevFreqGyroPeak = initFreqGyroPeak;

            currFreqAccX = 0;
            startTimeAccX = 0;
            lastStartTimeAccX= 0;
            endTimeAccX = 0;
            initFreqAccX = 5f;
            prevFreqAccX = initFreqAccX;

            currFreqAccY = 0;
            startTimeAccY = 0;
            lastStartTimeAccY= 0;
            endTimeAccY = 0;
            initFreqAccY = 5f;
            prevFreqAccY = initFreqAccY;

            currFreqAccZ = 0;
            startTimeAccZ = 0;
            lastStartTimeAccZ= 0;
            endTimeAccZ = 0;
            initFreqAccZ = 5f;
            prevFreqAccZ = initFreqAccZ;

            currFreqMagX = 0;
            startTimeMagX = 0;
            lastStartTimeMagX= 0;
            endTimeMagX = 0;
            initFreqMagX = 5f;
            prevFreqMagX = initFreqMagX;

            currFreqMagY = 0;
            startTimeMagY = 0;
            lastStartTimeMagY= 0;
            endTimeMagY = 0;
            initFreqMagY = 5f;
            prevFreqMagY = initFreqMagY;

            currFreqMagZ = 0;
            startTimeMagZ = 0;
            lastStartTimeMagZ= 0;
            endTimeMagZ = 0;
            initFreqMagZ = 5f;
            prevFreqMagZ = initFreqMagZ;




            OMTime = 0;


            previous_MX = 0;
            previous_MY = 0;
            previous_MZ = 0;
            previous_MTime = 0;


            prevOrientationTime = 0;
            orientationX = 0;
            orientationY = 0;
            orientationZ = 0;
            OXMAX = 0f;
            OXMIN = 0f;
            OYMAX = 0f;
            OYMIN = 0f;
            OZMAX = 0f;
            OZMIN = 0f;


            timeConstant = 0.5f;

            currFreqGyroX = 0;
            startTimeGyroX = 0;
            lastStartTimeGyroX= 0;
            endTimeGyroX = 0;
            initFreqGyroX = 5f;
            prevFreqGyroX = initFreqGyroX;

            currFreqGyroY = 0;
            startTimeGyroY = 0;
            lastStartTimeGyroY= 0;
            endTimeGyroY = 0;
            initFreqGyroY = 5f;
            prevFreqGyroY = initFreqGyroY;

            currFreqGyroZ = 0;
            startTimeGyroZ = 0;
            lastStartTimeGyroZ= 0;
            endTimeGyroZ = 0;
            initFreqGyroZ = 5f;
            prevFreqGyroZ = initFreqGyroZ;

            //initialize orientation buffer
            oXMax = new OrientationBuffer();
            oXMin = new OrientationBuffer();
            oYMax = new OrientationBuffer();
            oYMin = new OrientationBuffer();
            oZMax = new OrientationBuffer();
            oZMin = new OrientationBuffer();

            butterworthAX = new Butterworth();
            butterworthAY = new Butterworth();
            butterworthAZ = new Butterworth();
            butterworthGX = new Butterworth();
            butterworthGY = new Butterworth();
            butterworthGZ = new Butterworth();
            butterworthMX = new Butterworth();
            butterworthMY = new Butterworth();
            butterworthMZ = new Butterworth();
            butterworthGyroPeakX = new Butterworth();
            butterworthGyroPeakY = new Butterworth();
            butterworthThetaY = new Butterworth();
            butterworthThetaX = new Butterworth();
            //if cannot change cutoffFrequency in runtime, then reuse lowPass
            butterworthAX.lowPass(5, frequency.doubleValue(), initFreqAccX);
            butterworthAY.lowPass(5, frequency.doubleValue(), initFreqAccY);
            butterworthAZ.lowPass(5, frequency.doubleValue(), initFreqAccZ);
            butterworthGX.lowPass(5, frequency.doubleValue(), initFreqGyroX);
            butterworthGY.lowPass(5, frequency.doubleValue(), initFreqGyroY);
            butterworthGZ.lowPass(5, frequency.doubleValue(), initFreqGyroZ);
            butterworthMX.lowPass(5, frequency.doubleValue(), initFreqMagX);
            butterworthMY.lowPass(5, frequency.doubleValue(), initFreqMagY);
            butterworthMZ.lowPass(5, frequency.doubleValue(), initFreqMagZ);

            butterworthGyroPeakX.lowPass(5, frequency.doubleValue(), initFreqGyroPeak);
            butterworthGyroPeakY.lowPass(5, frequency.doubleValue(), initFreqGyroPeak);

            butterworthThetaY.highPass(5, frequency.doubleValue(), 0.5f);
            butterworthThetaX.highPass(5, frequency.doubleValue(),0.5f);


            try {
                String additional;
                if (sensorConfigType.equals("acceleration-mag")){
                    additional = CSV_HEADERS.get("acceleration-mag");
                }
                else if (sensorConfigType.equals("angular-velocity-peak")){
                    additional = CSV_HEADERS.get("angular-velocity-peak");
                }
                else if (CSV_HEADERS.containsKey(sensorConfigType)) {
                    additional = CSV_HEADERS.get(sensorConfigType);
                } else {
                    throw new InvalidParameterException("Unknown identifier: " + identifier);
                }
                fos.write(String.format(Locale.US, "epoch (ms),time (%s),elapsed (s),%s%n", TZ_OFFSET, additional).getBytes());
            } catch (IOException ignored) {
            }
        }

        //private methods used by FFT filter
        private  float calculateCaptureTime(double startedTime, double finishTime) {

            return 10; //time captured between each sample?
        }

        private float calculateFreq(double sampleSize, double sampleTime) {
            return  (float) sampleSize/ (float)(frequency * 10);
        }

        private int findButterWorthFreq(float freq){
            int temp = (int) ceil((double)freq);
            if(temp <= 2){
                return 5;
            }else{
                return 5 + (temp - 2);
            }
        }
        private void calcRealTimestamp(Data data, long mask) {
            if (!isStreaming) {
                last = data.timestamp();
                if (start == null) {
                    first = last;
                    start = first.getTimeInMillis();
                }

                return;
            } else {
                if (start == null) {
                    first = Calendar.getInstance();
                    start = first.getTimeInMillis();
                }
            }

            long count = data.extra(Long.class);
            if (prev == null) {
                prev = count;
                next = start;
            } else if (prev == count) {
                next += period.longValue();
            }

            if (count < prev) {
                long diff = (count - prev) & mask;
                next += (long) (diff * period);
            } else {
                next += (long) ((count - prev) * period);
            }

            prev = count;
            last = Calendar.getInstance();
            last.setTimeInMillis(next);
        }


        //Orientation helper functions
        public static double[] getOrientationVectorFromAccMag
        (float Ax, float Ay, float Az, float Mx, float My, float Mz){
            float[] rotationMatrix = new float[9];
            rotationMatrix = getRotationMatrix(rotationMatrix, Ax,Ay,Az,Mx,My,Mz);
            return getQuaternion(rotationMatrix);

        }

        public static float[] getRotationMatrix
                (float[] R, float Ax, float Ay, float Az, float Ex, float Ey, float Ez){
            //Mx, My, Mz are refactored to Ex, Ey, Ez
            final float normsqA =(Ax * Ax + Ay * Ay + Az * Az);
            final float g = 9.81f;
            final float freeFallGravitySquared = 0.01f * g * g;
            /*
            if (normsqA < freeFallGravitySquared) {
                // gravity less than 10% of normal value
                return null;
            }

             */
            float Hx = Ey * Az - Ez * Ay;
            float Hy = Ez * Ax - Ex * Az;
            float Hz = Ex * Ay - Ey * Ax;
            final float normH = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
            /*
            if (normH < 0.1f) {
                // device is close to free fall (or in space?), or close to
                // magnetic north pole. Typical values are  > 100.
                return null;
            }

             */
            final float invH = 1.0f / normH;
            Hx *= invH;
            Hy *= invH;
            Hz *= invH;
            final float invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
            Ax *= invA;
            Ay *= invA;
            Az *= invA;

            final float Mx = Ay * Hz - Az * Hy;
            final float My = Az * Hx - Ax * Hz;
            final float Mz = Ax * Hy - Ay * Hx;
            if (R != null) {
                if (R.length == 9) {
                    R[0] = Hx;     R[1] = Hy;     R[2] = Hz;
                    R[3] = Mx;     R[4] = My;     R[5] = Mz;
                    R[6] = Ax;     R[7] = Ay;     R[8] = Az;
                } else if (R.length == 16) {
                    R[0]  = Hx;    R[1]  = Hy;    R[2]  = Hz;   R[3]  = 0;
                    R[4]  = Mx;    R[5]  = My;    R[6]  = Mz;   R[7]  = 0;
                    R[8]  = Ax;    R[9]  = Ay;    R[10] = Az;   R[11] = 0;
                    R[12] = 0;     R[13] = 0;     R[14] = 0;    R[15] = 1;
                }
            }

            return R;
        }

        public static float get(float[] R, int x, int y) {
            return R[x*3 + y];
        }

        private static double[] getQuaternion(float[] R) {
            double w = Math.sqrt(1.0 + get(R,0,0) + get(R, 1,1) + get(R,2,2)) / 2.0;
            double w4 = (4.0 * w);
            double x = (get(R,2,1) - get(R,1,2)) / w4 ;
            double y = (get(R,0,2) - get(R,2,0)) / w4 ;
            double z = (get(R,1,0) - get(R,0,1)) / w4 ;

            return new double[]{w,x,y,z};
        }


        public static float[] Complementary(float Ax, float Ay, float Az, float Mx, float My, float Mz,float Gx, float Gy, float Gz, float timeDiff){
            if (!ifRotationVectorSet) {
                if (ifAcc && ifMag) {
                    ifRotationVectorSet = true;
                    setBaseOrientation(getOrientationVectorFromAccMag(Ax,Ay,Az,Mx,My,Mz));
                    return new float[]{0,0,0};
                }
            } else {
                return calculateFusedOrientation(Ax,Ay,Az,Mx,My,Mz,Gx,Gy,Gz,timeDiff);
            }
            return new float[]{0,0,0};
        }





        public static float[] calculateFusedOrientation(float Ax, float Ay, float Az, float Mx, float My, float Mz,float Gx, float Gy, float Gz, float timeDiff) {
            float[] output = new float[3];
            //if (isBaseOrientationSet()) {
                    //final float dT = timeDiff * NS2S;
                    final float dT = timeDiff;
                    float alpha = timeConstant / (timeConstant + dT);
                    float oneMinusAlpha = (1.0f - alpha);
                    double[] rotationVectorAccelerationMagnetic = getOrientationVectorFromAccMag(Ax,Ay,Az,Mx,My,Mz);
                    /*
                    if (rotationVector == null){
                        rotationVector  = rotationVectorAccelerationMagnetic;
                    }
                     */
                    //do integration on the rotationVector itself
                    rotationVector = integrateGyroscopeRotation(rotationVector, Gx,Gy,Gz, dT, EPSILON);
                    // Apply the complementary fusedOrientation. // We multiply each rotation by their
                    // coefficients (scalar matrices)...
                    //Quaternion scaledRotationVectorAccelerationMagnetic = rotationVectorAccelerationMagnetic.multiply(oneMinusAlpha);
                    double[] scaledRotationVectorAccelerationMagnetic = multiply(rotationVectorAccelerationMagnetic, oneMinusAlpha);
                    // Scale our quaternion for the gyroscope
                    //Quaternion scaledRotationVectorGyroscope = rotationVector.multiply(alpha);
                    double[] scaledRotationVectorGyroscope = multiply(rotationVector, alpha);
                    //...and then add the two quaternions together.
                    // output[0] = alpha * output[0] + (1 - alpha) * input[0];
                    //Quaternion result = scaledRotationVectorGyroscope.add(scaledRotationVectorAccelerationMagnetic);
                    double[] result = add(scaledRotationVectorGyroscope, scaledRotationVectorAccelerationMagnetic);
                    //output = add(scaledRotationVectorGyroscope, scaledRotationVectorAccelerationMagnetic);
                    output = getAngles(result[0], result[1], result[2], result[3]);
                    // }
                //return output;
                return output;
        }

        public static double[] add(double[] q1, double[] q2) {
            return new double[]{q1[0] + q2[0], q1[1] + q2[1], q1[2] + q2[2], q1[3] + q2[3]};
        }

        public static float[] getAngles(double w, double z, double x, double y) {
            double heading;
            double pitch;
            double roll;

            double test = x*y + z*w;
            if (test > 0.499) { // singularity at north pole
                heading = 2 * Math.atan2(x,w);
                pitch = -Math.PI/2;
                roll = 0;
                //Log.e(TAG, "singularity at north pole");
                return new float[]{(float)heading, (float)pitch, (float)roll};
            }
            if (test < -0.499) { // singularity at south pole
                heading = -2 * Math.atan2(x,w);
                pitch = Math.PI/2;
                roll = 0;
                //Log.e(TAG, "singularity at south pole");
                return new float[]{(float)heading, (float)pitch, (float)roll};
            }
            double sqx = x*x;
            double sqy = y*y;
            double sqz = z*z;
            heading = -Math.atan2(2*y*w-2*x*z , 1 - 2*sqy - 2*sqz);
            pitch = -Math.asin(2*test);
            roll = -Math.atan2(2*x*w-2*y*z , 1 - 2*sqx - 2*sqz);

            return new float[]{(float) heading, (float) pitch, (float) roll};
        }



        public static double[] integrateGyroscopeRotation
                (double[] previousRotationVector,float Gx,float Gy,float Gz,float dt,float epsilon) {
            // Calculate the angular speed of the sample
            float magnitude = (float) Math.sqrt(Math.pow(Gx, 2)
                    + Math.pow(Gy, 2) + Math.pow(Gz, 2));

            // Normalize the rotation vector if it's big enough to get the axis
            if (magnitude > epsilon) {
                Gx /= magnitude;
                Gy /= magnitude;
                Gz /= magnitude;
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = magnitude * dt / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

            double[] deltaVector = new double[]{0,0,0,0};

            deltaVector[0] = sinThetaOverTwo * Gx;
            deltaVector[1] = sinThetaOverTwo * Gy;
            deltaVector[2] = sinThetaOverTwo * Gz;
            deltaVector[3] = cosThetaOverTwo;

            // Since it is a unit quaternion, we can just multiply the old rotation
            // by the new rotation delta to integrate the rotation.
            /*
            return previousRotationVector.multiply(new Quaternion(deltaVector[3], Arrays.copyOfRange(
                    deltaVector, 0, 3)));

             */
            //q0: deltaVector[3], q1:deltaVector[0],q2:deltaVector[1],q3:deltaVector[2]
            double[] newRotationVector = new double[]{deltaVector[3], deltaVector[0],deltaVector[1],deltaVector[2]};
            return multiply2Vector(previousRotationVector,newRotationVector);
        }

        public static void setBaseOrientation(double[] baseOrientation) {
            rotationVector = new double[baseOrientation.length];
            for  (int i = 0; i < baseOrientation.length; i++){
                rotationVector[i] = baseOrientation[i];
            }
        }

        public static boolean isBaseOrientationSet() {
            return (rotationVector != null);
        }

        public static double[] multiply2Vector(double[] q1, double[] q2){

            /*
                double q1a = q1.getQ0();
                double q1b = q1.getQ1();
                double q1c = q1.getQ2();
                double q1d = q1.getQ3();
                double q2a = q2.getQ0();
                double q2b = q2.getQ1();
                double q2c = q2.getQ2();
                double q2d = q2.getQ3();
                double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
                double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
                double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
                double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;
                return new Quaternion(w, x, y, z);
             */
            double q1a = q1[0];
            double q1b = q1[1];
            double q1c = q1[2];
            double q1d = q1[3];
            double q2a = q2[0];
            double q2b = q2[1];
            double q2c = q2[2];
            double q2d = q2[3];
            double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
            double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
            double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
            double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;
            //return new Quaternion(w, x, y, z);

            return  new double[]{w, x, y, z};
        }

        public static double[] multiply(double[] q, double alpha) {
            double[] output;
            //return new Quaternion(alpha * this.q0, alpha * this.q1, alpha * this.q2, alpha * this.q3);
            output = new double[]{alpha * q[0],alpha * q[1],alpha * q[2], alpha * q[3]};
            return output;
        }



        /*

         */

        public static List<Float> resample(List<Float> ls, int originalSamplingRate, int targetSamplingRate) {
            // Calculate the resampling ratio
            double ratio = (double) targetSamplingRate / originalSamplingRate;
            // Initialize the resampled list
            List<Float> resampledValues = new ArrayList<>();
            // Iterate over the original values
            for (int i = 0; i < ls.size() - 1; i++) {
                float currentValue = ls.get(i);
                float nextValue = ls.get(i + 1);

                // Calculate the number of interpolated values between the current and next value
                //int numInterpolatedValues = (int) Math.round((1 - ratio) * (i + 1));
                //int numInterpolatedValues = 1;

                int numInterpolatedValues = (int) Math.round((ratio - 1) * (i + 1));
                Log.d("interpolated val", "interpolated val =" + numInterpolatedValues);
                Log.d("interpolated val", "interpolated val =" + (ratio - 1) * (i + 1));

                // Calculate the step size for interpolation
                float stepSize = (nextValue - currentValue) / (numInterpolatedValues + 1);

                // Interpolate the values
                for (int j = 0; j < numInterpolatedValues; j++) {
                    float interpolatedValue = currentValue + stepSize * (j + 1);
                    resampledValues.add(interpolatedValue);
                }

                // Add the next original value to the resampled list
                resampledValues.add(nextValue);
            }
            return resampledValues;
        }


        public static float[] orientation (float samplingRate, float Ax, float Ay, float Az,
                                           float Gx,float Gy, float Gz, MetaBaseDeviceData m){
                float dt = (1 / samplingRate);

                /*
                % Compute roll and pitch angles using accelerometer data

                rollAcc = atan2(ay, az) * 180 / pi; % Roll angle from accelerometer data

                pitchAcc = atan2(-ax, sqrt(ay^2 + az^2)) * 180 / pi; % Pitch angle from accelerometer data

                % Integrate gyroscope data to update roll, pitch, and yaw angle

                roll = roll + gx * dt; % Roll angle from gyroscope data

                pitch = pitch + gy * dt; % Pitch angle from gyroscope data

                yaw = yaw + gz * dt; % Yaw angle from gyroscope data
                 */

                //Compute roll and pitch angles using accelerometer data
                double rollAcc = Math.atan2(Ay, Az) * 180 / Math.PI; //Roll angle from accelerometer data
                double pitchAcc = Math.atan2(-Ax, Math.sqrt(Ay*Ay + Az*Az)) * 180 / Math.PI; //Pitch angle from accelerometer data

                //Integrate gyroscope data to update roll, pitch, and yaw angles
                m.setRoll(m.getRoll() + Gx * dt);       //Roll angle from gyroscope data
                m.setPitch(m.getPitch()+ Gy * dt);      //Pitch angle from gyroscope data
                m.setYaw(m.getYaw() + Gz * dt);         //Yaw angle from gyroscope data

                /*
                    % Compute dynamic alpha based on gyroscope magnitude
                    gyroMagnitude = norm([gx, gy, gz])；
                    alpha = alphaMin + (gyroMagnitude - min(gyroMagnitude)) * (alphaMax - alphaMin) / (max(gyroMagnitude) - min(gyroMagnitude));
                 */

                //Compute dynamic alpha based on gyroscope magnitude
                double norm = Math.sqrt(Gx*Gx + Gy*Gy + Gz* Gz);
                //gyroMagnitude.add(norm);
                if (norm < m.getGyroMin()){
                    m.setGyroMin(norm);
                }
                if (norm > m.getGyroMax()){
                    m.setGyroMax(norm);
                }
                double alpha = alphaMin + (norm - m.getGyroMin()) * (alphaMax - alphaMin) / (m.getGyroMax() - m.getGyroMin());

                /*
                    % Apply complementary filter with the dynamic alpha
                    roll = alpha * roll + (1 - alpha) * rollAcc; % Fused roll angle
                    pitch = alpha * pitch + (1 - alpha) * pitchAcc; % Fused pitch angle
                    % Update yaw angle by incorporating gyroscope data
                    yaw = alphaGyro * (yaw + gz * dt) + (1 - alphaGyro) * atan2d(ax, az); % Fused yaw angle
                    yaw = (1 - alpha)* (yaw + gz * dt) + alpha * atan2d(ax, az); % Fused yaw angle​
                    % Store the computed Euler angles
                    eulerAngles(i, :) = [roll, pitch, yaw];
                    end
                 */

                //Apply complementary filter with the dynamic alpha
                   //Fused roll angle
                m.setRoll((float) (alpha * m.getRoll() + (1-alpha) * rollAcc));
                m.setPitch((float) (alpha * m.getPitch() + (1-alpha) * pitchAcc));//Fused pitch angle
                //Update yaw angle by incorporating gyroscope data
                double RTD = 180.0/Math.PI;
                m.setYaw((float) ((1 - alpha)* (m.getYaw() + Gz * dt) + alpha * (Math.atan2(Ax, Az) * RTD)));//Fused yaw angle
                float[] eulerAngles = new float []{m.getRoll(), m.getPitch(), m.getYaw()};
                /*
                if(gyroMagnitude.size() >= 1000){
                    gyroMagnitude.clear();
                }

                 */
                return eulerAngles;
        }


        /*
                function resampled_data = resample_custom(data, new_length)
                % Compute the original length of the data
                original_length = length(data);

                % Compute the resampling factor
                resampling_factor = original_length / new_length;

                % Compute the indices for the new samples
                new_indices = 1 + (0:new_length-1) * resampling_factor;

                % Perform the resampling by interpolating the data
                resampled_data = zeros(1, new_length);
                for i = 1:new_length
                    % Find the indices of the neighboring samples
                    index_lower = floor(new_indices(i));
                    index_upper = ceil(new_indices(i));

                    % Check if the indices are the same
                    if index_lower == index_upper
                        resampled_data(i) = data(index_lower);
                    else
                        % Perform linear interpolation
                        alpha = new_indices(i) - index_lower;
                        resampled_data(i) = (1 - alpha) * data(index_lower) + alpha * data(index_upper);
                    end
                end
            end
         */
        public float[] resample_time(ArrayList<Float> time, int new_length){
            //Compute the original length of the data
            int original_length = time.size();
            //Compute the resampling factor
            float resampling_factor =  (float) original_length / new_length;
            Log.d("resample", "resample factor= "+ resampling_factor);
            //Compute the indices for the new samples
            float[] new_indices = new float[new_length];
            for (int i = 0; i < new_length; i++){
                new_indices[i] = i * resampling_factor;
            }
            Log.d("resample", "resample raSize = "+new_length);
            Log.d("resample", "resample rgSize = "+original_length);
            //Perform the resampling by interpolating the data
            float[] resampled_time = new float[new_length];

                /*
                 for i = 1:new_length
                    % Find the indices of the neighboring samples
                    index_lower = floor(new_indices(i));
                    index_upper = ceil(new_indices(i));

                    % Check if the indices are the same
                    if index_lower == index_upper
                        resampled_data(i) = data(index_lower);
                    else
                        % Perform linear interpolation
                        alpha = new_indices(i) - index_lower;
                        resampled_data(i) = (1 - alpha) * data(index_lower) + alpha * data(index_upper);
                    end
                end
                 */
            for(int i = 0; i < new_length;i++){
                //Find the indices of the neighboring samples
                int index_lower = (int) floor(new_indices[i]);
                Log.d("resample", "resample index_lower = "+ index_lower);
                int index_upper = (int) ceil(new_indices[i]);
                Log.d("resample", "resample index_upper = "+ index_upper);

                //Check if the indices are the same
                if (index_lower == index_upper){
                        resampled_time[i] = time.get(index_lower);
                }else{
                    //Perform linear interpolation
                    float alpha = new_indices[i] - index_lower;
                    if(index_upper < time.size()){
                        resampled_time[i] = (1-alpha) * time.get(index_lower) + alpha * time.get(index_upper);
                    }else{
                        resampled_time[i] = (1-alpha) * time.get(index_lower) + alpha * time.get(index_lower);
                    }


                }
            }

            return resampled_time;

        }
        public float[][] resample_data(ArrayList<float[]> data, int new_length){
                //Compute the original length of the data
                int original_length = data.size();
                //Compute the resampling factor
                float resampling_factor = (float) original_length / new_length;
                Log.d("resample", "resample factor= "+ resampling_factor);
                //Compute the indices for the new samples
                float[] new_indices = new float[new_length];
                for (int i = 0; i < new_length; i++){
                    new_indices[i] = i * resampling_factor;
                }
                //Perform the resampling by interpolating the data
                float[] resampled_dataX = new float[new_length];
                float[] resampled_dataY = new float[new_length];
                float[] resampled_dataZ = new float[new_length];
                Log.d("resample", "resample raDataSize = "+new_length);
                Log.d("resample", "resample rgDataSize = "+original_length);
                /*
                 for i = 1:new_length
                    % Find the indices of the neighboring samples
                    index_lower = floor(new_indices(i));
                    index_upper = ceil(new_indices(i));

                    % Check if the indices are the same
                    if index_lower == index_upper
                        resampled_data(i) = data(index_lower);
                    else
                        % Perform linear interpolation
                        alpha = new_indices(i) - index_lower;
                        resampled_data(i) = (1 - alpha) * data(index_lower) + alpha * data(index_upper);
                    end
                end
                 */
                for(int i = 0; i < new_length;i++){
                    //Find the indices of the neighboring samples
                    int index_lower = (int) floor(new_indices[i]);
                    int index_upper = (int) ceil(new_indices[i]);
                    //Check if the indices are the same
                    if (index_lower == index_upper){
                        resampled_dataX[i] = data.get(index_lower)[0];
                        resampled_dataY[i] = data.get(index_lower)[1];
                        resampled_dataZ[i] = data.get(index_lower)[2];
                    }else{
                        //Perform linear interpolation
                        float alpha = new_indices[i] - index_lower;
                        if(index_upper < data.size()) {
                            resampled_dataX[i] = (1 - alpha) * data.get(index_lower)[0] + alpha * data.get(index_upper)[0];
                            resampled_dataY[i] = (1 - alpha) * data.get(index_lower)[1] + alpha * data.get(index_upper)[1];
                            resampled_dataZ[i] = (1 - alpha) * data.get(index_lower)[2] + alpha * data.get(index_upper)[2];
                        }else{
                            resampled_dataX[i] = (1 - alpha) * data.get(index_lower)[0] + alpha * data.get(index_lower)[0];
                            resampled_dataY[i] = (1 - alpha) * data.get(index_lower)[1] + alpha * data.get(index_lower)[1];
                            resampled_dataZ[i] = (1 - alpha) * data.get(index_lower)[2] + alpha * data.get(index_lower)[2];
                        }

                    }
                }

                return new float[][]{resampled_dataX, resampled_dataY, resampled_dataZ};

        }



        @Override
        public void process(Data data) {
            try {
                if (prefix.equals("acceleration") && sensorConfigType.equals("acceleration")) {
                    ifAcc = true;
                    calcRealTimestamp(data, 0xffffffffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;

                    float[] vector = data.value(float[].class);
                    /*
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            vector[0], vector[1], vector[2]).getBytes());

                    Log.d("acc", "pooAcc");

                     */


                    accXValue = vector[0];
                    accYValue = vector[1];
                    accZValue = vector[2];

                    //DoFFT
                    endTimeAcc = offset;
                    double timeDifference = endTimeAcc - startTimeAcc;
                    fftDataAccX.add(accXValue);
                    fftDataAccY.add(accYValue);
                    fftDataAccZ.add(accZValue);


                    if(ifStop){
                        float sampleSizeX          = fftDataAccX.size();    //size of the sample
                        float sampleSizeY          = fftDataAccY.size();
                        float sampleSizeZ          = fftDataAccZ.size();



                        float sampleTime            = calculateCaptureTime(startTimeAcc, endTimeAcc);       //time when the sample is collected
                        float samplingFreqX       = calculateFreq(sampleSizeX, sampleTime);
                        float samplingFreqY       = calculateFreq(sampleSizeY, sampleTime);
                        float samplingFreqZ       = calculateFreq(sampleSizeZ, sampleTime);
                        samplingFreqsAX.add(samplingFreqX);
                        samplingFreqsAY.add(samplingFreqY);
                        samplingFreqsAZ.add(samplingFreqZ);

                    }
                    else if(timeDifference >= 10){

                        //apply FFT filter
                        float sampleSizeX          = fftDataAccX.size();    //size of the sample
                        float sampleSizeY          = fftDataAccY.size();
                        float sampleSizeZ          = fftDataAccZ.size();

                        float sampleTime          = calculateCaptureTime(startTimeAcc, endTimeAcc);       //time when the sample is collected
                        float samplingFreqX        = calculateFreq(sampleSizeX, sampleTime);
                        samplingFreqsAX.add(samplingFreqX);
                        float samplingFreqY        = calculateFreq(sampleSizeY, sampleTime);
                        samplingFreqsAY.add(samplingFreqY);
                        float samplingFreqZ        = calculateFreq(sampleSizeZ, sampleTime);
                        samplingFreqsAZ.add(samplingFreqZ);

                        currFreqAccX = samplingFreqX;
                        lastStartTimeAccX = startTimeAccX;
                        endTimeAccX = offset;
                        startTimeAccX = offset;

                        fftDataAccX.clear();

                        currFreqAccY = samplingFreqY;
                        lastStartTimeAccY = startTimeAccY;
                        endTimeAccY = offset;
                        startTimeAccY = offset;

                        fftDataAccY.clear();

                        currFreqAccZ = samplingFreqZ;
                        lastStartTimeAccZ = startTimeAccZ;
                        endTimeAccZ = offset;
                        startTimeAccZ = offset;

                        fftDataAccZ.clear();


                        if (Math.abs(samplingFreqX - prevFreqAccX) > 1){
                            butterworthAX.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqX));
                        }
                        prevFreqAccX = samplingFreqX;

                        if (Math.abs(samplingFreqY - prevFreqAccY) > 1){
                            butterworthAY.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqY));
                        }
                        prevFreqAccY = samplingFreqY;

                        if (Math.abs(samplingFreqZ - prevFreqAccZ) > 1){
                            butterworthAZ.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqZ));
                        }
                        prevFreqAccZ = samplingFreqZ;
                    }
                    else{
                        currFreqAccX = 0;
                        startTimeAccX = 0;
                        endTimeAccX = 0;

                        currFreqAccY = 0;
                        startTimeAccY = 0;
                        endTimeAccY = 0;

                        currFreqAccZ = 0;
                        startTimeAccZ = 0;
                        endTimeAccZ = 0;
                    }


                    accFilteredX = (float)(butterworthAX.filter((double) vector[0]));

                    accFilteredY = (float)(butterworthAY.filter((double) vector[1]));

                    accFilteredZ = (float)(butterworthAZ.filter((double) vector[2]));


                    /*
                    OAX = accFilteredX;
                    OAXmap.put(OATime, accFilteredX);
                    OAY = accFilteredY;
                    OAYmap.put(OATime, accFilteredY);
                    OAZ = accFilteredZ;
                    OAZmap.put(OATime, accFilteredZ);

                     */


                    if(m.isORunning() && m.isRRunning()) {
                        m.setOATime(offset);
                        m.setRATime(offset);


                        t = m.getRATime();
                        t = Float.parseFloat(df.format(t));

                        m.putOAD(t, new float[]{accFilteredX, accFilteredY, accFilteredZ});
                        m.putOATimes(offset);
                        m.putOAData(new float[]{accFilteredX, accFilteredY, accFilteredZ});

                        m.putRAD(t, new float[]{accFilteredX, accFilteredY, accFilteredZ});
                        m.putRATimes(offset);
                        m.putRAData(new float[]{accFilteredX, accFilteredY, accFilteredZ});
                    }



                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), t,
                            accXValue, accFilteredX, accYValue, accFilteredY, accZValue, accFilteredZ).getBytes());



                    accSampleNum++;
                }
                else if (prefix.equals("acceleration") && sensorConfigType.equals("acceleration-mag")) {
                    calcRealTimestamp(data, 0xffffffffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    float[] vector = data.value(float[].class);
                    Double mag = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2]* vector[2]);
                    float magValue = mag.floatValue();
                    accMagSampleNum++;
                    accMag = magValue;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            vector[0], vector[1], vector[2], magValue).getBytes());
                    //accXValue = vector[0];
                    Log.d("acc", "poo");
                }
                else if (prefix.equals("angular-velocity") && sensorConfigType.equals("angular-velocity")) {
                    //define the filter algorithm object
                    ifGyro = true;  //check if Gyroscope is enabled


                    calcRealTimestamp(data, 0xffffffffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f; //get current time


                    float[] vector = data.value(float[].class);
                    //get current data
                    gyroXValue = vector[0];
                    gyroYValue = vector[1];
                    gyroZValue = vector[2];

                    //DoFFT
                    endTimeGyro = offset;
                    double timeDifference = endTimeGyro - startTimeGyroX;
                    fftDataGyroX.add(gyroXValue);
                    fftDataGyroY.add(gyroYValue);
                    fftDataGyroZ.add(gyroZValue);


                    if(ifStop){
                        //after user stop collecting data, the program collects all samples between
                        //last cycle and stop time
                        float sampleSizeX          = fftDataGyroX.size();    //size of the sample
                        float sampleSizeY          = fftDataGyroY.size();
                        float sampleSizeZ          = fftDataGyroZ.size();



                        float sampleTime            = calculateCaptureTime(startTimeGyro, endTimeGyro);       //time when the sample is collected
                        float samplingFreqX       = calculateFreq(sampleSizeX, sampleTime);
                        float samplingFreqY       = calculateFreq(sampleSizeY, sampleSizeY);
                        float samplingFreqZ       = calculateFreq(sampleSizeZ, sampleSizeZ);
                        samplingFreqsGX.add(samplingFreqX);
                        samplingFreqsGY.add(samplingFreqY);
                        samplingFreqsGZ.add(samplingFreqZ);

                    }
                    else if(timeDifference >= 5){
                        //update data every 5 sec

                        //apply FFT filter
                        float sampleSizeX          = fftDataGyroX.size();    //size of the sample
                        float sampleSizeY          = fftDataGyroY.size();
                        float sampleSizeZ          = fftDataGyroZ.size();

                        float sampleTime          = calculateCaptureTime(startTimeGyro, endTimeGyro);       //time when the sample is collected
                        float samplingFreqX        = calculateFreq(sampleSizeX, sampleTime);
                        samplingFreqsGX.add(samplingFreqX);
                        float samplingFreqY        = calculateFreq(sampleSizeY, sampleTime);
                        samplingFreqsGY.add(samplingFreqY);
                        float samplingFreqZ        = calculateFreq(sampleSizeZ, sampleTime);
                        samplingFreqsGZ.add(samplingFreqZ);

                        currFreqGyroX = samplingFreqX;
                        lastStartTimeGyroX = startTimeGyroX;
                        endTimeGyroX = offset;
                        startTimeGyroX = offset;

                        fftDataGyroX.clear();

                        currFreqGyroY = samplingFreqY;
                        lastStartTimeGyroY = startTimeGyroY;
                        endTimeGyroY = offset;
                        startTimeGyroY = offset;

                        fftDataGyroY.clear();

                        currFreqGyroZ = samplingFreqZ;
                        lastStartTimeGyroZ = startTimeGyroZ;
                        endTimeGyroZ = offset;
                        startTimeGyroZ = offset;
                        startTimeGyro = endTimeGyro;
                        fftDataGyroZ.clear();

                        //update samplingFreq of Accelerometer if difference in frequency > 1.0
                        if (Math.abs(samplingFreqX - prevFreqGyroX) > 1){
                            butterworthGX.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqX));
                            m.setGyroPeakX( findButterWorthFreq(samplingFreqX));
                        }
                        prevFreqGyroX = samplingFreqX;
                        //update samplingFreq of Gyroscope if difference in frequency > 1.0
                        if (Math.abs(samplingFreqY - prevFreqGyroY) > 1){
                            butterworthGY.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqY));
                            m.setGyroPeakY( findButterWorthFreq(samplingFreqX));
                        }
                        prevFreqGyroY = samplingFreqY;

                        if (Math.abs(samplingFreqZ - prevFreqGyroZ) > 1){
                            butterworthGZ.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqZ));
                        }
                        prevFreqGyroZ = samplingFreqZ;
                    }
                    else{
                        //otherwise don't worry about difference in frequency
                        currFreqGyroX = 0;
                        startTimeGyroX = 0;
                        endTimeGyroX = 0;

                        currFreqGyroY = 0;
                        startTimeGyroY = 0;
                        endTimeGyroY = 0;

                        currFreqGyroZ = 0;
                        startTimeGyroZ = 0;
                        endTimeGyroZ = 0;
                    }



                    gyroFilteredX = (float)(butterworthGX.filter((double) vector[0]));
                    gyroFilteredY = (float)(butterworthGY.filter((double) vector[1]));

                    gyroFilteredZ = (float)(butterworthGZ.filter((double) vector[2]));


                    if(m.isORunning() && m.isRRunning()) {
                        //collect Accelerometer data and Gyroscope data when they are enabled
                        //the data are collected for calculating orientation
                        m.setOGTime(offset);
                        m.setRGTime(offset);

                        t = m.getOGTime();


                        t = Float.parseFloat(df.format(t));

                        m.putOGD(t, new float[]{gyroFilteredX, gyroFilteredY, gyroFilteredZ});
                        m.putOGTimes(offset);
                        m.putOGData(new float[]{gyroFilteredX, gyroFilteredY, gyroFilteredZ});
                        m.putRGD(t, new float[]{gyroFilteredX, gyroFilteredY, gyroFilteredZ});
                        m.putRGTimes(offset);
                        m.putRGData(new float[]{gyroFilteredX, gyroFilteredY, gyroFilteredZ});
                    }



                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), t,
                            gyroXValue, gyroFilteredX, gyroYValue, gyroFilteredY, gyroZValue, gyroFilteredZ).getBytes());



                    gyroSampleNum++;

                }
                else if (prefix.equals("angular-velocity") && sensorConfigType.equals("angular-velocity-peak") ) {
                    //define the filter algorithm object


                    calcRealTimestamp(data, 0xffffffffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;

                    gyroPeakSampleNum++;
                    float[] vector = data.value(float[].class);

                    gyroPeakXValue = vector[0];
                    gyroPeakYValue = vector[1];
                    gyroPeakZValue = vector[2];

                    gyroPeakFilteredX = (float)(butterworthGyroPeakX.filter((double) vector[0]));
                    gyroPeakFilteredY = (float)(butterworthGyroPeakY.filter((double) vector[0]));
                    //gyroFilteredZ = (float)(butterworth3.filter((double) vector[2]));

//                    //DoFFT
//                    endTimePeak = offset;
//                    double timeDifference = endTimePeak - startTimePeak;
//                    //fftData.add(gyroPeakYValue);
//
//
//
//                    if(ifStop){
//                        float sampleSize          = fftData.size();    //size of the sample
//                        float sampleTime          = calculateCaptureTime(startTimePeak, endTimePeak);       //time when the sample is collected
//                        float samplingFreq        = calculateFreq(sampleSize, sampleTime);
//                        samplingFreqs.add(samplingFreq);
//
//                    }
//                    else if(timeDifference >= 10){
//
//                        //apply FFT filter
//                        float sampleSize          = fftData.size();    //size of the sample
//                        float sampleTime          = calculateCaptureTime(startTimePeak, endTimePeak);       //time when the sample is collected
//                        float samplingFreq        = calculateFreq(sampleSize, sampleTime);
//                        samplingFreqs.add(samplingFreq);
//
//                        currFreqGyroPeak = samplingFreq;
//                        lastStartTimeGyroPeak = startTimeGyroPeak;
//                        endTimeGyroPeak = offset;
//                        startTimeGyroPeak = offset;
//                        startTimePeak = endTimePeak;
//                        fftData.clear();
//
//
//
//                        if (Math.abs(samplingFreq - prevFreqGyroPeak) > 1){
//                            gyroPeakOrder = findButterWorthFreq(samplingFreq);
//                            butterworthGyroPeak.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreq));
//                        }
//                        prevFreqGyroPeak = samplingFreq;
//                    }
//                    else{
//                        currFreqGyroPeak = 0;
//                        startTimeGyroPeak = 0;
//                        endTimeGyroPeak = 0;
//                        gyroPeakOrder = 0;
//                    }

                    //get v1, v2, v3
                    if(gyroPeakSampleNum > 2){
                        v1  = v2;
                        v2 = v3;
                        v3 = gyroPeakFilteredY;
                    }
                    else if(gyroPeakSampleNum > 1){
                        v1  = 0f;
                        v2 = v3;
                        v3 = gyroPeakFilteredY;
                    }
                    else {
                        v1  = 0f;
                        v2 = 0f;
                        v3 = gyroPeakFilteredY;
                    }


                    // Update for thetaY
                    thetaY =  gyroPeakYValue * period;
                    thetaYFilt = (float) m.getGyroPeakY().filter(thetaY);

                    // Convert to radians and check for valid range
                    if (Math.abs(thetaY) < 1E7) { // Adjust this threshold as needed
                        thetaY_rad = (float) (thetaY * Math.PI/180);
                        thetaY_cos = (float)Math.cos(thetaY_rad);
                        thetaY_sin = (float)Math.sin(thetaY_rad);
                    } else {
//                        // Handle error: thetaYFilt out of range
//                        thetaY_rad = 0;
//                        thetaY_cos = 1;
//                        thetaY_sin = 0;
                    }
                    prevThetaY = thetaY;

                    // Update for thetaX
                    thetaX =  gyroPeakXValue * period;
                    thetaXFilt = (float) m.getGyroPeakX().filter(thetaX);

// Convert to radians and check for valid range
//                    if (Math.abs(thetaX) < 1E7) { // Adjust this threshold as needed
//                        thetaX_rad = (float) (thetaX * Math.PI/180);
//                        thetaX_cos = (float) Math.cos(thetaX_rad);
//                        thetaX_sin = (float) Math.sin(thetaX_rad);
//                    } else {
//                        // Handle error: thetaXFilt out of range
//                        thetaX_rad = 0;
//                        thetaX_cos = 1;
//                        thetaX_sin = 0;
//                    }
                    thetaX_rad = (float) (thetaX * Math.PI/180);
                    thetaX_cos = (float) Math.cos(thetaX_rad);
                    thetaX_sin = (float) Math.sin(thetaX_rad);
                    prevThetaX = thetaX;

                    // Compute horizontal velocity
                    knee_h = (float) (m.getKneeHeight() * 1.0f / 100.0f);
                    v_ay_gyro_hor = (float) (- gyroPeakFilteredY * Math.PI / (180 * knee_h));

                    v_ay_hor = v_ay_gyro_hor * thetaY_cos;

                    knee_r = (float) (m.getKneeRadius() * 1.0f / (2*Math.PI * 100));
                    v_ax_gyro_hor = (float) (- gyroPeakFilteredX * Math.PI/(180*knee_r));
                    v_ax_hor = v_ax_gyro_hor * thetaX_cos;

                    v_hor = v_ax_hor + v_ay_hor;
                    Log.d("walking speed", v_ax_hor +"/"+v_ay_hor+"/"+knee_h + "/" + thetaY_cos + "/" + thetaY_sin);

                    // Logging for debugging


                    if((v2 > v3) && (v2 > v1) && (v2 > threshold_max)){
                        count_peak ++;
                        data_num++;
                        peak_index = gyroPeakSampleNum -1;
                        peak = v2;
                        mid_swing = peak;
                        mid_swing_index = peak_index;
                        ifMidSwing = true;
                        last_heal_strike = offset;
                        special_value = peak;
                    }
                    else if((v2 < v1) && (v2 < v3) && (v2 < threshold_min)){
                        count_valley++;
                        valley_index = gyroPeakSampleNum - 1;
                        valley = v2;
                        if(count_valley % 2 != 0 && valley < 200f){
                            data_num++;
                            prev_data_num = data_num;
                            toe_off = valley;
                            m.addToTotalToeOff(toe_off);
                            m.setRealToeOff(toe_off);
                            m.addToNumToeOff();
                            toe_off_index = valley_index;
                            ifToeOff = true;

                            if(last_toe_off != 2001f
                            ){
                                stride_phase = offset - last_toe_off;
                                m.addToStrideLength(stride_phase);
                                m.addToTotalStride();
                                m.setRealStrideTime(stride_phase);

                                //calculate stride len
                                strideLen = Math.abs(v_hor) * period;
                                //strideLen = stride_phase;
                                m.addToTotalStrideLen(strideLen);
                                m.setRealStrideLen(strideLen);

                                m.addToNumStrideLen();
                                prevStrideLen = strideLen;
                                if( ((offset - last_toe_off) * period) != 0) {
                                    walkingSpeed = strideLen / (offset - last_toe_off);
                                }
                                m.addToTotalWalkingSpeed(walkingSpeed);
                                m.setRealWalkingSpeed(walkingSpeed);
                                m.addToNumWalkingSpeed();

                            }

                            last_toe_off = offset;
                            if(last_heal_strike != 2001f){
                                stance_phase = offset - last_heal_strike;
                                m.addToStance(stance_phase);
                                m.addToTotalStance();
                                m.setRealStanceTime(stance_phase);
                            }

                            special_value = valley;
                        }else if (count_valley % 2 == 0  && valley < threshold_min){
                            data_num++;
                            heel_strike = valley;
                            m.addToTotalHeelStrike(heel_strike);
                            m.setRealHeelStrike(heel_strike);
                            m.addToNumHeelStrike();
                            heel_strike_index = valley_index;
                            ifHealStrike = true;
                            last_heal_strike = offset;
                            if(last_toe_off != 2001f){
                                swing_phase = offset - last_toe_off;
                                m.addToSwing(swing_phase);
                                m.addToTotalSwing();
                                m.setRealSwingTime(swing_phase);
                            }
                            special_value = valley;
                        }


                    }else{
                        peak = 0f;
                        valley = 0f;
                        peak_index = 0;
                        valley_index = 0;
                        mid_swing_index = 0;
                        toe_off_index = 0;
                        heel_strike_index = 0;
                        mid_swing = 0f;
                        toe_off = 0f;
                        heel_strike = 0f;
                        ifMidSwing = false;
                        ifToeOff = false;
                        ifHealStrike = false;
                        swing_phase = 0f;
                        stance_phase = 0f;
                        special_value = 0f;

                    }

                    //compute vertical velocity
                    v_ay_gyro_ver = (float) (gyroPeakYValue * Math.PI / 180 * knee_h);
                    v_ay_ver = v_ay_gyro_ver * thetaY_sin;

                    //update fc
                    fc = prev_fc + v_ay_ver * period;

                    //find MFC peak
                    if(prev_prev_fc != Integer.MAX_VALUE && prev_fc < prev_prev_fc && prev_fc < fc){
                        fc_min = fc;
                        fc_min_time = offset;

                        if(prev_prev_fc_min != Integer.MAX_VALUE && prev_fc < prev_prev_fc_min
                            && prev_fc < fc_min){
                            MFC = Math.abs(fc_min - prev_fc);
                            m.addToTotalMFC(MFC);
                            m.setRealMFC(MFC);
                            m.addToNumMFC();
                        }

                        prev_prev_fc_min = prev_fc_min;
                        prev_prev_fc_min_time = prev_fc_min_time;
                        prev_fc_min = fc_min;
                        prev_fc_min_time = fc_min_time;



                    }

                    prev_prev_fc = prev_fc;
                    prev_fc = fc;


                    

                    if(ifStop){
                        int freqSize = samplingFreqs.size();
                        //find average

                        float sumFreq = 0;
                        for(int i = 0; i < samplingFreqs.size(); i ++){
                            sumFreq += samplingFreqs.get(i);
                        }
                        //avoid DivisionByZero Exception
                        if(samplingFreqs.size() != 0){
                            currFreqGyroPeak = sumFreq / samplingFreqs.size();
                        }
                        endTimeGyroPeak = offset;
                        float dst = (strideLen / 2) - swing_phase;
                        fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f," +
                                        "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                last.getTimeInMillis(), formatTimestamp(last), offset,
                                gyroPeakFilteredY, peak_index, peak, valley_index, valley, mid_swing_index, mid_swing, toe_off_index, toe_off,
                                heel_strike_index, heel_strike, currFreqGyroPeak, lastStartTimeGyroPeak, endTimeGyroPeak,swing_phase, stance_phase,
                                stride_phase,swing_phase,stance_phase,toe_off,heel_strike,dst,swing_phase,strideLen/2,strideLen,walkingSpeed,
                                MFC, (int)m.getTotalStride()/2).getBytes());
                    }
                    else if(peak != 0 || valley != 0||currFreqGyroPeak !=0 || startTimeGyroPeak != 0||endTimeGyroPeak!=0 ) {
                        float dst = (strideLen / 2) - swing_phase;
                        fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f," +
                                        "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                last.getTimeInMillis(), formatTimestamp(last), offset,
                                gyroPeakFilteredY, peak_index, peak, valley_index, valley, mid_swing_index, mid_swing, toe_off_index, toe_off,
                                heel_strike_index, heel_strike, currFreqGyroPeak, lastStartTimeGyroPeak, endTimeGyroPeak,swing_phase, stance_phase,
                                stride_phase,swing_phase,stance_phase,toe_off,heel_strike,dst,swing_phase,strideLen/2,strideLen,walkingSpeed,
                                MFC, (int)m.getTotalStride()/2).getBytes());
                    }
                    float dst = (strideLen / 2) - swing_phase;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f," +
                                    "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gyroPeakFilteredY, peak_index, peak, valley_index, valley, mid_swing_index, mid_swing, toe_off_index, toe_off,
                            heel_strike_index, heel_strike, currFreqGyroPeak, lastStartTimeGyroPeak, endTimeGyroPeak,swing_phase, stance_phase,
                            stride_phase,swing_phase,stance_phase,toe_off,heel_strike,dst,swing_phase,strideLen/2,strideLen,walkingSpeed,
                            MFC, (int)m.getTotalStride()/2).getBytes());

                }
                else if (prefix.equals("magnetic-field")) {
                    ifMag = true;
                    calcRealTimestamp(data, 0xffffffffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    OMTime = offset;

                    float[] vector = data.value(float[].class);

                    //DoFFT
                    endTimeMag = offset;
                    double timeDifference = endTimeMag - startTimeMag;


                    fftDataMagX.add(vector[0]);
                    fftDataMagY.add(vector[1]);
                    fftDataMagZ.add(vector[2]);





                    if(ifStop){
                        float sampleSizeX          = fftDataMagX.size();    //size of the sample
                        float sampleSizeY          = fftDataMagY.size();
                        float sampleSizeZ          = fftDataMagZ.size();



                        float sampleTime            = calculateCaptureTime(startTimeMag, endTimeMag);       //time when the sample is collected
                        float samplingFreqX       = calculateFreq(sampleSizeX, sampleTime);
                        float samplingFreqY       = calculateFreq(sampleSizeY, sampleTime);
                        float samplingFreqZ       = calculateFreq(sampleSizeZ, sampleTime);
                        samplingFreqsMX.add(samplingFreqX);
                        samplingFreqsMY.add(samplingFreqY);
                        samplingFreqsMZ.add(samplingFreqZ);

                    }
                    else if(timeDifference >= 10){

                        //apply FFT filter
                        float sampleSizeX          = fftDataMagX.size();    //size of the sample
                        float sampleSizeY          = fftDataMagY.size();
                        float sampleSizeZ          = fftDataMagZ.size();

                        float sampleTime          = calculateCaptureTime(startTimeMag, endTimeMag);       //time when the sample is collected
                        float samplingFreqX        = calculateFreq(sampleSizeX, sampleTime);
                        samplingFreqsMX.add(samplingFreqX);
                        float samplingFreqY        = calculateFreq(sampleSizeY, sampleTime);
                        samplingFreqsMY.add(samplingFreqY);
                        float samplingFreqZ        = calculateFreq(sampleSizeZ, sampleTime);
                        samplingFreqsMZ.add(samplingFreqZ);

                        currFreqMagX = samplingFreqX;
                        lastStartTimeMagX = startTimeMagX;
                        endTimeMagX = offset;
                        startTimeMagX = offset;

                        fftDataMagX.clear();

                        currFreqMagY = samplingFreqY;
                        lastStartTimeMagY = startTimeMagY;
                        endTimeMagY = offset;
                        startTimeMagY = offset;

                        fftDataMagY.clear();

                        currFreqMagZ = samplingFreqZ;
                        lastStartTimeMagZ = startTimeMagZ;
                        endTimeMagZ = offset;
                        startTimeMagZ = offset;

                        fftDataMagZ.clear();


                        if (Math.abs(samplingFreqX - prevFreqMagX) > 1){
                            butterworthMX.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqX));
                        }
                        prevFreqMagX = samplingFreqX;

                        if (Math.abs(samplingFreqY - prevFreqMagY) > 1){
                            butterworthMY.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqY));
                        }
                        prevFreqMagY = samplingFreqY;

                        if (Math.abs(samplingFreqZ - prevFreqMagZ) > 1){
                            butterworthMZ.lowPass(5, frequency.doubleValue(), findButterWorthFreq(samplingFreqZ));
                        }
                        prevFreqMagZ = samplingFreqZ;
                    }
                    else{
                        currFreqMagX = 0;
                        startTimeMagX = 0;
                        endTimeMagX = 0;

                        currFreqMagY = 0;
                        startTimeMagY = 0;
                        endTimeMagY = 0;

                        currFreqMagZ = 0;
                        startTimeMagZ = 0;
                        endTimeMagZ = 0;
                    }
                    float MX = (float)(butterworthMX.filter((double) vector[0]));

                    float MY = (float)(butterworthMY.filter((double) vector[1]));

                    float MZ= (float)(butterworthMZ.filter((double) vector[2]));






                    List<Float> magnetometerValuesX = List.of(previous_MX, MX);

                    int originalSamplingRate = 25; // Hz
                    int targetSamplingRate = 50; // Hz

                    List<Float> resampledValuesX = resample(magnetometerValuesX, originalSamplingRate, targetSamplingRate);

                    List<Float> magnetometerValuesY = List.of(previous_MY, MY);

                    List<Float> resampledValuesY = resample(magnetometerValuesY, originalSamplingRate, targetSamplingRate);

                    List<Float> magnetometerValuesZ = List.of(previous_MZ, MZ);

                    List<Float> resampledValuesZ = resample(magnetometerValuesZ, originalSamplingRate, targetSamplingRate);

                    float midTime = (OMTime + previous_MTime) / 2f;


                    /*
                    OMX = MX;
                    float midMX = (MX + previous_MX)/2;
                    OMXmap.put(midTime, resampledValuesX.get(0));
                    OMXmap.put(OMTime, resampledValuesX.get(1));

                    OMY = MY;
                    float midMY = (MY + previous_MY)/2f;
                    OMYmap.put(midTime, resampledValuesY.get(0));
                    OMYmap.put(OMTime, resampledValuesY.get(1));

                    OMZ = MZ;
                    float midMZ = (MZ + previous_MZ)/2f;
                    OMZmap.put(midTime, resampledValuesZ.get(0));
                    OMZmap.put(OMTime, resampledValuesZ.get(1));


                     */



                    if (offset != 0 ){
                        fos.write(String.format(Locale.US, "%d,%s,%.3f,%.9f,%.9f,%.9f%n",
                                last.getTimeInMillis(), formatTimestamp(last), midTime,
                                resampledValuesX.get(0), resampledValuesY.get(0),  resampledValuesZ.get(0)).getBytes());
                    }

                    float mx = resampledValuesX.get(1);
                    float my =  resampledValuesY.get(1);
                    float mz = resampledValuesZ.get(1);
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.9f,%.9f,%.9f%n",
                            last.getTimeInMillis(), formatTimestamp(last), OMTime,
                            mx, my, mz).getBytes());

                    magSampleNum += 2;
                    magXValue = mx;
                    magYValue = my;
                    magZValue = mz;


                    previous_MX = MX;
                    previous_MY = MY;
                    previous_MZ = MZ;
                    previous_MTime = offset;



                    //resample
                    /*
                    double originalRate = 10.0; // Hz
                    double[] originalTime = new double[(int) originalRate];
                    for (int i = 0; i < originalTime.length; i++) {
                        originalTime[i] = i / originalRate;
                    }

                    // original magnetometer data
                    double[] originalData = new double[originalTime.length];
                    for (int i = 0; i < originalData.length; i++) {
                        originalData[i] = Math.sin(2 * Math.PI * 2 * originalTime[i]);
                    }

                    // desired resampling rate and time values
                    double resampleRate = 100.0; // Hz
                    double[] resampleTime = new double[(int) (originalTime.length * (resampleRate / originalRate))];
                    for (int i = 0; i < resampleTime.length; i++) {
                        resampleTime[i] = i / resampleRate;
                    }

                    // use linear interpolation to resample the data
                    UnivariateInterpolator interpolator = new LinearInterpolator();
                    double[] resampleData = interpolator.interpolate(originalTime, originalData).value(resampleTime);

                     */







                }
                else if (prefix.equals("linear-acceleration") || prefix.equals("gravity")) {
                        float offset = (last.getTimeInMillis() - start) / 1000.f;

                    float[] vector = data.value(float[].class);
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            vector[0], vector[1], vector[2]).getBytes());
                }
                else if (prefix.equals("euler-angles")) {
                    calcRealTimestamp(data, 0xffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;

                    float[] vector4 = data.value(float[].class);
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            vector4[1], vector4[2], vector4[3], vector4[0]).getBytes());
                }
                else if (prefix.equals("quaternion")) {
                    calcRealTimestamp(data, 0xffL);
                    float offset = (last.getTimeInMillis() - start) / 1000.f;

                    float[] vector4 = data.value(float[].class);
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            vector4[0], vector4[1], vector4[2], vector4[3]).getBytes());
                }

                else if (prefix.equals("pressure")||prefix.equals("relative-humidity") || prefix.equals("illuminance") || prefix.equals("temperature")) {
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;

                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            data.value(Float.class)).getBytes());
                } else if (prefix.equals("adc") && sensorConfigType.equals("gpio0")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAdcSampleNum ++;
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            val).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio0")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    //Log.d("adc", "poo");
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                }
                else if (prefix.equals("adc") && sensorConfigType.equals("gpio1")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    gpioAdcSampleNum ++;
                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            val).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio1")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                }
                else if (prefix.equals("adc") && sensorConfigType.equals("gpio2")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    gpioAdcSampleNum ++;
                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAdcValue).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio2")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                }
                else if (prefix.equals("adc") && sensorConfigType.equals("gpio3")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    gpioAdcSampleNum ++;
                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAdcValue).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio3")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                }
                else if (prefix.equals("adc") && sensorConfigType.equals("gpio4")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    gpioAdcSampleNum ++;
                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAdcValue).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio4")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                } else if (prefix.equals("adc") && sensorConfigType.equals("gpio5")) {
                    //Log.d("adc", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");
                    gpioAdcSampleNum ++;
                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    float val = data.value(Short.class);
                    gpioAdcValue = val;
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAdcValue).getBytes());
                    Log.d("adc", "poo");

                } else if (prefix.equals("abs-ref")&& sensorConfigType.equals("gpio5")) {
                    Log.d("abs", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("adc", "poo");

                    //Log.d("adc", "poo");
                    //TODO: need to modify here !
                    gpioAbsSampleNum ++;
                    float val = data.value(Float.class);
                    gpioAbsValue = val;
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            gpioAbsValue).getBytes());
                    //Log.d("adc", "poo");

                }



                else if (prefix.equals("digital")) {
                    //Log.d("digital", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }

                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("digital", "poo");

                    //Log.d("digital", "poo");
                    //TODO: need to modify here !
                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                            last.getTimeInMillis(), formatTimestamp(last), offset,
                            data.value(Byte.class)).getBytes());
                    Log.d("digital", "poo");

                }
                else if (prefix.equals("angular-velocity")&& sensorConfigType.equals("orientation")) {
                    //Log.d("digital", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }
                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("digital", "poo");

                    //Log.d("digital", "poo");
                    //TODO: need to modify here !
                    float OATime = m.getOATime();
                    float time;
                    time = OATime;
                    if (!m.oTimesContains(time)) {
                        m.addtoTimes(time);

                    }
                    float OGTime = m.getOGTime();

                    if (!ifStop) {
                        ArrayList<Float> oaTimes = m.getOATimes();
                        int oaTimesSize = m.getOATimes().size();
                        ArrayList<Float> ogTimes = m.getOGTimes();
                        int ogTimesSize = m.getOGTimes().size();
                        float oaLast = oaTimes.get(oaTimes.size()-1);
                        float ogLast = ogTimes.get(ogTimes.size()-1);

                        float prevT = m.getOAPrevTime();
                        float minT;
                        if (oaLast < ogLast) {
                            minT = oaLast;
                        } else {
                            minT = ogLast;
                        }

                        if(minT != prevOrientationTime || minT == 0){
                            float[] oa = m.getOA(minT);
                            float[] og = m.getOG(minT);
                            float[] o = orientation(50, oa[0], oa[1], oa[2], og[0], og[1], og[2],m);
                            orientationX = o[0];
                            orientationY = o[1];
                            orientationZ = o[2];
                            orientationSampleNum+=2;
                            prevOrientationTime = minT;
                        }

                        if (minT - prevT >= 2) {

                            List<Float> oX = new ArrayList<>();
                            List<Float> oY = new ArrayList<>();
                            List<Float> oZ = new ArrayList<>();

                            for (float t = prevT; t <= minT; t += 0.02) {
                                t = Float.parseFloat(df.format(t));
                                if (m.oaDContains(t) && m.ogDContains(t)) {
                                    float[] oa = m.getOA(t);
                                    float[] og = m.getOG(t);
                                    float[] orientation = orientation(50, oa[0], oa[1], oa[2], og[0], og[1], og[2],m);
                                    oX.add(orientation[0]);
                                    oY.add(orientation[1]);
                                    oZ.add(orientation[2]);
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), t,
                                            oa[0], oa[1], oa[2], og[0], og[1],
                                            og[2], orientation[0], orientation[1], orientation[2]).getBytes());
                                }
                                m.removeOAD(t);
                                m.removeOGD(t);

                            }

                            for(int i = 0; i < oX.size(); i ++){
                                if(i == 0){
                                    float currOX = oX.get(i);
                                    float nextOX = oX.get(i+1);
                                    float currOY = oY.get(i);
                                    float nextOY = oY.get(i+1);
                                    float currOZ = oZ.get(i);
                                    float nextOZ = oZ.get(i+1);

                                    //check value and the edge of the interval, make sure find local maximums
                                    if(currOX > nextOX && oXMax.isIfUpdated() && currOX > oXMax.getBuffer()){
                                        m.addToOXMax(currOX);
                                        m.addToNumOXMax();
                                    }
                                    if(currOY > nextOY && oYMax.isIfUpdated() && currOY > oYMax.getBuffer()){
                                        m.addToOYMax(currOY);
                                        m.addToNumOYMax();
                                    }
                                    if(currOZ > nextOZ && oZMax.isIfUpdated() && currOZ > oZMax.getBuffer()){
                                        m.addToOZMax(currOZ);
                                        m.addToNumOZMax();
                                    }

                                    //check value and the edge of the interval, make sure find local minimums
                                    if(currOX < nextOX && oXMin.isIfUpdated() && currOX < oXMin.getBuffer()){
                                        m.addToOXMin(currOX);
                                        m.addToNumOXMin();
                                    }
                                    if(currOY < nextOY && oYMin.isIfUpdated() && currOY < oYMin.getBuffer()){
                                        m.addToOYMin(currOY);
                                        m.addToNumOYMin();
                                    }
                                    if(currOZ < nextOZ && oZMin.isIfUpdated() && currOZ < oZMin.getBuffer()){
                                        m.addToOZMin(currOZ);
                                        m.addToNumOZMin();
                                    }

                                    oXMax.setIfUpdated(false);
                                    oXMin.setIfUpdated(false);
                                    oYMax.setIfUpdated(false);
                                    oYMin.setIfUpdated(false);
                                    oZMax.setIfUpdated(false);
                                    oZMin.setIfUpdated(false);
                                }
                                else if (i == (oX.size() -1)){
                                    float currOX = oX.get(i);
                                    float prevOX = oX.get(i-1);
                                    float currOY = oY.get(i);
                                    float prevOY = oY.get(i-1);
                                    float currOZ = oZ.get(i);
                                    float prevOZ = oZ.get(i-1);

                                    if(currOX > prevOX) {
                                        oXMax.setBuffer(currOX);
                                        oXMax.setIfUpdated(true);
                                    }
                                    if(currOX < prevOX){
                                        oXMin.setBuffer(currOX);
                                        oXMin.setIfUpdated(true);
                                    }
                                    if(currOY > prevOY) {
                                        oYMax.setBuffer(currOY);
                                        oYMax.setIfUpdated(true);
                                    }
                                    if(currOY < prevOY){
                                        oYMin.setBuffer(currOY);
                                        oYMin.setIfUpdated(true);
                                    }
                                    if(currOZ > prevOZ) {
                                        oZMax.setBuffer(currOZ);
                                        oZMax.setIfUpdated(true);
                                    }
                                    if(currOZ < prevOZ){
                                        oZMin.setBuffer(currOZ);
                                        oZMin.setIfUpdated(true);
                                    }
                                }
                                else{
                                    float currOX = oX.get(i);
                                    float nextOX = oX.get(i+1);
                                    float prevOX = oX.get(i-1);
                                    float currOY = oY.get(i);
                                    float nextOY = oY.get(i+1);
                                    float prevOY = oY.get(i-1);
                                    float currOZ = oZ.get(i);
                                    float nextOZ = oZ.get(i+1);
                                    float prevOZ = oZ.get(i-1);

                                    if(currOX > prevOX && currOX > nextOX){
                                        m.addToOXMax(currOX);
                                        m.addToNumOXMax();
                                    }
                                    if(currOX < prevOX && currOX < nextOX){
                                        m.addToOXMin(currOX);
                                        m.addToNumOXMin();
                                    }
                                    if(currOY > prevOY && currOY > nextOY){
                                        m.addToOYMax(currOY);
                                        m.addToNumOYMax();
                                    }
                                    if(currOY < prevOY && currOY < nextOY){
                                        m.addToOYMin(currOY);
                                        m.addToNumOYMin();
                                    }
                                    if(currOZ > prevOZ && currOZ > nextOZ){
                                        m.addToOZMax(currOZ);
                                        m.addToNumOZMax();
                                    }
                                    if(currOZ < prevOZ && currOZ < nextOZ){
                                        m.addToOZMin(currOZ);
                                        m.addToNumOZMin();
                                    }

                                }
                            }

                            m.clearOAData();
                            m.clearOGData();
                            m.setOAPrevTime(minT);
                            m.clearOATimes();
                            m.clearOGTimes();
                            m.clearOTimes();


                        }
                        //}
                    }else {
                        if (!m.isIfOrientated()) {
                            ArrayList<Float> oaTimes = m.getOATimes();
                            int oaTimesSize = m.getOATimes().size();
                            ArrayList<Float> ogTimes = m.getOGTimes();
                            int ogTimesSize = m.getOGTimes().size();
                            float oaLast = oaTimes.get(oaTimesSize - 1);
                            float ogLast = ogTimes.get(ogTimesSize - 1);

                            Log.d("last time", "last time a= " + oaLast);
                            Log.d("last time", "last time g= " + ogLast);

                            if (oaLast < ogLast) {
                                float[] resampledTime = resample_time(oaTimes, ogTimesSize);
                                float[][] resampledDataG = resample_data(m.getOGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];
                                float[][] resampledDataA = resample_data(m.getOAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];

                                List<Float> oX = new ArrayList<>();
                                List<Float> oY = new ArrayList<>();
                                List<Float> oZ = new ArrayList<>();

                                for (int i = 0; i < resampledTime.length; i++) {
                                    float[] orientation = orientation(50,resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i], m );
                                    oX.add(orientation[0]);
                                    oY.add(orientation[1]);
                                    oZ.add(orientation[2]);
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i],
                                            orientation[0], orientation[1], orientation[2]).getBytes());
                                }

                                for(int i = 0; i < oX.size(); i ++){
                                    if(i == 0){
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);

                                        //check value and the edge of the interval, make sure find local maximums
                                        if(currOX > nextOX && oXMax.isIfUpdated() && currOX > oXMax.getBuffer()){
                                            m.addToOXMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOY > nextOY && oYMax.isIfUpdated() && currOY > oYMax.getBuffer()){
                                            m.addToOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOZ > nextOZ && oZMax.isIfUpdated() && currOZ > oZMax.getBuffer()){
                                            m.addToOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }

                                        //check value and the edge of the interval, make sure find local minimums
                                        if(currOX < nextOX && oXMin.isIfUpdated() && currOX < oXMin.getBuffer()){
                                            m.addToOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY < nextOY && oYMin.isIfUpdated() && currOY < oYMin.getBuffer()){
                                            m.addToOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ < nextOZ && oZMin.isIfUpdated() && currOZ < oZMin.getBuffer()){
                                            m.addToOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                        oXMax.setIfUpdated(false);
                                        oXMin.setIfUpdated(false);
                                        oYMax.setIfUpdated(false);
                                        oYMin.setIfUpdated(false);
                                        oZMax.setIfUpdated(false);
                                        oZMin.setIfUpdated(false);
                                    }
                                    else if (i == (oX.size() -1)){
                                        float currOX = oX.get(i);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX) {
                                            oXMax.setBuffer(currOX);
                                            oXMax.setIfUpdated(true);
                                        }
                                        if(currOX < prevOX){
                                            oXMin.setBuffer(currOX);
                                            oXMin.setIfUpdated(true);
                                        }
                                        if(currOY > prevOY) {
                                            oYMax.setBuffer(currOY);
                                            oYMax.setIfUpdated(true);
                                        }
                                        if(currOY < prevOY){
                                            oYMin.setBuffer(currOY);
                                            oYMin.setIfUpdated(true);
                                        }
                                        if(currOZ > prevOZ) {
                                            oZMax.setBuffer(currOZ);
                                            oZMax.setIfUpdated(true);
                                        }
                                        if(currOZ < prevOZ){
                                            oZMin.setBuffer(currOZ);
                                            oZMin.setIfUpdated(true);
                                        }
                                    }
                                    else{
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX && currOX > nextOX){
                                            m.addToOXMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOX < prevOX && currOX < nextOX){
                                            m.addToOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY > prevOY && currOY > nextOY){
                                            m.addToOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOY < prevOY && currOY < nextOY){
                                            m.addToOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ > prevOZ && currOZ > nextOZ){
                                            m.addToOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }
                                        if(currOZ < prevOZ && currOZ < nextOZ){
                                            m.addToOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                    }
                                }

                                m.setOAPrevTime(t);
                                m.clearOAData();
                                m.clearOGData();

                                m.clearOATimes();
                                m.clearOGTimes();
                                m.clearOTimes();


                            } else if (oaLast > ogLast) {
                                float[] resampledTime = resample_time(ogTimes, oaTimesSize);
                                float[][] resampledDataA = resample_data(m.getOAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];
                                float[][] resampledDataG = resample_data(m.getOGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];

                                List<Float> oX = new ArrayList<>();
                                List<Float> oY = new ArrayList<>();
                                List<Float> oZ = new ArrayList<>();


                                for (int i = 0; i < resampledTime.length; i++) {
                                    float[] orientation = orientation(50,resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i], m );
                                    oX.add(orientation[0]);
                                    oY.add(orientation[1]);
                                    oZ.add(orientation[2]);
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i],
                                            orientation[0], orientation[1], orientation[2]).getBytes());
                                }

                                for(int i = 0; i < oX.size(); i ++){
                                    if(i == 0){
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);

                                        //check value and the edge of the interval, make sure find local maximums
                                        if(currOX > nextOX && oXMax.isIfUpdated() && currOX > oXMax.getBuffer()){
                                            m.addToOXMax(currOX);
                                            m.setRealOXMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOY > nextOY && oYMax.isIfUpdated() && currOY > oYMax.getBuffer()){
                                            m.addToOYMax(currOY);
                                            m.setRealOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOZ > nextOZ && oZMax.isIfUpdated() && currOZ > oZMax.getBuffer()){
                                            m.addToOZMax(currOZ);
                                            m.setRealOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }

                                        //check value and the edge of the interval, make sure find local minimums
                                        if(currOX < nextOX && oXMin.isIfUpdated() && currOX < oXMin.getBuffer()){
                                            m.addToOXMin(currOX);
                                            m.setRealOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY < nextOY && oYMin.isIfUpdated() && currOY < oYMin.getBuffer()){
                                            m.addToOYMin(currOY);
                                            m.setRealOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ < nextOZ && oZMin.isIfUpdated() && currOZ < oZMin.getBuffer()){
                                            m.addToOZMin(currOZ);
                                            m.setRealOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                        oXMax.setIfUpdated(false);
                                        oXMin.setIfUpdated(false);
                                        oYMax.setIfUpdated(false);
                                        oYMin.setIfUpdated(false);
                                        oZMax.setIfUpdated(false);
                                        oZMin.setIfUpdated(false);
                                    }
                                    else if (i == (oX.size() -1)){
                                        float currOX = oX.get(i);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX) {
                                            oXMax.setBuffer(currOX);
                                            oXMax.setIfUpdated(true);
                                        }
                                        if(currOX < prevOX){
                                            oXMin.setBuffer(currOX);
                                            oXMin.setIfUpdated(true);
                                        }
                                        if(currOY > prevOY) {
                                            oYMax.setBuffer(currOY);
                                            oYMax.setIfUpdated(true);
                                        }
                                        if(currOY < prevOY){
                                            oYMin.setBuffer(currOY);
                                            oYMin.setIfUpdated(true);
                                        }
                                        if(currOZ > prevOZ) {
                                            oZMax.setBuffer(currOZ);
                                            oZMax.setIfUpdated(true);
                                        }
                                        if(currOZ < prevOZ){
                                            oZMin.setBuffer(currOZ);
                                            oZMin.setIfUpdated(true);
                                        }
                                    }
                                    else{
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX && currOX > nextOX){
                                            m.addToOXMax(currOX);
                                            m.setRealOXMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOX < prevOX && currOX < nextOX){
                                            m.addToOXMin(currOX);
                                            m.setRealOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY > prevOY && currOY > nextOY){
                                            m.addToOYMax(currOY);
                                            m.setRealOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOY < prevOY && currOY < nextOY){
                                            m.addToOYMin(currOY);
                                            m.setRealOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ > prevOZ && currOZ > nextOZ){
                                            m.addToOZMax(currOZ);
                                            m.setRealOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }
                                        if(currOZ < prevOZ && currOZ < nextOZ){
                                            m.addToOZMin(currOZ);
                                            m.setRealOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                    }
                                }

                                m.setOAPrevTime(t);
                                m.clearOATimes();
                                m.clearOGTimes();
                                m.clearOAData();
                                m.clearOGData();
                                m.clearOTimes();


                            } else {
                                float[] resampledTime = resample_time(ogTimes, oaTimesSize);
                                float[][] resampledDataA = resample_data(m.getOAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];
                                float[][] resampledDataG = resample_data(m.getOGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];

                                List<Float> oX = new ArrayList<>();
                                List<Float> oY = new ArrayList<>();
                                List<Float> oZ = new ArrayList<>();


                                for (int i = 0; i < resampledTime.length; i++) {
                                    float[] orientation = orientation(50,resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i], m );
                                    oX.add(orientation[0]);
                                    oY.add(orientation[1]);
                                    oZ.add(orientation[2]);
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i],
                                            orientation[0], orientation[1], orientation[2]).getBytes());
                                }

                                for(int i = 0; i < oX.size(); i ++){
                                    if(i == 0){
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);

                                        //check value and the edge of the interval, make sure find local maximums
                                        if(currOX > nextOX && oXMax.isIfUpdated() && currOX > oXMax.getBuffer()){
                                            m.addToOXMax(currOX);
                                            m.setRealOXMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOY > nextOY && oYMax.isIfUpdated() && currOY > oYMax.getBuffer()){
                                            m.addToOYMax(currOY);
                                            m.setRealOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOZ > nextOZ && oZMax.isIfUpdated() && currOZ > oZMax.getBuffer()){
                                            m.addToOZMax(currOZ);
                                            m.setRealOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }

                                        //check value and the edge of the interval, make sure find local minimums
                                        if(currOX < nextOX && oXMin.isIfUpdated() && currOX < oXMin.getBuffer()){
                                            m.addToOXMin(currOX);
                                            m.setRealOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY < nextOY && oYMin.isIfUpdated() && currOY < oYMin.getBuffer()){
                                            m.addToOYMin(currOY);
                                            m.addToOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ < nextOZ && oZMin.isIfUpdated() && currOZ < oZMin.getBuffer()){
                                            m.addToOZMin(currOZ);
                                            m.setRealOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                        oXMax.setIfUpdated(false);
                                        oXMin.setIfUpdated(false);
                                        oYMax.setIfUpdated(false);
                                        oYMin.setIfUpdated(false);
                                        oZMax.setIfUpdated(false);
                                        oZMin.setIfUpdated(false);
                                    }
                                    else if (i == (oX.size() -1)){
                                        float currOX = oX.get(i);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX) {
                                            oXMax.setBuffer(currOX);
                                            oXMax.setIfUpdated(true);
                                        }
                                        if(currOX < prevOX){
                                            oXMin.setBuffer(currOX);
                                            oXMin.setIfUpdated(true);
                                        }
                                        if(currOY > prevOY) {
                                            oYMax.setBuffer(currOY);
                                            oYMax.setIfUpdated(true);
                                        }
                                        if(currOY < prevOY){
                                            oYMin.setBuffer(currOY);
                                            oYMin.setIfUpdated(true);
                                        }
                                        if(currOZ > prevOZ) {
                                            oZMax.setBuffer(currOZ);
                                            oZMax.setIfUpdated(true);
                                        }
                                        if(currOZ < prevOZ){
                                            oZMin.setBuffer(currOZ);
                                            oZMin.setIfUpdated(true);
                                        }
                                    }
                                    else{
                                        float currOX = oX.get(i);
                                        float nextOX = oX.get(i+1);
                                        float prevOX = oX.get(i-1);
                                        float currOY = oY.get(i);
                                        float nextOY = oY.get(i+1);
                                        float prevOY = oY.get(i-1);
                                        float currOZ = oZ.get(i);
                                        float nextOZ = oZ.get(i+1);
                                        float prevOZ = oZ.get(i-1);

                                        if(currOX > prevOX && currOX > nextOX){
                                            m.addToOXMax(currOX);
                                            m.setRealOYMax(currOX);
                                            m.addToNumOXMax();
                                        }
                                        if(currOX < prevOX && currOX < nextOX){
                                            m.addToOXMin(currOX);
                                            m.setRealOXMin(currOX);
                                            m.addToNumOXMin();
                                        }
                                        if(currOY > prevOY && currOY > nextOY){
                                            m.addToOYMax(currOY);
                                            m.setRealOYMax(currOY);
                                            m.addToNumOYMax();
                                        }
                                        if(currOY < prevOY && currOY < nextOY){
                                            m.addToOYMin(currOY);
                                            m.setRealOYMin(currOY);
                                            m.addToNumOYMin();
                                        }
                                        if(currOZ > prevOZ && currOZ > nextOZ){
                                            m.addToOZMax(currOZ);
                                            m.setRealOZMax(currOZ);
                                            m.addToNumOZMax();
                                        }
                                        if(currOZ < prevOZ && currOZ < nextOZ){
                                            m.addToOZMin(currOZ);
                                            m.setRealOZMin(currOZ);
                                            m.addToNumOZMin();
                                        }

                                    }
                                }

                                m.setOAPrevTime(t);
                                m.clearOATimes();
                                m.clearOGTimes();
                                m.clearOTimes();
                                m.clearOAData();
                                m.clearOGData();

                            }
                            m.setIfOrientated(true);
                            m.setORunning(false);

                            Log.d("resample running", "resample running");
                        }
                    }
                }
                else if (prefix.equals("angular-velocity")&& sensorConfigType.equals("resample")) {
                    //Log.d("digital", "doo");
                    last = data.timestamp();
                    if (start == null) {
                        first = last;
                        start = first.getTimeInMillis();
                    }
                    float offset = (last.getTimeInMillis() - start) / 1000.f;
                    //Log.d("digital", "poo");

                    //Log.d("digital", "poo");
                    //TODO: need to modify here !
                    float RATime = m.getRATime();
                    float time;
                    time = RATime;
                    if (!m.rTimesContains(time)) {
                        m.addtrTimes(time);

                    }
                    float RGTime = m.getRGTime();

                    if (!ifStop) {
                        float prevTime = m.getRAPrevTime();
                        t = m.getRATime();

                        if (t - prevTime >= 2) {

                            float prevT = m.getRAPrevTime();
                            float minT;
                            if (RATime < RGTime) {
                                minT = RATime;
                            } else {
                                minT = RGTime;
                            }
                            if (minT - prevT >= 2) {


                                for (float t = prevT; t <= minT; t += 0.02) {
                                    t = Float.parseFloat(df.format(t));
                                    if (m.raDContains(t) && m.rgDContains(t)) {
                                        float[] ra = m.getRA(t);
                                        float[] rg = m.getRG(t);
                                        fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                                last.getTimeInMillis(), formatTimestamp(last), t,
                                                ra[0], ra[1], ra[2], rg[0], rg[1],
                                                rg[2]).getBytes());
                                    }
                                    m.removeRAD(t);
                                    m.removeRGD(t);

                                }


                                m.setRAPrevTime(minT);
                                m.clearRATimes();
                                m.clearRGTimes();
                                m.clearRTimes();
                                m.clearRAData();
                                m.clearRGData();

                            }
                        }
                    }else {
                        if (!m.isIfResampled()) {
                            ArrayList<Float> raTimes = m.getRATimes();
                            int raTimesSize = m.getRATimes().size();
                            ArrayList<Float> rgTimes = m.getRGTimes();
                            int rgTimesSize = m.getRGTimes().size();
                            float raLast = raTimes.get(raTimesSize - 1);
                            float rgLast = rgTimes.get(rgTimesSize - 1);

                            Log.d("last time", "last time a= " + raLast);
                            Log.d("last time", "last time g= " + rgLast);

                            if (raLast < rgLast) {
                                float[] resampledTime = resample_time(raTimes, rgTimesSize);
                                float[][] resampledDataG = resample_data(m.getRGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];
                                float[][] resampledDataA = resample_data(m.getRAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];


                                for (int i = 0; i < resampledTime.length; i++) {
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i]).getBytes());
                                }

                                /*
                                ArrayList<float[]> extra_data = new ArrayList<>();
                                ArrayList<Float> extra_time = new ArrayList<>();
                                for(int i = raTimesSize ; i < m.getRGData().size();i++){
                                    extra_data.add(m.getRGData().get(i));
                                }
                                for(int i = raTimes.size() ; i < rgTimes.size();i++){
                                    extra_time.add(rgTimes.get(i));
                                }

                                 */

                                m.setRAPrevTime(t);

                                m.clearRATimes();
                                m.clearRGTimes();
                                m.clearRTimes();
                                m.clearRAData();
                                m.clearRGData();



                                /*
                                for (float[] d: extra_data){
                                    m.putRGData(d);
                                }
                                for (float t: extra_time){
                                    m.putRGTimes(t);
                                }

                                 */
                                /*
                                for(int i = 0; i < raTimesSize; i ++){
                                    m.getRATimes().remove(i);
                                    m.getRAData().remove(i);
                                }
                                for(int i = 0; i < rgTimesSize; i++){
                                    m.getRGTimes().remove(i);
                                    m.getRGData().remove(i);
                                }

                                 */


                            } else if (raLast > rgLast) {
                                float[] resampledTime = resample_time(rgTimes, raTimesSize);
                                float[][] resampledDataA = resample_data(m.getRAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];
                                float[][] resampledDataG = resample_data(m.getRGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];


                                for (int i = 0; i < resampledTime.length; i++) {
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i]).getBytes());
                                }
                                ArrayList<float[]> extra_data = new ArrayList<>();
                                ArrayList<Float> extra_time = new ArrayList<>();
                                for (int i = rgTimesSize; i < m.getRAData().size(); i++) {
                                    extra_data.add(m.getRAData().get(i));
                                }
                                for (int i = rgTimes.size(); i < raTimes.size(); i++) {
                                    extra_time.add(raTimes.get(i));
                                }
                                m.setRAPrevTime(t);
                                m.clearRATimes();
                                m.clearRGTimes();
                                m.clearRTimes();
                                m.clearRAData();
                                m.clearRGData();

                                /*
                                for (float[] d: extra_data){
                                    m.putRAData(d);
                                }

                                for (float t: extra_time){
                                    m.putRATimes(t);
                                }

                                 */
                            } else {
                                float[] resampledTime = resample_time(rgTimes, raTimesSize);
                                float[][] resampledDataA = resample_data(m.getRAData(), resampledTime.length);
                                float[] resampled_xA = resampledDataA[0];
                                float[] resampled_yA = resampledDataA[1];
                                float[] resampled_zA = resampledDataA[2];
                                float[][] resampledDataG = resample_data(m.getRGData(), resampledTime.length);
                                float[] resampled_xG = resampledDataG[0];
                                float[] resampled_yG = resampledDataG[1];
                                float[] resampled_zG = resampledDataG[2];


                                for (int i = 0; i < resampledTime.length; i++) {
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), resampledTime[i],
                                            resampled_xA[i], resampled_yA[i], resampled_zA[i],
                                            resampled_xG[i], resampled_yG[i], resampled_zG[i]).getBytes());
                                }

                                m.setRAPrevTime(t);
                                m.clearRATimes();
                                m.clearRGTimes();
                                m.clearRTimes();
                                m.clearRAData();
                                m.clearRGData();

                            }


                            Log.d("resample running", "resample running");
                            m.setRRunning(false);
                            m.setIfResampled(true);
                        }
                    }


                    /*
                    if (m.getrTimes().size() >= 2) {
                        if (m.getResampleTurn() == 0) {
                            if(m.getrTimes().size()>0){
                                //m.getrTimes().remove(0);
                                m.setResampleTurn(1);
                            }


                        } else {
                            float prevTime = m.getRAPrevTime();
                            t = m.getrTimes().get(0) ;
                            t = Float.parseFloat(df.format(t));
                            if(t - prevTime >= 1){
                                float[] resampledTime = resample_time(m.getRGTimes(), m.getRATimes().size());

                                for(float ts: resampledTime){
                                    fos.write(String.format(Locale.US, "%d,%s,%.3f%n",
                                            last.getTimeInMillis(), formatTimestamp(last), ts).getBytes());

                                    Log.d("resample running", "resample time =" + ts);
                                }



                                m.setRAPrevTime(t);

                                m.clearRATimes();
                                m.clearRGTimes();
                                m.clearRTimes();
                                m.setResampleTurn(0);

                                Log.d("resample running", "resample running");

                            }else{
                                if(m.getrTimes().size() > 0){
                                    m.getrTimes().remove(0);

                                }
                            }


                            //orientationTurn = 0;
                        }
                        //timeRecorded.remove(1);
                    }
                    /*
                    else{
                        fos.write(String.format(Locale.US, "%d,%s,%.3f,%d%n",
                                last.getTimeInMillis(), formatTimestamp(last), -1f
                                , orientationCount).getBytes());
                    }

                     */



                }
            }catch (IOException ignored) {
                Log.d("mytest", "nono");
            }
        }

        @Override
        public void stop() {
            try {
                fos.close();
            } catch (IOException ignored) {
            }
        }

        public void rewrite(){
            try {
                fos.flush();
            } catch (IOException ignored) {
            }
        }

        @Override
        public String getIdentifier() {
            return this.identifier;
        }

    }


    class SampleCountDataHandler implements DataHandler {
        int samples;
        TextView sampleCountView;

        @Override
        public void init() {
            samples = 0;
        }

        @Override
        public void process(Data data) {
            samples++;
        }

        @Override
        public void stop() {

        }

        @Override
        public void rewrite() {

        }

        @Override
        public String getIdentifier() {
            return "";
        }
    }

    class RewriteDataHandler implements DataHandler {


        @Override
        public void init() {

        }

        @Override
        public void process(Data data) {
            float[] vector = data.value(float[].class);

     
        }

        @Override
        public void stop() {

        }

        @Override
        public void rewrite() {

        }

        @Override
        public String getIdentifier() {
            return "";
        }
    }



}
