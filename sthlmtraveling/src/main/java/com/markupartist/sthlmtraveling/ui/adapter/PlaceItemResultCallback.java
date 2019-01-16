package com.markupartist.sthlmtraveling.ui.adapter;

import com.markupartist.sthlmtraveling.provider.site.Site;

public interface PlaceItemResultCallback {
    // TODO: Replace with Place.
    void onResult(Site site);

    void onError();
}