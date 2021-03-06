package com.github.tommywalsh.map;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MapController;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;

import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.location.Criteria;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.content.Context;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Paint;


// This activity handles the Map application.
public class BikeMapActivity extends MapActivity implements LocationListener
{

    Location m_oldLoc;
    public void onLocationChanged(Location loc) {
	// Always make sure our current position is centered on the screen
	if (loc != null && !loc.equals(m_oldLoc)) {
	    m_oldLoc = loc;
	    if (m_mode == LOCK) {
		m_location = new GeoPoint((int)(loc.getLatitude() * 1E6),
					  (int)(loc.getLongitude() * 1E6));
		m_controller.animateTo(m_location);
	    }
	}
    }
    public void onProviderDisabled(String provider) {
    }
    public void onProviderEnabled(String provider) {
    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    // Draw a little circle at our current position
    public class LocationIndicator extends Overlay {
	
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
	    super.draw(canvas, mapView, shadow, when);
	    
	    if (m_location != null) {
		Point screenCoords = new Point();

		Paint paint = new Paint();		
		mapView.getProjection().toPixels(m_location, screenCoords);
		paint.setStrokeWidth(3);
		if (shadow) {
		    paint.setARGB(255,0,0,255);
		    paint.setStyle(Paint.Style.FILL_AND_STROKE);
		} else {		    
		    paint.setARGB(255,0,255,0);
		    paint.setStyle(Paint.Style.STROKE);
		}
		canvas.drawCircle(screenCoords.x, screenCoords.y, 10, paint);

	    }
	    return true;
	}
    }

    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	
	m_locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	
	MapView mapview = (MapView) findViewById(R.id.mapview);
	mapview.setBuiltInZoomControls(false);
	mapview.getOverlays().add(new LocationIndicator());
	mapview.getOverlays().add(m_route);
	
	m_controller = mapview.getController();

	m_controller.setZoom(16); // 16 seems best for biking speeds
	m_controller.setCenter(new GeoPoint(42378778, -71095667)); // Union Square

	updateTitle();
    }

    @Override protected boolean isRouteDisplayed() {
	return false;
    }

    @Override protected void onStart() {
	super.onStart();
	m_locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 50, this);
    }

    @Override protected void onStop() {
	// Conserve battery by not asking for location updates when we're not visible
	super.onStop();
	m_locManager.removeUpdates(this);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
	m_kmlMenu = new KMLSubmenu(menu);
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main_menu, menu);
	return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
	int id = item.getItemId();
	switch (id) {
	case R.id.quit:
	    this.finish();
	    return true;
	default:
	    boolean retval = m_kmlMenu.processSelection(id, m_route);
	    if (!retval) {
		retval = super.onOptionsItemSelected(item);	       
	    }
	    return retval;
	}
    }

    private void updateTitle()
    {
	String title = getString(R.string.app_name) + ": ";
	switch (m_mode) {
	case LOCK:
	    title += getString(R.string.position_lock);
	    break;
	case ZOOM:
	    title += getString(R.string.zoom);
	    break;
	case PAN:
	    title += getString(R.string.pan);
	    break;
	}
	setTitle(title);
    }

    // The map has three modes:
    // 1) Locked to your current position
    // 2) Manual scroll/pan
    // 3) Manual zoom in/out
    private int m_mode = LOCK;
    public static final int LOCK = 0;
    public static final int ZOOM = 1;
    public static final int PAN = 2;
    private boolean m_moved = false;
    @Override public boolean onTrackballEvent(MotionEvent event) {
	switch (event.getAction()) {
	case MotionEvent.ACTION_DOWN:
	    m_moved = false;
	    break;
	case MotionEvent.ACTION_UP:
	    if (!m_moved) {
		m_mode++; if(m_mode > PAN) m_mode = LOCK;
	    }
	    break;
	case MotionEvent.ACTION_MOVE:
	    m_moved = true;
	    if (m_mode == ZOOM) {
		if (event.getY() > 0.0) {
		    m_controller.zoomOut();
		} else {
		    m_controller.zoomIn();
		}
	    } else if (m_mode == PAN) {
		int x = (int)(event.getX() * 30);
		int y = (int)(event.getY() * 30);
		m_controller.scrollBy(x, y);
	    }
	    break;
	}
	updateTitle();
	return true;
    }
    
    KMLSubmenu m_kmlMenu;
    MapController m_controller;
    LocationManager m_locManager;
    GeoPoint m_location;
    RouteOverlay m_route = new RouteOverlay();

}


