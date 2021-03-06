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
package com.mulesoft.build.domain

import com.mulesoft.build.MulePluginConstants
import com.mulesoft.build.MulePluginConvention
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task in charge of verifying if subprojects have the right configuration to be deployed as part of this domain.
 *
 * Created by juancavallotti on 30/05/14.
 */
class CheckDomainTask extends DefaultTask {

    private static final Logger logger = LoggerFactory.getLogger(CheckDomainTask)

    CheckDomainTask() {
        description = "Verify that all modules in the project are correctly configured for the Domain."
        group = MulePluginConstants.MULE_GROUP
    }

    @TaskAction
    void checkSubprojects() {

        String domainName = project.mule.resolveDomainName()
        MulePluginConvention convention  = null

        project.subprojects.each {Project subproj ->
            convention = subproj.convention.findByType(MulePluginConvention)

            //where the app resides?
            File deployProps = subproj.file("${convention.appSourceDir}").listFiles().find({File file ->
                 file.name.equals('mule-deploy.properties')
            })

            if (!deployProps || !deployProps.exists()) {
                logger.warn("Cannot verify module ${subproj.name}, file mule-deploy.properties not present.")
                throw new IllegalStateException("mule-deploy.properties not found in module: ${subproj.name}")
            }

            Properties props = new Properties()
            props.load(deployProps.newInputStream())

            String domain = props['domain']

            if (!domain) {
                logger.warn("Property domain not present in deployment descriptor of module ${subproj.name}")
                throw new IllegalStateException("Module ${subproj.name} does not have the domain property in the deployment descriptor.")
            }

            if (!domain.equals(domainName)) {
                logger.warn("Module ${subproj.name} is not properly configured, expecting $domainName but domain is: $domain")
                throw new IllegalStateException("Module ${subproj.name} is configured with $domainName but current domain name is: $domain.")
            }

            logger.info("Module ${subproj.name} is correctly configured for domain $domainName.")
        }
    }

}
