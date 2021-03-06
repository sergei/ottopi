import React from 'react';
import {useStyles} from "./RouteListView";
import {ListItem, ListItemText} from "@material-ui/core";
import ChevronRightIcon from '@material-ui/icons/ChevronRight';

function RoutePointView(props) {
    const classes = useStyles();

    return (
        <ListItem button className={classes.nested}
                  onClick={ () => props.selectRoute(props.routeIdx, props.wptIdx)}>
            {props.active ? <ChevronRightIcon/> : ''}
            <ListItemText primary={props.name} />
        </ListItem>
    );
}
export default RoutePointView;

