import React from 'react';
import {screens} from "./MainScreen";

function MenuButtons(props) {

    return (
        <div>
            <button onClick={ () => props.setScreen(screens.NAVIGATION)}>Home</button>
            <button onClick={ () => props.setScreen(screens.ROUTES)}>ROUTES</button>
            <button onClick={ () => props.setScreen(screens.FILE_MANAGER)}>FILE_MANAGER</button>
            <button onClick={ () => props.setScreen(screens.AUTOPILOT)}>AUTOPILOT</button>
        </div>
    );

}

export default MenuButtons;
