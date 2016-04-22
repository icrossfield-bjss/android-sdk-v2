package com.gsma.android;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.gsma.android.BuildConfig;
import com.gsma.android.xoperatorapidemo.utils.PhoneState;
import com.gsma.android.xoperatorapidemo.utils.PhoneUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP, manifest = "src/main/AndroidManifest.xml")
public class PhoneStateTest {

    TelephonyManager telephonyManager;
    ConnectivityManager connectivityManager;
    NetworkInfo networkInfo;
    PhoneState state;
    PhoneState nullPhoneState = new PhoneState(null,null, null,null,false,false, false,null);
    String usernumber = "07111111111";
    String simSerialNumber = "xxxx";

    String validSimOperator = "123456";
    String inValidSimOperator = "11";
    String negativeSimOperator = "-123456";

    boolean networkIsConnected = true;
    boolean roamingIsOn = true;

    @Before
    public void setup() {
        telephonyManager = mock(TelephonyManager.class);
        connectivityManager = mock(ConnectivityManager.class);
        networkInfo = mock(NetworkInfo.class);
    }

    /*Tests*/

    @Test
    public void getMcc_ShouldReturnCorrectMCC_WhenValidSimOperator() {
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM serial number
        when(telephonyManager.getSimSerialNumber()).thenReturn(simSerialNumber);
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(validSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals("123", state.getMcc());
    }

    @Test
    public void getMcc_ShouldReturnNull_WhenInValidSimOperator(){
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(inValidSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertNull(state.getMcc());
    }

    @Test
    public void getMcc_ShouldReturnNull_WhenNegativeSimOperator(){
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(negativeSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertNull(state.getMcc());
    }


    @Test
    public void getMnc_ShouldReturnCorrectMNC_WhenValidSimOperator() {
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM serial number
        when(telephonyManager.getSimSerialNumber()).thenReturn(simSerialNumber);
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(validSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals("456", state.getMnc());
    }

    @Test
    public void getMnc_ShouldReturnNull_WhenInValidSimOperator(){
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(inValidSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertNull(state.getMnc());
    }

    @Test
    public void getMnc_ShouldReturnNull_WhenNegativeSimOperator(){
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(negativeSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertNull(state.getMnc());
    }

    @Test
    public void getMsisdn_ShouldReturnCorrectMsisdn_WhenValidNumber() {
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock mobile telephone number of user
        when(telephonyManager.getLine1Number()).thenReturn(usernumber);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(usernumber, state.getMsisdn());
    }

    @Test
    public void getSimOperator_ShouldReturnCorrectSimOperator_WhenValidSimOperator() {
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM operator
        when(telephonyManager.getSimOperator()).thenReturn(validSimOperator);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(validSimOperator, state.getSimOperator());
    }

    @Test
    public void getSimSerialNumber_ShouldReturnCorrectSimSerialNumber_WhenValidSimSerialNumber() {
        //set up connectivity manager
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        //mock SIM serial number
        when(telephonyManager.getSimSerialNumber()).thenReturn(simSerialNumber);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(simSerialNumber, state.getSimSerialNumber());
    }

    @Test
    public void isConnected_ShouldReturnConnectionStatusAsTrue_WhenStatusConnectionIsTrue() {
        //mock Active Network Info, mock NetworkInfo first
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(networkIsConnected, state.isConnected());
    }

    @Test
    public void isRoaming_ShouldReturnRoamingStatusAsTrue_WhenStatusConnectionIsFalse() {

        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE );
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(roamingIsOn, state.isRoaming());
    }


    @Test
    public void isUsingMobileData_ShouldReturnTrue_WhenValidNetworkInfo() {
        //mock Active Network Info, mock NetworkInfo first
        networkInfoSetUpForConnectivityManager(networkIsConnected, roamingIsOn, ConnectivityManager.TYPE_MOBILE);
        state = PhoneUtils.getPhoneState(telephonyManager, connectivityManager);
        assertEquals(true, state.isUsingMobileData());
    }


    /**
     * This method sets up the Connectivity Manager and mocks the methods associated to it according to the method parameters
     *
     * @param isNetworkConnected
     * @param isRoaming
     * @param type
     */
    public void networkInfoSetUpForConnectivityManager(boolean isNetworkConnected, boolean isRoaming, int type){
        //mock Active Network Info, mock NetworkInfo first
        when(networkInfo.isConnected()).thenReturn(isNetworkConnected);
        when(networkInfo.isRoaming()).thenReturn(isRoaming);
        when(networkInfo.getType()).thenReturn(type);

        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    }
}