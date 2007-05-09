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

package org.apache.cxf.maven_plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.java2wsdl.JavaToWSDL;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.util.optional.NoExitSecurityManager;

/**
 * @goal java2wsdl
 * @description CXF Java To WSDL Tool
 */
public class Java2WSDLMojo extends AbstractMojo {
    /**
     * @parameter
     * @required
     */
    String className;

    /**
     * @parameter  expression="${project.build.outputDirectory}"
     * @required
     */
    String classpath;

    /**
     * @parameter
     */
    String outputFile;
    
    /**
     * @parameter
     */
    Boolean soap12;

    /**
     * @parameter
     */
    String targetNamespace;

    /**
     * @parameter
     */
    String serviceName;

    /**
     * @parameter
     */
    Boolean verbose;

    /**
     * @parameter
     */
    Boolean quiet;

    /**
     * @parameter  expression="${project.compileClasspathElements}"
     * @required
     */
    List classpathElements;

    /**
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;

    public void execute() throws MojoExecutionException {
        File classesDir = new File(classpath);
        FileUtils.mkDir(classesDir);

        StringBuffer buf = new StringBuffer();
        for (Object classpathElement : classpathElements) {
            buf.append(classpathElement.toString());
            buf.append(File.pathSeparatorChar);
        }
        String newCp = buf.toString();

        String cp = System.getProperty("java.class.path");
        SecurityManager oldSm = System.getSecurityManager();
        try {
            System.setProperty("java.class.path", newCp);
            System.setSecurityManager(new NoExitSecurityManager());
            processJavaClass();
        } finally {
            System.setSecurityManager(oldSm);
            System.setProperty("java.class.path", cp);
        }

        System.gc();
    }

    private void processJavaClass() throws MojoExecutionException {
        List<String> args = new ArrayList<String>();

        // outputfile arg
        if (outputFile == null && project != null) {
            // Put the wsdl in target/generated/wsdl
            int i = className.lastIndexOf('.');
            // Prone to OoBE, but then it's wrong anyway
            String name = className.substring(i + 1); 
            outputFile = (project.getBuild().getDirectory() + "/generated/wsdl/" + name + ".wsdl")
                .replace("/", File.separator);
        }
        if (outputFile != null) {
            // JavaToWSDL freaks out if the directory of the outputfile doesn't exist, so lets
            // create it since there's no easy way for the user to create it beforehand in maven
            FileUtils.mkDir(new File(outputFile).getParentFile());
            args.add("-o");
            args.add(outputFile);

            /*
              Contributor's comment:
              Sometimes JavaToWSDL creates Java code for the wrappers.  I don't *think* this is
              needed by the end user.
            */
            
            // Commiter's comment:
            // Yes, it's required, it's defined in the JAXWS spec.

            if (project != null) {
                project.addCompileSourceRoot(new File(outputFile).getParentFile().getAbsolutePath());
            }
        }

        // classpath arg
        args.add("-cp");
        args.add(classpath);

        // soap12 arg
        if (soap12 != null && soap12.booleanValue()) {
            args.add("-soap12");
        }

        // target namespace arg
        if (targetNamespace != null) {
            args.add("-t");
            args.add(targetNamespace);
        }

        // servicename arg
        if (serviceName != null) {
            args.add("-servicename");
            args.add(serviceName);
        }

        // verbose arg
        if (verbose != null && verbose.booleanValue()) {
            args.add("-verbose");
        }

        // quiet arg
        if (quiet != null && quiet.booleanValue()) {
            args.add("-quiet");
        }

        // classname arg
        args.add(className);

        String exitOnFinish = System.getProperty("exitOnFinish", "");
        try {
            JavaToWSDL.main(args.toArray(new String[args.size()]));
        } catch (ExitException e) {
            if (e.getStatus() != 0) {
                throw e;
            }
        } finally {
            System.setProperty("exitOnFinish", exitOnFinish);
        }
    }
}
