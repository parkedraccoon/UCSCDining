package com.nickivy.ucscdining.widget;


import android.util.Log;
import android.widget.RemoteViews;

import com.nickivy.ucscdining.parser.MenuParser;
import com.nickivy.ucscdining.util.Util;

import java.util.Calendar;

/**
 * Object for storing widget data, helps make each widget able to be independent.
 *
 * Released under GNU GPL v2 - see doc/LICENCES.txt for more info.
 *
 * @author Nick Ivy parkedraccoon@gmail.com
 */
public class WidgetData {

    private int widgetId,
    currentCollege,
    currentMeal,
    currentMonth,
    currentDay,
    currentYear;

    private RemoteViews views;

    public WidgetData(int widgetId) {
        this(widgetId, 0);
    }

    public WidgetData(int widgetId, int initialCollege) {
        this.widgetId = widgetId;
        this.currentCollege = initialCollege;
        currentMeal = Util.getCurrentMeal(this.currentCollege);
        // Set current day to today
        int today[] = Util.getToday();
        currentMonth = today[0];
        currentDay = today[1];
        currentYear = today[2];
    }

    public int getWidgetId() {
        return widgetId;
    }

    public int getCollege() {
        //Log.v("ucscdining", "id when getting college: " + widgetId);
        return currentCollege;
    }

    public int getMeal() {
        return currentMeal;
    }

    public int getMonth() {
        return currentMonth;
    }

    public int getDay() {
        return currentDay;
    }

    public int getYear() {
        return currentYear;
    }


    public void setCollege(int currentCollege) {
        this.currentCollege = currentCollege;
    }

    public void setMeal(int currentMeal) {
        this.currentMeal = currentMeal;
    }

    public void setMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public void setDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public void setYear(int currentYear) {
        this.currentYear = currentYear;
    }

    public void incrementCollege() {
        currentCollege++;
        if (currentCollege >= 5) {
            currentCollege = 0;
        }
            /*
             * If college is not open, cycle until find one that is. Only try five times
             *
             * All 5 closed is handled separately
             */
        if (!MenuParser.fullMenuObj.get(currentCollege).getIsOpen()) {
            for (int i = 0; i < 5; i++) {
                currentCollege++;
                if (currentCollege == 5) {
                    currentCollege = 0;
                }
                if (MenuParser.fullMenuObj.get(currentCollege).getIsOpen()) {
                    break;
                }
            }
        }
    }

    public void decrementCollege() {
        currentCollege--;
        if (currentCollege <= -1) {
            currentCollege = 4;
        }
            /*
             * If college is not open, cycle until find one that is. Only try five times
             *
             * All 5 closed is handled separately
             */
        if (!MenuParser.fullMenuObj.get(currentCollege).getIsOpen()) {
            for (int i = 0; i < 5; i++) {
                currentCollege--;
                if (currentCollege == -1) {
                    currentCollege = 4;
                }
                if (MenuParser.fullMenuObj.get(currentCollege).getIsOpen()) {
                    break;
                }
            }
        }
    }

    public void incrementMeal() {
        currentMeal++;
        if (currentMeal == 3) {
            currentMeal = 0;
            // If at dinner, and right button pressed, advance to next day
            changeDay(1);
        }
    }

    public void decrementMeal() {
        currentMeal--;
        if (currentMeal == -1) {
            currentMeal = 2;
            // If at breakfast and left button pressed, go to previous day
            changeDay(-1);
        }
        if (currentMeal == Util.BREAKFAST &&
                MenuParser.fullMenuObj.get(currentCollege).getBreakfast().size() > 0) {
            if (MenuParser.fullMenuObj.get(currentCollege).getBreakfast().get(0)
                    .equals(Util.brunchMessage)) {
                changeDay(-1);
                currentMeal = Util.DINNER;
            }
        }
    }

    private void changeDay(int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, currentMonth - 1);
        calendar.set(Calendar.DAY_OF_MONTH, currentDay);
        calendar.set(Calendar.YEAR, currentYear);
        calendar.add(Calendar.DATE, amount);
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        currentYear = calendar.get(Calendar.YEAR);
    }

    public void setToToday() {
        int today[] = Util.getToday();
        currentMonth = today[0];
        currentDay = today[1];
        currentYear = today[2];
    }
}
