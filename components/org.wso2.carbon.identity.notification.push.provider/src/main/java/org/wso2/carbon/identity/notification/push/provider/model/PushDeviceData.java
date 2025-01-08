package org.wso2.carbon.identity.notification.push.provider.model;

/**
 * Model class to hold push device data.
 */
public class PushDeviceData {

    private String deviceToken;
    private String deviceHandle;
    private String provider;

    private PushDeviceData() {

    }

    /**
     * Constructor to initialize the push device data.
     *
     * @param deviceToken   Device token.
     * @param deviceHandle  Device handle.
     * @param provider      Provider.
     */
    public PushDeviceData(String deviceToken, String deviceHandle, String provider) {

        this.deviceToken = deviceToken;
        this.deviceHandle = deviceHandle;
        this.provider = provider;
    }

    public String getDeviceToken() {

        return deviceToken;
    }

    public String getDeviceHandle() {

        return deviceHandle;
    }

    public String getProvider() {

        return provider;
    }
}
