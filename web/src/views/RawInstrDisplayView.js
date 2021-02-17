import React from 'react';
import {Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";
import {toFixed} from "./Utils";

function RawInstrDisplayView(props) {

        if (props.loading){
            return(<div> Loading ...</div>);
        }else if( props.ok ){
            return (
                <TableContainer component={Paper}>
                    <Table size={'small'} >
                        <TableBody>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">AWS</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.aws,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">AWA</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.awa,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">TWS</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.tws,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">TWA</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.twa,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">SOW</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.sow,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">HDG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.hdg,0)}</Typography></TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center"><Typography variant="h6">SOG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.sog,1)}</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">COG</Typography></TableCell>
                                <TableCell align="center"><Typography variant="h6">{toFixed(props.instr.cog,0)}</Typography></TableCell>
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

export default RawInstrDisplayView;
