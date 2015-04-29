package com.nickivy.ucscdining;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.example.android.common.view.SlidingTabLayout;
import com.nickivy.ucscdining.parser.MealStorage;
import com.nickivy.ucscdining.parser.MenuParser;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment for displaying one menu. Uses sliding tab
 * layout from Google's examples.
 *
 * @author Nick Ivy parkedraccoon@gmail.com
 */

@SuppressWarnings("ResourceType")
public class MealViewFragment extends ListFragment{
	
	final static String ARG_COLLEGE_NUMBER = "college_number";
	
	public static final int LISTVIEW_ID1 = 12,
			LISTVIEW_ID2 = 13,
			LISTVIEW_ID3 = 14,
			SWIPEREF_ID1 = 15,
			SWIPEREF_ID2 = 16,
			SWIPEREF_ID3 = 17;

	
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private ViewPager mViewPager;
	private SlidingTabLayout mSlidingTabLayout;
	private ListView mDrawerList;
	private DrawerLayout mDrawerLayout;
    private ListView mMealList;
	
	private static int collegeNum = 0;
	
	public static int displayedMonth = 0;
	public static int displayedDay = 0;
	
	private boolean refreshStarted = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		collegeNum = getArguments().getInt(ARG_COLLEGE_NUMBER);
        getActivity().setTitle(MenuParser.collegeList[collegeNum]);
    	
		return inflater.inflate(R.layout.pager_fragment, container, false);
	}
	
	public void onViewCreated(View view, Bundle savedInstanceState){
		mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MenuAdapter());
		mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
		mSlidingTabLayout.setViewPager(mViewPager);
	}
	
    public void selectItem(int position) {
    	collegeNum = position;
        // update the main content by replacing listview adapters
    	if(MenuParser.fullMenuObj.get(position).getIsOpen()){
    		if(MenuParser.fullMenuObj.get(position).getIsSet()){    			
    			// Set title to include date
    	        getActivity().setTitle(MenuParser.collegeList[position] + " " + displayedMonth + "/" + displayedDay);

    	        mMealList = (ListView) getActivity().findViewById(MealViewFragment.LISTVIEW_ID1);    		
        		ArrayList<String> testedArray = new ArrayList<String>();
        		testedArray = MenuParser.fullMenuObj.get(position).getBreakfast();
        		if (testedArray != null && mMealList != null){
        			mMealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        					MenuParser.fullMenuObj.get(position).getBreakfast()));
        		}
        		mMealList = (ListView) getActivity().findViewById(MealViewFragment.LISTVIEW_ID2);
        		testedArray = MenuParser.fullMenuObj.get(position).getLunch();
        		if (testedArray != null && mMealList != null){
        			mMealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        					MenuParser.fullMenuObj.get(position).getLunch()));
        		}
        		mMealList = (ListView) getActivity().findViewById(MealViewFragment.LISTVIEW_ID3);
        		testedArray = MenuParser.fullMenuObj.get(position).getDinner();
        		if (testedArray != null && mMealList != null){
        			mMealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        				MenuParser.fullMenuObj.get(position).getDinner()));
        		}
    		}

			mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
	        mDrawerList = (ListView) getActivity().findViewById(R.id.left_drawer);
	        
    		// update selected item and title, then close the drawer
    		mDrawerList.setItemChecked(position, true);
    		mDrawerLayout.closeDrawer(mDrawerList);
    	}else{
    		Toast.makeText(getActivity(), MenuParser.collegeList[position] + " dining hall closed today!", Toast.LENGTH_SHORT).show();
    	}
    }
	
	@SuppressWarnings("ResourceType")
    private class RetrieveMenuTask extends AsyncTask<Integer, Integer, Long>{
		
//		private int college;
		
		@Override
		protected void onPreExecute(){
			refreshStarted = true;
		}

		@Override
		protected Long doInBackground(Integer... arg0) {
			MealStorage mealStore = new MealStorage(getActivity());
			SQLiteDatabase db;
			
			// Date info is passed in as argument, to allow user to select a date that is not today
			int month = arg0[0];
			int day = arg0[1];
			int year = arg0[2];
			
			// Keep track of latest date called for displaying in title bar
			displayedMonth = month;
			displayedDay = day;
			
			db = mealStore.getReadableDatabase();
			
			String selection = MealStorage.COLUMN_MONTH + "= ? AND " + MealStorage.COLUMN_DAY + "= ? AND "
					+ MealStorage.COLUMN_YEAR + "= ?";
			String[] selectionArgs = new String[3];
			
			selectionArgs[0] = "" + month;
			selectionArgs[1] = "" + day;
			selectionArgs[2] = "" + year;
			
			String[] projection = {
				    MealStorage.COLUMN_MONTH,
				    MealStorage.COLUMN_DAY,
				    MealStorage.COLUMN_YEAR
			};
			
			Cursor c = db.query(MealStorage.TABLE_MEALS, 
					projection, selection, selectionArgs, null, null, null);
			c.moveToFirst();
			boolean cexists = (c.getCount() == 0);
			c.close();
			db.close();
			// If data for today does not exist, or manual refresh is requested, load data from web and store it
			if(cexists || MenuParser.needsRefresh){
				MenuParser.getMealList(month, day, year);
				
				db = mealStore.getWritableDatabase();
				
				/*
				 * Delete data from today and before, requires a couple different sql commands
				 * Note: today, not date the retrieve menu task was called with
				 * 
				 * if:
				 * month is less than, year equal
				 * year less than current year
				 * day < current, year + month equal
				 * if manual refresh (MenuParser.needsRefresh true), delete all (assume database got messed up somehow)
				 */
				
				int today[] = getToday();
				
				if (MenuParser.needsRefresh) {
					db.delete(MealStorage.TABLE_MEALS, null,null);
				} else {				
					db.delete(MealStorage.TABLE_MEALS, MealStorage.COLUMN_MONTH + "<? AND " + MealStorage.COLUMN_YEAR + " =?",
							new String[] {"" + today[0], "" + today[2]});
				
					db.delete(MealStorage.TABLE_MEALS, MealStorage.COLUMN_YEAR + " <?", new String[] {"" + today[2]});
				
					db.delete(MealStorage.TABLE_MEALS, MealStorage.COLUMN_MONTH + "=? AND " + MealStorage.COLUMN_DAY + "<? AND " +
							MealStorage.COLUMN_YEAR + " =?", new String[] {"" + today[0], "" + today[1], "" + today[2]});
				}
				
				// We need to write at the end of the table - so find size and offset first column by that much				
				String countQuery = "SELECT * FROM " + MealStorage.TABLE_MEALS;
				Cursor cursor = db.rawQuery(countQuery, null);
				int offset = cursor.getCount();
				cursor.close();
				
				// Begin writing data
				
				SQLiteStatement statement = db.compileStatement("INSERT INTO "+ MealStorage.TABLE_MEALS +" VALUES (?,?,?,?,?,?,?);");
				
				// Using sqlite statement keeps the database 'open' and apparently is a bit faster
				db.beginTransaction();
				
				// Accumulate amount of nodes written in
				int accumulatedBreakfast = 0,
						accumulatedLunch = 0,
						accumulatedDinner = 0;
				
				for(int j = 0; j < 5; j++){
			
					statement.clearBindings();
					for (int i = 0; i < MenuParser.fullMenuObj.get(j).getBreakfast().size(); i++) {
						statement.bindLong(1, offset + i + accumulatedBreakfast + accumulatedLunch + accumulatedDinner);
						statement.bindLong(2,j);
						statement.bindLong(3, 0);
						statement.bindString(4, MenuParser.fullMenuObj.get(j).getBreakfast().get(i));
						statement.bindLong(5, month);
						statement.bindLong(6, day);
						statement.bindLong(7, year);
						statement.execute();
					}
					accumulatedBreakfast += MenuParser.fullMenuObj.get(j).getBreakfast().size();
					for (int i = 0; i < MenuParser.fullMenuObj.get(j).getLunch().size(); i++) {
						statement.bindLong(1, offset + i + accumulatedBreakfast + accumulatedLunch + accumulatedDinner);
						statement.bindLong(2,j);
						statement.bindLong(3, 1);
						statement.bindString(4, MenuParser.fullMenuObj.get(j).getLunch().get(i));
						statement.bindLong(5, month);
						statement.bindLong(6, day);
						statement.bindLong(7, year);
						statement.execute();
					}
					accumulatedLunch += MenuParser.fullMenuObj.get(j).getLunch().size();
					for (int i = 0; i < MenuParser.fullMenuObj.get(j).getDinner().size(); i++) {
						statement.bindLong(1, offset + i + accumulatedBreakfast + accumulatedLunch + accumulatedDinner);
						statement.bindLong(2,j);
						statement.bindLong(3, 2);
						statement.bindString(4, MenuParser.fullMenuObj.get(j).getDinner().get(i));
						statement.bindLong(5, month);
						statement.bindLong(6, day);
						statement.bindLong(7, year);
						statement.execute();
					}
					accumulatedDinner += MenuParser.fullMenuObj.get(j).getDinner().size();
				}
				db.setTransactionSuccessful();
				db.endTransaction();
				db.close();
				
			} else {
				// I closed this up above, not sure why I have to do it again here
				db.close();
				db = mealStore.getReadableDatabase();
			
				String[] mainProjection = {
				    MealStorage.COLUMN_MENUITEM,
				    MealStorage.COLUMN_COLLEGE,
				    MealStorage.COLUMN_MONTH,
				    MealStorage.COLUMN_DAY,
				    MealStorage.COLUMN_YEAR
			    };
				
				ArrayList<String> breakfastLoaded,
				lunchLoaded, dinnerLoaded;
				
				selection = MealStorage.COLUMN_COLLEGE + "= ? AND " + MealStorage.COLUMN_MEAL + "= ? AND "
						+ MealStorage.COLUMN_MONTH + "= ? AND " + MealStorage.COLUMN_DAY + "= ? AND "
						+ MealStorage.COLUMN_YEAR + "= ?";
				String[] mainSelectionArgs = new String[5];
			
				// For each of the 5 colleges, load data into the full menu object
				for(int j = 0; j < 5; j++){
					mainSelectionArgs[0] = "" + j;
					
					mainSelectionArgs[1] = "" + 0;
					
					mainSelectionArgs[2] = "" + month;
					mainSelectionArgs[3] = "" + day;
					mainSelectionArgs[4] = "" + year;
			
					c = db.query(MealStorage.TABLE_MEALS, 
							mainProjection, selection, mainSelectionArgs, null, null, null);
					
					c.moveToFirst();
					breakfastLoaded = new ArrayList<String>();
			
					for(int i = 0; i < c.getCount(); i++){
						breakfastLoaded.add(c.getString(c.getColumnIndexOrThrow(MealStorage.COLUMN_MENUITEM)));
						c.moveToNext();
					}			
					MenuParser.fullMenuObj.get(j).setBreakfast(breakfastLoaded);
					c.close();
					
					mainSelectionArgs[1] = "" + 1;
			
					c = db.query(MealStorage.TABLE_MEALS, 
							mainProjection, selection, mainSelectionArgs, null, null, null);
					
					c.moveToFirst();
					lunchLoaded = new ArrayList<String>();
			
					for(int i = 0; i < c.getCount(); i++){
						lunchLoaded.add(c.getString(c.getColumnIndexOrThrow(MealStorage.COLUMN_MENUITEM)));
						c.moveToNext();
					}			
					MenuParser.fullMenuObj.get(j).setLunch(lunchLoaded);
					c.close();
					
					mainSelectionArgs[1] = "" + 2;
			
					c = db.query(MealStorage.TABLE_MEALS, 
							mainProjection, selection, mainSelectionArgs, null, null, null);
					
					c.moveToFirst();
					dinnerLoaded = new ArrayList<String>();
			
					for(int i = 0; i < c.getCount(); i++){
						dinnerLoaded.add(c.getString(c.getColumnIndexOrThrow(MealStorage.COLUMN_MENUITEM)));
						c.moveToNext();
					}
					MenuParser.fullMenuObj.get(j).setDinner(dinnerLoaded);
					c.close();
			
				}
				db.close();
				mealStore.close();

			}			
			return null;
		}
		
		protected void onPostExecute(Long result){
			// If breakfast is in brunch (on weekends), set active tab to lunch - brunch message will be displayed in breakfast tabs
			if(!MenuParser.fullMenuObj.get(collegeNum).getBreakfast().isEmpty() &&
					MenuParser.fullMenuObj.get(collegeNum).getBreakfast().get(0).equals(MenuParser.brunchMessage)){
				mViewPager.setCurrentItem(1,false);
			}

			// If Lunch  and breakfast are empty automatically set tab to dinner
			// (rare occurence, pretty much only on return from holidays)
			if(MenuParser.fullMenuObj.get(collegeNum).getBreakfast().isEmpty() && MenuParser.fullMenuObj.get(collegeNum).getLunch().isEmpty()){
				mViewPager.setCurrentItem(2,false);
			}
			
			// if all meals empty (dining hall closed), pop open nav drawer
			if(MenuParser.fullMenuObj.get(collegeNum).getBreakfast().isEmpty() && MenuParser.fullMenuObj.get(collegeNum).getLunch().isEmpty()
					&& MenuParser.fullMenuObj.get(collegeNum).getDinner().isEmpty()){
				mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
				mDrawerLayout.openDrawer(Gravity.START);
			}
			
			mDrawerList = (ListView) getActivity().findViewById(R.id.left_drawer);
			
			mDrawerList.setAdapter(new ColorAdapter(getActivity(),
					R.layout.drawer_list_item, MenuParser.collegeList));
			/*
			 * only 2 views exist at a time, so the third returns null, but
			 * we don't know which one it is, so check each one. try catch
			 * would work but is performance-inefficient
			 */
			MenuParser.needsRefresh= false;
			ListView listView = (ListView) getActivity().findViewById(LISTVIEW_ID1);
			if(listView != null){
				listView.setAdapter(new ArrayAdapter<String>(getActivity(),
						android.R.layout.simple_list_item_activated_1,
						MenuParser.fullMenuObj.get(collegeNum).getBreakfast()));
			}
			listView = (ListView) getActivity().findViewById(LISTVIEW_ID2);
			if(listView != null){
				listView.setAdapter(new ArrayAdapter<String>(getActivity(),
						android.R.layout.simple_list_item_activated_1,
						MenuParser.fullMenuObj.get(collegeNum).getLunch()));
			}
			listView = (ListView) getActivity().findViewById(LISTVIEW_ID3);
			if(listView != null){
				listView.setAdapter(new ArrayAdapter<String>(getActivity(),
						android.R.layout.simple_list_item_activated_1,
						MenuParser.fullMenuObj.get(collegeNum).getDinner()));
			}
			
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int height = size.y;
			/*
			 *  Manually try to recreate what the swiperefresh layout has by default.
			 *  Same deal as above with nulls.
			 */
			mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID1);
			if(mSwipeRefreshLayout != null){
				mSwipeRefreshLayout.setProgressViewOffset(false, -100, height / 40);
				mSwipeRefreshLayout.setRefreshing(false);
			}
			mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID2);
			if(mSwipeRefreshLayout != null){
				mSwipeRefreshLayout.setProgressViewOffset(false, -100, height / 40);
				mSwipeRefreshLayout.setRefreshing(false);
			}
			mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID3);
			if(mSwipeRefreshLayout != null){
				mSwipeRefreshLayout.setProgressViewOffset(false, -100, height / 40);
				mSwipeRefreshLayout.setRefreshing(false);
			}

			// Set title to include date
	        getActivity().setTitle(MenuParser.collegeList[collegeNum] + " " + displayedMonth + "/" + displayedDay);
		}
		
	}
	
	class MenuAdapter extends PagerAdapter {	
		
        @Override
        public int getCount() {
            return 3;
        }
        
        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return MenuParser.meals[position];
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int mealnum) {
            // Inflate a new layout from our resources
            View view = getActivity().getLayoutInflater().inflate(R.layout.meal,
                    container, false);
        	container.addView(view, mealnum);
        	
        	ListView mealList = (ListView) view.findViewById(android.R.id.list);
        	/*
        	 * set ID - add 12 in case 0 is something
        	 * This is to reference it later for setting
        	 * its contents
        	 */
        	mealList.setId(mealnum + LISTVIEW_ID1);
        	
            mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.meal_refresh_layout);
            //Add 15 instead of 12 for swipelayout
            mSwipeRefreshLayout.setId(mealnum + SWIPEREF_ID1);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                	MenuParser.needsRefresh = true;
    				int[] today = getToday();
    				// When doing swipe refresh, reload to the same day as what is currently displayed
        	    	new RetrieveMenuTask().execute(displayedMonth, displayedDay, today[2]);
            	}
            });
            
            /*
             * ASynctask to load menu, either from web
             * or from SQLite db.
             * 
             * Load circle *should* be shown, but currently there's 
             * no proper way to manually trigger the reload animation. So
             * we're stuck doing it in a hacky way.
             */
    		if(!refreshStarted){
    			Display display = getActivity().getWindowManager().getDefaultDisplay();
	            Point size = new Point();
	            display.getSize(size);
	            int height = size.y;
	            // manually try to recreate where the spinner ends up in a normal swipe
	            mSwipeRefreshLayout.setProgressViewOffset(false, -50, height / 800);
				mSwipeRefreshLayout.setRefreshing(true);
				// Default loading to today
				int[] today = getToday();
    	    	new RetrieveMenuTask().execute(today[0], today[1], today[2]);
    	    }
    		
    		
    		ArrayList<String> testedArray = new ArrayList<String>();
        	switch(mealnum){
        	case 0:
        		testedArray = MenuParser.fullMenuObj.get(collegeNum).getBreakfast();
        		if (testedArray != null){
        			mealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        					MenuParser.fullMenuObj.get(collegeNum).getBreakfast()));
        		}
        		break;
        	case 1:
        		testedArray = MenuParser.fullMenuObj.get(collegeNum).getLunch();
        		if (testedArray != null){
        			mealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        					MenuParser.fullMenuObj.get(collegeNum).getLunch()));
        		}
        		break;
        	case 2:
        		testedArray = MenuParser.fullMenuObj.get(collegeNum).getDinner();
        		if (testedArray != null){
        			mealList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        				MenuParser.fullMenuObj.get(collegeNum).getDinner()));
        		}
        		break;
        	default:
        		Log.v("ucscdining","We have a problem");
        	}
            return view;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }
	

    /**
     * Allows us to set colors of entries in the college list to denote
     * events
     */
	public class ColorAdapter extends ArrayAdapter<String> {

		public ColorAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
		}

		public ColorAdapter(Context context, int resource, String[] objects) {
			super(context, resource, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			View v = super.getView(position,  convertView,  parent);
			// Blue text for college night
			if(MenuParser.fullMenuObj.get(position).getIsCollegeNight()){
				((TextView) v).setTextColor(Color.BLUE); // 
			}
			// Grayed out if dining hall is closed
			if(!MenuParser.fullMenuObj.get(position).getIsOpen()){
				((TextView) v).setTextColor(Color.LTGRAY); // 
			}
			// Green for Healthy Monday / Farm Friday
			if(MenuParser.fullMenuObj.get(position).getIsFarmFriday() || 
					MenuParser.fullMenuObj.get(position).getIsHealthyMonday()){
				((TextView) v).setTextColor(Color.rgb(0x4C, 0xC5, 0x52)); // 'Green Apple'
			}
			return v;
		}		
	}
	
	public void selectNewDate(int month, int day, int year) {
		//Toast.makeText(getActivity(), month + " " + day + " " + year + "selected", Toast.LENGTH_SHORT).show();
		Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
		/*
		 *  Manually try to recreate what the swiperefresh layout has by default.
		 *  No idea which layouts are active, so check for nulls on all.
		 *  Also no idea why the -140 here is different than the -50 above, 
		 *  the whole thing makes no sense.
		 */
		mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID1);
		if(mSwipeRefreshLayout != null){
			mSwipeRefreshLayout.setProgressViewOffset(false, -140, height / 800);
			mSwipeRefreshLayout.setRefreshing(true);
		}
		mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID2);
		if(mSwipeRefreshLayout != null){
			mSwipeRefreshLayout.setProgressViewOffset(false, -140, height / 800);
			mSwipeRefreshLayout.setRefreshing(true);
		}
		mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(SWIPEREF_ID3);
		if(mSwipeRefreshLayout != null){
			mSwipeRefreshLayout.setProgressViewOffset(false, -140, height / 800);
			mSwipeRefreshLayout.setRefreshing(true);
		}
    	new RetrieveMenuTask().execute(month, day, year);
	}
	
	/**
	 * Returns today's date as a 3-number int array. [month, day, year]
	 */
	public static int[] getToday() {
		Date today = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(today);
		
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int year = cal.get(Calendar.YEAR);
		
		int ret[] = {month, day, year};
		return ret;
		
	}
    
}