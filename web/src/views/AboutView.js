import React from 'react';
import {IconButton, Link, makeStyles, Paper, Typography} from "@material-ui/core";
import GitHubIcon from '@material-ui/icons/GitHub';

const useStyles = makeStyles((theme) => ({
    root: {
        '& > * + *': {
            marginLeft: theme.spacing(2),
        },
    },
}));

function AboutView(props) {
    const classes = useStyles();

    if (props.loading) {
        return (<div> Loading ...</div>);
    }else{
        return (
            <Paper>
                <Typography className={classes.root}>
                        Otto Pi v{props.version}
                </Typography>
                <Typography className={classes.root}>
                    <Link href="https://github.com/sergei/ottopi/releases/" color="inherit">
                        See all releases
                    </Link>
                    <IconButton color="primary" href="https://github.com/sergei/ottopi/releases/">
                        <GitHubIcon />
                    </IconButton>
                </Typography>
            </Paper>
        )
    }
}

export default AboutView;
