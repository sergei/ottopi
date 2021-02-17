import React from 'react';
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";

class DestInfo extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        dest: null,
        response: null,
        timer: null,
    };

    componentDidMount() {
        this.updateDestStatus();
        let timer = setInterval(this.updateDestStatus, 5000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if( this.clearInterval )  // Prevent crash on sign out
            this.clearInterval(this.state.timer);
    }

    updateDestStatus = () => {
        console.log('Fetching destination');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_dest().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, dest: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
                })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false} )
            });
        }

    stopNavigation = (wpt) => {
        console.log('Clear detination', wpt);
        this.props.swaggerClient
            .then( client => {
                client.apis.nav.rest_api_clear_dest({},{})
                    .then(response => {
                        console.log(response);
                        this.requestWpts();
                    }).catch( error => {
                    console.log("API error" + error);
                })
            }).catch( error => {
            console.log("Client error" + error);
        });
    };

    toFixed = (val, dec) =>{
        if (val === null) return '';
        if (typeof val !== 'undefined') {
            return val.toFixed(dec);
        }else{
            return '---';
        }

    }

    render() {
        if (this.state.loading){
            return(<div> Loading ...</div>);
        }else if( this.state.ok ){
            const destSelected = 'name' in this.state.dest;
            if (destSelected) {
                const direction = this.state.dest.atw_up ? 'Up' : 'Down;'
                return (
                    <Paper>
                        <Paper>
                            <Typography variant="h5">{this.state.dest.name} {this.toFixed(Math.abs(this.state.dest.atw),1)}&#176; {direction} </Typography>
                        </Paper>

                        <TableContainer component={Paper}>
                            <Table size={'small'} >
                                <TableBody>
                                    <TableRow>
                                        <TableCell align="center"><Typography variant="h6">DTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.dest.dtw,3)}</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">BTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.dest.btw,0)}</Typography></TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <Paper>
                            <Button variant="contained" onClick={() => this.stopNavigation()}>Stop navigation</Button>
                        </Paper>
                    </Paper>
                );
            }else{
                return (
                    <Paper>No destination selected</Paper>
                );
            }
        }else{
            return (
                <Paper>Failed to fetch</Paper>
            );
        }
    }
}

DestInfo.propTypes = {};

export default DestInfo;
