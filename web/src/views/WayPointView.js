import React from 'react';
import {ListItem, ListItemIcon, ListItemText} from "@material-ui/core";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import {useStyles} from "./RouteListView";

function WayPointView(props) {
    const classes = useStyles();
    const wpt = {
        'name': props.name,
        'lat': props.lat,
        'lon': props.lon,
    }
    return (

        <ListItem button className={classes.nested}
                  onClick={ () => props.navigateTo(wpt)}>
            <ListItemIcon>
                {props.active ? <ChevronRightIcon/> : ''}
            </ListItemIcon>
            <ListItemText primary={props.name} />
        </ListItem>
    );
}
export default WayPointView;
