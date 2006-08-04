package org.objectweb.celtix.helpers;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Sorts methods according to their name, number of parameters, and parameter
 * types.
 */
public class MethodComparator implements Comparator<Method> {

    public int compare(Method m1, Method m2) {

        int val = m1.getName().compareTo(m2.getName());
        if (val == 0) {
            val = m1.getParameterTypes().length - m2.getParameterTypes().length;
            if (val == 0) {
                Class[] types1 = m1.getParameterTypes();
                Class[] types2 = m2.getParameterTypes();
                for (int i = 0; i < types1.length; i++) {
                    val = types1[i].getName().compareTo(types2[i].getName());

                    if (val != 0) {
                        break;
                    }
                }
            }
        }
        return val;
    }

}
