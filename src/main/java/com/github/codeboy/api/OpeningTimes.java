package com.github.codeboy.api;

class OpeningTimes {
    public static OpeningTimes closed=new OpeningTimes(0,0);
    float startTime;
    float endTime;

    public OpeningTimes(float startTime, float endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "OpeningTimes{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
