package com.mbientlab.metawear.metabase;

public class SpatialParameters {

    private float strideLength, walkingSpeed, MFC; // MFC = minimum foot clearance
    private int numOfSteps;

    public SpatialParameters(float strideLength, float walkingSpeed, float MFC, int numOfSteps) {
        this.strideLength = strideLength;
        this.walkingSpeed = walkingSpeed;
        this.MFC = MFC;
        this.numOfSteps = numOfSteps;
    }

    public SpatialParameters() {
        this.strideLength = 0f;
        this.walkingSpeed = 0f;
        this.MFC = 0f;
        this.numOfSteps = 0;
    }

    public float getStrideLength() {
        return strideLength;
    }

    public void setStrideLength(float strideLength) {
        this.strideLength = strideLength;
    }

    public float getWalkingSpeed() {
        return walkingSpeed;
    }

    public void setWalkingSpeed(float walkingSpeed) {
        this.walkingSpeed = walkingSpeed;
    }

    public float getMFC() {
        return MFC;
    }

    public void setMFC(float MFC) {
        this.MFC = MFC;
    }

    public int getNumOfSteps() {
        return numOfSteps;
    }

    public void setNumOfSteps(int numOfSteps) {
        this.numOfSteps = numOfSteps;
    }

}
