package com.markupartist.sthlmtraveling;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class FavoriteButtonHelper implements OnClickListener {
    private ImageButton mFavoriteButton;
    private Cursor mFavoriteCursor;
    private Activity mActivity;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private String mStartPoint;
    private String mEndPoint;
    private static int STAR_ON_RESOURCE = android.R.drawable.star_big_on;
    private static int STAR_OFF_RESOURCE = android.R.drawable.star_big_off;

    public FavoriteButtonHelper(Activity activity, 
                                FavoritesDbAdapter favoritesDbAdapter, 
                                String startPoint, 
                                String endPoint) {
        mActivity = activity;
        mFavoritesDbAdapter = favoritesDbAdapter;
        mStartPoint = startPoint;
        mEndPoint = endPoint;

        mFavoriteButton = (ImageButton) mActivity.findViewById(R.id.route_favorite);
        mFavoriteButton.setOnClickListener(this);
    }

    public FavoriteButtonHelper loadImage() {
        mFavoriteButton.setImageResource(getImageResource());

        return this;
    }
    
    private boolean isFavorite() {
        mFavoriteCursor = mFavoritesDbAdapter.fetch(mStartPoint, mEndPoint);
        mActivity.startManagingCursor(mFavoriteCursor);

        return mFavoriteCursor.getCount() > 0;
    }

    private int getImageResource() {
        if (isFavorite()) {
            return STAR_ON_RESOURCE;
        }
        return STAR_OFF_RESOURCE;
    }

    @Override
    public void onClick(View view) {
        if (isFavorite()) {
            long id = mFavoriteCursor.getLong(
                    mFavoriteCursor.getColumnIndex(FavoritesDbAdapter.KEY_ROWID));
            mFavoritesDbAdapter.delete(id);
        } else {
            mFavoritesDbAdapter.create(mStartPoint, mEndPoint);
        }

        mFavoriteButton.setImageResource(getImageResource());
    }
}
