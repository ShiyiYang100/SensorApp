package com.mbientlab.metawear.metabase;

import android.bluetooth.BluetoothDevice;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.mbientlab.function.Action;
import com.mbientlab.metawear.MetaWearBoard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The class for MetaBase device
 */
public class MetaBaseDevice {
    /**
     * The Bluetooth device of the MetaBase Device
     */
    BluetoothDevice btDevice;
    /**
     * The Name.
     */
    String name, /**
     * The Model name.
     */
    modelName, /**
     * The Model number.
     */
    modelNumber;
    /**
     * The Mac.
     */
    final String mac;
    /**
     * The Sessions which saves the CSV files
     */
    final List<AppState.Session> sessions;
    /**
     * The Config sessions which saves users' selection of Config session
     */
    List<AppState.ConfigSession> configSessions;
    /**
     * if the MetaBase device is recording
     */
    boolean isRecording,
    /**
     * if the MetaBase device is selected
     */
    isSelected,
    /**
     * if the MetaBase device is discovered
     */
    isDiscovered;
    /**
     * The Rssi.
     */
    int rssi,
    /**
     * The Battery.
     */
    battery;

    /**
     *  The MetaBaseDeviceData
     */
    MetaBaseDeviceData m;

    /**
     * The Reset discoered.
     */
    Runnable resetDiscoered;

    /**
     *  record data shown in the summary page fragment
     */
    AppState.SummaryItem summaryItem;

    /**
     * Instantiates a new MetaBase device object
     *
     * @param btDevice the Bluetooth device
     * @param name     the name
     */
    MetaBaseDevice(BluetoothDevice btDevice, String name) {
        this(name, btDevice.getAddress(), new ArrayList<>(), new ArrayList<>(),
                new AppState.SummaryItem(new TemperalParameters(),
                        new SpatialParameters(),
                        new AngularParameters()));
        this.btDevice = btDevice;
        m = new MetaBaseDeviceData();
    }

    /**
     * Instantiates a new MetaBase device.
     *
     * @param name           the name
     * @param mac            the mac
     * @param sessions       the sessions
     * @param configSessions the config sessions
     */
    MetaBaseDevice(String name, String mac, List<AppState.Session> sessions,List<AppState.ConfigSession> configSessions, AppState.SummaryItem summaryItem) {
        this.name = name;
        this.mac = mac;
        this.sessions = sessions;
        this.configSessions = configSessions;
        this.rssi = 0;
        this.battery = 0;
        m = new MetaBaseDeviceData();
        this.summaryItem = summaryItem;

    }

    MetaBaseDevice(String name, String mac, List<AppState.Session> sessions,List<AppState.ConfigSession> configSessions) {
        this.name = name;
        this.mac = mac;
        this.sessions = sessions;
        this.configSessions = configSessions;
        this.rssi = 0;
        this.battery = 0;
        m = new MetaBaseDeviceData();
        this.summaryItem = new AppState.SummaryItem(new TemperalParameters(),
                new SpatialParameters(),
                new AngularParameters());
    }

    /**
     * Instantiates a new MetaBase device.
     *
     * @param name     the name
     * @param mac      the mac
     * @param sessions the sessions
     */
    MetaBaseDevice(String name, String mac, List<AppState.Session> sessions) {
        this.name = name;
        this.mac = mac;
        this.sessions = sessions;
        this.configSessions = configSessions;
        this.rssi = 0;
        this.battery = 0;
        m = new MetaBaseDeviceData();
    }

    public MetaBaseDeviceData getMetaBaseDeviceData(){
        return this.m;
    }

    /**
     * Gets the MetaBase's mac id with ":" removed
     *
     * @return the MetaBase's mac id with ":" removed
     */
    String getFileFriendlyMac() {
        return mac.replaceAll(":", "");
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this) || ((obj instanceof MetaBaseDevice) && mac.equals(((MetaBaseDevice) obj).mac));
    }

    /**
     * The type Adapter for views that display one device per row
     */
    static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final static int RSSI_BAR_LEVELS= 5;
        private final static int RSSI_BAR_SCALE= 100 / RSSI_BAR_LEVELS;

        /**
         * The type View holder.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * Instantiates a new View holder.
             *
             * @param itemView the item view of each device item
             */
            ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(v -> {
                    MetaBaseDevice device = items.get(getAdapterPosition());
                    device.isSelected= !device.isSelected;

                    notifyItemChanged(getAdapterPosition());
                    if (itemClicked != null) {
                        itemClicked.apply(items.get(getAdapterPosition()));
                    }
                });
            }
        }

        /**
         * The Items.
         */
        final List<MetaBaseDevice> items;

        /**
         * The Item clicked.
         */
        Action<MetaBaseDevice> itemClicked;
        /**
         * The Selection mode, detect if a device is a single device or is in a group
         */
        AdapterSelectionMode selectionMode = AdapterSelectionMode.SINGLE;
        private Handler deviceUpdaer = new Handler();

        /**
         * Instantiates a new Adapter.
         */
// Provide a suitable constructor (depends on the kind of data set)
        Adapter() {
            items = new ArrayList<>();
        }

        /**
         * Toggle selection mode, set selectionMode to SINGLE for individual device and MULTIPLE for devices in a group
         */
        void toggleSelectionMode() {
            switch(selectionMode) {
                case SINGLE:
                    selectionMode = AdapterSelectionMode.MULTIPLE;
                    for(MetaBaseDevice d: items) {
                        d.isSelected = false;
                    }
                    break;
                case MULTIPLE:
                    selectionMode = AdapterSelectionMode.SINGLE;
                    break;
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.device_scan_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MetaBaseDevice device = items.get(position);

            CheckBox selected = holder.itemView.findViewById(R.id.selected);
            selected.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.VISIBLE : View.GONE);
            selected.setChecked(device.isSelected);

            ((TextView) holder.itemView.findViewById(R.id.device_name)).setText(device.name);
            ((TextView) holder.itemView.findViewById(R.id.device_mac)).setText(device.mac);

            ImageView rssi = holder.itemView.findViewById(R.id.device_rssi);
            rssi.setImageLevel(device.isDiscovered ? Math.min(RSSI_BAR_LEVELS - 1, (127 + device.rssi + 5) / RSSI_BAR_SCALE) :10000);
            rssi.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.INVISIBLE : View.VISIBLE);

            holder.itemView.findViewById(R.id.device_recording).setVisibility(device.isRecording ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * update the connection status of the MetaBase device
         * @param key
         * @param pos
         */
        private void update(MetaBaseDevice key, int pos) {
            MetaBaseDevice current = items.get(pos);
            if (current.btDevice == null) {
                current.btDevice = key.btDevice;
            }

            deviceUpdaer.removeCallbacks(current.resetDiscoered);

            current.isDiscovered = key.isDiscovered;
            current.rssi= key.rssi;
            current.isRecording= key.isRecording;
            long delayMillis;


            current.resetDiscoered = () -> {
                current.isDiscovered = false;
                notifyItemChanged(pos);
            };

            deviceUpdaer.postDelayed(current.resetDiscoered,10000L);

            notifyItemChanged(pos);
        }

        /**
         * Update.
         *
         * @param key the key
         */
        void update(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos != -1)  {
                update(key, pos);
            }
        }

        /**
         * Add.
         *
         * @param key the key
         */
        void add(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos == -1) {
                items.add(key);
                notifyDataSetChanged();
            } else {
                update(key, pos);
            }
        }

        /**
         * Remove.
         *
         * @param key the key
         */
        void remove(MetaBaseDevice key) {
            if (items.remove(key)) {
                notifyDataSetChanged();
            }
        }

        /**
         * Remove.
         *
         * @param pos the position of the target device
         */
        void remove(int pos) {
            items.remove(pos);
            notifyDataSetChanged();
        }

        /**
         * Get meta base device.
         *
         * @param pos the pos of the target device
         * @return the MetaBase device
         */
        MetaBaseDevice get(int pos) {
            return items.get(pos);
        }
    }

    /**
     * The type Adapter for views that display two device per row
     */
//adapter for grid view
    static class Adapter1 extends RecyclerView.Adapter<Adapter1.ViewHolder> {
        private final static int RSSI_BAR_LEVELS= 5;
        private final static int RSSI_BAR_SCALE= 100 / RSSI_BAR_LEVELS;

        /**
         * The type View holder.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * Instantiates a new View holder.
             *
             * @param itemView the item view of the device
             */
            ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(v -> {
                    MetaBaseDevice device = items.get(getAdapterPosition());
                    device.isSelected= !device.isSelected;

                    notifyItemChanged(getAdapterPosition());
                    if (itemClicked != null) {
                        itemClicked.apply(items.get(getAdapterPosition()));
                    }
                });
            }
        }

        /**
         * The Items.
         */
        final List<MetaBaseDevice> items;

        /**
         * The Item clicked.
         */
        Action<MetaBaseDevice> itemClicked;
        /**
         * The Selection mode.
         */
        AdapterSelectionMode selectionMode = AdapterSelectionMode.SINGLE;
        private Handler deviceUpdaer = new Handler();

        /**
         * Instantiates a new Adapter 1.
         */
// Provide a suitable constructor (depends on the kind of data set)
        Adapter1() {
            items = new ArrayList<>();
        }

        /**
         * Toggle selection mode, set selectionMode to SINGLE for individual device and MULTIPLE for devices in a group
         */
        void toggleSelectionMode() {
            switch(selectionMode) {
                case SINGLE:
                    selectionMode = AdapterSelectionMode.MULTIPLE;
                    for(MetaBaseDevice d: items) {
                        d.isSelected = false;
                    }
                    break;
                case MULTIPLE:
                    selectionMode = AdapterSelectionMode.SINGLE;
                    break;
            }
            notifyDataSetChanged();
        }


        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.device_home_item, parent, false));
        }



        public void onBindViewHolder(@NonNull Adapter1.ViewHolder holder, int position) {
            MetaBaseDevice device = items.get(position);


            CheckBox selected = holder.itemView.findViewById(R.id.selected_home);
            selected.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.VISIBLE : View.GONE);
            selected.setChecked(device.isSelected);

            ((TextView) holder.itemView.findViewById(R.id.device_name_home)).setText(device.name);
            ((TextView) holder.itemView.findViewById(R.id.device_mac_home)).setText(device.mac);

            ImageView rssi = holder.itemView.findViewById(R.id.device_rssi_home);
            rssi.setImageLevel(device.isDiscovered ? Math.min(RSSI_BAR_LEVELS - 1, (127 + device.rssi + 5) / RSSI_BAR_SCALE) : 10000);
            rssi.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.VISIBLE : View.VISIBLE);

            ImageView battery = holder.itemView.findViewById(R.id.device_battery_home);
            if(device.battery == 0){
                battery.setImageResource(R.mipmap.battery_0);
            }
            else if (device.battery > 0  && device.battery <= 25){
                battery.setImageResource(R.mipmap.battery_25);
            }
            else if (device.battery > 25  && device.battery <= 50){
                battery.setImageResource(R.mipmap.battery_50);
            }
            else if (device.battery > 50  && device.battery <= 75){
                battery.setImageResource(R.mipmap.battery_75);
            }
            else if (device.battery > 75  && device.battery <= 100){
                battery.setImageResource(R.mipmap.battery_100);
            }

            holder.itemView.findViewById(R.id.device_recording_home).setVisibility(device.isRecording ? View.VISIBLE : View.INVISIBLE);



            //add delete button for devices and groups
            ImageView deleteDevice = holder.itemView.findViewById(R.id.delete_device);


        }


        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * update the connection status of the MetaBase device
         * @param key
         * @param pos
         */

        private void update(MetaBaseDevice key, int pos) {
            MetaBaseDevice current = items.get(pos);
            if (current.btDevice == null) {
                current.btDevice = key.btDevice;
            }

            deviceUpdaer.removeCallbacks(current.resetDiscoered);

            current.isDiscovered = key.isDiscovered;
            current.rssi= key.rssi;
            current.isRecording= key.isRecording;
            long delayMillis;


            current.resetDiscoered = () -> {
                current.isDiscovered = false;
                notifyItemChanged(pos);
            };

            deviceUpdaer.postDelayed(current.resetDiscoered,10000L);

            notifyItemChanged(pos);
        }

        /**
         * Update.
         *
         * @param key the key
         */
        void update(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos != -1)  {
                update(key, pos);
            }
        }

        /**
         * Add.
         *
         * @param key the key
         */
        void add(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos == -1) {
                items.add(key);
                notifyDataSetChanged();
            } else {
                update(key, pos);
            }
        }

        /**
         * Remove.
         *
         * @param key the key
         */
        void remove(MetaBaseDevice key) {
            if (items.remove(key)) {
                notifyDataSetChanged();
            }
        }

        /**
         * Remove.
         *
         * @param pos the position of the MetaBase device
         */
        void remove(int pos) {
            items.remove(pos);
            notifyDataSetChanged();
        }

        /**
         * Get meta base device.
         *
         * @param pos the position of the device
         * @return the MetaBase device
         */
        MetaBaseDevice get(int pos) {
            return items.get(pos);
        }
    }

    /**
     * The type Adapter 2, adapter for the ScannerFragment
     */
//adapter for grid view
    static class Adapter2 extends RecyclerView.Adapter<Adapter2.ViewHolder> {
        private final static int RSSI_BAR_LEVELS= 5;
        private final static int RSSI_BAR_SCALE= 100 / RSSI_BAR_LEVELS;

        /**
         * The type View holder.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * Instantiates a new View holder.
             *
             * @param itemView the item view of the device
             */
            ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(v -> {
                    MetaBaseDevice device = items.get(getAdapterPosition());
                    device.isSelected= !device.isSelected;

                    notifyItemChanged(getAdapterPosition());
                    if (itemClicked != null) {
                        itemClicked.apply(items.get(getAdapterPosition()));
                    }
                });
            }
        }

        /**
         * The Items, a list of MetaBase devices
         */
        final List<MetaBaseDevice> items;

        /**
         * The Item clicked.
         */
        Action<MetaBaseDevice> itemClicked;
        /**
         * The Selection mode.
         */
        AdapterSelectionMode selectionMode = AdapterSelectionMode.SINGLE;
        private Handler deviceUpdaer = new Handler();

        /**
         * Instantiates a new Adapter 2.
         */
// Provide a suitable constructor (depends on the kind of data set)
        Adapter2() {
            items = new ArrayList<>();
        }

        /**
         * Toggle selection mode, set selectionMode to SINGLE for individual device and MULTIPLE for devices in a group
         */
        void toggleSelectionMode() {
            switch(selectionMode) {
                case SINGLE:
                    selectionMode = AdapterSelectionMode.MULTIPLE;
                    for(MetaBaseDevice d: items) {
                        d.isSelected = false;
                    }
                    break;
                case MULTIPLE:
                    selectionMode = AdapterSelectionMode.SINGLE;
                    break;
            }
            notifyDataSetChanged();
        }


        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.device_scanner_item, parent, false));
        }



        public void onBindViewHolder(@NonNull Adapter2.ViewHolder holder, int position) {
            MetaBaseDevice device = items.get(position);

            CheckBox selected = holder.itemView.findViewById(R.id.selected_scanner);
            selected.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.VISIBLE : View.GONE);
            selected.setChecked(device.isSelected);

            ((TextView) holder.itemView.findViewById(R.id.device_name_scanner)).setText(device.name);
            ((TextView) holder.itemView.findViewById(R.id.device_mac_scanner)).setText(device.mac);

            ImageView rssi = holder.itemView.findViewById(R.id.device_rssi_scanner);
            rssi.setImageLevel(device.isDiscovered ? Math.min(RSSI_BAR_LEVELS - 1, (127 + device.rssi + 5) / RSSI_BAR_SCALE) : 10000);
            rssi.setVisibility(selectionMode == AdapterSelectionMode.MULTIPLE ? View.INVISIBLE : View.VISIBLE);

            holder.itemView.findViewById(R.id.device_recording_scanner).setVisibility(device.isRecording ? View.VISIBLE : View.INVISIBLE);





        }


        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * update the connection status of the MetaBase device
         * @param key
         * @param pos
         */
        private void update(MetaBaseDevice key, int pos) {
            MetaBaseDevice current = items.get(pos);
            if (current.btDevice == null) {
                current.btDevice = key.btDevice;
            }

            deviceUpdaer.removeCallbacks(current.resetDiscoered);

            current.isDiscovered = key.isDiscovered;
            current.rssi= key.rssi;
            current.isRecording= key.isRecording;
            long delayMillis;


            current.resetDiscoered = () -> {
                current.isDiscovered = false;
                notifyItemChanged(pos);
            };

            deviceUpdaer.postDelayed(current.resetDiscoered,10000L);

            notifyItemChanged(pos);
        }

        /**
         * Update.
         *
         * @param key the key
         */
        void update(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos != -1)  {
                update(key, pos);
            }
        }

        /**
         * Add.
         *
         * @param key the key
         */
        void add(MetaBaseDevice key) {
            int pos= items.indexOf(key);
            if (pos == -1) {
                items.add(key);
                notifyDataSetChanged();
            } else {
                update(key, pos);
            }
        }

        /**
         * Remove.
         *
         * @param key the key
         */
        void remove(MetaBaseDevice key) {
            if (items.remove(key)) {
                notifyDataSetChanged();
            }
        }

        /**
         * Remove.
         *
         * @param pos the position of target MetaBase device
         */
        void remove(int pos) {
            items.remove(pos);
            notifyDataSetChanged();
        }

        /**
         * Get MetaBase device.
         *
         * @param pos the position of target MetaBase device
         * @return the MetaBase device
         */
        MetaBaseDevice get(int pos) {
            return items.get(pos);
        }
    }

}
