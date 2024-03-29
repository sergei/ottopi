package com.santacruzinstruments.ottopi.ui.race_setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

public class RoutesExpandableListAdapter extends BaseExpandableListAdapter {

    final private Context context;
    final private RouteCollection routeCollection;
    int [] NOT_FIXED_ENTRIES = {R.string.windward_mark,  R.string.leeward_mark};

    public RoutesExpandableListAdapter(Context context, RouteCollection routeCollection) {
        this.context = context;
        this.routeCollection = routeCollection;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        if( expandedListPosition == 0 ){
            return context.getString(R.string.add_entire_route);
        }else {
            return this.routeCollection.getRoutes().get(listPosition).getRpt(expandedListPosition - 1);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.routes_list_group, null);
        }
        TextView listTitleTextView = convertView
                .findViewById(R.id.routeName);
        listTitleTextView.setTypeface(null, Typeface.BOLD);

        String routeName;
        if( listPosition == 0 ){
            routeName = context.getString(R.string.inflatable_marks);
        }else{
            routeName = this.routeCollection.getRoutes().get(listPosition-1).getName();
        }
        listTitleTextView.setText(routeName);

        return convertView;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.routes_list_item, null);
        }

        String text;

        if( listPosition == 0 ) {
            text = context.getString(NOT_FIXED_ENTRIES[expandedListPosition]);
        }else{
            if ( expandedListPosition == 0){
                text = context.getString(R.string.add_entire_route);
            }else{
                RoutePoint rpt = this.routeCollection.getRoutes().get(listPosition-1).getRpt(expandedListPosition-1);
                text = rpt.name;
                if ( rpt.time.isValid() ){
                    long now = System.currentTimeMillis();
                    CharSequence ago =
                            DateUtils.getRelativeTimeSpanString(rpt.time.toMiliSec(), now, DateUtils.MINUTE_IN_MILLIS);
                    text = text + " [" + ago + "]";
                }
            }
        }

        TextView expandedListTextView = convertView.findViewById(R.id.wptName);
        expandedListTextView.setText(text);
        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        if( listPosition == 0){
            return NOT_FIXED_ENTRIES.length;
        }else {
            return this.routeCollection.getRoutes().get(listPosition - 1).getRptsNum() + 1;
        }
    }

    @Override
    public Object getGroup(int listPosition) {
        return null;
    }

    @Override
    public int getGroupCount() {
        return this.routeCollection.getRoutes().size() + 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}
