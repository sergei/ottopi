import React from 'react';
import WayPointView from "./WayPointView";
import {useStyles} from "./RouteListView";
import List from "@material-ui/core/List";
import {
    Collapse,
    Divider,
    IconButton,
    ListItem,
    ListItemText,
    Paper,
} from "@material-ui/core";
import ExploreOffIcon from '@material-ui/icons/ExploreOff';
import ExploreIcon from '@material-ui/icons/Explore';
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
                <CurrentRoutePointView {...wpt} key={i} idx={i} navigateTo={props.navigateTo} removeFromRoute={props.removeFromRoute}/>
                )
            );

            return (
                <Paper>
                    <div className={classes.root}>
                        <List component="nav" aria-label="main mailbox folders">
                            <ListItem button>
                                <ListItemText primary="Current Route (active)" />
                                <IconButton edge="end" aria-label="delete">
                                    <ExploreIcon />
                                </IconButton>
                            </ListItem>
                        </List>
                        <Divider />
                        <List component="nav" aria-label="secondary mailbox folders">
                            {routeWpts}
                        </List>
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


