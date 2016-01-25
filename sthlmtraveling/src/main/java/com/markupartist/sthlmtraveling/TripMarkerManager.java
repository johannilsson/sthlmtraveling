package com.markupartist.sthlmtraveling;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

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
    private final List<Marker> simpleMarkers = new ArrayList<>();
    private final List<DetailedMarker> detailedMarkers = new ArrayList<>();

    public void addMarker(LatLng position, String stopName, String time, int color, boolean endPoint) {
        BitmapDescriptor icon;
        float anchorU = 0.5f;
        float anchorV = 0.5f;
        float infoWindowAnchorV = 0f;
        if (endPoint) {
            Bitmap normalIcon = getColoredMarker(Color.RED);
            Bitmap glowingIcon = addGlow(getColoredMarker(color));
            icon = BitmapDescriptorFactory.fromBitmap(glowingIcon);

            // ((x-y)/2)/x
            infoWindowAnchorV = (((float) glowingIcon.getHeight() - normalIcon.getHeight()) / 2) / glowingIcon.getHeight();
        } else {
            icon = BitmapDescriptorFactory.fromBitmap(getColoredMarker(color));
        }


        Marker marker = mMap.addMarker(new MarkerOptions()
                .anchor(anchorU, anchorV)
                .infoWindowAnchor(0.5f, infoWindowAnchorV)
                .position(position)
                .title(stopName)
                .snippet(time)
                .icon(icon));
        simpleMarkers.add(marker);

        DetailedMarker dm = new DetailedMarker(position, stopName, time, color, endPoint);
        detailedMarkers.add(dm);
        this.addItem(dm);
        removeDuplicate(dm);
    }

    /**
     * Removes the duplicate DetailedMarker that is created in the start and the end of each Route.
     * It keeps the marker that looks like default google maps pin.
     * Meaning the supplied marker might be the one removed
     *
     * @param paramDetailedMarker marker to find duplicates for.
     */
    private void removeDuplicate(DetailedMarker paramDetailedMarker) {
        for (DetailedMarker detailedMarker : detailedMarkers) {
            if (paramDetailedMarker.getPosition().equals(detailedMarker.getPosition()) && !detailedMarker.endPoint && paramDetailedMarker.endPoint) {
                //found a duplicate
                detailedMarkers.remove(detailedMarker);
                this.removeItem(detailedMarker);
                break;
            } else if (paramDetailedMarker.getPosition().equals(detailedMarker.getPosition()) && detailedMarker.endPoint && !paramDetailedMarker.endPoint) {
                //found a duplicate
                detailedMarkers.remove(paramDetailedMarker);
                this.removeItem(paramDetailedMarker);
                break;
            }
        }
    }

    public TripMarkerManager(Context context, GoogleMap map) {
        super(context, map);
        setRenderer(new MyMarkerRenderer(context, map, this));
        this.context = context;
        this.mMap = map;

        //GridBasedAlgorithm supports delete, which we are using to avoid duplicates
        setAlgorithm(new GridBasedAlgorithm<DetailedMarker>());
        extra();
    }

    private View getDetailedMarkerView(DetailedMarker detailedMarker) {
        @SuppressLint("InflateParams")  //Ignore null parent here, as view is only used for creating marker bitmap
                View detailedMarkerView = LayoutInflater.from(context).inflate(R.layout.detailed_marker, null);
        detailedMarkerView.setBackgroundColor(detailedMarker.color);
        if (markerStyle == MarkerStyle.WHITE) {
            detailedMarkerView.setBackgroundColor(Color.WHITE);

            ((TextView) detailedMarkerView.findViewById(R.id.marker_time)).setTextColor(Color.BLACK);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_name)).setTextColor(Color.BLACK);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_extra)).setTextColor(Color.BLACK);
        } else {
            detailedMarkerView.setBackgroundColor(detailedMarker.color);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_time)).setTextColor(Color.WHITE);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_name)).setTextColor(Color.WHITE);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_extra)).setTextColor(Color.WHITE);
        }


        ((TextView) detailedMarkerView.findViewById(R.id.marker_name)).setText(detailedMarker.name);
        ((TextView) detailedMarkerView.findViewById(R.id.marker_time)).setText(detailedMarker.time);
        if (detailedMarker.endPoint) {
            ((TextView) detailedMarkerView.findViewById(R.id.marker_name)).setText("");
            ((TextView) detailedMarkerView.findViewById(R.id.marker_extra)).setText(detailedMarker.name);
            ((TextView) detailedMarkerView.findViewById(R.id.marker_extra)).setVisibility(View.VISIBLE);
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

            markerOptions.visible(details);


            mIconGenerator.setContentView(getDetailedMarkerView(item));
            if (markerStyle == MarkerStyle.WHITE) {
                mIconGenerator.setColor(Color.WHITE);
            } else if (markerStyle == MarkerStyle.COLOR) {
                mIconGenerator.setColor(item.color);
            }
            Bitmap icon = mIconGenerator.makeIcon();

            //V for detailedMarker is
            float anchorV = (((float) getColoredMarker(Color.RED).getHeight() / 2) / icon.getHeight()) + 1f;

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

        public DetailedMarker(LatLng position, String name, String time, int color, boolean endPoint) {
            this.position = position;
            this.name = name;
            this.time = time;
            this.color = color;
            this.endPoint = endPoint;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        boolean isSimpleMarker = false;
        for (Marker simpleMarker : simpleMarkers) {
            if (marker.getId().equals(simpleMarker.getId())) {
                isSimpleMarker = true;


            }
        }
        if (!isSimpleMarker) {
            //ok so this was a cluster marker, display the infoWindow of the corresponding simpleMarker
            for (Marker simpleMarker : simpleMarkers) {
                if (simpleMarker.getPosition().equals(marker.getPosition())) {
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

        LinearLayout bubbleLayout = new LinearLayout(context);

        for (DetailedMarker detailedMarker : detailedMarkers) {
            if (marker.getPosition().equals(detailedMarker.getPosition())) {
                View detailedMarkerView = getDetailedMarkerView(detailedMarker);
                bubbleLayout.addView(detailedMarkerView);
                BubbleDrawable bubbleDrawable = new BubbleDrawable(context.getResources());
                if (markerStyle == MarkerStyle.WHITE) {
                    bubbleDrawable.setColor(Color.WHITE);
                } else if (markerStyle == MarkerStyle.COLOR) {
                    bubbleDrawable.setColor(detailedMarker.color);
                }
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

    private Bitmap getColoredMarker(@ColorInt int colorInt) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_line_marker);
        Bitmap bitmapCopy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCopy);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
        return bitmapCopy;
    }

    private int getPixelsFromDp(float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float fpixels = metrics.density * dp;
        return (int) (fpixels + 0.5f);
    }

    Bitmap addGlow(Bitmap src) {
        // An added margin to the initial image

        int margin = getPixelsFromDp(6f);
        int halfMargin = margin / 2;

        // the glow radius
        int glowRadius = getPixelsFromDp(4f);
        ;

        // the glow color
        int glowColor = Color.rgb(0, 0, 255);

        // extract the alpha from the source image
        Bitmap alpha = src.extractAlpha();

        // The output bitmap (with the icon + glow)
        Bitmap bmp = Bitmap.createBitmap(src.getWidth() + margin,
                src.getHeight() + margin, Bitmap.Config.ARGB_8888);

        // The canvas to paint on the image
        Canvas canvas = new Canvas(bmp);

        Paint paint = new Paint();
        paint.setColor(glowColor);

        // outer glow
        paint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));
        canvas.drawBitmap(alpha, halfMargin, halfMargin, paint);

        // original icon
        canvas.drawBitmap(src, halfMargin, halfMargin, null);
        return bmp;
    }


    enum MarkerStyle {
        COLOR, WHITE
    }

    MarkerStyle markerStyle = MarkerStyle.COLOR;
    boolean details = true;

    private void extra() {
        CheckBox cb = (CheckBox) ((ViewOnMapActivity) context).findViewById(R.id.cb_toggle_detail_markers);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                details = isChecked;

                for (Marker marker : TripMarkerManager.this.getMarkerCollection().getMarkers()) {
                    marker.setVisible(details);
                }
                Log.e("Aksel", "checkbox=" + isChecked);
            }
        });
        RadioGroup rgViews = (RadioGroup) ((ViewOnMapActivity) context).findViewById(R.id.rg_styles);

        rgViews.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_color:
                        markerStyle = MarkerStyle.COLOR;
                        break;
                    case R.id.rb_white:
                        markerStyle = MarkerStyle.WHITE;
                        CameraPosition currentCameraPosition = mMap.getCameraPosition();
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition));
                        break;
                }
            }
        });
    }
}
