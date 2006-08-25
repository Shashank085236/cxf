package org.objectweb.celtix.tools.processors.wsdl2.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

public class TypesCodeWriter extends CodeWriter {

    /** The target directory to put source code. */
    private File target;
    
    private List<String> excludeFileList = new ArrayList<String>();
    private List<String> excludePkgList;

    public TypesCodeWriter(File ftarget, List<String> excludePkgs) throws IOException {
        target = ftarget;
        excludePkgList = excludePkgs;
    }

    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
        return new FileOutputStream(getFile(pkg, fileName));
    }

    protected File getFile(JPackage pkg, String fileName) throws IOException {
        String dirName = pkg.name().replace('.', File.separatorChar);
        File dir = pkg.isUnnamed() ? target : new File(target, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File fn = new File(dir, fileName);
        if (excludePkgList.contains(pkg.name())) {
            excludeFileList.add(dirName + File.separator + fileName);
        }
        if (fn.exists() && !fn.delete()) {
            throw new IOException(fn + ": Can't delete previous version");
        }
        return fn;
    }

    public void close() throws IOException {

    }
    
    public List<String> getExcludeFileList() {
        return excludeFileList;
    }
    
}
