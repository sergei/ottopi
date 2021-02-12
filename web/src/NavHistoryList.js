import React, {Component} from 'react';
import NavHistoryItem from "./NavHistoryItem";

class NavHistoryList extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
    };

    requestHistoryItems = () => {
        console.log('Fetching history items ');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_history().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, items: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    };

    componentDidMount() {
        this.requestHistoryItems();
    }

    render() {
        if( this.state.loading ) {
            return (<div>Loading items ...</div>)
        }else {
            let history_items = [];

            if( this.state.ok){
                let i;
                let prevTwa = 0;
                let courseChanged = false;
                const last_item = this.state.items.length - 1;
                for (i = last_item; i >= 0; i--) {
                    let twa = this.state.items[i].twa;
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
                    history_items.push( <NavHistoryItem {...this.state.items[i]} key={i} /> );
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
}

export default NavHistoryList;
