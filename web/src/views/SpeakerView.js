import React from 'react';
import {Checkbox, FormControlLabel, FormGroup, Grid, makeStyles, Paper, Slider, Typography} from "@material-ui/core";
import {VolumeDown, VolumeUp} from "@material-ui/icons";

const useStyles = makeStyles({
    root: {

    },
});
function SpeakerView(props) {
    const classes = useStyles();

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

                <div className={classes.root}>
                    <Typography id="continuous-slider" gutterBottom>
                        Volume
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item>
                            <VolumeDown />
                        </Grid>
                        <Grid item xs>
                            <Slider value={props.volumeValue} onChange={props.onVolumeChange} aria-labelledby="continuous-slider" />
                        </Grid>
                        <Grid item>
                            <VolumeUp />
                        </Grid>
                    </Grid>
                </div>
            </Paper>
        )
    }
}

export default SpeakerView;
