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

function GithubReleasesView() {
    const classes = useStyles();
        return (
            <Paper>

                <Typography className={classes.root}>
                    <Link href="https://github.com/sergei/ottopi/releases/" color="inherit">
                        Released packages
                    </Link>
                    <IconButton color="primary" href="https://github.com/sergei/ottopi/releases/">
                        <GitHubIcon />
                    </IconButton>
                </Typography>
            </Paper>
        )
}

export default GithubReleasesView;
