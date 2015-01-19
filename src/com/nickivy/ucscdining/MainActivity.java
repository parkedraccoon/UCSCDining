package com.nickivy.ucscdining;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.nickivy.ucscdining.R;
import com.nickivy.ucscdining.MealViewFragment;
import com.nickivy.ucscdining.parser.MenuParser;

/**
 * App for viewing UCSC dining menus. Currently can 
 * read all menus, display them based on time, with special colors displayed 
 * for events such as College Nights, Healthy Mondays, or Farm Fridays.
 * 
 * <p>Will eventually allow the user to fast forward to see planned menus for
 * days in the future.
 * 
 * <p>TODO: smart refresh
 * <p>TODO: tablet layout (display all 3 meals at once)
 *
 * @author Nick Ivy parkedraccoon@gmail.com
 */

public class MainActivity extends ActionBarActivity{
	
	static final int ITEMS = 3;
	
	private ListView mDrawerList;
	private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ActionBar mActionBar;
    private MealViewFragment fragment;
    
    private static int currentCollege = 0;
	
    @Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.mainview);
		
//		if(findViewById(R.id.fragment_container) != null) {
			
/*			if (savedInstanceState != null) {
				return;
			}*/
			
			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	        mDrawerList = (ListView) findViewById(R.id.left_drawer);
	        mActionBar = getSupportActionBar();

	        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			mDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.drawer_list_item, MenuParser.collegeList));
			
	        mActionBar.setDisplayHomeAsUpEnabled(true);
	        mActionBar.setHomeButtonEnabled(true);	        
	        
	        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
	        		R.string.drawer_open, R.string.drawer_close);
	        
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
	        
	        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        fragment = new MealViewFragment();
	        Bundle args = new Bundle();
	        args.putInt(MealViewFragment.ARG_COLLEGE_NUMBER, currentCollege);
	        fragment.setArguments(args);
	        transaction.replace(R.id.fragment_container, fragment);
	        transaction.commit();
	        
/*	        if (savedInstanceState == null) {
//	            selectItem(0);
	        	fragment.selectItem(0);
	        }*/
//		}
		
	}
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            selectItem(position);
        	fragment.selectItem(position);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       if (mDrawerToggle.onOptionsItemSelected(item)) {
           return true;
       }
       return super.onOptionsItemSelected(item);
   }

}
