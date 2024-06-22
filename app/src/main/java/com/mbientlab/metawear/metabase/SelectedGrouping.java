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

import java.util.ArrayList;
import java.util.List;

/**
 * The type SelectedGrouping class; stores currently selected group data when users click on a group object
 */
class SelectedGrouping {
    /**
     * a list of MetaBase devices in the group
     */
    List<MetaBaseDevice> devices;
    /**
     * a list of sessions of currently selected group
     */
    List<AppState.Session> sessions;
    /**
     *  A list records the MetaBase devices that run successfully.
     */
    List<MetaBaseDevice> devicesRunSuccessful;
    /**
     * The group name
     */
    String name;
    /**
     * a list of config sessions of currently selected group
     */
    List<AppState.ConfigSession> configSessions;

    AppState.SummaryItem summaryItem;

    /**
     * Instantiates a new SelectedGrouping object
     *
     * @param sessions       the list of sessions
     * @param configSessions the list of config sessions
     * @param name           the name of the group
     */
    SelectedGrouping(List<AppState.Session> sessions, List<AppState.ConfigSession> configSessions, String name,
                     AppState.SummaryItem summaryItem) {
        devices = new ArrayList<>();
        this.sessions = sessions;
        this.configSessions = configSessions;
        this.name = name;
        devicesRunSuccessful = new ArrayList<>();
        this.summaryItem = summaryItem;
    }
}
