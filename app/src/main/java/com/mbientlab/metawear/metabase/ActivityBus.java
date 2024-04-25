/*
 * Copyright 2014-2018 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.metabase;

import android.bluetooth.BluetoothDevice;

import com.mbientlab.function.Action;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.List;

import bolts.Task;


/**
 * The interface ActivityBus
 */
interface ActivityBus {
    /**
     * Scanner Bluetooth device scanner
     *
     * @return the Bluetooth device scanner 
     */
    BtleScanner scanner();

    /**
     * Go back to the previous page when click on the device's back button
     *
     * @param handler the handler
     */
    void onBackPressed(Action<Void> handler);

    /**
     * Pop backstack.
     */
    void popBackstack();

    /**
     * Navigate back to the previous page
     */
    void navigateBack();

    /**
     * Swap fragment.
     *
     * @param nextFragmentClass the next fragment class
     */
    void swapFragment(Class<? extends AppFragmentBase> nextFragmentClass);

    /**
     * Swap fragment and pass parameters
     *
     * @param nextFragmentClass the next fragment class
     * @param parameter         the parameter
     */
    void swapFragment(Class<? extends AppFragmentBase> nextFragmentClass, Object parameter);

    /**
     * Gets the MetaWear Board of the MetaBase device
     *
     * @param device the device
     * @return the meta wear board
     */
    MetaWearBoard getMetaWearBoard(BluetoothDevice device);

    /**
     * Remove MetaWear Board
     *
     * @param device the device
     */
    void removeMetaWearBoard(BluetoothDevice device);

    /**
     * Reconnect task.
     *
     * @param metawear the MetaWear Board of the MetaBase device
     * @param retries the MetaWear Board of the MetaBase device
     * @return the task
     */
    Task<Void> reconnect(MetaWearBoard metawear, int retries);

    /**
     * Reconnect task.
     *
     * @param metawear the MetaWear Board of the MetaBase device
     * @param delay    the delay time
     * @param retries  the MetaWear Board of the MetaBase device
     * @return the task
     */
    Task<Void> reconnect(MetaWearBoard metawear, long delay, int retries);

    /**
     * Parameter object.
     *
     * @return the object
     */
    Object parameter();
}
