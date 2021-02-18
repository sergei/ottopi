import React  from 'react';
import RouteView from "./RouteView";
import List from '@material-ui/core/List';
import { makeStyles } from '@material-ui/core/styles';
import {Button, Paper, Typography} from "@material-ui/core";

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
                <Paper>
                    <Button variant="contained" onClick={() => props.stopNavigation()}>Stop navigation</Button>
                </Paper>

                <Typography variant="h6">Routes</Typography>
                <List
                    component="nav"
                    className={classes.root}
                >
                    {routes}
                </List>
            </Paper>
                )
    }
}

export default RouteListView;
