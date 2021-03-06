import React from 'react';
import {ListItem, ListItemIcon, ListItemText} from "@material-ui/core";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import {useStyles} from "./RouteListView";
import ShowChartIcon from '@material-ui/icons/ShowChart';
import TimerIcon from '@material-ui/icons/Timer';
import BrightnessAutoIcon from '@material-ui/icons/BrightnessAuto';
import BluetoothConnectedIcon from '@material-ui/icons/BluetoothConnected';
import BluetoothIcon from '@material-ui/icons/Bluetooth';
import BluetoothDisabledIcon from '@material-ui/icons/BluetoothDisabled';
import IconButton from '@material-ui/core/IconButton';

function BtDeviceView(props) {
    const classes = useStyles();
    return (
        <div>
            {props.bd_addr}
            {props.name}
            {props.is_paired}
            {props.is_connected}
            {props.function}
        </div>
    );
}
export default BtDeviceView;
