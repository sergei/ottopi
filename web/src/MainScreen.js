import './App.css';
import RawInstrDisplay from "./controllers/RawInstrDisplay";
import SwaggerClient from 'swagger-client'
import FileUploader from "./controllers/FileUploader";
import WayPoints from "./controllers/WayPoints";
import LogFiles from "./controllers/LogFiles";
import Autopilot from "./controllers/Autopilot";
import Routes from "./controllers/Routes";
import DestInfo from "./controllers/DestInfo";
import NavHistory from "./controllers/NavHistory";
import {Component} from "react";
import MenuButtonsView from "./views/MenuButtonsView";

export const screens = {
    NAVIGATION: "navigation",
    AUTOPILOT: "autopilot",
    ROUTES: "routes",
    FILE_MANAGER: "file_manager",
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
        swaggerClient.then(client => {
            console.log("OpenAPi spec:");
            console.log(client.apis)
        });

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
                        <RawInstrDisplay swaggerClient={this.state.swaggerClient}/>
                        <DestInfo swaggerClient={this.state.swaggerClient}/>
                        <NavHistory swaggerClient={this.state.swaggerClient} />
                    </div>
                break;
            case screens.ROUTES:
                screen_jsx =
                    <div>
                        <Routes swaggerClient={this.state.swaggerClient} />
                        <WayPoints swaggerClient={this.state.swaggerClient} />
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
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'gpx'} label={'Select GPX file'}/>
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'polars'} label={'Select Polar file'}/>
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'sw_update'} label={'Select SW Update package'}/>
                        <LogFiles swaggerClient={this.state.swaggerClient} />
                    </div>
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
