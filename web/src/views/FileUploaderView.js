import React, {Component} from 'react';
import axios from 'axios';
import Url from "url-parse";

function FileUploaderView(props){

    let uploadStatus;
    if (props.finished && props.success)
        uploadStatus = "Sucessfully uploaded";
    else if (props.finished && !props.success)
        uploadStatus = "Upload failed";
    else
        uploadStatus = "";

    if ( props.uploading){
        return (
            <div> Uploading ... </div>
        )
    }else {
        return (
            <div>
                <div>
                    <h3>
                        {props.label}
                    </h3>
                    <div>
                        <input type="file" onChange={props.onFileChange} />
                        <button onClick={props.onFileUpload}>
                            Upload
                        </button>
                    </div>
                    {uploadStatus}
                </div>
            </div>
        );
    }
}

export default FileUploaderView;
