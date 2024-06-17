package com.mbientlab.metawear.metabase;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class SummaryFragment extends AppFragmentBase {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private SelectedGrouping parameter;
    private MetaBaseDevice.Adapter devicesAdapter;
    private AppState.Session.Adapter sessionsAdapter;
    private AppState.SummaryItem summaryItem;
    private AppState.ConfigSession.Adapter configSessionAdapter;
    private AppState.SummaryItem.Adapter summaryItemsAdapter;

    private TextView stepCount;
    private TextView totalD;
    private TextView totalT;

    private TextView gaitSpeed;

    private TextView ratio;

    private TextView swingTime, stanceTime, strideTime, toeOff, heelStrike,
                    singleSupportTime, numStep, stepTime, doubleSupportTime,strideLen, walkingSpeed,
                    MFC, OXMAX, OYMAX, OZMAX,
                    OXMIN, OYMIN, OZMIN;
    /**
     * The Position of current selected config session
     */
    static  int position;

    public SummaryFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devicesAdapter = new MetaBaseDevice.Adapter();
        sessionsAdapter = new AppState.Session.Adapter();
        configSessionAdapter = new AppState.ConfigSession.Adapter();
        summaryItemsAdapter = new AppState.SummaryItem.Adapter();

    }

    @Override
    Integer getMenuGroupResId() {
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button continueBut = view.findViewById(R.id.summary_next);
        swingTime = view.findViewById(R.id.summary_swing_time);
        stanceTime = view.findViewById(R.id.summary_stance_time);
        strideTime = view.findViewById(R.id.summary_stride_time);
        heelStrike = view.findViewById(R.id.summary_heel_strike);
        toeOff = view.findViewById(R.id.summary_toe_off);
        singleSupportTime = view.findViewById(R.id.summary_sst);
        numStep = view.findViewById(R.id.summary_num_of_step);
        stepTime = view.findViewById(R.id.summary_step_time);
        doubleSupportTime = view.findViewById(R.id.summary_dst);
        strideLen = view.findViewById(R.id.summary_stride_time);
        walkingSpeed = view.findViewById(R.id.summary_walking_speed);
        MFC = view.findViewById(R.id.summary_mfc);
        OXMAX = view.findViewById(R.id.summary_OXMax);
        OXMIN = view.findViewById(R.id.summary_OXMin);
        OYMAX = view.findViewById(R.id.summary_OYMax);
        OYMIN = view.findViewById(R.id.summary_OYMin);
        OZMAX = view.findViewById(R.id.summary_OZMax);
        OZMIN = view.findViewById(R.id.summay_OZMin);

        continueBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityBus.navigateBack();
            }
        });

        RecyclerView summarySesssions = view.findViewById(R.id.realtime_device_info);

        // Set the layout manager here
        summarySesssions.setLayoutManager(new LinearLayoutManager(getContext()));

        summarySesssions.setAdapter(summaryItemsAdapter);
        summaryItemsAdapter.notifyDataSetChanged();


       
    }


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

        summaryItemsAdapter.summaryItems.clear();

        summaryItem = parameter.summaryItem;

        summaryItemsAdapter.notifyDataSetChanged();

        swingTime.setText(parameter.summaryItem.temperalParameters.getSwingTime() + "");
        stanceTime.setText(parameter.summaryItem.temperalParameters.getStanceTime() + "");
        strideTime.setText(parameter.summaryItem.getTemperalParameters().getStrideTime() + "");
        heelStrike.setText(parameter.summaryItem.getTemperalParameters().getHeelStrike() + "");
        toeOff.setText(parameter.summaryItem.getTemperalParameters().getToeOff()+"");
        singleSupportTime.setText(parameter.summaryItem.getTemperalParameters().getSingleSupportTime() +"");
        numStep.setText(parameter.summaryItem.getSpatialParameters().getNumOfSteps()+"");
        stepTime.setText(parameter.summaryItem.getTemperalParameters().getStepTime()+"");
        doubleSupportTime.setText(parameter.summaryItem.getTemperalParameters().getDoubleSupportTime()+"");
        strideLen.setText(parameter.summaryItem.getSpatialParameters().getStrideLength() + "");
        walkingSpeed.setText(parameter.summaryItem.getSpatialParameters().getWalkingSpeed() + "");
        MFC.setText(parameter.summaryItem.getSpatialParameters().getMFC() + "");



        OXMAX.setText(summaryItem.getAngularParameters().getxAvgMax()+"");
        OYMAX.setText(summaryItem.getAngularParameters().getyAvgMax()+"");
        OZMAX.setText(summaryItem.getAngularParameters().getzAvgMax()+"");
        OXMIN.setText(summaryItem.getAngularParameters().getxAvgMin()+"");
        OYMIN.setText(summaryItem.getAngularParameters().getyAvgMin()+"");
        OZMIN.setText(summaryItem.getAngularParameters().getzAvgMin()+"");

        List<AppState.SummaryItem> summaryItemList = new ArrayList<>();
        for(MetaBaseDevice d: parameter.devices){
            summaryItemList.add(d.summaryItem);
        }
        Log.d("summaryfragment number", "summaryFragment number = " + summaryItemList.size());

        summaryItemsAdapter.summaryItems.addAll(summaryItemList);
        summaryItemsAdapter.notifyDataSetChanged();


    }

}