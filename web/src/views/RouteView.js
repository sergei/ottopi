import React from 'react';
import RoutePointView from "./RoutePointView";
import {Collapse, ListItem, ListItemText} from "@material-ui/core";
import List from "@material-ui/core/List";
import {ExpandLess, ExpandMore} from "@material-ui/icons";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";

function RouteView(props) {
    const [open, setOpen] = React.useState(false);

    const handleClick = () => {
        setOpen(!open);
    };
    console.log("RouteView");

    const wpts = props.wpts.map( (wpt, i) => (
            <RoutePointView {...wpt} routeIdx={props.routeIdx} wptIdx={i} key={i}
                            selectRoute={props.selectRoute}/>
        )
    );

    return (
        <div>
            <ListItem  button onClick={handleClick}>
                {props.active ? <ChevronRightIcon/> : ''}
                <ListItemText primary={'Route ' + props.name} />
                {open ? <ExpandLess /> : <ExpandMore />}
            </ListItem>
            <Collapse in={open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                    {wpts}
                </List>
            </Collapse>
        </div>
    );
}

export default RouteView;
