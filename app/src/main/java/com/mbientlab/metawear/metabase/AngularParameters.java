package com.mbientlab.metawear.metabase;

public class AngularParameters {
    private float xAvgMax;
    private float xAvgMin;
    private float yAvgMax;
    private float yAvgMin;
    private float zAvgMax;
    private float zAvgMin;

    public AngularParameters(float xAvgMax, float xAvgMin, float yAvgMax, float yAvgMin, float zAvgMax, float zAvgMin) {
        this.xAvgMax = xAvgMax;
        this.xAvgMin = xAvgMin;
        this.yAvgMax = yAvgMax;
        this.yAvgMin = yAvgMin;
        this.zAvgMax = zAvgMax;
        this.zAvgMin = zAvgMin;
    }
    public AngularParameters() {
        this.xAvgMax = 0f;
        this.xAvgMin = 0f;
        this.yAvgMax = 0f;
        this.yAvgMin = 0f;
        this.zAvgMax = 0f;
        this.zAvgMin = 0f;
    }


    public float getxAvgMax() {
        return xAvgMax;
    }

    public void setxAvgMax(float xAvgMax) {
        this.xAvgMax = xAvgMax;
    }

    public float getxAvgMin() {
        return xAvgMin;
    }

    public void setxAvgMin(float xAvgMin) {
        this.xAvgMin = xAvgMin;
    }

    public float getyAvgMax() {
        return yAvgMax;
    }

    public void setyAvgMax(float yAvgMax) {
        this.yAvgMax = yAvgMax;
    }

    public float getyAvgMin() {
        return yAvgMin;
    }

    public void setyAvgMin(float yAvgMin) {
        this.yAvgMin = yAvgMin;
    }

    public float getzAvgMax() {
        return zAvgMax;
    }

    public void setzAvgMax(float zAvgMax) {
        this.zAvgMax = zAvgMax;
    }

    public float getzAvgMin() {
        return zAvgMin;
    }

    public void setzAvgMin(float zAvgMin) {
        this.zAvgMin = zAvgMin;
    }



}
