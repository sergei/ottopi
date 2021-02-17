import './App.css';
import RawInstrDisplay from "./RawInstrDisplay";
import SwaggerClient from 'swagger-client'
import FileUploader from "./FileUploader";
import WayPointsList from "./WayPointsList";
import LogFileList from "./LogFileList";
import Autopilot from "./Autopilot";
import RoutesList from "./RoutesList";
import DestInfo from "./DestInfo";
import NavHistoryList from "./NavHistoryList";
import {Component} from "react";
import MenuButtons from "./MenuButtons";

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
                        <NavHistoryList swaggerClient={this.state.swaggerClient} />
                    </div>
                break;
            case screens.ROUTES:
                screen_jsx =
                    <div>
                        <WayPointsList swaggerClient={this.state.swaggerClient} />
                        <RoutesList swaggerClient={this.state.swaggerClient} />
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
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'gpx'} label={'Upload GPX'}/>
                        <LogFileList swaggerClient={this.state.swaggerClient} />
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'polars'} label={'Upload Polar file'}/>
                        <FileUploader swaggerClient={this.state.swaggerClient} uploadPath={'sw_update'} label={'Upload Software Update'}/>
                    </div>
                break;
            default:
                screen_jsx = 'Ooops';
                break
        }
        return (
            <div className="App">
                <MenuButtons currentScreen={this.state.screen} setScreen={this.setScreen}/>
                {screen_jsx}
            </div>
        );
    }

}

export default MainScreen;
