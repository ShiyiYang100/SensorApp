package com.mbientlab.metawear.metabase;

public class TemperalParameters {

    private float strideTime, swingTime,stanceTime, toeOff,
            heelStrike, doubleSupportTime, singleSupportTime, stepTime;

    public TemperalParameters(float strideTime, float swingTime, float stanceTime, float toeOff,
                              float heelStrike, float doubleSupportTime, float singleSupportTime, float stepTime) {
        this.strideTime = strideTime;
        this.swingTime = swingTime;
        this.stanceTime = stanceTime;
        this.toeOff = toeOff;
        this.heelStrike = heelStrike;
        this.doubleSupportTime = doubleSupportTime;
        this.singleSupportTime = singleSupportTime;
        this.stepTime = stepTime;
    }
    public TemperalParameters() {
        this.strideTime = 0f;
        this.swingTime = 0f;
        this.stanceTime = 0f;
        this.toeOff = 0f;
        this.heelStrike = 0f;
        this.doubleSupportTime = 0f;
        this.singleSupportTime = 0f;
        this.stepTime = 0f;
    }



    public float getStrideTime() {
        return strideTime;
    }

    public void setStrideTime(float strideTime) {
        this.strideTime = strideTime;
    }

    public float getSwingTime() {
        return swingTime;
    }

    public void setSwingTime(float swingTime) {
        this.swingTime = swingTime;
    }

    public float getStanceTime() {
        return stanceTime;
    }

    public void setStanceTime(float stanceTime) {
        this.stanceTime = stanceTime;
    }

    public float getToeOff() {
        return toeOff;
    }

    public void setToeOff(float toeOff) {
        this.toeOff = toeOff;
    }

    public float getHeelStrike() {
        return heelStrike;
    }

    public void setHeelStrike(float heelStrike) {
        this.heelStrike = heelStrike;
    }

    public float getDoubleSupportTime() {
        return doubleSupportTime;
    }

    public void setDoubleSupportTime(float doubleSupportTime) {
        this.doubleSupportTime = doubleSupportTime;
    }

    public float getSingleSupportTime() {
        return singleSupportTime;
    }

    public void setSingleSupportTime(float singleSupportTime) {
        this.singleSupportTime = singleSupportTime;
    }

    public float getStepTime() {
        return stepTime;
    }

    public void setStepTime(float stepTime) {
        this.stepTime = stepTime;
    }


}
