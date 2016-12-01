package com.taskwhere.android.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.taskwhere.android.model.Task;

public class TaskListDbAdapter {

	private final static String TW = "TaskWhere";
	private final Context context;
	private SQLiteDatabase db;
	private TaskListDbOpenHelper dbHelper;
	
	private static final String DATABASE_NAME = "tasklist.db";
	private static final String DATABASE_TABLE = "tasks";
	private static final int DATABASE_VERSION = 1;
	
	public static final String KEY_ID = "_id";
	public static final String TASK_TEXT = "task_text";
	public static final String TASK_LOC = "loc_name";
	public static final String TASK_LAT = "latitude";
	public static final String TASK_LON = "longitude";
	public static final String UNIQUEID = "uniqueid";
	public static final String STATUS = "status";
	public static final String PROX_RADIUS = "prox_radius";
	
	public TaskListDbAdapter(Context context) {
		this.context = context;
		dbHelper = new TaskListDbOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void close(){
		db.close();
	}
	
	/**
	 * @throws SQLiteException
	 * 
	 * Create/Open database for
	 * reading/writing
	 */
	public void open() throws SQLiteException{
		
		db = dbHelper.getWritableDatabase();
		db = dbHelper.getReadableDatabase();
	}
	
	/**
	 * 
	 * @param task
	 * @return KEY_ID
	 */
	public long insertNewTask(Task task){
		
		ContentValues taskValues = new ContentValues();
		
		taskValues.put(TASK_TEXT, task.getTaskText());
		taskValues.put(TASK_LOC, task.getTaskLoc());
		taskValues.put(TASK_LAT, task.getTaskLat());
		taskValues.put(TASK_LON, task.getTaskLon());
		taskValues.put(UNIQUEID, task.getUnique_taskid());
		taskValues.put(STATUS, task.getStatus());
		taskValues.put(PROX_RADIUS, task.getProx_radius());
		
		return db.insert(DATABASE_TABLE, null, taskValues);
	}
	
	/**
	 * Get all task items from table
	 * @return {@link Cursor}
	 */
	public Cursor getAllTasks() {
		return db.query(DATABASE_TABLE, new String [] { KEY_ID, TASK_TEXT , TASK_LOC,
				TASK_LAT, TASK_LON, UNIQUEID, STATUS, PROX_RADIUS }, null, null, null, null, null, null);
	}
	
	
	public boolean deleteTaskByUniqueId(int uniqueid){
		
		return db.delete(DATABASE_TABLE, UNIQUEID + "=" + uniqueid, null) > 0;
	}
	
	/**
	 * 
	 * @param locationName
	 * @return {@link Cursor}
	 */
	public Cursor getTaskByLocationName(String locationName){
		
		return db.query(DATABASE_TABLE, null, TASK_LOC + "=" + "\""+ locationName +"\"", null, null, null, null);
	}
	
	/**
	 * Get task by its unique proximity id
	 * @param unique_id
	 * @return
	 */
	public boolean updateTaskByUniqueId(Task task){
		
		String whereClause = UNIQUEID + "=" + task.getUnique_taskid();
		
		ContentValues cv = new ContentValues();
		cv.put(TASK_TEXT, task.getTaskText());
		cv.put(TASK_LOC, task.getTaskLoc());
		cv.put(TASK_LAT, task.getTaskLat());
		cv.put(TASK_LON, task.getTaskLon());
		cv.put(UNIQUEID, task.getUnique_taskid());
		cv.put(STATUS, task.getStatus());
		cv.put(PROX_RADIUS, task.getProx_radius());
		
		return db.update(DATABASE_TABLE, cv, whereClause, null) > 0;
	}
	
	/**
	 * 
	 * @author burak
	 * @date 26 Aug 2011
	 * 
	 * Databaes create and upgrade operations
	 */
	private static class TaskListDbOpenHelper extends SQLiteOpenHelper{

		public TaskListDbOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " (" + KEY_ID + " integer primary key autoincrement, " +
			TASK_TEXT + " text not null ," + TASK_LOC + " text ,"+ TASK_LAT + " real ," + TASK_LON + " real ," + UNIQUEID + " integer ," + PROX_RADIUS
			+ " integer ," + STATUS + " integer );";

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TW, "Upgrading from version "+ oldVersion + " to "+ newVersion + " ,which will destroy all data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

}
