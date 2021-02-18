import React from 'react';
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableRow} from "@material-ui/core";

function AutopilotView(props){
        if( props.loading ) {
            return (<div>Sending command ...</div>)
        } else {
            return (
                <TableContainer component={Paper}>
                    <Table size={'small'} >
                        <TableBody>
                            <TableRow>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(-1)} variant="contained" color="primary">
                                        1 deg left
                                    </Button>
                                </TableCell>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(1)} variant="contained" color="primary">
                                        1 deg right
                                    </Button>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(-5)} variant="contained" color="primary">
                                        5 deg left
                                    </Button>
                                </TableCell>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(5)} variant="contained" color="primary">
                                        5 deg right
                                    </Button>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(-10)} variant="contained" color="primary">
                                        10 deg left
                                    </Button>
                                </TableCell>
                                <TableCell align="center">
                                    <Button onClick={() => props.turn(10)} variant="contained" color="primary">
                                        10 deg right
                                    </Button>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell colSpan={2} align="center">
                                    <Button onClick={() => props.tack()} variant="contained" color="primary">
                                        Tack
                                    </Button>
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            );
    }
}

export default AutopilotView;
