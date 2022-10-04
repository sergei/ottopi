package com.santacruzinstruments.ottopi.ui.race_setup;

import android.text.format.DateUtils;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import com.google.android.material.lists.*;

@SuppressWarnings("rawtypes")
public class RaceRouteAdapter extends RecyclerView.Adapter {
    Route route;
    final NavViewModel navViewModel;
    public RaceRouteAdapter(Route route, NavViewModel navViewModel) {
        this.route = route;
        this.navViewModel = navViewModel;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return TwoLineItemViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TwoLineItemViewHolder vh = (TwoLineItemViewHolder) holder;

        String text;
        if( position == 0 && this.route.hasStartLine()) {
            final RoutePoint pinRpt = this.route.getRpt(position);
            final RoutePoint rcbRpt = this.route.getRpt(position + 1);
            text = pinRpt.name + " - " + rcbRpt.name;
            vh.secondary.setText(R.string.start_line);
            // If set as active,  navigate to PIN
            vh.text.setOnClickListener(view -> navViewModel.ctrl().makeActiveWpt(position));
            vh.icon.setOnClickListener(view -> navViewModel.ctrl().removeRaceRouteWpt(position));
            if ( this.route.getActiveWptIdx() < 2 ){
                text = "> " + text;
            }
        } else if (position == getItemCount() - 1  && this.route.hasFinishLine()){
            int lastIdx = this.route.getRptsNum() -1;
            final RoutePoint pinRpt = this.route.getRpt(lastIdx - 1);
            final RoutePoint rcbRpt = this.route.getRpt(lastIdx);
            text = pinRpt.name
                    + " - "
                    + rcbRpt.name;
            vh.secondary.setText(R.string.finish_line);
            // If set as active,  navigate to RCB
            vh.text.setOnClickListener(view -> navViewModel.ctrl().makeActiveWpt(lastIdx));
            vh.icon.setOnClickListener(view -> navViewModel.ctrl().removeRaceRouteWpt(lastIdx - 1));
            if ( this.route.getActiveWptIdx() >= lastIdx - 1 ){
                text = "> " + text;
            }
        } else {
            final int rptIdx = this.route.hasStartLine() ? position + 1 : position;

            final RoutePoint rpt = this.route.getRpt(rptIdx);
            if ((rpt.type == RoutePoint.Type.START || rpt.type == RoutePoint.Type.FINISH) && rpt.leaveTo == RoutePoint.LeaveTo.STARBOARD) {
                text = "____ - " + rpt.name;
                vh.secondary.setText(R.string.select_port_mark);
            } else if ((rpt.type == RoutePoint.Type.START || rpt.type == RoutePoint.Type.FINISH) && rpt.leaveTo == RoutePoint.LeaveTo.PORT) {
                text = rpt.name + " - ____";
                vh.secondary.setText(R.string.long_press_for_startboad_mark);
            } else {
                text = rpt.name;
            }
            vh.text.setOnClickListener(view -> navViewModel.ctrl().makeActiveWpt(rptIdx));
            vh.icon.setOnClickListener(view -> navViewModel.ctrl().removeRaceRouteWpt(rptIdx));
            if ( this.route.getActiveWptIdx() == rptIdx ){
                text = "> " + text;
            }
            if( !rpt.loc.isValid()){
                vh.secondary.setText(R.string.unknown_location);
            }else if ( rpt.time.isValid()){
                long now = System.currentTimeMillis();
                CharSequence ago =
                        DateUtils.getRelativeTimeSpanString(rpt.time.toMiliSec(), now, DateUtils.MINUTE_IN_MILLIS);
                vh.secondary.setText(ago);
            }else{
                vh.secondary.setText("");
            }
        }

        vh.text.setText(text);
    }

    @Override
    public int getItemCount() {
        int ptsCount = this.route.getRptsNum();

        if ( this.route.hasStartLine() )
            ptsCount --;

        if ( this.route.hasFinishLine() )
            ptsCount --;

        return ptsCount;
    }

    public void updateRoute(Route route) {
        this.route  = route;
    }
}
