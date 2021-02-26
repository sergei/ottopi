import React from 'react';
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from "@material-ui/core";
import {formatDuration} from "./Utils";

function RaceTimerView(props){

    function startButton (props){
        return(
            <TableRow>
                <TableCell align="center" colspan={2} >
                    <Button onClick={() => props.start()} variant="contained" color="primary">
                        Start
                    </Button>
                </TableCell>
            </TableRow>
        );
    }

    function stopButton (props){
        return(
            <TableRow>
                <TableCell align="center" colspan={2} >
                    <Button onClick={() => props.stop()} variant="contained" color="primary">
                        Stop
                    </Button>
                </TableCell>
            </TableRow>
        );
    }

    function syncAndStopButtons (props){
        return(
            <TableRow>
                <TableCell align="center">
                    <Button onClick={() => props.sync()} variant="contained" color="primary">
                        Sync
                    </Button>
                </TableCell>
                <TableCell align="center">
                    <Button onClick={() => props.stop()} variant="contained" color="primary">
                        Stop
                    </Button>
                </TableCell>
            </TableRow>
        );
    }

    if( props.loading ) {
        return (<div>Sending command ...</div>)
    } else {
        let elapsed_time_sec = 300 ;
        if (props.elapsed_time !== undefined)
            elapsed_time_sec = Math.abs(props.elapsed_time) ;
        const pre_start = props.is_running && (props.elapsed_time < 0);



        return (
            <TableContainer component={Paper}>
                <Table size={'small'} >
                    <TableBody>
                        <TableRow>
                            <TableCell colspan={2} align="center">
                                <Typography variant="h1">{formatDuration(elapsed_time_sec)}</Typography>
                            </TableCell>
                        </TableRow>
                        {!props.is_running ? startButton(props) : ''}
                        {pre_start ? syncAndStopButtons(props) : ''}
                        {!pre_start && props.is_running ? stopButton(props) : ''}
                    </TableBody>
                </Table>
            </TableContainer>
        );
    }
}

export default RaceTimerView;
