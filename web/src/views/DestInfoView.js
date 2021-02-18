import React from 'react';
import {Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";
import {toFixed} from "./Utils";

function DestInfoView(props) {

    if (props.loading){
            return(<div> Loading ...</div>);
        }else if( props.ok ){
            const destSelected = 'name' in props.dest;
            if (destSelected) {
                const direction = props.dest.atw_up ? 'Up' : 'Down'
                return (
                    <Paper>
                        <Paper>
                            <Typography variant="h5">{props.dest.name} {toFixed(Math.abs(props.dest.atw),0)}&#176; {direction} </Typography>
                        </Paper>

                        <TableContainer component={Paper}>
                            <Table size={'small'} >
                                <TableBody>
                                    <TableRow>
                                        <TableCell align="center"><Typography variant="h6">DTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{toFixed(props.dest.dtw,2)}</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">BTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{toFixed(props.dest.btw,0)}&#176;</Typography></TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>
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


export default DestInfoView;
