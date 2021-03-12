import React, {Component} from 'react';
import axios from 'axios';
import Url from "url-parse";
import FileUploaderView from "../views/FileUploaderView";

class FileUploader extends Component {
    state = {
        // Initially, no file is selected
        selectedFile: null,
        selectedFileName: null,
        uploading: false,
        finished: false,
        success: null,
        errorMessage: null,
    };

    // On file select (from the pop up)
    onFileChange = event => {
        this.setState({
            selectedFile: event.target.files[0],
            selectedFileName: event.target.files[0].name,
        });
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
            uploadUrl.set('pathname', this.props.uploadPath);
            axios.post(uploadUrl.toString(), formData).then((response) => {
                console.log(response);
                this.setState({ uploading: false, finished: true, success: true });
            }, (error) => {
                console.log('Response', error.response.data);
                console.log('Status', error.response.status);
                this.setState({ uploading: false, finished: true, success: false,
                    errorMessage:error.response.data });
            });
        });
        this.setState({ uploading: true, success: null });
    };

    render() {
        return ( <FileUploaderView uploadPath={this.props.uploadPath} label={this.props.label}
                                   finished={this.state.finished} selectedFileName={this.state.selectedFileName}
                                   success={this.state.success} uploading={this.state.uploading}
                                   errorMessage={this.state.errorMessage}
                                   onFileChange={this.onFileChange} onFileUpload={this.onFileUpload}
        />);
    }
}

export default FileUploader;