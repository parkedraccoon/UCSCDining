package com.nickivy.ucscdining.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.nickivy.ucscdining.R;
import com.nickivy.ucscdining.parser.MenuParser;
import com.nickivy.ucscdining.util.Util;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * RemoteViewsService for the UCSCDining Widget. This only handles the ListView.
 *
 * @author Nick Ivy parkedraccoon@gmail.com
 */

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return(new MealWidgetViewsFactory(this.getApplicationContext(),
                intent));
    }
}

class MealWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context = null;
    private int appWidgetId;

    public MealWidgetViewsFactory(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        switch (MenuWidget.currentMeal) {
            case Util.BREAKFAST:
                return MenuParser.fullMenuObj.get(MenuWidget.currentCollege).getBreakfast().size();
            case Util.LUNCH:
                return MenuParser.fullMenuObj.get(MenuWidget.currentCollege).getLunch().size();
            case Util.DINNER:
                return MenuParser.fullMenuObj.get(MenuWidget.currentCollege).getDinner().size();
            default:
                return -1;
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);
        // If user mashes buttons too fast things can crash. This should prevent it
        if (position >= getCurrentMenu(MenuWidget.currentCollege).size()) {
            return row;
        }
        row.setTextViewText(android.R.id.text1,
                getCurrentMenu(MenuWidget.currentCollege).get(position));

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(Util.EXTRA_WORD,
                getCurrentMenu(MenuWidget.currentCollege).get(position));
        intent.putExtras(extras);
        row.setOnClickFillInIntent(android.R.id.text1, intent);
        return row;
    }

    /**
     * Given parameters of current college, decides what meal is current and returns its list
     * @param college
     * @return
     */
    private ArrayList<String> getCurrentMenu(final int college) {
        if (!MenuParser.fullMenuObj.get(college).getIsOpen()) {
            ArrayList<String> ret = new ArrayList<String>();
            ret.add(Util.collegeList[college] +
                    "dining hall closed today");
            return ret;
        }
        switch(MenuWidget.currentMeal) {
            case Util.BREAKFAST:
                return MenuParser.fullMenuObj.get(college).getBreakfast();
            case Util.LUNCH:
                return MenuParser.fullMenuObj.get(college).getLunch();
            case Util.DINNER:
                return MenuParser.fullMenuObj.get(college).getDinner();
            default:
                return null;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
