package com.taskwhere.android.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.taskwhere.android.adapter.TaskListDbAdapter;
import com.taskwhere.android.model.Task;

public class AddTaskActivity extends MapActivity{

	
	private final static String TW = "TaskWhere";
	private MapView locMapView;
	private MapController mapController;
	private MyLocationOverlay me=null;
	private GeoPoint point;
	private Drawable marker;
	private LocationManager locationManager;
	private ProgressDialog loadingDialog;
	private final static String SEARCH_REDIRECT = "com.taskwhere.android.activity.SEARCH_REDIRECT";
	private final static String SEARCH_ADDRESS = "com.taskwhere.android.activity.SEARCH_ADDRESS";
	private final static String EDIT_TASK = "com.taskwhere.android.Task";
	private static final String ARRIVED_ACTION = "com.taskwhere.android.ARRIVED_ACTION";
	private static final String ACTIVE_TASK_LOC = "com.taskwhere.android.model.TaskLoc";
	private static final String ACTIVE_TASK_TEXT = "com.taskwhere.android.model.TaskText";
	private SharedPreferences preferences;
	private TaskListDbAdapter adapter;
	public static boolean isEditing = false;
	
	private static int unique_id;
	private Button saveButton;
	private EditText taskLoc;
	private EditText taskText;
	private SeekBar radiusBar;
	private TextView radiusValue;
	private Task editTask;
	
	boolean gpsEnabled;
	boolean wirelessEnabled;
	Location location;
	
	/**
	 * setup actionbar pattern using {@link ActionBar}
	 * class and set {@link IntentAction} accordingly
	 * 
	 * try to detect current location of user on start
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_task);
		
		
		locMapView = (MapView) findViewById(R.id.locMapView);
		locMapView.setBuiltInZoomControls(true);
		mapController = locMapView.getController();
		
		locMapView.setStreetView(true);
		mapController.setZoom(15);
		
		marker = getResources().getDrawable(R.drawable.marker);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(),marker.getIntrinsicHeight());
		

		saveButton = (Button) findViewById(R.id.saveButton);
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setHomeAction(new IntentAction(this, TaskWhereActivity.createIntent(this),R.drawable.home));
		
		final Action infoAction = new IntentAction(this, SearchAddressActivity.createIntent(this), R.drawable.search_magnifier);
        actionBar.addAction(infoAction);
        
        actionBar.addAction(new CheckLocation());
        
        taskLoc = (EditText) findViewById(R.id.taskLocEdit);
        taskText = (EditText) findViewById(R.id.taskDetailEdit);
        radiusValue = (TextView) findViewById(R.id.radiusValue);
        radiusBar = (SeekBar) findViewById(R.id.radiusBar);
        radiusBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				radiusValue.setText(progress+ 100 + " meter");
			}
		});
			
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		unique_id = preferences.getInt("UNIQUEID", 0);
		Log.d(TW, "Preferences Unique_id = " + unique_id);
		
		Bundle extras = getIntent().getExtras();

	    if(extras != null){
	        	
	    	if(extras.getBoolean(SEARCH_REDIRECT) && extras.getString(SEARCH_ADDRESS) != null){
	        		
	    		Log.d(TW, "Redirected from search activity just animate to point");
	            String address = extras.getString(SEARCH_ADDRESS);
	            Log.d(TW, "Search address : " + address);
	        	
	            Geocoder gc = new Geocoder(getApplicationContext());
	            try {
	    			List<Address> results = gc.getFromLocationName(address, 1);
	    			if(results != null){
	    				if(results.size() >0){
	    					showDialog(1);
	    					Address x = results.get(0);
	    					location = new Location(LocationManager.NETWORK_PROVIDER);
	    					location.setLatitude(x.getLatitude());
	    					location.setLongitude(x.getLongitude());
	    					updateWithNewLocation(location, marker);
	    				}
	    			}else{ // no result
	    				Toast.makeText(getApplicationContext(), "Can not find the given address", Toast.LENGTH_LONG).show();
	    			}
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	
	    	if(extras.getSerializable(EDIT_TASK) != null){

	        	editTask = (Task) extras.getSerializable(EDIT_TASK);
	        	Log.d(TW, editTask.toString());
	        	isEditing = true;
	        	saveButton.setText("Update Task");
	        	taskLoc.setText(editTask.getTaskLoc());
	        	taskText.setText(editTask.getTaskText());
	        	radiusBar.setProgress(editTask.getProx_radius());
	        	radiusValue.setText(editTask.getProx_radius() + " meter");
	        	location = new Location(LocationManager.NETWORK_PROVIDER);
				location.setLatitude(editTask.getTaskLat());
				location.setLongitude(editTask.getTaskLon());
				updateWithNewLocation(location, marker);
	    	}
	    }else{
	    	//its new task
	        isEditing = false;
	        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	        if(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()){
	        		//network enabled go on to find current location
	        		showDialog(1);
	    	    	getCurrentLocation();		
	        }else{
	        	Toast.makeText(getApplicationContext(), "Seems like your network connectivity does not exist. Try again later...", Toast.LENGTH_LONG).show();
	        }
	    }
	    
	    saveButton.setOnClickListener(new OnClickListener() {
	    	
			@Override
			public void onClick(View v) {
				
				
				if(!isEditing){ //seems like its a new task insertion
					
					Task newTask = new Task(taskText.getText().toString(), taskLoc.getText().toString()
							, location.getLatitude(), location.getLongitude(),0, radiusBar.getProgress()+100);
					Log.d(TW, "Inserting Task : \n" + newTask.toString());
					
					newTask.setUnique_taskid(registerNewTaskProximityAlert(newTask));
					adapter = new TaskListDbAdapter(getApplicationContext());
					adapter.open();
					adapter.insertNewTask(newTask);
					
				}else{ //nope its a edit task operation
					
					Log.d(TW,"Editing Task :\n" + editTask.toString());
					//update with new settings
					editTask.setTaskText(taskText.getText().toString());
					editTask.setTaskLoc(taskLoc.getText().toString());
					editTask.setTaskLat(location.getLatitude());
					editTask.setTaskLon(location.getLongitude());
					editTask.setProx_radius(radiusBar.getProgress());
					
					//update task on db
					adapter = new TaskListDbAdapter(getApplicationContext());
					adapter.open();
					Log.d(TW, "Number of rows effected : " + adapter.updateTaskByUniqueId(editTask));
					
					//remove old proximity and register new one if location changed
					if(location.getLatitude() != editTask.getTaskLat() || location.getLongitude() != editTask.getTaskLon()){
						removeOldProximityAlert(editTask.getUnique_taskid());
						editTask.setTaskLat(location.getLatitude());
						editTask.setTaskLon(location.getLongitude());
						editTask.setUnique_taskid(registerNewTaskProximityAlert(editTask));
					}
				}
				
				Intent listIntent = new Intent();
				listIntent.setClass(getApplicationContext(), TaskWhereActivity.class);
				startActivity(listIntent);
			}

			/*
			 * register new proximity alert according to
			 * task cridentials
			 */
			private int registerNewTaskProximityAlert(Task newTask) {
				
				String contenxt = Context.LOCATION_SERVICE;
				locationManager = (LocationManager) getSystemService(contenxt);
				
				Intent anIntent = new Intent(ARRIVED_ACTION);
				anIntent.putExtra(ACTIVE_TASK_LOC, newTask.getTaskLoc());
				anIntent.putExtra(ACTIVE_TASK_TEXT, newTask.getTaskText());

				PendingIntent operation = PendingIntent.getBroadcast(getApplicationContext(), ++unique_id , anIntent, 0);
				locationManager.addProximityAlert(newTask.getTaskLat(), newTask.getTaskLon(), newTask.getProx_radius() , -1, operation);
				Log.d(TW, "Unique id of the saved profile is : " + unique_id);
				
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("UNIQUEID",unique_id);
				editor.commit();
				Log.d(TW, "Latest saved Unique_id = " + unique_id);
				return unique_id;
			}
		});
		
		me=new MyLocationOverlay(this, locMapView);
		locMapView.getOverlays().add(me);
	}

	protected void removeOldProximityAlert(int unique_taskid) {
    	
    	String context = Context.LOCATION_SERVICE;
    	LocationManager locationManager = (LocationManager) getSystemService(context);

    	Intent anIntent = new Intent(ARRIVED_ACTION);
		PendingIntent operation = 
				PendingIntent.getBroadcast(getApplicationContext(), unique_taskid , anIntent, 0);
		locationManager.removeProximityAlert(operation);
	}
	
	@Override
	protected void onDestroy() {

		super.onDestroy();
		if(adapter!=null)
			adapter.close();
	}

	/**
	 * check providers enabled or not
	 * if enabled both get location listeners from both
	 * after 10 second with no update check lastknownlocations
	 */
	public void getCurrentLocation(){
		
		String contenxt = Context.LOCATION_SERVICE;
		locationManager = (LocationManager) getSystemService(contenxt);
		
		gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		wirelessEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		
		if(!gpsEnabled && !wirelessEnabled){
			
			Toast disableToast = Toast.makeText(getApplicationContext(), "It seems both your GPS and WIFI are disabled", Toast.LENGTH_LONG);
			disableToast.show();
		}
		
		if (gpsEnabled) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
		}
		
		if(wirelessEnabled){
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networklocationListener);
		}
		
		Timer timerLoc = new Timer();
		timerLoc.schedule(new LocationTaks(), 8000);
	}
	
    public static Intent createIntent(Context context) {
		
		Intent i = new Intent(context, AddTaskActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
	}
    
    /**
     * dialog shows up while trying to get
     * current known location
     */
    @Override
    protected Dialog onCreateDialog(int id) {
    	
		loadingDialog = ProgressDialog.show(AddTaskActivity.this, "", "Getting your current location.Please wait...",true);
    	loadingDialog.setCancelable(true);
    	
		return loadingDialog;
    }
    
    /**
     * 
     * @author burak
     * Simple {@link TimerTask} class
     * just getLastKnownLocations and return the most
     * up to date one
     * 
     * also clean former location listeners to not drain
     * battery of device
     */
    class LocationTaks extends TimerTask{
		
		@Override
		public void run() {

			Location gpsLoc = null,networkLoc = null;
			locationManager.removeUpdates(gpsLocationListener);
			locationManager.removeUpdates(networklocationListener);
			
			if(gpsEnabled){
				gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				Log.d(TW, "Last known gps location is : " + gpsLoc);
			}
			
			if(wirelessEnabled){
				networkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				Log.d(TW, "Last known network location is : " + networkLoc);
			}
			
			if(gpsLoc != null && networkLoc != null){
				
				if(gpsLoc.getTime() > networkLoc.getTime())
					location = gpsLoc;
				else
					location = networkLoc;
			}
			
			if(gpsLoc != null)
				location = gpsLoc;
			
			if(networkLoc != null)
				location = networkLoc;
			
			updateWithNewLocation(location,marker);
		}
	}
    
    /**
     * 
     * @param location
     * @param marker
     * 
     * Update on map with given location object
     * and marker
     */
    private void updateWithNewLocation(Location location,Drawable marker){
		
		if(location != null){
		
			if(loadingDialog != null)
				loadingDialog.dismiss();
			
			locMapView.getOverlays().clear();
			Double lat = location.getLatitude() * 1E6;
			Double lon = location.getLongitude()* 1E6;
			
			Log.d(TW,"New updated location : " + "Latitude => " + lat.intValue() +" | Longitude => "+ lon.intValue());
			point = new GeoPoint(lat.intValue(), lon.intValue());
			mapController.animateTo(point);
			locMapView.getOverlays().add(new SitesOverlay(marker,location));

		}
	}

    /**
     * {@link LocationListener} for GPS provider
     * to listen updates
     */
	private final LocationListener gpsLocationListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
		@Override
		public void onProviderEnabled(String provider) {}
		
		@Override
		public void onProviderDisabled(String provider) {}
		
		@Override
		public void onLocationChanged(Location location) {
			Log.d(TW, "There are some updates from GSP listener");
			updateWithNewLocation(location,marker);
		}
	};

	/**
     * {@link LocationListener} for NETWORK provider
     * to listen updates
     */
	private final LocationListener networklocationListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
		@Override
		public void onProviderEnabled(String provider) {}
		
		@Override
		public void onProviderDisabled(String provider) {}
		
		@Override
		public void onLocationChanged(Location location) {
			Log.d(TW, "There are some updates from Network listener");
			updateWithNewLocation(location,marker);
		}
	};
    
	private GeoPoint getPoint(double lat, double lon) {
		return(new GeoPoint((int)(lat*1000000.0),(int)(lon*1000000.0)));
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/**
	 * 
	 * @author burak
	 * 
	 * Overlay class to show marker overlay and implement
	 * actions like drag & drop, get marker dropped location etc.
	 */
	private class SitesOverlay extends ItemizedOverlay<OverlayItem> {
		
		public List<OverlayItem> items=new ArrayList<OverlayItem>();
		private Drawable marker=null;
		private OverlayItem inDrag=null;
		private ImageView dragImage=null;
		private int xDragImageOffset=0;
		private int yDragImageOffset=0;
		private int xDragTouchOffset=0;
		private int yDragTouchOffset=0;

		/**
		 * 
		 * @param marker
		 * @param location
		 * 
		 * Add marker to the center of the map 
		 * with getInstricWidth/Height and populate
		 */
		public SitesOverlay(Drawable marker,Location location) {
			
			super(marker);
			this.marker=marker;
			dragImage=(ImageView)findViewById(R.id.drag);
			xDragImageOffset=dragImage.getDrawable().getIntrinsicWidth()/2;
			yDragImageOffset=dragImage.getDrawable().getIntrinsicHeight();
			items.add(new OverlayItem(getPoint(location.getLatitude(),location.getLongitude()), "Current Location", "You are here"));
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return(items.get(i));
		}

		@Override
		public void draw(Canvas canvas, MapView mapView,boolean shadow) {
			if(!shadow){
				super.draw(canvas, mapView, shadow);
			}
			boundCenterBottom(marker);
		}
 		
		@Override
		public int size() {
			return(items.size());
		}

		/**
		 * implementation of the some {@link MotionEvent}
		 * like ACTION_DOWN, ACTION_UP, ACTION_MOVE
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			
			final int action=event.getAction();
			final int x=(int)event.getX();
			final int y=(int)event.getY();
			boolean result=false;

			if (action == MotionEvent.ACTION_DOWN) {
				
				mapView.getParent().requestDisallowInterceptTouchEvent(true);
				for (OverlayItem item : items) {
					Point p=new Point(0,0);

					locMapView.getProjection().toPixels(item.getPoint(), p);

					if (hitTest(item, marker, x-p.x, y-p.y)) {
						result=true;
						inDrag=item;
						items.remove(inDrag);
						populate();

						xDragTouchOffset=0;
						yDragTouchOffset=0;

						setDragImagePosition(p.x, p.y);
						dragImage.setVisibility(View.VISIBLE);

						xDragTouchOffset=x-p.x;
						yDragTouchOffset=y-p.y;

						break;
					}
				}
			}
			else if (action == MotionEvent.ACTION_MOVE && inDrag!=null) {
				mapView.getParent().requestDisallowInterceptTouchEvent(true);
				setDragImagePosition(x, y);
				result=true;
			}
			else if (action==MotionEvent.ACTION_UP && inDrag!=null) {
				
				mapView.getParent().requestDisallowInterceptTouchEvent(true);
				dragImage.setVisibility(View.GONE);

				GeoPoint pt=locMapView.getProjection().fromPixels(x-xDragTouchOffset,y-yDragTouchOffset);
				Log.d(TW, "Dragged Location is : " +" Lattitude => "+ pt.getLatitudeE6() +" | Longitude =>" + pt.getLongitudeE6());
				
				OverlayItem toDrop=new OverlayItem(pt, inDrag.getTitle(),inDrag.getSnippet());
				point = pt;
				mapController.animateTo(pt);
				items.clear();
				items.add(toDrop);
				populate();

				inDrag=null;
				result=true;
			}
			return(result || super.onTouchEvent(event, mapView));
		}

		private void setDragImagePosition(int x, int y) {
			LinearLayout.LayoutParams lp=
				(LinearLayout.LayoutParams)dragImage.getLayoutParams();

			lp.setMargins(x-xDragImageOffset-xDragTouchOffset,y-yDragImageOffset-yDragTouchOffset, 0, 0);
			dragImage.setLayoutParams(lp);
		}
	}
	
	/**
	 * @author burak
	 * 
	 * {@link Action} class provide custom
	 * action on {@link ActionBar}
	 */
	private class CheckLocation implements Action {

	    @Override
	    public int getDrawable() {
	        return R.drawable.location;
	    }

	    @Override
	    public void performAction(View view) {
	    	showDialog(1);
	        getCurrentLocation();
	    }
	}
}
