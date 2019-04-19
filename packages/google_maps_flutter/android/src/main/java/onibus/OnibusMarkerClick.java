package onibus;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class OnibusMarkerClick implements GoogleMap.OnMarkerClickListener {

    private final Handler mHandler;
    private Runnable mAnimation;
    private Runnable mTimeUpdate;

    public OnibusMarkerClick() {
        mHandler = new Handler();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        // This causes the marker at Perth to bounce into position when it is clicked.
        final long start = SystemClock.uptimeMillis();
        final long duration = 900L;

        // Cancels the previous animation
        mHandler.removeCallbacks(mAnimation);
        mHandler.removeCallbacks(mTimeUpdate);

        // Starts the bounce animation
        mAnimation = new OpenAnimation(start, duration, marker, mHandler);
        mTimeUpdate = new TimeUpdate(marker, mHandler);
        mTimeUpdate.run();

        mHandler.postDelayed(mTimeUpdate, 1000L);
        mHandler.post(mAnimation);

        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    /**
     * Performs a bounce animation on a {@link Marker}.
     */
    private static class TimeUpdate implements Runnable {

        private final Marker mMarker;
        private final Handler mHandler;
        private long dateForLastExecution;

        private TimeUpdate(Marker marker, Handler handler) {
            mMarker = marker;
            mHandler = handler;
            dateForLastExecution = 0;
        }


        @Override
        public void run() {
            try {
                JSONObject json = new JSONObject(mMarker.getSnippet());
                long date = json.getLong("D");
                if(date != dateForLastExecution) {
                    dateForLastExecution = date;
                    long dateDiff = Calendar.getInstance().getTimeInMillis() - date;
                    json.put("dataIncremental", dateDiff);
                } else {
                    json.put("dataIncremental", json.getLong("dataIncremental") + 1000);
                }

                mMarker.setSnippet(json.toString());

                if (mMarker.isInfoWindowShown()) {
                    mMarker.showInfoWindow();
                    mHandler.postDelayed(this, 1000L);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Performs a bounce animation on a {@link Marker}.
     */
    private static class OpenAnimation implements Runnable {

        private final long mStart, mDuration;
        private final Interpolator mInterpolator;
        private final Marker mMarker;
        private final Handler mHandler;

        private OpenAnimation(long start, long duration, Marker marker, Handler handler) {
            mStart = start;
            mDuration = duration;
            mMarker = marker;
            mHandler = handler;
            mInterpolator = new AccelerateDecelerateInterpolator();
        }

        @Override
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - mStart;
            float t = Math.max(1 - mInterpolator.getInterpolation((float) elapsed / mDuration), 0f);
            mMarker.setInfoWindowAnchor(0.5f, 1.0f + 1.2f * t);

            if (elapsed < mDuration) {
                // Post again 16ms later.
                mHandler.postDelayed(this, 16L);
            }
        }
    }
}