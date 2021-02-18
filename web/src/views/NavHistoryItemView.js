import React from 'react';
import Moment from 'react-moment';
import {TableCell, TableRow, Typography} from "@material-ui/core";

function NavHistoryItemView(props) {

    const is_starboard = props.twa >= 0;

    return (
        <TableRow>
            <TableCell align="center"><Typography variant="h6"><Moment date={props.utc} format="hh:mm:ss" /></Typography></TableCell>
            <TableCell align="center">
                <Typography variant="h6">{ is_starboard ? props.hdg.toFixed(0) : ''}</Typography>
            </TableCell>
            <TableCell align="center"><Typography variant="h6">{ !is_starboard ? props.hdg.toFixed(0) : ''}</Typography></TableCell>
        </TableRow>
    );
}
export default NavHistoryItemView;
