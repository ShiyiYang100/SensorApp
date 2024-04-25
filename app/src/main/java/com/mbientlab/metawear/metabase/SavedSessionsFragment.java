package com.mbientlab.metawear.metabase;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;


/**
 * The type SavedSessionsFragment
 */
public class SavedSessionsFragment extends AppFragmentBase  {

    // TODO: Rename parameter arguments, choose names that match

    private SelectedGrouping parameter;
    private MetaBaseDevice.Adapter devicesAdapter;
    private AppState.Session.Adapter sessionsAdapter;
    private AppState.ConfigSession.Adapter configSessionAdapter;
    /**
     * a boolean records if the users is in the Saved Config Sessions page
     */
    static boolean ifAccessedSavedConfigSessions;
    /**
     * The Position of current selected config session
     */
    static  int position;


    /**
     * Instantiates a new SavedSessionsFragment object
     */
    public SavedSessionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        parameter = (SelectedGrouping) activityBus.parameter();

        devicesAdapter.items.clear();
        devicesAdapter.items.addAll(parameter.devices);
        devicesAdapter.notifyDataSetChanged();

        sessionsAdapter.items.clear();
        sessionsAdapter.items.addAll(parameter.sessions);
        sessionsAdapter.notifyDataSetChanged();

        configSessionAdapter.configItems.clear();
        configSessionAdapter.configItems.addAll(parameter.configSessions);
        configSessionAdapter.notifyDataSetChanged();


        //configSessionAdapter.clear();
        //configSessionAdapter.items.addAll(parameter.configSessions);






    }

    /**
     * New instance SavedSessionsFragment
     *
     * @param param1 the param 1
     * @param param2 the param 2
     * @return the saved sessions fragment
     */
    public static SavedSessionsFragment newInstance(String param1, String param2) {
        SavedSessionsFragment fragment = new SavedSessionsFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicesAdapter = new MetaBaseDevice.Adapter();
        sessionsAdapter = new AppState.Session.Adapter();
        configSessionAdapter = new AppState.ConfigSession.Adapter();
        //set the stored data of current selected config session
        configSessionAdapter.itemClicked = arg1 -> {
                for (int i = 0; i < 16; i ++){
                    SensorConfig.Adapter.setTrue[i] = parameter.configSessions.get(position).enableStatus[i];
                    SensorConfig.Adapter.setGraphTrue[i] = parameter.configSessions.get(position).enableGraphs[i];
                }
                activityBus.swapFragment(DeviceConfigFragment.class, parameter);
        };
    }

    @Override
    Integer getMenuGroupResId() {
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_saved_sessions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button testButton = view.findViewById(R.id.test_Button);
        //go to the DeviceConfig Fragment when individual config session is clicked 
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityBus.swapFragment(DeviceConfigFragment.class, parameter);
            }
        });

        RecyclerView configSessions = view.findViewById(R.id.config_sessions);
        configSessions.setAdapter(configSessionAdapter);
        configSessionAdapter.notifyDataSetChanged();
    }
}