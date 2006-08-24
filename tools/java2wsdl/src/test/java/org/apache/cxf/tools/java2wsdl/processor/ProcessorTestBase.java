package org.apache.cxf.tools.java2wsdl.processor;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.cxf.tools.common.ProcessorEnvironment;
import org.apache.cxf.tools.common.ToolException;

public class ProcessorTestBase extends TestCase {

    private static final int RETRY_SLEEP_MILLIS = 10;
    protected ProcessorEnvironment env = new ProcessorEnvironment();
    protected File output;

    public void setUp() throws Exception {
        URL url = ProcessorTestBase.class.getResource(".");
        output = new File(url.getFile());
        output = new File(output, "/resources");
        mkDir(output);
    }

    public void tearDown() {
        removeDir(output);
        output = null;
        env = null;
    }

    private void mkDir(File dir) {
        if (dir == null) {
            throw new ToolException("dir attribute is required");
        }

        if (dir.isFile()) {
            throw new ToolException("Unable to create directory as a file "
                                    + "already exists with that name: " + dir.getAbsolutePath());
        }

        if (!dir.exists()) {
            boolean result = doMkDirs(dir);
            if (!result) {
                String msg = "Directory " + dir.getAbsolutePath()
                             + " creation was not successful for an unknown reason";
                throw new ToolException(msg);
            }
        }
    }

    /**
     * Attempt to fix possible race condition when creating directories on
     * WinXP, also Windows2000. If the mkdirs does not work, wait a little and
     * try again.
     */
    private boolean doMkDirs(File f) {
        if (!f.mkdirs()) {
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
                return f.mkdirs();
            } catch (InterruptedException ex) {
                return f.mkdirs();
            }
        }
        return true;
    }

    private void removeDir(File d) {
        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                removeDir(f);
            } else {
                delete(f);
            }
        }
        delete(d);
    }

    private void delete(File f) {
        if (!f.delete()) {
            if (isWindows()) {
                System.gc();
            }
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        return osName.indexOf("windows") > -1;
    }

    protected String getClassPath() {
        ClassLoader loader = getClass().getClassLoader();
        StringBuffer classPath = new StringBuffer();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader)loader;
            for (URL url : urlLoader.getURLs()) {               
                File file = new File(url.getFile());
                String filename = file.getAbsolutePath();                
                if (filename.indexOf("junit") == -1) {
                    classPath.append(filename);
                    classPath.append(System.getProperty("path.separator"));
                }
            }
        }
        return classPath.toString();
    }

}
