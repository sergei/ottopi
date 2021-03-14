import React from 'react';
import {Checkbox, FormControlLabel, FormGroup, Paper} from "@material-ui/core";

function SpeakerView(props) {

    if (props.loading) {
        return (<div> Loading ...</div>);
    }else{
        return (
            <Paper>
                <FormGroup row>
                    <FormControlLabel
                        control={<Checkbox checked={props.speakerState.performance}
                                           onChange={props.togglePerformance} name="Performance" />}
                        label="Performance Talk"
                    />
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={props.speakerState.route}
                                onChange={props.toggleRoute}
                                name="Route"
                            />
                        }
                        label="Route Announcements"
                    />
                </FormGroup>
            </Paper>
        )
    }
}

export default SpeakerView;
