import React from 'react';
import {screens} from "./MainScreen";
import {BottomNavigation, BottomNavigationAction} from "@material-ui/core";
import ExploreIcon from '@material-ui/icons/Explore';
import ShowChartIcon from '@material-ui/icons/ShowChart';
import BrightnessAutoIcon from '@material-ui/icons/BrightnessAuto';
import SdCardIcon from '@material-ui/icons/SdCard';

function MenuButtons(props) {

    return (
            <BottomNavigation
                value={props.currentScreen}
                onChange={(event, newValue) => {
                    props.setScreen(newValue);
                }}
                showLabels
            >
                <BottomNavigationAction value={screens.NAVIGATION} label="Navigation" icon={<ExploreIcon />} />
                <BottomNavigationAction value={screens.ROUTES} label="Routes" icon={<ShowChartIcon />} />
                <BottomNavigationAction value={screens.FILE_MANAGER} label="Files" icon={<SdCardIcon />} />
                <BottomNavigationAction value={screens.AUTOPILOT} label="Autopilot" icon={<BrightnessAutoIcon />} />
            </BottomNavigation>
    );

}

export default MenuButtons;
