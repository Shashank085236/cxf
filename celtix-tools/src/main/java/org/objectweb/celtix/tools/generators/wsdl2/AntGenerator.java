package org.objectweb.celtix.tools.generators.wsdl2;

import org.objectweb.celtix.tools.common.ProcessorEnvironment;
import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.common.ToolException;
import org.objectweb.celtix.tools.common.model.JavaModel;
import org.objectweb.celtix.tools.generators.AbstractGenerator;

public class AntGenerator extends AbstractGenerator {

    private static final String ANT_TEMPLATE = TEMPLATE_BASE + "/build.vm";

    public AntGenerator(JavaModel jmodel, ProcessorEnvironment env) {
        super(jmodel, env);
        this.name = ToolConstants.ANT_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_ANT)
                || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        return true;
    }

    public void generate() throws ToolException {
        if (passthrough()) {
            return;
        }
        clearAttributes();
        setAttributes("intfs", javaModel.getInterfaces().values());
        setAttributes("wsdlLocation", javaModel.getLocation());
        setCommonAttributes();

        doWrite(ANT_TEMPLATE, parseOutputName(null, "build", ".xml"));
    }
}
