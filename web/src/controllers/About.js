import React from 'react';
import AboutView from "../views/AboutView";

class About extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        version: null,
    };

    componentDidMount() {
        this.getAboutInfo();
    }

    getAboutInfo = () => {

        this.props.swaggerClient.then(client => {
            console.log("OpenAPi client");
            console.log(client)
            this.setState( {loading:false, version: client.spec.info.version} )
        });

    }

    render() {
        return ( <AboutView loading={this.state.loading}  version={this.state.version}/>);
    }
}

export default About;
