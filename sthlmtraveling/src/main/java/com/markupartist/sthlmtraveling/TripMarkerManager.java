package com.markupartist.sthlmtraveling;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.markupartist.sthlmtraveling.data.models.Entrance;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds two Lists of markers. simpleMarkers and detailedMarkers
 * <p/>
 * The simpleMarkers are always shown and when clicked an
 * InfoWindow with same content as the DetailedMarker is shown
 * <p/>
 * The DetailedMarker is only shown if the zoom-level is high enough,
 * so the markers doesn't overlap. The DetailedMarker doesn't have its own InfoWindow,
 * but when clicked, it displays the InfoWindow of the SimpleMarker. So even if the user zooms out,
 * the InfoWindow will still be shown
 */
public class TripMarkerManager extends ClusterManager<TripMarkerManager.DetailedMarker> implements GoogleMap.OnMarkerClickListener, GoogleMap.InfoWindowAdapter {
    private final GoogleMap mMap;
    private final Context context;
    private final List<Marker> entranceMarkers = new ArrayList<>();
    private final List<Marker> simpleMarkers = new ArrayList<>();
    private final List<DetailedMarker> detailedMarkers = new ArrayList<>();
    private final List<Marker> endpointMarkers = new ArrayList<>();

    public void addEntrance(Entrance entrance, boolean isExits) {
        BitmapDescriptor icon = getColoredMarker(
                ContextCompat.getColor(context, R.color.primary), R.drawable.ic_entrance_exit_12dp);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(entrance.getLat(), entrance.getLon()))
                .anchor(0.5f, 0.5f)
                .icon(icon);
        if (!TextUtils.isEmpty(entrance.getName())) {
            if (isExits) {
                markerOptions.title(context.getString(R.string.exit_and_name, entrance.getName()));
            } else {
                markerOptions.title(context.getString(R.string.entrance_and_name, entrance.getName()));
            }
        } else {
            if (isExits) {
                markerOptions.title(context.getString(R.string.exit));
            } else {
                markerOptions.title(context.getString(R.string.entrance));
            }
        }
        Marker entranceMarker = mMap.addMarker(markerOptions);
        entranceMarkers.add(entranceMarker);
        DetailedMarker dm = new DetailedMarker(new LatLng(entrance.getLat(), entrance.getLon()), markerOptions.getTitle(), "", Color.BLACK, false, false);
        detailedMarkers.add(dm);

    }

    public void addMarker(LatLng position, String stopName, String time, int color, boolean endPoint, boolean startpoint) {
        BitmapDescriptor icon = getColoredMarker(color, R.drawable.ic_line_marker);

        float anchorU = 0.5f;
        float anchorV = 0.5f;
        float infoWindowAnchorV = 0f;

        Marker marker = mMap.addMarker(new MarkerOptions()
                .anchor(anchorU, anchorV)
                .infoWindowAnchor(0.5f, infoWindowAnchorV)
                .position(position)
                .title(stopName)
                .snippet(time)
                .icon(icon));
        simpleMarkers.add(marker);

        DetailedMarker dm = new DetailedMarker(position, stopName, time, color, endPoint, startpoint);

        if (endPoint) {
            IconGenerator mIconGenerator = new IconGenerator(context);
            mIconGenerator.setContentView(getDetailedMarkerView(dm));

            mIconGenerator.setColor(dm.color);

            Bitmap endPointIcon = mIconGenerator.makeIcon();

//V for detailedMarker is
            float endpointAnchorV = (((float) getColoredMarkerBitmap(Color.RED, R.drawable.ic_line_marker).getHeight() / 2) / endPointIcon.getHeight()) + 1f;
            Marker endpointMarker = mMap.addMarker(new MarkerOptions()
                    .anchor(anchorU, endpointAnchorV)
                    .infoWindowAnchor(0.5f, infoWindowAnchorV)
                    .position(position)
                    .title(stopName)
                    .snippet(time)
                    .icon(BitmapDescriptorFactory.fromBitmap(endPointIcon)));
            endpointMarkers.add(endpointMarker);
        }
        detailedMarkers.add(dm);
        this.addItem(dm);
    }

    public TripMarkerManager(Context context, GoogleMap map) {
        super(context, map);
        setRenderer(new MyMarkerRenderer(context, map, this));
        this.context = context;
        this.mMap = map;
    }

    private View getDetailedMarkerView(DetailedMarker detailedMarker) {
        @SuppressLint("InflateParams")  //Ignore null parent here, as view is only used for creating marker bitmap
                View detailedMarkerView = LayoutInflater.from(context).inflate(R.layout.detailed_marker, null);

        detailedMarkerView.setBackgroundColor(detailedMarker.color);
        TextView timeTV = (TextView) detailedMarkerView.findViewById(R.id.marker_time);
        TextView nameTV = (TextView) detailedMarkerView.findViewById(R.id.marker_name);
        TextView extraTV = (TextView) detailedMarkerView.findViewById(R.id.marker_extra);

        timeTV.setTextColor(Color.WHITE);
        nameTV.setTextColor(Color.WHITE);
        extraTV.setTextColor(Color.WHITE);


        nameTV.setText(detailedMarker.name);
        timeTV.setText(detailedMarker.time);
        if (detailedMarker.startpoint) {
            nameTV.setText("");
            extraTV.setText(detailedMarker.name);
            extraTV.setVisibility(View.VISIBLE);
        }

        return detailedMarkerView;
    }

    private class MyMarkerRenderer extends DefaultClusterRenderer<DetailedMarker> {
        final IconGenerator mIconGenerator;

        public MyMarkerRenderer(Context context, GoogleMap map, ClusterManager<DetailedMarker> clusterManager) {
            super(context, map, clusterManager);
            this.mIconGenerator = new IconGenerator(context);
        }

        @Override
        protected void onBeforeClusterItemRendered(DetailedMarker item, MarkerOptions markerOptions) {
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);

            boolean detailedMapMarkers = SP.getBoolean("detailed_map_markers", false);
            if (detailedMapMarkers) {
                markerOptions.visible(true);
            } else {
                if (item.endPoint) {
                    markerOptions.visible(true);
                } else {
                    markerOptions.visible(false);
                }
            }


            mIconGenerator.setContentView(getDetailedMarkerView(item));

            mIconGenerator.setColor(item.color);

            Bitmap icon = mIconGenerator.makeIcon();

            //V for detailedMarker is

            float anchorV = (((float) getColoredMarkerBitmap(Color.RED, R.drawable.ic_line_marker).getHeight() / 2) / icon.getHeight()) + 1f;

            markerOptions
                    .anchor(0.5f, anchorV)
                    .icon(BitmapDescriptorFactory.fromBitmap(icon));
            super.onBeforeClusterItemRendered(item, markerOptions);
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<DetailedMarker> cluster, MarkerOptions markerOptions) {
            //Obviously we don't want to display cluster markers, they would represent nothing useful
            markerOptions.visible(false);


        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<DetailedMarker> cluster) {
            //1 is a good number, or markers will start to overlap
            return cluster.getSize() > 1;
        }
    }

    public class DetailedMarker implements ClusterItem {

        public final LatLng position;
        public final String name;
        public final String time;
        public final int color;
        public final boolean endPoint;
        public final boolean startpoint;

        public DetailedMarker(LatLng position, String name, String time, int color, boolean endPoint, boolean startpoint) {
            this.position = position;
            this.name = name;
            this.time = time;
            this.color = color;
            this.endPoint = endPoint;
            this.startpoint = startpoint;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }
    }

    @Override
    public void clearItems() {
        super.clearItems();
        detailedMarkers.clear();
        simpleMarkers.clear();
        endpointMarkers.clear();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (entranceMarkers.contains(marker)) {
            return false;
        }

        boolean isSimpleMarker = false;
        for (Marker simpleMarker : simpleMarkers) {
            if (marker.getId().equals(simpleMarker.getId())) {
                isSimpleMarker = true;
            }
        }
        if (!isSimpleMarker) {
            //ok so this was a detailed marker, display the infoWindow of the corresponding simpleMarker
            for (Marker simpleMarker : simpleMarkers) {
                if (simpleMarker.getPosition().equals(marker.getPosition())) {
                    Log.e("aksel", "found matching simplemarker, title=" + simpleMarker.getTitle() + " vis=" + simpleMarker.isVisible() + " id=" + simpleMarker.getId());
                    simpleMarker.setTitle(simpleMarker.getTitle());
                    simpleMarker.showInfoWindow();
                    break;
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }
        return !isSimpleMarker;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        if (marker.getTitle() == null || marker.getTitle().equals("")) {
            //this is a detailed marker, we only show InfoWindows for simple markers
            return null;
        }
        Log.e("Aksel", "marker.getTitle()=" + marker.getTitle() + " ");
        LinearLayout bubbleLayout = new LinearLayout(context);

        for (DetailedMarker detailedMarker : detailedMarkers) {
            if (marker.getPosition().equals(detailedMarker.getPosition())) {
                View detailedMarkerView = getDetailedMarkerView(detailedMarker);
                bubbleLayout.addView(detailedMarkerView);
                BubbleDrawable bubbleDrawable = new BubbleDrawable(context.getResources());

                bubbleDrawable.setColor(detailedMarker.color);

                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    //noinspection deprecation
                    bubbleLayout.setBackgroundDrawable(bubbleDrawable);
                } else {
                    bubbleLayout.setBackground(bubbleDrawable);
                }

                break;
            }
        }


        return bubbleLayout;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }


    /**
     * Borrowed class from Android Map Utils Library. Code is Apache 2.0
     * <p/>
     * Draws a bubble with a shadow, filled with any color.
     */
    class BubbleDrawable extends Drawable {

        private final Drawable mShadow;
        private final Drawable mMask;
        private int mColor = Color.WHITE;

        public BubbleDrawable(Resources res) {
            mMask = res.getDrawable(com.google.maps.android.R.drawable.bubble_mask);
            mShadow = res.getDrawable(com.google.maps.android.R.drawable.bubble_shadow);
        }

        public void setColor(int color) {
            mColor = color;
        }

        @Override
        public void draw(Canvas canvas) {
            mMask.draw(canvas);
            canvas.drawColor(mColor, PorterDuff.Mode.SRC_IN);
            mShadow.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            mMask.setBounds(left, top, right, bottom);
            mShadow.setBounds(left, top, right, bottom);
        }

        @Override
        public void setBounds(Rect bounds) {
            mMask.setBounds(bounds);
            mShadow.setBounds(bounds);
        }

        @Override
        public boolean getPadding(Rect padding) {
            return mMask.getPadding(padding);
        }
    }

    Bitmap getColoredMarkerBitmap(@ColorInt int colorInt, @DrawableRes int drawableRes) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableRes);
        Bitmap bitmapCopy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCopy);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
        return bitmapCopy;
    }

    BitmapDescriptor getColoredMarker(@ColorInt int colorInt, @DrawableRes int drawableRes) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableRes);
        Bitmap bitmapCopy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCopy);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmapCopy);
    }

    private int getPixelsFromDp(float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float fpixels = metrics.density * dp;
        return (int) (fpixels + 0.5f);
    }


}
