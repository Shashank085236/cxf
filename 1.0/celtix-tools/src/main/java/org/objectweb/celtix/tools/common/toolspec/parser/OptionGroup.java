package org.objectweb.celtix.tools.common.toolspec.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.tools.common.toolspec.Tool;

public class OptionGroup implements TokenConsumer {

    private static final Logger LOG = LogUtils.getL7dLogger(OptionGroup.class);
    private final Element element;

    private final List<Object> options = new ArrayList<Object>();

    public OptionGroup(Element el) {
        this.element = el;
        NodeList optionEls = element.getElementsByTagNameNS(Tool.TOOL_SPEC_PUBLIC_ID, "option");

        for (int i = 0; i < optionEls.getLength(); i++) {
            options.add(new Option((Element)optionEls.item(i)));
        }
    }

    public boolean accept(TokenInputStream args, Element result, ErrorVisitor errors) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Accepting token stream for optionGroup: " + this + ", tokens are now " + args
                     + ", running through " + options.size() + " options");
        }
        // Give all the options the chance to exclusively consume the given
        // string:
        boolean accepted = false;

        for (Iterator it = options.iterator(); it.hasNext();) {
            Option option = (Option)it.next();

            if (option.accept(args, result, errors)) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Option " + option + " accepted the token");
                }
                accepted = true;
                break;
            }
        }
        if (!accepted) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("No option accepted the token, returning");
            }
            return false;
        }

        return true;
    }

    public boolean isSatisfied(ErrorVisitor errors) {
        // Return conjunction of all isSatisfied results from every option
        for (Iterator it = options.iterator(); it.hasNext();) {
            if (!((Option)it.next()).isSatisfied(errors)) {
                return false;
            }
        }
        return true;
    }

    public String getId() {
        return element.getAttribute("id");
    }

    public String toString() {
        if (element.hasAttribute("id")) {
            return getId();
        } else {
            return super.toString();
        }
    }
}
