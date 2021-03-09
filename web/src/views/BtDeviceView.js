import React from 'react';
import {Collapse, ListItem, ListItemIcon, ListItemText, Paper} from "@material-ui/core";
import {useStyles} from "./RouteListView";
import ShowChartIcon from '@material-ui/icons/ShowChart';
import TimerIcon from '@material-ui/icons/Timer';
import BrightnessAutoIcon from '@material-ui/icons/BrightnessAuto';
import BluetoothDisabledIcon from '@material-ui/icons/BluetoothDisabled';
import {ExpandLess, ExpandMore} from "@material-ui/icons";
import List from "@material-ui/core/List";

function BtDeviceView(props) {
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);

    const handleClick = () => {
        setOpen(!open);
    };

    let conn_icon;
    if (props.is_paired){
        if (props.function === 'route')
            conn_icon =<ShowChartIcon />;
        else if (props.function === 'timer')
            conn_icon =<TimerIcon />;
        else if (props.function === 'autopilot')
            conn_icon =<BrightnessAutoIcon />;
        else
            conn_icon = '';
    }else{
        conn_icon ='';
    }

    return (
        <Paper>
            <ListItem button onClick={handleClick}>
                <ListItemIcon color={'secondary'}>
                    {conn_icon}
                </ListItemIcon>
                <ListItemText primary={props.name} secondary={props.bd_addr} />
                {open ? <ExpandLess /> : <ExpandMore />}
            </ListItem>
            <Collapse in={open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                    <ListItem button className={classes.nested}
                              onClick={ () => props.pairAs(props.bd_addr, 'route')}>
                        <ListItemIcon>
                            <ShowChartIcon />
                        </ListItemIcon>
                        <ListItemText primary="Use for Route" />
                    </ListItem>
                    <ListItem button className={classes.nested}
                              onClick={ () => props.pairAs(props.bd_addr, 'timer')}>
                        <ListItemIcon>
                            <TimerIcon />
                        </ListItemIcon>
                        <ListItemText primary="Use for Timer" />
                    </ListItem>
                    <ListItem button className={classes.nested}
                              onClick={ () => props.pairAs(props.bd_addr, 'autopilot')}>
                        <ListItemIcon>
                            <BrightnessAutoIcon />
                        </ListItemIcon>
                        <ListItemText primary="Use for Autopilot"/>
                    </ListItem>
                    <ListItem button className={classes.nested} onClick={ () => props.unPair(props.bd_addr)}>
                        <ListItemIcon>
                            <BluetoothDisabledIcon />
                        </ListItemIcon>
                        <ListItemText primary="Stop using" />
                    </ListItem>
                </List>
            </Collapse>
        </Paper>
    );
}
export default BtDeviceView;
