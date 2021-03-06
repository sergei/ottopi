import React from 'react';
import WayPointView from "./WayPointView";
import {useStyles} from "./RouteListView";
import List from "@material-ui/core/List";
import {Button, Paper, Typography} from "@material-ui/core";

function BtDevicesListView (props){
    const classes = useStyles();

    if( props.loading ) {
        return (<div>Loading BT devices ...</div>)
    }else {
        if( props.ok){
             const devices = props.devices.map( (device, i) => (
                    <WayPointView {...device} key={i} pairAs={props.pairAs} unPair={props.unPair}/>
                )
            );
            return (
                <Paper>
                    <Typography variant="h6">BT Remotes</Typography>
                    <Paper>
                        <Button variant="contained" onClick={() => props.startScan()}>Start Scan</Button>
                    </Paper>

                    <List component="nav" className={classes.root}>
                        {devices}
                    </List>
                </Paper>
            )
        }else{
            return (<div>Failed to fetch BT devices</div>)
        }
    }
}

export default BtDevicesListView;

