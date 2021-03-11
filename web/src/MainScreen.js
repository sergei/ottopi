import './App.css';
import RawInstrDisplay from "./controllers/RawInstrDisplay";
import SwaggerClient from 'swagger-client'
import FileUploader from "./controllers/FileUploader";
import WayPoints from "./controllers/WayPoints";
import Autopilot from "./controllers/Autopilot";
import Routes from "./controllers/Routes";
import DestInfo from "./controllers/DestInfo";
import NavHistory from "./controllers/NavHistory";
import {Component} from "react";
import MenuButtonsView from "./views/MenuButtonsView";
import LogFileListView from "./views/LogFileListView";
import About from "./controllers/About";
import RaceTimer from "./controllers/RaceTimer";
import BtDevices from "./controllers/BtDevices";

export const screens = {
    NAVIGATION: "navigation",
    AUTOPILOT: "autopilot",
    ROUTES: "routes",
    FILE_MANAGER: "file_manager",
    RACE_TIMER: "race_timer",
}

class MainScreen extends Component {

    constructor(props) {
        super(props);

        let specUrl;
        if (!process.env.NODE_ENV || process.env.NODE_ENV === 'development') {
            specUrl =  "http://localhost:5555/openapi.json";
        } else {
            // production code
            specUrl =  "openapi.json";
        }
        const swaggerClient = new SwaggerClient(specUrl);

        this.state = {
            swaggerClient: swaggerClient,
            screen: screens.NAVIGATION
        }
    }

    setScreen = (screen) => {
        console.log('Set screen to', screen)
        this.setState({screen: screen})
    }

    render() {
        let screen_jsx;
        switch ( this.state.screen){
            case screens.NAVIGATION:
                screen_jsx =
                    <div>
                        <DestInfo swaggerClient={this.state.swaggerClient}/>
                        <RawInstrDisplay swaggerClient={this.state.swaggerClient}/>
                        <NavHistory swaggerClient={this.state.swaggerClient} />
                    </div>
                break;
            case screens.ROUTES:
                screen_jsx =
                    <div>
                        <WayPoints swaggerClient={this.state.swaggerClient} />
                        <Routes swaggerClient={this.state.swaggerClient} />
                    </div>
                break;
            case screens.AUTOPILOT:
                screen_jsx =
                    <div>
                        <Autopilot swaggerClient={this.state.swaggerClient} />
                    </div>
                break;
            case screens.FILE_MANAGER:
                screen_jsx =
                    <div>
                        <FileUploader key="1" swaggerClient={this.state.swaggerClient} uploadPath={'gpx'} label={'Select GPX file'}/>
                        <FileUploader key="2" swaggerClient={this.state.swaggerClient} uploadPath={'polars'} label={'Select Polar file'}/>
                        <FileUploader key="3" swaggerClient={this.state.swaggerClient} uploadPath={'sw_update'} label={'Select SW Update package'}/>
                        <LogFileListView/>
                        <BtDevices swaggerClient={this.state.swaggerClient}  />
                        <About swaggerClient={this.state.swaggerClient}/>
                    </div>
                break;
            case screens.RACE_TIMER:
                screen_jsx =
                    <RaceTimer swaggerClient={this.state.swaggerClient} />
                break;
            default:
                screen_jsx = 'Ooops';
                break
        }
        return (
            <div className="App">
                <MenuButtonsView currentScreen={this.state.screen} setScreen={this.setScreen}/>
                {screen_jsx}
            </div>
        );
    }

}

export default MainScreen;
