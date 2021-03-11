import React from 'react';
import WayPointView from "./WayPointView";
import {useStyles} from "./RouteListView";
import List from "@material-ui/core/List";
import {
    Collapse,
    Divider,
    ListItem, ListItemSecondaryAction,
    ListItemText,
    Paper, Switch,
} from "@material-ui/core";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import {ExpandLess, ExpandMore} from "@material-ui/icons";
import CurrentRoutePointView from "./CurrentRoutePointView";

function WayPointsListView (props){
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);
    const handleClick = () => {
        setOpen(!open);
    };

    if( props.loading ) {
        return (<div>Loading WPTs ...</div>)
    }else {
        if( props.ok){
             const wpts = props.wpts.map( (wpt, i) => (
                    <WayPointView {...wpt} key={i} addToRoute={props.addToRoute}/>
                )
            );

            const routeWpts = props.routeWpts.map( (wpt, i) => (
                <CurrentRoutePointView {...wpt} key={i} idx={i} activeWptIdx={props.activeWptIdx}
                                       // Methods
                                       navigateTo={props.navigateTo}
                                       removeFromRoute={props.removeFromRoute}/>
                )
            );

            const routeName = props.routeIsActive ? "Current route" : "No route is selected";

            return (
                <Paper>
                    <div className={classes.root}>
                        <List component="nav" aria-label="main mailbox folders">
                            <ListItem button>
                                <ListItemText primary={routeName} />
                                    <ListItemSecondaryAction>
                                        <Switch
                                            edge="end"
                                            onChange={props.toggleActiveRoute}
                                            checked={props.routeIsActive}
                                            inputProps={{ 'aria-labelledby': 'switch-list-label-wifi' }}
                                        />
                                    </ListItemSecondaryAction>
                            </ListItem>
                        </List>
                        <Divider />
                        <Collapse in={props.routeIsActive} timeout="auto" unmountOnExit>
                            <List component="nav" aria-label="secondary mailbox folders">
                                {routeWpts}
                            </List>
                        </Collapse>
                    </div>

                    <List component="nav" className={classes.root}>
                        <Divider />
                        <ListItem button onClick={handleClick}>
                            {props.active ? <ChevronRightIcon/> : ''}
                            <ListItemText primary="Available Waypoints" />
                            {open ? <ExpandLess /> : <ExpandMore />}
                        </ListItem>
                        <Divider />
                        <Collapse in={open} timeout="auto" unmountOnExit>
                            <List component="div" disablePadding>
                                {wpts}
                            </List>
                        </Collapse>
                    </List>
                </Paper>
            )
        }else{
            return (<div>Failed to fetch waypoints</div>)
        }
    }
}

export default WayPointsListView;


