package com.mbientlab.metawear.metabase;

import java.util.ArrayList;
import java.util.HashMap;

import uk.me.berndporr.iirj.Butterworth;

public class MetaBaseDeviceData {
    private HashMap<Float,  ArrayList<Float>>  OMmap;
    private float OMTime;
    private float OMTimePrev;
    private float previous_MX;
    private float previous_MY;
    private float previous_MZ;
    //complementary filter data
    private float roll, pitch,yaw;
    private double gyroMin, gyroMax;
    private double[] rotationVector;
    private boolean ifRotationVectorSet;

    //orientation
    private ArrayList<Float> OATimes;
    private boolean ifOrientated;
    private boolean ifResampled;

    private ArrayList<float[]> OAData;

    private ArrayList<Float> OGTimes;
    private ArrayList<float[]> OGData;

    private ArrayList<Float> oTimes;

    private float OGTime;

    private int OrientationTurn;

    private float OATime;
    private float OAPrevTime;

    //alignment attributes
    private ArrayList<Float> RATimes;

    private ArrayList<float[]> RAData;

    private ArrayList<Float> RGTimes;
    private ArrayList<float[]> RGData;

    private ArrayList<Float> rTimes;

    private float RGTime;

    private int ResampleTurn;

    private float RATime;
    private float RAPrevTime;

    private HashMap<Float, float[]> RAD;

    private HashMap<Float, float[]> RGD;

    private HashMap<Float, float[]> OAD;

    private HashMap<Float, float[]> OGD;

    private boolean ORunning;
    private boolean RRunning;

    private float strideLength;
    private int totalStride;

    private float stance;
    private int totalStance;

    private float swing;

    private int totalSwing;

    private double totalTime;
    private float toeOff, midSwing, hillStrike, totalToeOff, totalHeelStrike;
    private int numToeOff, numHeelStrike, numOXMax, numOXMin,
            numOYMax, numOYMin, numOZMax, numOZMin;
    private float oXMax, oXMin, oYMax, oYMin, oZMax, oZMin;//orientation values

    private float kneeHeight;
    private float kneeRadius;

    private float totalStrideLen;
    private float totalWalkingSpeed;
    private float totalMFC;

    private int numStrideLen, numWalkingSpeed, numMFC;

    private float realStrideTime;
    private float realSwingTime;
    private float realStanceTime;
    private float realToeOff;
    private float realHeelStrike;
    private float realDST;
    private float realSST;
    private float realStepTime;
    private float realStrideLen;
    private float realWalkingSpeed;
    private float realMFC;
    private float realNumStep;
    private float realOXMax;
    private float realOXMin;
    private float realOYMax;
    private float realOYMin;
    private float realOZMax;
    private float realOZMin;


    private Butterworth gyroPeakX =  new Butterworth();
    private Butterworth gyroPeakY =  new Butterworth();





    public MetaBaseDeviceData() {
        this.OMmap = new HashMap<>();
        this.OMTime = 0f;
        this.OMTimePrev = 0f;
        this.previous_MX = 0f;
        this.previous_MY = 0f;
        this.previous_MZ = 0f;
        this.roll = 0f;
        this.pitch = 0f;
        this.yaw = 0f;
        this.gyroMin = 0;
        this.gyroMax = 0;
        this.ifRotationVectorSet = false;

        this.RATimes = new ArrayList<>();
        this.RAData = new ArrayList<>();
        this.rTimes = new ArrayList<>();
        this.RGTimes = new ArrayList<>();
        this.RGData = new ArrayList<>();
        this.ResampleTurn = 0;
        this.RATime = 0;
        this.RAPrevTime = 0;
        this.OAPrevTime = 0;
        this.RGTime = 0;
        this.RAD = new HashMap<>();
        this.RGD = new HashMap<>();
        this.ifOrientated = false;
        this.ifResampled =  false;

        this.OATimes = new ArrayList<>();
        this.OAData = new ArrayList<>();
        this.oTimes = new ArrayList<>();
        this.OGTimes = new ArrayList<>();
        this.OGData = new ArrayList<>();
        this.OrientationTurn = 0;
        this.OATime = 0;
        this.OAPrevTime = 0;
        this.OGTime = 0;
        this.OAD = new HashMap<>();
        this.OGD = new HashMap<>();

        this.ORunning = true;
        this.RRunning = true;

        //gait parameters
        this.strideLength = 0;
        this.totalStride = 0;
        this.stance = 0;
        this.totalStance = 0;
        this.swing = 0;
        this.totalSwing = 0;
        this.totalTime = 0;
        this.toeOff =  2001f;  //maximum value = 2000f, so never reach 2001f
        this.midSwing = 2001f;
        this.hillStrike = 2001f;
        this.totalToeOff = 0f;
        this.numToeOff = 0;
        this.totalHeelStrike = 0f;
        this.numHeelStrike = 0;
        this.oXMax = 0f;
        this.oXMin = 0f;
        this.oYMax = 0f;
        this.oYMin = 0f;
        this.oZMax = 0f;
        this.oZMin = 0f;
        this.numOXMax = 0;
        this.numOXMin = 0;
        this.numOYMax = 0;
        this.numOYMin = 0;
        this.numOZMax = 0;
        this.numOZMin = 0;
        this.kneeHeight = 0f;
        this.kneeRadius = 0f;

        this.totalStrideLen = 0f;
        this.numStrideLen = 0;
        this.totalWalkingSpeed = 0f;
        this.numWalkingSpeed = 0;

        this.totalMFC = 0f;
        this.numMFC = 0;

        //real time data
        this.realStrideTime = 0f;
        this.realSwingTime = 0f;
        this.realStanceTime = 0f;
        this.realToeOff = 0f;
        this.realHeelStrike = 0f;
        this.realDST = 0f;
        this.realSST = 0f;
        this.realStepTime = 0f;
        this.realStrideLen = 0f;
        this.realWalkingSpeed = 0f;
        this.realMFC = 0f;
        this.realNumStep = 0f;
        this.realOXMax = 0f;
        this.realOXMin = 0f;
        this.realOYMax = 0f;
        this.realOYMin = 0f;
        this.realOZMax = 0f;
        this.realOZMin = 0f;
        this.gyroPeakX.lowPass(5, 50, 5f);
        this.gyroPeakY.lowPass(5, 50, 5f);
    }



    public void init(){
        this.OMmap.clear();
        this.OMTime = 0f;
        this.OMTimePrev = 0f;
        this.previous_MX = 0f;
        this.previous_MY = 0f;
        this.previous_MZ = 0f;
        this.roll = 0f;
        this.pitch = 0f;
        this.yaw = 0f;
        this.gyroMin = 0;
        this.gyroMax = 0;
        this.ifRotationVectorSet = false;
        this.ifOrientated = false;
        this.ifResampled =  false;


        this.RATimes.clear();
        this.RAData.clear();
        this.rTimes.clear();
        this.RGTimes.clear();
        this.RGData.clear();
        this.ResampleTurn = 0;
        this.RATime = 0;
        this.RAPrevTime = 0;
        this.RGTime = 0;
        this.RAD.clear();
        this.RGD.clear();


        this.OATimes.clear();
        this.OAData.clear();
        this.oTimes.clear();
        this.OGTimes.clear();
        this.OGData.clear();
        this.OrientationTurn = 0;
        this.OATime = 0;
        this.OAPrevTime = 0;
        this.OGTime = 0;
        this.OAD.clear();
        this.OGD.clear();


        this.ORunning = true;
        this.RRunning = true;

        this.toeOff =  2001f;  //maximum value = 2000f, so never reach 2001f
        this.midSwing = 2001f;
        this.hillStrike = 2001f;
        this.totalHeelStrike = 0f;
        this.numHeelStrike = 0;
        this.totalStride = 0;


        this.oXMax = 0f;
        this.oXMin = 0f;
        this.oYMax = 0f;
        this.oYMin = 0f;
        this.oZMax = 0f;
        this.oZMin = 0f;
        this.numOXMax = 0;
        this.numOXMin = 0;
        this.numOYMax = 0;
        this.numOYMin = 0;
        this.numOZMax = 0;
        this.numOZMin = 0;

        this.totalStrideLen = 0f;
        this.numStrideLen = 0;
        this.totalWalkingSpeed = 0f;
        this.numWalkingSpeed = 0;

        this.totalMFC = 0f;
        this.numMFC = 0;

        //real time data
        this.realStrideTime = 0f;
        this.realSwingTime = 0f;
        this.realStanceTime = 0f;
        this.realToeOff = 0f;
        this.realHeelStrike = 0f;
        this.realDST = 0f;
        this.realSST = 0f;
        this.realStepTime = 0f;
        this.realStrideLen = 0f;
        this.realWalkingSpeed = 0f;
        this.realMFC = 0f;
        this.realNumStep = 0f;
        this.realOXMax = 0f;
        this.realOXMin = 0f;
        this.realOYMax = 0f;
        this.realOYMin = 0f;
        this.realOZMax = 0f;
        this.realOZMin = 0f;

        this.gyroPeakX.lowPass(5, 50, 5f);
        this.gyroPeakY.lowPass(5, 50, 5f);
    }


    public ArrayList<Float> getoTimes() {
        return oTimes;
    }

    public void setoTimes(ArrayList<Float> oTimes) {
        this.oTimes = oTimes;
    }

    public void addtoTimes(float t) {
        this.oTimes.add(t);
    }

    public boolean oTimesContains(float t) {
        return this.oTimes.contains(t);
    }

    public float getOATime() {
        return OATime;
    }

    public void setOATime(float OATime) {
        this.OATime = OATime;
    }

    public float getOGTime() {
        return OGTime;
    }

    public void setOGTime(float OGTime) {
        this.OGTime = OGTime;
    }

    public float getOMTime() {
        return OMTime;
    }

    public void setOMTime(float OMTime) {
        this.OMTime = OMTime;
    }

    public float getOMTimePrev() {
        return OMTimePrev;
    }

    public void setOMTimePrev(float OMTimePrev) {
        this.OMTimePrev = OMTimePrev;
    }

    public float getPrevious_MX() {
        return previous_MX;
    }

    public void setPrevious_MX(float previous_MX) {
        this.previous_MX = previous_MX;
    }

    public float getPrevious_MY() {
        return previous_MY;
    }

    public void setPrevious_MY(float previous_MY) {
        this.previous_MY = previous_MY;
    }

    public float getPrevious_MZ() {
        return previous_MZ;
    }

    public void setPrevious_MZ(float previous_MZ) {
        this.previous_MZ = previous_MZ;
    }
    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public double getGyroMin() {
        return gyroMin;
    }

    public void setGyroMin(double gyroMin) {
        this.gyroMin = gyroMin;
    }

    public double getGyroMax() {
        return gyroMax;
    }

    public void setGyroMax(double gyroMax) {
        this.gyroMax = gyroMax;
    }

    public double[] getRotationVector() {
        return rotationVector;
    }

    public void setRotationVector(double[] rotationVector) {
        this.rotationVector = rotationVector;
    }
    public void setRotationVecI(int idx, double data) {
        if (this.rotationVector != null){
            this.rotationVector[idx] = data;
        }
    }

    public boolean isIfRotationVectorSet() {
        return ifRotationVectorSet;
    }

    public void setIfRotationVectorSet(boolean ifRotationVectorSet) {
        this.ifRotationVectorSet = ifRotationVectorSet;
    }

    public int getOrientationTurn() {
        return this.OrientationTurn;
    }

    public void setOrientationTurn(int orientationTurn) {
        this.OrientationTurn = orientationTurn;
    }

    public void putOMmap(float t, ArrayList<Float> l) {
        OMmap.put(t,l);
    }


    public ArrayList<Float> getOMmap(float t) {
        return OMmap.get(t);
    }

    public boolean ifOMmapContain(float t){
        return this.OMmap.containsKey(t);
    }


    public void removeOMmap(float t){
        this.OMmap.remove(t);
    }

    //alignment attributes
    public void putRATimes(float t){this.RATimes.add(t);}
    public void putOATimes(float t){this.OATimes.add(t);}
    public void clearRATimes(){this.RATimes.clear();}
    public void clearOATimes(){this.OATimes.clear();}


    public void putRGTimes (float t){this.RGTimes.add(t);}
    public void clearRGTimes(){this.RGTimes.clear();}
    public void putOGTimes (float t){this.OGTimes.add(t);}
    public void clearOGTimes(){this.OGTimes.clear();}


    public void putOAData(float[] data){this.OAData.add(data);}

    public void clearOAData(){this.OAData.clear();}
    public void OADataClear(int t){
       for(int i = 0; i < t; i++){
           this.OAData.remove(i);
       }
    }
    public void putRAData(float[] data){this.RAData.add(data);}
    public void clearRAData(){this.RAData.clear();}
    public void putRGData(float[] data){this.RGData.add(data);}
    public void clearRGData () {this.RGData.clear();}
    public void putOGData(float[] data){this.OGData.add(data);}
    public void clearOGData(){this.OGData.clear();}

    public void OGDataClear(float t){
        for(int i = 0; i < t; i++){
            this.OGData.remove(i);
        }
    }

    public boolean rTimesContains(float t){
        return rTimes.contains(t);
    }

    public void addtrTimes (float t){
        this.rTimes.add(t);
    }
    public ArrayList<Float> getrTimes (){
        return this.rTimes;
    }

    public void clearRTimes(){
        this.rTimes.clear();
    }
    public void clearOTimes(){
        this.oTimes.clear();
    }
    public int getResampleTurn() {
        return ResampleTurn;
    }

    public void setResampleTurn(int resampleTurn) {
        ResampleTurn = resampleTurn;
    }

    public ArrayList<Float> getRATimes() {
        return this.RATimes;
    }
    public ArrayList<Float> getOATimes() {
        return this.OATimes;
    }

    public void setRATimes(ArrayList<Float> RATime) {
        this.RATimes = RATime;
    }

    public ArrayList<float[]> getRAData() {
        return RAData;
    }
    public ArrayList<float[]> getOAData() {
        return this.OAData;
    }



    public void setRATime(float RATime) {
        this.RATime = RATime;
    }
    public float getRATime(){
        return this.RATime;
    }

    public ArrayList<Float> getRGTimes() {
        return this.RGTimes;
    }
    public ArrayList<Float> getOGTimes() {
        return this.OGTimes;
    }

    public void setRGTimes(ArrayList<Float> RGTime) {
        this.RGTimes = RGTime;
    }

    public ArrayList<float[]> getRGData() {
        return this.RGData;
    }
    public ArrayList<float[]> getOGData() {
        return this.OGData;
    }

    public void setRAData(ArrayList<float[]> RAData) {
        this.RAData = RAData;
    }

    public float getRAPrevTime() {
        return this.RAPrevTime;
    }
    public float getOAPrevTime() {
        return RAPrevTime;
    }

    public void setRAPrevTime(float RAPrevTime) {
        this.RAPrevTime = RAPrevTime;
    }
    public void setOAPrevTime(float OAPrevTime) {
        this.OAPrevTime = OAPrevTime;
    }

    public void setRGTime (float t){this.RGTime = t;}
    public float getRGTime(){ return this.RGTime;}

    public void putRAD(float time, float[] data){this.RAD.put(time, data);}
    public void putRGD(float time, float[] data){this.RGD.put(time, data);}

    public boolean raDContains (float t){return this.RAD.containsKey(t);}

    public boolean rgDContains (float t){return this.RGD.containsKey(t);}
    public void removeRAD(float t){this.RAD.remove(t);}
    public void removeRGD(float t){this.RGD.remove(t);}
    public float[] getRA(float t){return this.RAD.get(t);}
    public float[] getRG(float t){return this.RGD.get(t);}

    public void putOAD(float time, float[] data){this.OAD.put(time, data);}
    public void putOGD(float time, float[] data){this.OGD.put(time, data);}

    public boolean oaDContains (float t){return this.OAD.containsKey(t);}

    public boolean ogDContains (float t){return this.OGD.containsKey(t);}
    public void removeOAD(float t){this.OAD.remove(t);}
    public void removeOGD(float t){this.OGD.remove(t);}
    public float[] getOA(float t){return this.OAD.get(t);}
    public float[] getOG(float t){return this.OGD.get(t);}
    public boolean isIfOrientated() {
        return ifOrientated;
    }

    public void setIfOrientated(boolean ifOrientated) {
        this.ifOrientated = ifOrientated;
    }

    public boolean isIfResampled() {
        return ifResampled;
    }

    public void setIfResampled(boolean ifResampled) {
        this.ifResampled = ifResampled;
    }
    public boolean isORunning() {
        return ORunning;
    }

    public void setORunning(boolean ORunning) {
        this.ORunning = ORunning;
    }

    public boolean isRRunning() {
        return RRunning;
    }

    public void setRRunning(boolean RRunning) {
        this.RRunning = RRunning;
    }

    public void setTotalStride(int totalStride) {
        this.totalStride = totalStride;
    }

    public void addToStrideLength (float t){
        this.strideLength += t;
    }
    public void addToTotalStride (){
        this.totalStride ++;
    }

    public int getTotalStride(){return this.totalStride;}

    public float getStride(){
        if(this.totalStride!=0) {
            return this.strideLength / this.totalStride;
        }else{
            return 0;
        }
    }


    public void addToStance(float t){
        this.stance += t;
    }

    public void addToTotalStance(){
        this.totalStance ++;
    }


    public void addToSwing(float t){
        this.swing += t;
    }

    public void addToTotalSwing(){
        this.totalSwing ++;
    }

    public float getAvgStance(){
        if(this.totalStance != 0){
            return this.stance / this.totalStance;
        }else{
            return 0;
        }
    }

    public float getAvgSwing(){
        if(this.totalSwing != 0){
            return this.swing / this.totalSwing;
        }else{
            return 0;
        }
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }
    public void setMidSwing(float t) {
        this.midSwing = t;
    }
    public void setToeOff(float t) {
        this.toeOff = t;
    }
    public float getToeOff() {
        return this.toeOff;
    }

    public void addToTotalToeOff(float t){this.totalToeOff += t;}
    public void addToNumToeOff(){this.numToeOff ++;}

    public float getAvgToeOff(){return this.totalToeOff / this.numToeOff;}

    public void addToTotalHeelStrike(float t){this.totalHeelStrike += t;}
    public void addToNumHeelStrike(){this.numHeelStrike++;}

    public float getAvgHeelStrike(){return this.totalHeelStrike / this.numHeelStrike;}

    public void addToOXMax(float t){this.oXMax += t;}
    public void addToOXMin(float t){this.oXMin += t;}
    public void addToOYMax(float t){this.oYMax += t;}
    public void addToOYMin(float t){this.oYMin += t;}
    public void addToOZMax(float t){this.oZMax += t;}
    public void addToOZMin(float t){this.oZMin += t;}

    public void addToNumOXMax(){this.numOXMax ++;}
    public void addToNumOXMin(){this.numOXMin ++;}
    public void addToNumOYMax(){this.numOYMax ++;}
    public void addToNumOYMin(){this.numOYMin ++;}
    public void addToNumOZMax(){this.numOZMax ++;}
    public void addToNumOZMin(){this.numOZMin ++;}

    public float getOXMax(){
        if(this.numOXMax > 0) {
            return this.oXMax / this.numOXMax;
        }else{
            return 0;
        }
    }
    public float getOXMin() {
        if(this.numOXMin > 0) {
            return this.oXMin / this.numOXMin;
        }else {
            return 0f;
        }
    }
    public float getOYMax(){
        if(this.numOYMax > 0) {
            return this.oYMax / this.numOYMax;
        }else{
            return 0f;
        }
    }
    public float getOYMin(){
        if(this.numOYMin > 0) {
            return this.oYMin / this.numOYMin;
        }else{
            return 0f;
        }
    }
    public float getOZMax(){
        if(this.numOZMax > 0) {
            return this.oZMax / this.numOZMax;
        }else{
            return 0f;
        }
    }
    public float getOZMin(){
        if(this.numOZMin > 0) {
            return this.oZMin / this.numOZMin;
        }else{
            return 0f;
        }
    }

    public float getKneeHeight() {
        return kneeHeight;
    }

    public void setKneeHeight(float kneeHeight) {
        this.kneeHeight = kneeHeight;
    }

    public void addToTotalStrideLen(float t){this.totalStrideLen += t;}
    public float getAvgStrideLen(){
        if(numStrideLen != 0){
            return this.totalStrideLen / this.numStrideLen;
        }else{
            return 0f;
        }
    }
    public void addToNumStrideLen(){this.numStrideLen ++;}
    public void addToTotalWalkingSpeed(float t){this.totalWalkingSpeed += t;}
    public float getAvgWalkingSpeed(){
        if(numWalkingSpeed != 0){
            return this.totalWalkingSpeed / numWalkingSpeed;
        }else{
            return 0f;
        }
    }
    public void addToNumWalkingSpeed(){this.numWalkingSpeed ++;}

    public void addToTotalMFC(float t){this.totalMFC += t;}
    public float getAvgMFC(){
        if(numMFC != 0){
            return this.totalMFC / numMFC;
        }else{
            return 0f;
        }
    }
    public void addToNumMFC(){this.numMFC ++;}

    public float getRealStrideTime() {
        return realStrideTime;
    }

    public void setRealStrideTime(float realStrideTime) {
        this.realStrideTime = realStrideTime;
    }

    public float getRealSwingTime() {
        return realSwingTime;
    }

    public void setRealSwingTime(float realSwingTime) {
        this.realSwingTime = realSwingTime;
    }

    public float getRealStanceTime() {
        return realStanceTime;
    }

    public void setRealStanceTime(float realStanceTime) {
        this.realStanceTime = realStanceTime;
    }

    public float getRealToeOff() {
        return realToeOff;
    }

    public void setRealToeOff(float realToeOff) {
        this.realToeOff = realToeOff;
    }

    public float getRealHeelStrike() {
        return realHeelStrike;
    }

    public void setRealHeelStrike(float realHeelStrike) {
        this.realHeelStrike = realHeelStrike;
    }

    public float getRealDST() {
        return realDST;
    }

    public void setRealDST(float realDST) {
        this.realDST = realDST;
    }

    public float getRealSST() {
        return realSST;
    }

    public void setRealSST(float realSST) {
        this.realSST = realSST;
    }

    public float getRealStepTime() {
        return realStepTime;
    }

    public void setRealStepTime(float realStepTime) {
        this.realStepTime = realStepTime;
    }

    public float getRealStrideLen() {
        return realStrideLen;
    }

    public void setRealStrideLen(float realStrideLen) {
        this.realStrideLen = realStrideLen;
    }

    public float getRealWalkingSpeed() {
        return realWalkingSpeed;
    }

    public void setRealWalkingSpeed(float realWalkingSpeed) {
        this.realWalkingSpeed = realWalkingSpeed;
    }

    public float getRealMFC() {
        return realMFC;
    }

    public void setRealMFC(float realMFC) {
        this.realMFC = realMFC;
    }

    public float getRealNumStep() {
        return realNumStep;
    }

    public void setRealNumStep(float realNumStep) {
        this.realNumStep = realNumStep;
    }

    public float getRealOXMax() {
        return realOXMax;
    }

    public void setRealOXMax(float realOXMax) {
        this.realOXMax = realOXMax;
    }

    public float getRealOXMin() {
        return realOXMin;
    }

    public void setRealOXMin(float realOXMin) {
        this.realOXMin = realOXMin;
    }

    public float getRealOYMax() {
        return realOYMax;
    }

    public void setRealOYMax(float realOYMax) {
        this.realOYMax = realOYMax;
    }

    public float getRealOYMin() {
        return realOYMin;
    }

    public void setRealOYMin(float realOYMin) {
        this.realOYMin = realOYMin;
    }

    public float getRealOZMax() {
        return realOZMax;
    }

    public void setRealOZMax(float realOZMax) {
        this.realOZMax = realOZMax;
    }

    public float getRealOZMin() {
        return realOZMin;
    }

    public void setRealOZMin(float realOZMin) {
        this.realOZMin = realOZMin;
    }

    public float getKneeRadius() {
        return kneeRadius;
    }

    public void setKneeRadius(float kneeRadius) {
        this.kneeRadius = kneeRadius;
    }



    public Butterworth getGyroPeakX() {
        return gyroPeakX;
    }

    public void setGyroPeakX(float f) {
       gyroPeakX.lowPass(5, 50, f);
    }
    public Butterworth getGyroPeakY() {
        return gyroPeakY;
    }

    public void setGyroPeakY(float f) {
        gyroPeakY.lowPass(5, 50, f);
    }

}


