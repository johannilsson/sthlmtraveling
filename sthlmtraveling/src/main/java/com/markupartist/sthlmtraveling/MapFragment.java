package com.markupartist.sthlmtraveling;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.markupartist.sthlmtraveling.ui.view.TouchImageView;

public class MapFragment extends BaseFragment{
     private TouchImageView touch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.map_fragment, container, false);
        touch = rootView.findViewById(R.id.map_view);

        return rootView;
    }
}
