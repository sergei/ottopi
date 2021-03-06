import React, {Component} from 'react';
import BtDevicesListView from "../views/BtDeviceListView";

class BtDevices extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
    };

    requestCachedDevices = () => {
        console.log('Fetching cached devices');
        this.props.swaggerClient
            .then( client => {client.apis.bluetooth.rest_api_get_bt_devices().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, devices: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    };

    componentDidMount() {
        this.requestCachedDevices();
    }

    // pairAs = (bt_addr, remote_func) => {
    //     console.log('Requested pairing ', bt_addr, bt_addr);
    //     let route = this.state.routes[routeIdx];
    //     route.active = true;
    //     route.active_wpt_idx = wptIdx;
    //     this.props.swaggerClient
    //         .then( client => {
    //             client.apis.nav.rest_api_select_route({}, {requestBody:route})
    //                 .then(response => {
    //                     console.log(response);
    //                     this.requestRoutes();
    //                 }).catch( error => {
    //                 console.log("API error" + error);
    //             })
    //         }).catch( error => {
    //         console.log("Client error" + error);
    //     });
    // };


    render() {
        return ( <BtDevicesListView loading={this.state.loading}  ok={this.state.ok}
                                devices={this.state.devices} pairAs={this.pairAs} unPair={this.unPair}
                                    startScan={this.startScan}
        />);
    }
}

export default BtDevices;
