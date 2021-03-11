import React from 'react';
import {IconButton, ListItem, ListItemIcon, ListItemSecondaryAction, ListItemText} from "@material-ui/core";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import {useStyles} from "./RouteListView";
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline';

function WayPointView(props) {
    const classes = useStyles();
    const wpt = {
        'name': props.name,
        'lat': props.lat,
        'lon': props.lon,
    }
    return (

        <ListItem button className={classes.nested}
                  onClick={ () => props.addToRoute(wpt)}>
            <ListItemIcon>
                {props.active ? <ChevronRightIcon/> : ''}
            </ListItemIcon>
            <ListItemText primary={props.name} />
            <ListItemSecondaryAction>
                <IconButton edge="end" aria-label="add" onClick={ () => props.addToRoute(wpt)}>
                    <AddCircleOutlineIcon />
                </IconButton>
            </ListItemSecondaryAction>
        </ListItem>
    );
}
export default WayPointView;
