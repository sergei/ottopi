import React  from 'react';
import RouteView from "./RouteView";
import List from '@material-ui/core/List';
import { makeStyles } from '@material-ui/core/styles';
import {Divider, ListItem, ListItemText, Paper} from "@material-ui/core";

export const useStyles = makeStyles((theme) => ({
    root: {
        width: '100%',
        maxWidth: 360,
        backgroundColor: theme.palette.background.paper,
    },
    nested: {
        paddingLeft: theme.spacing(4),
    },
}));

function RouteListView(props) {
    const classes = useStyles();

    if( props.loading ) {
        return (<div>Loading routes ...</div>)
    }else {
        let routes;
        if( props.ok){
            routes = props.routes.map( (route, i) => (
                    <RouteView {...route} key={i} routeIdx={i} selectRoute={props.selectRoute}/>
                )
            );
        }else{
            routes = 'Failed to fetch routes';
        }
        return (
            <Paper>
                <List component="nav" className={classes.root}>
                    <Divider />
                    <ListItem>
                        <ListItemText primary="Available Routes" />
                    </ListItem>
                    <Divider />
                    {routes}
                </List>
            </Paper>
        )
    }
}

export default RouteListView;
