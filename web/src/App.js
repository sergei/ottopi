import './App.css';
import RawInstrDisplay from "./RawInstrDisplay";
import SwaggerClient from 'swagger-client'
import GpxUploader from "./GpxUploader";

function App() {
    const specUrl =  "http://localhost:5555/ottopi/openapi.json";
    const swaggerClient = new SwaggerClient(specUrl);
    swaggerClient.then(client => {
        console.log("OpenAPi spec:");
        console.log(client.apis)
    });

    return (
    <div className="App">
      <RawInstrDisplay  swaggerClient={swaggerClient}/>
      <GpxUploader swaggerClient={swaggerClient}/>
    </div>
  );
}

export default App;
