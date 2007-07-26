/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
package org.apache.cxf.ant.extensions;

import java.io.File;
import java.io.IOException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.java2wsdl.JavaToWSDL;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

public class Java2WSDLTask extends CxfAntTask {

    private Path classpath;

    private File wsdlDir;
    private String protocol;
    private String className;

    public void setGenwsdl(boolean gw) {
        //the point of this is to gen the wsdl, always do it
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }
    public void setClasspath(Path c) {
        if (classpath == null) {
            classpath = c;
        } else {
            classpath.append(c);
        }
    }
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }


    public void setProtocol(String p) {
        protocol = p;
    }

    public void setResourcedestdir(File f) {
        wsdlDir = f;
    }

    public void setSei(String clz) {
        className = clz;
    }

    public void execute() throws BuildException {
        buildCommandLine();

        LogStreamHandler log = new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN);
        Execute exe = new Execute(log);
        exe.setAntRun(getProject());
        exe.setCommandline(cmd.getCommandline());
        try {
            int rc = exe.execute();
            if (exe.killedProcess()
                || rc != 0) {
                throw new BuildException("java2wsdl failed", getLocation());
            }
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    public void buildCommandLine() {
        ClassLoader loader = this.getClass().getClassLoader();
        Path runCp = new Path(getProject());
        if (loader instanceof AntClassLoader) {
            runCp = new Path(getProject(), ((AntClassLoader)loader).getClasspath());
        }
        cmd.createClasspath(getProject()).append(runCp);
        cmd.createVmArgument().setLine("-Djava.util.logging.config.file=");

        cmd.setClassname(JavaToWSDL.class.getName());

        if (classpath != null && !classpath.toString().equals("")) {
            cmd.createArgument().setValue("-cp");
            cmd.createArgument().setPath(classpath);
        }


        if (null != classesDir
            && !StringUtils.isEmpty(classesDir.getName())) {
            cmd.createArgument().setValue("-classdir");
            cmd.createArgument().setFile(classesDir);
        }
        if (null != sourcesDir
            && !StringUtils.isEmpty(sourcesDir.getName())) {
            cmd.createArgument().setValue("-s");
            cmd.createArgument().setFile(sourcesDir);
        }

        // verbose option
        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        if ("Xsoap1.2".equals(protocol)) {
            cmd.createArgument().setValue("-soap12");
        }

        cmd.createArgument().setValue("-createxsdimports");

        if (null != wsdlDir
            && !StringUtils.isEmpty(wsdlDir.getName())) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(wsdlDir);
        }


        if (!StringUtils.isEmpty(className)) {
            cmd.createArgument().setValue(className);
        }
    }
}
