import React from 'react';
import NavHistoryItemView from "./NavHistoryItemView";
import {Paper, Table, TableBody, TableContainer} from "@material-ui/core";

function NavHistoryListView(props){

        if( props.loading ) {
            return (<div>Loading items ...</div>)
        }else {
            let history_items = [];

            if( props.ok ){
                let i;
                const last_item = props.items.length - 1;
                for (i = last_item; i >= 0; i--) {
                    history_items.push(
                        <NavHistoryItemView {...props.items[i]} key={i} />
                        );
                }
            }else{
                history_items = 'Failed to fetch nav history';
            }
            return (
                <TableContainer component={Paper}>
                    <Table size={'small'} >
                        <TableBody>
                    {history_items}
                        </TableBody>
                    </Table>
                </TableContainer>
            )
        }
}

export default NavHistoryListView;
