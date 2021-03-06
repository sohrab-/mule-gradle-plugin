/*
 * Copyright 2014 juancavallotti.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mulesoft.build.cloudhub

import com.mulesoft.build.util.FileUtils
import com.mulesoft.build.util.HttpUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by juancavallotti on 06/06/14.
 */
class UploadToCloudhubTask extends DefaultTask {

    @TaskAction
    public void uploadToCloudhub() {

        logger.debug('Uploading archive to cloudhub...')

        //get the plugin convention
        CloudhubPluginConvention conv = project.convention.getByType(CloudhubPluginConvention)

        //get the extension
        CloudhubPluginExtension ext = project.extensions.getByType(CloudhubPluginExtension)

        //build the target uri
        String chApi = conv.clouduhbApiEndpoint

        //get the configuration where to upload the app.
        CloudhubEnvironment env = ext.resolveTargetDomain()

        if (env == null && ext.domains.isEmpty()) {
            logger.error('No environment has been configured, aborting...')
            throw new IllegalStateException('Could not find a configured cloudhub environment.')
        }

        if (env == null) {
            logger.error('Multiple environments found but none defined as default...')
            throw new IllegalStateException("Multiple cloudhub domains found but none selected as default: ${ext.domains.keySet()}")
        }

        //display the environment that will be used.
        if (logger.isInfoEnabled()) {
            logger.info("About to deploy to environment $env")
        }

        File uploadedFile = project.configurations.archives.allArtifacts.files.singleFile


        //try and upload the app to cloudhub
        String url = "$chApi/applications/$env.domainName/deploy"

        if (logger.isInfoEnabled()){
            logger.info("Will deploy file: $uploadedFile.absolutePath")
            logger.info("Will upload to artifact to url: $url")
        }

        //configure authentication
        HttpUtils.configureNetworkAuthenticator(env.username, env.password)

        //build an url connection.
        HttpURLConnection conn = url.toURL().openConnection()

        try {

            println "\t Uploading file $uploadedFile.name to URL: $url"

            //set the headers

            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestMethod('POST')
            conn.setRequestProperty('Authorization', HttpUtils.generateAuthenticationHeader(env.username, env.password))
            conn.setRequestProperty('Content-Type', 'application/octet-stream')

            //write the payload
            OutputStream os = conn.getOutputStream()

            FileUtils.copyStream(uploadedFile.newInputStream(), os)

            os.flush()
            os.close()

            //check the response
            if (conn.responseCode != 200) {

                switch (conn.responseCode) {
                    case 401:
                        logger.warn("Invalid credentials were used to upload the artifact, please verify your username and password and retry.")
                        break;
                    default:
                        logger.warn("Cloudhub responded with status code: $conn.responseCode")
                }
                throw new IllegalStateException('Deployment to cloudhub failed.')
            }

            println "\t Done!"

        } finally {
            conn.disconnect()
        }

    }

}
