import React from 'react';
import WayPointView from "./WayPointView";
import {useStyles} from "./RouteListView";
import List from "@material-ui/core/List";
import {Paper, Typography} from "@material-ui/core";

function WayPointsListView (props){
    const classes = useStyles();

    if( props.loading ) {
        return (<div>Loading WPTs ...</div>)
    }else {
        if( props.ok){
             const wpts = props.wpts.map( (wpt, i) => (
                    <WayPointView {...wpt} key={i} navigateTo={props.navigateTo}/>
                )
            );
            return (
                <Paper>
                    <Typography variant="h6">Waypoints</Typography>
                    <List
                        component="nav"
                        className={classes.root}
                    >
                        {wpts}
                    </List>
                </Paper>
            )
        }else{
            return (<div>Failed to fetch waypoints</div>)
        }
    }
}

export default WayPointsListView;
