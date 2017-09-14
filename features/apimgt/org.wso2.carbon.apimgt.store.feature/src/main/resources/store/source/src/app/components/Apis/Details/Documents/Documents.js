/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {Component} from 'react'
import API from '../../../../data/api'
import {Button, message} from 'antd';
import DocumentsTable from './DocumentsTable';
import Loading from '../../../Base/Loading/Loading'


/*
 Documents tab related React components.
 # Component hierarchy
 -Documents
    -DocumentsTable
        -InlineEditor
    -NewDocDiv
        -NewDocInfoDiv
        -NewDocSourceDiv
 */
class Documents extends Component {
    constructor(props) {
        super(props);
        this.client = new API();
        this.api_id = this.props.match.params.api_uuid;
        //New or editing documents' information are maintained in state
        // (docName, documentId, docSourceType, docSourceURL, docFilePath, docSummary, docFile)
        this.state = {
            docName: "",
            documentId: "",
            docSourceType: "INLINE",
            docSourceURL: "",
            docFilePath: null,
            docSummary: "",
            docFile: null,
            addingNewDoc: false,
            documentsList: null,
            updatingDoc: false
        };
        this.initialDocSourceType = null;
    }

    componentDidMount() {
        const api = new API();
        let promised_api = api.get(this.api_id);
        promised_api.then(
            response => {
                this.setState({api: response.obj});
            }
        ).catch(
            error => {
                if (process.env.NODE_ENV !== "production") {
                    console.log(error);
                }
                let status = error.status;
                if (status === 404) {
                    this.setState({notFound: true});
                }
            }
        );
        this.getDocumentsList();
    }

    /*
     Get the document list attached to current API and set it to the state
     */
    getDocumentsList() {
        let docs = client.getAPIById(this.api_uuid).getDocuments();
        docs.then(response => {
            this.setState({documentsList: response.obj.list});
        }).catch(error_response => {
            let error_data = JSON.parse(error_response.message);
            let messageTxt = "Error[" + error_data.code + "]: " + error_data.description + " | " + error_data.message + ".";
            console.error(messageTxt);
            message.error("Error in fetching documents list of the API");
        });
    }


    /*
     Download the document related file
     */
    downloadFile(response) {
        let fileName = "";
        const contentDisposition = response.headers["content-disposition"];

        if (contentDisposition && contentDisposition.indexOf('attachment') !== -1) {
            const fileNameReg = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
            const matches = fileNameReg.exec(contentDisposition);
            if (matches != null && matches[1]) fileName = matches[1].replace(/['"]/g, '');
        }
        const contentType = response.headers["content-type"];
        const blob = new Blob([response.data], {
            type: contentType
        });
        if (typeof window.navigator.msSaveBlob !== 'undefined') {
            window.navigator.msSaveBlob(blob, fileName);
        } else {
            const URL = window.URL || window.webkitURL;
            const downloadUrl = URL.createObjectURL(blob);

            if (fileName) {
                const aTag = document.createElement("a");
                if (typeof aTag.download === 'undefined') {
                    window.location = downloadUrl;
                } else {
                    aTag.href = downloadUrl;
                    aTag.download = fileName;
                    document.body.appendChild(aTag);
                    aTag.click();
                }
            } else {
                window.location = downloadUrl;
            }

            setTimeout(function () {
                URL.revokeObjectURL(downloadUrl);
            }, 100);
        }
    }

    render() {
        if (!this.state.api) {
            return <Loading/>
        }
        return (
            <div>
              {/* Allowing adding doc to an API based on scopes */}
                <div>
                    {(this.state.addingNewDoc || this.state.updatingDoc) && <div>test</div>}

                </div>
                <hr color="#f2f2f2"/>
                {
                    (this.state.documentsList && (this.state.documentsList.length > 0) ) ? (
                        <DocumentsTable apiId={this.api_id}
                                        client={this.client}
                                        documentsList={this.state.documentsList}
                                        viewDocContentHandler={this.viewDocContentHandler}
                                        downloadFile={this.downloadFile}
                        /> ) :
                        (<div style={{paddingTop: 20}}><p>No documents added into the API</p></div>)
                }
            </div>
        );
    }
}

export default Documents;
