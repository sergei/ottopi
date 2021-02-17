import React from 'react';
import Moment from 'react-moment';

function NavHistoryItemView(props) {
    return (
        <div>
            HDG {props.hdg.toFixed(0)}
            ( <Moment date={props.utc} format="hh:mm:ss" /> )
        </div>
    );
}
export default NavHistoryItemView;
