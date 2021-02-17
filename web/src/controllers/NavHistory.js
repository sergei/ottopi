import React, {Component} from 'react';
import NavHistoryListView from "../views/NavHistoryListView";

class NavHistory extends Component {
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
        return (
            <NavHistoryListView loading={this.state.loading}  ok={this.state.ok}
                                items={this.state.items}
            />
        );
    }
}

export default NavHistory;
