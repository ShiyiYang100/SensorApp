/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mbientlab.function.Action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AppState class, save the app's data during execution time when the app is running in background
 */
class AppState {
    /**
     * The Session class
     * session saves the CSV files of a MetaBase device
     */
    static class Session {
        /**
         * The Name.
         */
        public String name;
        /**
         * The Time when the Session object is created
         */
        public final String time;
        /**
         * The Files.
         */
        public final List<File> files;

        /**
         * Instantiates a new Session object
         *
         * @param name the name
         * @param time the time when the Session object is created
         */
        Session(String name, String time) {
            this(name, time, new ArrayList<>());
        }

        /**
         * Instantiates a new Session object
         *
         * @param name  the name
         * @param time  the time when the Session object is created
         * @param files the files
         */
        Session(String name, String time, List<File> files) {
            this.name = name;
            this.time = time;
            this.files = files;
        }

        /**
         * Filter list of the files
         *
         * @param device the device
         * @return the filtered list of the files
         */
        List<File> filter(MetaBaseDevice device) {
            final List<File> result = new ArrayList<>();
            String target = device.getFileFriendlyMac();
            for(File f: files) {
                if (target.equals(f.getName().split("_")[3])) {
                    result.add(f);
                }
            }
            return result;
        }

        /**
         * The Adapter of the sessions
         */
        static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
            /**
             * The type View holder.
             */
            class ViewHolder extends RecyclerView.ViewHolder {
                /**
                 * Instantiates a new View holder.
                 *
                 * @param itemView the item view of the Session object
                 */
                ViewHolder(View itemView) {
                    super(itemView);

                    itemView.findViewById(R.id.session_share).setOnClickListener(v -> {
                        if (shareSession != null) {
                            shareSession.apply(items.get(getAdapterPosition()));
                        }
                    });
                    //itemView.findViewById(R.id.session_sync).setOnClickListener(v -> {
                    //    if (syncSession != null) {
                    //        syncSession.apply(items.get(getAdapterPosition()));
                    //    }
                    //});
                }
            }

            /**
             * The items, a list of Session objects
             */
            final List<Session> items;
            /**
             * The Share session.
             */
            Action<Session> shareSession, /**
             * The Sync session.
             */
            syncSession;

            /**
             * Instantiates a new Adapter.
             */
// Provide a suitable constructor (depends on the kind of data set)
            Adapter() {
                items = new ArrayList<>();
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.session_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Session session = items.get(position);

                ((TextView) holder.itemView.findViewById(R.id.config_session_name)).setText(session.name);
                ((TextView) holder.itemView.findViewById(R.id.config_session_time)).setText(session.time);
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            /**
             * Add a new session data
             *
             * @param value a Session object
             */
            void add(Session value) {
                items.add(value);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * The Config Session class
     */
    static class ConfigSession {
        /**
         * The Name.
         */
        String name;
        /**
         * The Time when the config session is created
         */
        public final String time;
        /**
         * The enableStatus, a boolean list which records if the enable checkbox of a sensor is checked
         */
//record which one is enabled
        boolean [] enableStatus = new boolean [16];
        /**
         * The enableGraphs, a boolean list which records if the graph checkbox of a sensor is checked
         */
        boolean [] enableGraphs = new boolean[16];


        /**
         * Instantiates a new Config Session object
         *
         * @param name         the name
         * @param time         the time when the config session is created
         * @param enableStatus the enableStatus list
         * @param enableGraphs the enableGraphs list
         */
        ConfigSession(String name, String time, boolean []enableStatus,boolean [] enableGraphs){
            this.name = name;
            this.enableStatus = enableStatus;
            this.enableGraphs = enableGraphs;
            this.time = time;
        }


        /**
         * The Adapter of the config sessions
         */
        static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

            /**
             * A list of Config Session items
             */
            List<ConfigSession> configItems = new ArrayList<>();
            /**
             * The Item clicked.
             */
            Action<ConfigSession> itemClicked;

            /**
             * The type View holder.
             */
            class ViewHolder extends RecyclerView.ViewHolder {
                /**
                 * Instantiates a new View holder.
                 *
                 * @param itemView the item view of the Config Session object
                 */
                ViewHolder(View itemView) {
                    super(itemView);
                    itemView.setOnClickListener(v -> {

                        ConfigSession configSession = configItems.get(getAdapterPosition());
                        SavedSessionsFragment.position = getAdapterPosition();

                        notifyItemChanged(getAdapterPosition());
                        if (itemClicked != null) {
                            itemClicked.apply(configItems.get(getAdapterPosition()));
                        }
                    });
                }
            }


            /**
             * Instantiates a new Adapter.
             */
            Adapter() {
                configItems = new ArrayList<>();
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_session_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                ConfigSession configSession = configItems.get(position);


                ((TextView) holder.itemView.findViewById(R.id.config_session_name)).setText(configSession.name);
                ((TextView) holder.itemView.findViewById(R.id.config_session_time)).setText(configSession.time);
            }

            @Override
            public int getItemCount() {
                return configItems.size();
            }

            /**
             * Add.
             *
             * @param value a Config Session object
             */
            void add(ConfigSession value) {
                configItems.add(value);
                notifyDataSetChanged();
            }
        }
    }
    static class SummaryItem{

        TemperalParameters temperalParameters;
        SpatialParameters spatialParameters;
        AngularParameters angularParameters;

        String deviceName;

        public SummaryItem(TemperalParameters temperalParameters, SpatialParameters spatialParameters, AngularParameters angularParameters) {
            this.temperalParameters = temperalParameters;
            this.spatialParameters = spatialParameters;
            this.angularParameters = angularParameters;
            this.deviceName = "";
        }

        public TemperalParameters getTemperalParameters() {
            return temperalParameters;
        }

        public void setTemperalParameters(TemperalParameters temperalParameters) {
            this.temperalParameters = temperalParameters;
        }

        public SpatialParameters getSpatialParameters() {
            return spatialParameters;
        }

        public void setSpatialParameters(SpatialParameters spatialParameters) {
            this.spatialParameters = spatialParameters;
        }

        public AngularParameters getAngularParameters() {
            return angularParameters;
        }

        public void setAngularParameters(AngularParameters angularParameters) {
            this.angularParameters = angularParameters;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }



        static class Adapter extends RecyclerView.Adapter<SummaryItem.Adapter.ViewHolder> {

            /**
             * A list of Config Session items
             */
            List<SummaryItem> summaryItems = new ArrayList<>();
            /**
             * The Item clicked.
             */
            Action<SummaryItem> itemClicked;

            /**
             * The type View holder.
             */
            class ViewHolder extends RecyclerView.ViewHolder {
                /**
                 * Instantiates a new View holder.
                 *
                 * @param itemView the item view of the Config Session object
                 */
                ViewHolder(View itemView) {
                    super(itemView);
                    itemView.setOnClickListener(v -> {

                        SummaryItem summaryItem = summaryItems.get(getAdapterPosition());
                        SummaryFragment.position = getAdapterPosition();

                        notifyItemChanged(getAdapterPosition());
                        if (itemClicked != null) {
                            itemClicked.apply(summaryItems.get(getAdapterPosition()));
                        }
                    });
                }
            }


            /**
             * Instantiates a new Adapter.
             */
            Adapter() {
                summaryItems = new ArrayList<>();
            }

            @NonNull
            @Override

            public SummaryItem.Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new SummaryItem.Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.orientation_max_min, parent, false));
            }



            @Override
            public void onBindViewHolder(@NonNull SummaryItem.Adapter.ViewHolder holder, int position) {
                SummaryItem summaryItem = summaryItems.get(position);

                ((TextView) holder.itemView.findViewById(R.id.orientation_name)).setText(summaryItem.deviceName+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_oxMax)).setText(summaryItem.getAngularParameters().getxAvgMax()+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_oyMax)).setText(summaryItem.getAngularParameters().getyAvgMax()+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_ozMax)).setText(summaryItem.getAngularParameters().getzAvgMax()+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_oxMin)).setText(summaryItem.getAngularParameters().getxAvgMin()+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_oyMin)).setText(summaryItem.getAngularParameters().getyAvgMin()+"");
                ((TextView) holder.itemView.findViewById(R.id.summary_ozMin)).setText(summaryItem.getAngularParameters().getzAvgMin()+"");

            }

            @Override
            public int getItemCount() {
                return summaryItems.size();
            }

            void add(SummaryItem value) {
                summaryItems.add(value);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * The Group class
     */
    static class Group {
        /**
         * The Name.
         */
        public final String name;
        /**
         * The map of the devices in the group
         * key : mac id of the MetaBase devices with ":" removed (File friendly mac id)
         * value: MetaBase devices
         */
        public final Map<String, MetaBaseDevice> devices;
        /**
         * sessions: a list of Session objects
         */
        public final List<Session> sessions;
        /**
         * configSessions: a list of Config Session objects
         */
        List<ConfigSession> configSessions;

        AppState.SummaryItem summaryItem;

        /**
         * Instantiates a new Group object
         *
         * @param name    the name
         * @param devices the map of the devices in the group
         */
        Group(String name, Map<String, MetaBaseDevice> devices) {
            this.name = name;
            this.devices = devices;
            this.sessions = new ArrayList<>();
            this.configSessions  = new ArrayList<>();
            this.summaryItem = new SummaryItem(new TemperalParameters(),
                    new SpatialParameters(),
                    new AngularParameters());
        }

        /**
         * The Adapter of Group objects
         */
        static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
            /**
             * The type View holder.
             */
            class ViewHolder extends RecyclerView.ViewHolder {
                /**
                 * Instantiates a new View holder.
                 *
                 * @param itemView the item view of the Group object
                 */
                ViewHolder(View itemView) {
                    super(itemView);

                    itemView.setOnClickListener(v -> {
                        if (itemSelected != null) {
                            itemSelected.apply(items.get(getAdapterPosition()));
                        }
                    });
                }
            }

            /**
             * The list of Group objects
             */
            final List<Group> items;
            /**
             * The Group items selected.
             */
            Action<Group> itemSelected;

            /**
             * Instantiates a new Adapter.
             */
            Adapter() {
                items = new ArrayList<>();
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new Adapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Group group =items.get(position);

                ((TextView) holder.itemView.findViewById(R.id.group_name)).setText(group.name);
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            /**
             * Add.
             *
             * @param key the key
             */
            void add(Group key) {
                items.add(key);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * The map of the MetaBase devices
     */
    static Map<String, MetaBaseDevice> devices = new HashMap<>();
    /**
     * The map of the groups
     */
    static Map<String, Group> groups = new HashMap<>();

    //variables below are used to store the devices and groups information
    //the information is stored even if the app is removed from the background task of the mobile device
    /**
     * The Devices path, used to store the devices information
     */
    static File devicesPath = null, /**
     * The Groups path, used to store the group information
     */
    groupsPath = null, /**
     * The Old devices path, store the information of existing devices of the app
     */
    oldDevicesPath = null;
}
