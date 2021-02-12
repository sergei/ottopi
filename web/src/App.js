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

function App() {
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

    return (
    <div className="App">
      <RawInstrDisplay swaggerClient={swaggerClient}/>
      <DestInfo swaggerClient={swaggerClient}/>
      <NavHistoryList swaggerClient={swaggerClient} />
      <Autopilot swaggerClient={swaggerClient} />
      <WayPointsList swaggerClient={swaggerClient} />
      <RoutesList swaggerClient={swaggerClient} />
      <FileUploader swaggerClient={swaggerClient} uploadPath={'gpx'} label={'Upload GPX'}/>
      <LogFileList swaggerClient={swaggerClient} />
      <FileUploader swaggerClient={swaggerClient} uploadPath={'polars'} label={'Upload Polar file'}/>
    </div>
  );
}

export default App;
