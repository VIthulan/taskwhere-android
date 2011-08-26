package com.taskwhere.android.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.taskwhere.android.adapter.TaskListAdapter;
import com.taskwhere.android.adapter.TaskListDbAdapter;
import com.taskwhere.android.model.Task;
import com.taskwhere.android.widget.ActionItem;
import com.taskwhere.android.widget.QuickAction;

/**
 * 
 * @author burak
 * @date 26 Aug 2011
 * main activity that show the list 
 * of saved tasks to the user
 */
public class TaskWhereActivity extends Activity {
    
	private final static String TW = "TaskWhere";
	private ArrayList<Task> taskList;
	private ListView taskListView;
	private static ActionBar actionBar;
	private TaskListDbAdapter dbAdapter;
	private Cursor taskCursor;
	private int mSelectedRow = 0;
	private final static String EDIT_TASK = "com.taskwhere.android.Task";
	
	/**
	 * setup actionbar pattern using {@link ActionBar}
	 * class and set {@link IntentAction} accordingly
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        
        taskList = new ArrayList<Task>();
        openDatabaseAccess();
        taskCursor = dbAdapter.getAllTasks();
        
        if(taskCursor.getCount() == 0){
        	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.tasklist_empty);
        	setContentView(R.layout.tasklist_empty);
        	
        }else{
        	
        	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main);
        	setContentView(R.layout.main);
        	
        	showSavedProfiles();
            taskListView = (ListView) findViewById(R.id.taskListView);
            taskListView.setAdapter(new TaskListAdapter(this, taskList));
            
        	ActionItem addAction = new ActionItem();
			addAction.setTitle("Edit");
			addAction.setIcon(getResources().getDrawable(R.drawable.ic_add));
	
			ActionItem accAction = new ActionItem();
			accAction.setTitle("Mark As Done");
			accAction.setIcon(getResources().getDrawable(R.drawable.ic_accept));
			
			ActionItem upAction = new ActionItem();
			upAction.setTitle("Upload");
			upAction.setIcon(getResources().getDrawable(R.drawable.ic_up));
			
			final QuickAction mQuickAction 	= new QuickAction(this);
			mQuickAction.addActionItem(addAction);
			mQuickAction.addActionItem(accAction);
			mQuickAction.addActionItem(upAction);

			
			/*
			 * click listener for quick action
			 * widget
			 */
			mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
				@Override
				public void onItemClick(int pos) {
					
					Intent callIntent;
					Task selectedTask = taskList.get(pos);
					
					if (pos == 0) {//edit item 
						
						callIntent = new Intent();
						callIntent.setClass(getApplicationContext(), AddTaskActivity.class);
						callIntent.putExtra(EDIT_TASK, selectedTask);
						startActivity(callIntent);
						//Toast.makeText(AddTaskActivity.this, "Add item selected on row " + mSelectedRow, Toast.LENGTH_SHORT).show();
					} else if (pos == 1) {

						Toast.makeText(TaskWhereActivity.this, "Accept item selected on row " + mSelectedRow, Toast.LENGTH_SHORT).show();
					} else if (pos == 2) {
						Toast.makeText(TaskWhereActivity.this, "Upload items selected on row " + mSelectedRow, Toast.LENGTH_SHORT).show();
					}	
				}
			});

			/*
			 * send clicked list item info to quick action
			 * widget
			 */
			taskListView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View view,
						int arg2, long arg3) {
					mSelectedRow = arg2;
					mQuickAction.show(view);
				}
			});
        }
        
        
        //set actionbar intents and activities accordingly
        actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setHomeAction(new IntentAction(this, createIntent(this),R.drawable.home));
        final Action infoAction = new IntentAction(this, InfoActivity.createIntent(this), R.drawable.info);
        actionBar.addAction(infoAction);
        final Action addAction = new IntentAction(this, AddTaskActivity.createIntent(this), R.drawable.add_item);
        actionBar.addAction(addAction);
    }
    
    /**
     * get profiles from {@link TaskListDbAdapter}
     * and store in list to show with listview
     */
    public void showSavedProfiles(){
    	
    	startManagingCursor(taskCursor);
    	Log.d(TW, "Cursor count : " + taskCursor.getCount());
    	
    	if(taskCursor.getCount() > 0){
    		taskCursor.moveToFirst();
    		do{
        		taskList.add(new Task(taskCursor.getString(1), taskCursor.getString(2),
        				taskCursor.getDouble(3), taskCursor.getDouble(4), taskCursor.getInt(5),taskCursor.getInt(6)));
    		}while(taskCursor.moveToNext());
    	}
    	
    	if(taskCursor != null)
    		taskCursor.close();
    }
    
    
    public void openDatabaseAccess(){
    	dbAdapter = new TaskListDbAdapter(getApplicationContext());
    	dbAdapter.open();
    }
    
    
    /**
     * do some database cleanup
     */
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	dbAdapter.close();
    }
    
    public static Intent createIntent(Context context) {
    	Intent i = new Intent(context, TaskWhereActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
}
