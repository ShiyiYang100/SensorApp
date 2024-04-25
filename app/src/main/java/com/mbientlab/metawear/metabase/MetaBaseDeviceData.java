package com.mbientlab.metawear.metabase;

import java.util.ArrayList;
import java.util.HashMap;

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






}


