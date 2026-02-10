package com.markupartist.sthlmtraveling.ui.adapter

import com.markupartist.sthlmtraveling.provider.site.Site

interface PlaceItemResultCallback {
    // TODO: Replace with Place.
    fun onResult(site: Site)
    fun onError()
}
