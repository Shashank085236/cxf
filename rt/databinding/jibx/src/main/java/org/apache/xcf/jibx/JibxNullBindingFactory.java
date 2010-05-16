package org.apache.xcf.jibx;

import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.impl.BindingFactoryBase;

/**
 * Dummy binding factory for when the generated Axis2 linkage code uses only simple value
 * conversions (and hence doesn't need a real JiBX binding).
 */
public class JibxNullBindingFactory extends BindingFactoryBase implements IBindingFactory {

    private static final String[] EMPTY_ARRAY = new String[0];
    
    public JibxNullBindingFactory() {
        super("null", 0, 0, "", "", "", "", EMPTY_ARRAY, EMPTY_ARRAY, "", "",
            EMPTY_ARRAY, "", "", "", "", "", EMPTY_ARRAY);
    }

    public String getCompilerDistribution() {
        // normally only used by BindingDirectory code, so okay to punt
        return "";
    }

    public int getCompilerVersion() {
        // normally only used by BindingDirectory code, so okay to punt
        return 0;
    }

    public int getTypeIndex(String type) {
        return -1;
    }
}