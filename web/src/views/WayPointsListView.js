import React from 'react';
import WayPointView from "./WayPointView";

function WayPointsListView (props){

        if( props.loading ) {
            return (<div>Loading WPTs ...</div>)
        }else {
            let wpts;
            if( props.ok){
                 wpts = props.wpts.map( (wpt, i) => (
                        <WayPointView {...wpt} key={i} navigateTo={this.navigateTo}/>
                    )
                );
            }else{
                wpts = 'Failed to fetch WPTs';
            }
            return (
                <div>
                    {wpts}
                </div>
            )
        }
}

export default WayPointsListView;
