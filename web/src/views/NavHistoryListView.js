import React from 'react';
import NavHistoryItemView from "./NavHistoryItemView";

function NavHistoryListView(props){

        if( props.loading ) {
            return (<div>Loading items ...</div>)
        }else {
            let history_items = [];

            if( props.ok ){
                let i;
                let prevTwa = 0;
                let courseChanged = false;
                const last_item = props.items.length - 1;
                for (i = last_item; i >= 0; i--) {
                    let twa = props.items[i].twa;
                    if ( i === last_item ){
                        courseChanged = true;
                    }else{
                        courseChanged = Math.abs(twa - prevTwa) > 45;
                    }
                    if ( courseChanged) {
                        prevTwa =  twa;
                        const tack = twa >= 0 ? 'starboard' : 'port';
                        twa = Math.abs(twa)
                        const upDown = twa <= 90 ? 'up wind' : 'downwind';
                        history_items.push( <div> {upDown} {tack} </div>);
                    }
                    history_items.push( <NavHistoryItemView {...props.items[i]} key={i} /> );
                }
            }else{
                history_items = 'Failed to fetch nav history';
            }
            return (
                <div>
                    {history_items}
                </div>
            )
        }
}

export default NavHistoryListView;
