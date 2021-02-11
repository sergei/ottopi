import React, {Component} from 'react';
import RoutePoint from "./RoutePoint";

class Route  extends Component {

    // State of this component
    state = {
        collapsed: true, // Don't show individual waypoints
    };

    render (){
        console.log("Route");
        console.log(this.props);

        let wpts = this.props.wpts.map( (wpt, i) => (
                <RoutePoint {...wpt} routeIdx={this.props.routeIdx} wptIdx={i} selectRoute={this.props.selectRoute}/>
            )
        );

        if ( this.state.collapsed) {
            return (
                <div>
                    <button onClick={ () => this.setState({collapsed: false}) }>Show WPTs</button>
                    {this.props.name}
                    <button onClick={ () => this.props.selectRoute(this.props.routeIdx, 0)}>Activate</button>
                </div>
            );
        }else{
            return (
                <div>
                    <button onClick={ () => this.setState({collapsed: true}) }>Hide WPTs</button>
                    {this.props.name}
                    {wpts}
                </div>
            );
        }
    }
}

export default Route;
