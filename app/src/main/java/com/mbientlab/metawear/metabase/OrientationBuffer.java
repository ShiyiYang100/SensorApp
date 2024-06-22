package com.mbientlab.metawear.metabase;

public class OrientationBuffer {
    private float buffer;
    private boolean ifUpdated;

    public OrientationBuffer(){
        this.buffer = 0f;
        this.ifUpdated = false;
    }

    public float getBuffer() {
        return buffer;
    }

    public void setBuffer(float buffer) {
        this.buffer = buffer;
    }

    public boolean isIfUpdated() {
        return ifUpdated;
    }

    public void setIfUpdated(boolean ifUpdated) {
        this.ifUpdated = ifUpdated;
    }



}
