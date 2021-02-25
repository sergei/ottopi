import React from 'react';
import {Button, Paper, Typography} from "@material-ui/core";
import {useStyles} from "./RouteListView";

function FileUploaderView(props){
    const classes = useStyles();

    let uploadStatus;
    if (props.finished && props.success)
        uploadStatus = "Sucessfully uploaded";
    else if (props.finished && !props.success)
        uploadStatus = "Upload failed";
    else
        uploadStatus = "";

    const uploadButton = (props) => {
        return (
            <span>
                <Button onClick={props.onFileUpload} variant="contained" color="primary" component="span">
                    Upload
                </Button>
                {props.selectedFileName}
            </span>
        )
    };

    if ( props.uploading){
        return (
            <div> Uploading ... </div>
        )
    }else {
        const fileSelected = props.selectedFileName !== null;
        return (
            <Paper>
                    <input
                        className={classes.input}
                        style={{ display: 'none' }}
                        id={props.uploadPath}
                        type="file"
                        onChange={props.onFileChange}
                    />

                    <label htmlFor={props.uploadPath}>
                        <Button variant="contained" color="secondary"  component="span" className={classes.button}>
                            {props.label}
                        </Button>
                    </label>

                    {fileSelected ? uploadButton(props) : ''}
                    <Typography variant="body1">{uploadStatus}</Typography>

            </Paper>
        );
    }
}

export default FileUploaderView;
