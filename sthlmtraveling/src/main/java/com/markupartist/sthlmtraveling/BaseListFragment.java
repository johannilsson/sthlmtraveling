package com.markupartist.sthlmtraveling;

import android.content.Intent;
import androidx.fragment.app.ListFragment;

import com.markupartist.sthlmtraveling.utils.Analytics;

import java.util.Map;

public class BaseListFragment extends ListFragment {


    protected void registerScreen(String event) {
        Analytics.getInstance(getActivity()).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {

    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void startActivityWithDefaultTransition(Intent intent) {
        super.startActivity(intent);
    }
}
