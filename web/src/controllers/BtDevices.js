import React, {Component} from 'react';
import BtDevicesListView from "../views/BtDeviceListView";

class BtDevices extends Component {
    // State of this component
    state = {
        loading: true, // true when ajax request is running
        scanIsActive: false,  // True when scan is active
        deviceListPending: true, // We haven't got list of the devices yet
        scanPollTimer: null,
        devices: [],
    };

    componentDidMount() {
        // Request scan results first, to see if scan is in progress, don't request anything
        // until scan is done
        this.requestScanResults();
    }

    componentWillUnmount() {
        if ( typeof clearInterval === "function")
            clearInterval(this.state.scanPollTimer);
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (prevState.scanIsActive && !this.state.scanIsActive){  // Scan stopped
            console.log('Stop timer')
            console.log('clearInterval type', typeof clearInterval)
            if ( typeof clearInterval === "function") {
                console.log('Stop timer 2', this.state.scanPollTimer)
                clearInterval(this.state.scanPollTimer);
            }
            clearInterval(this.state.scanPollTimer);
            // The scan was active, so we will use the devices received in the scan status
            this.setState( {deviceListPending: false, devices: this.state.scanned_devices} )
        }
    }

    startScan = () => {
        this.setState( {loading:true, ok: false,})
        this.props.swaggerClient
        .then( client =>
            {client.apis.bluetooth.bt_rest_api_start_scan({},{}).then(response => {
                console.log(response)
                // Start poling for scan status
                this.requestScanResults();
                let timer = setInterval(this.requestScanResults, 1000);
                this.setState( {loading:false, ok: true, scanIsActive: true, scanPollTimer: timer})
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
        }).catch( error => {
            console.log("Client error" + error);
            this.setState( {true:false, ok: false} )
        });
    }

    requestScanResults = () => {
        console.log('Requesting scan results');
        this.setState( {loading:true, ok: false})

        this.props.swaggerClient
            .then( client => {client.apis.bluetooth.bt_rest_api_get_bt_scan_result().then(response => {
                // console.log(response)
                let scanIsActive = response.body.in_progress;
                this.setState( {
                    loading:false, ok: true, scanIsActive: scanIsActive, scanned_devices: response.body.devices,
                })
                // No scan is running, so let's just request cached devices
                if ( ! scanIsActive && this.state.deviceListPending ){
                    this.requestCachedDevices();
                }
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false} )
        });
    };

    requestCachedDevices = () => {
        console.log('Fetching cached devices');
        this.props.swaggerClient
            .then( client => {client.apis.bluetooth.bt_rest_api_get_bt_devices().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, deviceListPending: false, devices: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false, deviceListPending: false, } )
            })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false, deviceListPending: false, } )
        });
    };

    pairAs = (bd_addr, remote_func) => {
        console.log('Requested pairing ', bd_addr, remote_func);
        this.setState( {loading:true, ok: false})
        this.props.swaggerClient
            .then( client => {
                client.apis.bluetooth.bt_rest_api_pair_bt_device({},
                    {requestBody: {bd_addr: bd_addr, function: remote_func}})
                    .then(response => {
                        console.log(response);
                        this.requestCachedDevices();
                    }).catch( error => {
                        console.log("API error" + error);
                        this.setState( {loading:false, ok: false} )
                })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false} )
        });
    };

    unPair = (bd_addr) => {
        console.log('Requested unpairing ', bd_addr);
        this.setState( {loading:true, ok: false})
        this.props.swaggerClient
            .then( client => {
                client.apis.bluetooth.bt_rest_api_unpair_bt_device({bd_addr}, {})
                    .then(response => {
                        console.log(response);
                        this.requestCachedDevices();
                    }).catch( error => {
                        console.log("API error" + error);
                        this.setState( {loading:false, ok: false} )
                })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false} )
        });
    };


    render() {
        return ( <BtDevicesListView loading={this.state.loading}
                                    scanIsActive={this.state.scanIsActive}
                                    ok={this.state.ok}
                                    devices={this.state.devices}
                                    pairAs={this.pairAs}
                                    unPair={this.unPair}
                                    startScan={this.startScan}
        />);
    }
}

export default BtDevices;
