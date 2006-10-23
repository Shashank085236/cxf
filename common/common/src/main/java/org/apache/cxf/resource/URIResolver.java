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
package org.apache.cxf.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

/**
 * Resolves a File, classpath resource, or URL according to the follow rules:
 * <ul>
 * <li>Check to see if a file exists, relative to the base URI.</li>
 * <li>If the file doesn't exist, check the classpath</li>
 * <li>If the classpath doesn't exist, try to create URL from the URI.</li>
 * </ul>
 * 
 * @author Dan Diephouse
 */
public class URIResolver {
    private File file;
    private URI uri;
    private InputStream is;
    private Class calling;
    
    public URIResolver(String path) throws IOException {
        this("", path);
    }

    public URIResolver(String baseUriStr, String uriStr) throws IOException {
        this(baseUriStr, uriStr, null);
    }
    
    public URIResolver(String baseUriStr, String uriStr, Class calling) throws IOException {
        this.calling = (calling != null) ? calling : getClass();

        if (uriStr.startsWith("classpath:")) {
            tryClasspath(uriStr);
        } else if (baseUriStr != null && baseUriStr.startsWith("jar:")) {
            tryJar(baseUriStr, uriStr);
        } else if (uriStr.startsWith("jar:")) {
            tryJar(uriStr);
        } else {
            tryFileSystem(baseUriStr, uriStr);
        }
    }

    private void tryFileSystem(String baseUriStr, String uriStr) throws IOException, MalformedURLException {
        try {
            URI relative;
            File uriFile = new File(uriStr);
            uriFile = new File(uriFile.getAbsolutePath());

            if (uriFile.exists()) {
                relative = uriFile.toURI();
            } else {
                relative = new URI(uriStr);
            }
            
            if (relative.isAbsolute()) {
                uri = relative;
                is = relative.toURL().openStream();
            } else if (baseUriStr != null) {
                URI base;
                File baseFile = new File(baseUriStr);

                if (!baseFile.exists() && baseUriStr.startsWith("file:/")) {
                    baseFile = new File(baseUriStr.substring(6));
                }

                if (baseFile.exists()) {
                    base = baseFile.toURI();
                } else {
                    base = new URI(baseUriStr);
                }
                
                base = base.resolve(relative);
                if (base.isAbsolute()) {
                    is = base.toURL().openStream();
                    uri = base;
                }
            }
        } catch (URISyntaxException e) {
            // do nothing
        }

        if (uri != null && "file".equals(uri.getScheme())) {
            file = new File(uri);
        }

        if (is == null && file != null && file.exists()) {
            uri = file.toURI();
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File was deleted! " + uriStr, e);
            }
        } else if (is == null) {
            tryClasspath(uriStr);
        }
    }

    private void tryJar(String baseStr, String uriStr) throws IOException {
        int i = baseStr.indexOf('!');
        if (i == -1) {
            tryFileSystem(baseStr, uriStr);
        }
        baseStr = baseStr.substring(i + 1);
        try {
            URI u = new URI(baseStr).resolve(uriStr);
            tryClasspath(u.toString());
            if (is != null) {
                return;
            }
        } catch (URISyntaxException e) {
            // do nothing
        }
        
        tryFileSystem("", uriStr);
    }
    
    private void tryJar(String uriStr) throws IOException {
        int i = uriStr.indexOf('!');
        if (i == -1) {
            return;
        }
        uriStr = uriStr.substring(i + 1);
        tryClasspath(uriStr);
    }
    private void tryClasspath(String uriStr) throws IOException {
        if (uriStr.startsWith("classpath:")) {
            uriStr = uriStr.substring(10);
        }

        URL url = ClassLoaderUtils.getResource(uriStr, calling);

        if (url == null) {
            tryRemote(uriStr);
        } else {
            try {
                uri = new URI(url.toString());
            } catch (URISyntaxException e) {
                // How would this occurr??
            }
            is = url.openStream();
        }
    }

    private void tryRemote(String uriStr) throws IOException {
        URL url;
        try {
            url = new URL(uriStr);
            uri = new URI(url.toString());
            is = url.openStream();
        } catch (MalformedURLException e) {
            // do nothing
        } catch (URISyntaxException e) {
            // do nothing
        }
    }

    public URI getURI() {
        return uri;
    }

    public InputStream getInputStream() {
        return is;
    }

    public boolean isFile() {
        return file.exists();
    }

    public File getFile() {
        return file;
    }
    
    public boolean isResolved() {
        return is != null;
    }
}
