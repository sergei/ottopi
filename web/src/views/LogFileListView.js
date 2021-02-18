import React from 'react';
import {Button, Paper} from "@material-ui/core";

function LogFileListView() {
        return (
            <Paper>
                <Button variant="contained" color="primary" href="all_logs.zip">
                    Download log files in one ZIP
                </Button>
            </Paper>
        )
}

export default LogFileListView;
