import React, {Component} from 'react';
import axios from 'axios';
import Url from "url-parse";

class GpxUploader extends Component {
    state = {
        // Initially, no file is selected
        selectedFile: null,
        uploading: false,
        finished: false,
        success: null
    };

    // On file select (from the pop up)
    onFileChange = event => {
        this.setState({ selectedFile: event.target.files[0] });
    };

    // On file upload (click the upload button)
    onFileUpload = () => {

        // Create an object of formData
        const formData = new FormData();

        // Update the formData object
        formData.append(
            "fileName",
            this.state.selectedFile,
            this.state.selectedFile.name
        );

        // Details of the uploaded file
        console.log(this.state.selectedFile);

        this.props.swaggerClient.then(client => {
            let uploadUrl = new Url(client.url);
            uploadUrl.set('pathname','gpx');
            axios.post(uploadUrl.toString(), formData).then((response) => {
                console.log(response);
                this.setState({ uploading: false, finished: true, success: true });
            }, (error) => {
                this.setState({ uploading: false, finished: true, success: false });
            });
        });
        this.setState({ uploading: true, success: null });
    };

    render() {
        let uploadStatus;
        if (this.state.finished && this.state.success)
            uploadStatus = "Sucessfully uploaded";
        else if (this.state.finished && !this.state.success)
            uploadStatus = "Upload failed";
        else
            uploadStatus = "";

        if ( this.state.uploading){
            return (
                <div> Uploading ... </div>
            )
        }else {
            return (
                <div>
                    <div>
                        <h3>
                            Upload GPX file
                        </h3>
                        <div>
                            <input type="file" onChange={this.onFileChange} />
                            <button onClick={this.onFileUpload}>
                                Upload
                            </button>
                        </div>
                        {uploadStatus}
                    </div>
                </div>
            );
        }
    }
}

export default GpxUploader;