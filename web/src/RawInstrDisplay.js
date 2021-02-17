import React from 'react';
import {Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";

class RawInstrDisplay extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        instr: null,
        response: null,
        timer: null,
    };

    componentDidMount() {
        this.updateInstrumentsStatus();
        let timer = setInterval(this.updateInstrumentsStatus, 5000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if( this.clearInterval )  // Prevent crash on sign out
            this.clearInterval(this.state.timer);
    }

    updateInstrumentsStatus = () => {
        console.log('Fetching raw instruments');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_raw_instr().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, instr: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
                })
            }).catch( error => {
                console.log("Client error" + error);
                this.setState( {loading:false, ok: false} )
            });
        }

    toFixed = (val, dec) =>{
        if (val === null) return '---';
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
            return (
                <TableContainer component={Paper}>
                    <Table size={'small'} >
                        <TableBody>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">AWS</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.aws,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">AWA</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.awa,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">TWS</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.tws,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">TWA</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.twa,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">SOW</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.sow,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">HDG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.hdg,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">SOG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.sog,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">COG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{this.toFixed(this.state.instr.cog,0)}</Typography></TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            );
        }else{
            return (
                <div>Failed to fetch</div>
            );
        }
    }
}

RawInstrDisplay.propTypes = {};

export default RawInstrDisplay;
