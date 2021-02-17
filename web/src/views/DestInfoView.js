import React from 'react';
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";
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
                            <Typography variant="h5">{props.dest.name} {toFixed(Math.abs(props.dest.atw),1)}&#176; {direction} </Typography>
                        </Paper>

                        <TableContainer component={Paper}>
                            <Table size={'small'} >
                                <TableBody>
                                    <TableRow>
                                        <TableCell align="center"><Typography variant="h6">DTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{toFixed(props.dest.dtw,3)}</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">BTW</Typography></TableCell>
                                        <TableCell align="center"><Typography variant="h6">{toFixed(props.dest.btw,0)}</Typography></TableCell>
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


export default DestInfoView;
