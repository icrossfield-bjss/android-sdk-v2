package com.gsma.android.model;

/**
 * This is where the MMC etc will be stored.
 * Created by nick.copley on 19/02/2016.
 */
public class DeviceModel {

    private static DeviceModel ourInstance = new DeviceModel();

    public static DeviceModel getInstance() {
        return ourInstance;
    }

    private DeviceModel() {
    }
}
