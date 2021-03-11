import React from 'react';
import {useStyles} from "./RouteListView";
import {IconButton, ListItem, ListItemSecondaryAction, ListItemText} from "@material-ui/core";
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import DeleteIcon from "@material-ui/icons/Delete";

function CurrentRoutePointView(props) {
    const classes = useStyles();

    return (
        <ListItem button className={classes.nested}
                  onClick={ () => props.navigateTo(props.idx)}>
            {props.active ? <ChevronRightIcon/> : ''}
            <ListItemText primary={props.name} />
            <ListItemSecondaryAction>
                <IconButton edge="end" aria-label="delete"
                            onClick={ () => props.removeFromRoute(props.idx)}>
                    <DeleteIcon />
                </IconButton>
            </ListItemSecondaryAction>
        </ListItem>
    );
}
export default CurrentRoutePointView;

