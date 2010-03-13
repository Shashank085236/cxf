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
package org.apache.cxf.jaxrs.ext.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple search condition comparing primitive objects or complex object by its getters. For details see
 * {@link #isMet(Object)} description.
 * 
 * @param <T> type of search condition.
 */
public class SimpleSearchCondition<T> implements SearchCondition<T> {

    private static Set<ConditionType> supportedTypes = new HashSet<ConditionType>();
    static {
        supportedTypes.add(ConditionType.EQUALS);
        supportedTypes.add(ConditionType.NOT_EQUALS);
        supportedTypes.add(ConditionType.GREATER_THAN);
        supportedTypes.add(ConditionType.GREATER_OR_EQUALS);
        supportedTypes.add(ConditionType.LESS_THAN);
        supportedTypes.add(ConditionType.LESS_OR_EQUALS);
    }
    private ConditionType cType;
    private T condition;
    private Map<String, ConditionType> getters2operators;
    private Map<String, Object> getters2values;
    private Beanspector<T> beanspector;

    /**
     * Creates search condition with same operator (equality, inequality) applied in all comparison; see
     * {@link #isMet(Object)} for details of comparison.
     * 
     * @param cType shared condition type
     * @param condition template object
     */
    public SimpleSearchCondition(ConditionType cType, T condition) {
        if (cType == null) {
            throw new IllegalArgumentException("cType is null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition is null");
        }
        if (!supportedTypes.contains(cType)) {
            throw new IllegalArgumentException("unsupported condition type: " + cType.name());
        }
        this.cType = cType;
        this.getters2operators = null;
        this.condition = condition;
    }

    /**
     * Creates search condition with different operators (equality, inequality etc) specified for each getter;
     * see {@link #isMet(Object)} for details of comparison. Cannot be used for primitive T type due to
     * per-getter comparison strategy.
     * 
     * @param getters2operators getters names and operators to be used with them during comparison
     * @param condition template object
     */
    public SimpleSearchCondition(Map<String, ConditionType> getters2operators, T condition) {
        if (getters2operators == null) {
            throw new IllegalArgumentException("getters2operators is null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition is null");
        }
        if (isPrimitive(condition)) {
            throw new IllegalArgumentException("mapped operators strategy is "
                                               + "not supported for primitive type "
                                               + condition.getClass().getName());
        }
        for (ConditionType ct : getters2operators.values()) {
            if (!supportedTypes.contains(ct)) {
                throw new IllegalArgumentException("unsupported condition type: " + ct.name());
            }
        }
        this.cType = null;
        this.getters2operators = getters2operators;
        this.condition = condition;
    }

    public T getCondition() {
        return condition;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When constructor with map is used it returns null.
     */
    public ConditionType getConditionType() {
        return cType;
    }

    public List<SearchCondition<T>> getConditions() {
        return null;
    }

    /**
     * Compares given object against template condition object.
     * <p>
     * For primitive type T like String, Number (precisely, from type T located in subpackage of
     * "java.lang.*") given object is directly compared with template object. Comparison for
     * {@link ConditionType#EQUALS} requires correct implementation of {@link Object#equals(Object)}, using
     * inequalities requires type T implementing {@link Comparable}.
     * <p>
     * For other types comparison of given object against template object is done using these <b>getters</b>;
     * returned "is met" value is <b>conjunction ('and' operator)</b> of comparisons per each getter. Getters
     * of template object that return null or throw exception are not used in comparison, in extreme if all
     * getters are excluded it means every given pojo object matches. If
     * {@link #SimpleSearchCondition(ConditionType, Object) constructor with shared operator} was used, then
     * getters are compared using the same operator. If {@link #SimpleSearchCondition(Map, Object) constructor
     * with map of operators} was used then for every getter specified operator is used (getters for missing
     * mapping are ignored). The way that comparison per getter is done depends on operator type per getter -
     * comparison for {@link ConditionType#EQUALS} requires correct implementation of
     * {@link Object#equals(Object)}, using inequalities requires that getter type implements
     * {@link Comparable}.
     * <p>
     * For equality comparison and String type in template object (either being primitive or getter from
     * complex type) it is allowed to used asterisk at the beginning or at the end of text as wild card (zero
     * or more of any characters) e.g. "foo*", "*foo" or "*foo*". Inner asterisks are not interpreted as wild
     * cards.
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * SimpleSearchCondition&lt;Integer&gt; ssc = new SimpleSearchCondition&lt;Integer&gt;(
     *   ConditionType.GREATER_THAN, 10);    
     * ssc.isMet(20);
     * // true since 20&gt;10 
     * 
     * class Entity {
     *   public String getName() {...
     *   public int getLevel() {...
     *   public String getMessage() {...
     * }
     * 
     * Entity template = new Entity("bbb", 10, null);
     * ssc = new SimpleSearchCondition&lt;Entity&gt;(
     *   ConditionType.GREATER_THAN, template);    
     * 
     * ssc.isMet(new Entity("aaa", 20, "some mesage")); 
     * // false: is not met, expression '"aaa"&gt;"bbb" and 20&gt;10' is not true  
     * // since "aaa" is not greater than "bbb"; not that message is null in template hence ingored
     * 
     * ssc.isMet(new Entity("ccc", 30, "other message"));
     * // true: is met, expression '"ccc"&gt;"bbb" and 30&gt;10' is true
     * 
     * Map&lt;String,ConditionType&gt; map;
     * map.put("name", ConditionType.EQUALS);
     * map.put("level", ConditionType.GREATER_THAN);
     * ssc = new SimpleSearchCondition&lt;Entity&gt;(
     *   ConditionType.GREATER_THAN, template);
     *   
     * ssc.isMet(new Entity("ccc", 30, "other message"));
     * // false due to expression '"aaa"=="ccc" and 30&gt;10"' (note different operators)
     * 
     * </pre>
     * 
     * @throws IllegalAccessException when security manager disallows reflective call of getters.
     */
    public boolean isMet(T pojo) {
        if (isPrimitive(pojo)) {
            return compare(pojo, cType, condition);
        } else {
            boolean matches = false;
            Map<String, Object> get2val = getGettersAndValues();
            for (String getter : get2val.keySet()) {
                ConditionType ct = cType;
                if (ct == null) {
                    ct = getters2operators.get(getter);
                    if (ct == null) {
                        continue;
                    }
                }
                Object lval = getValue(getter, pojo);
                Object rval = get2val.get(getter);
                matches = compare(lval, ct, rval);
                if (!matches) {
                    break;
                }
            }
            return matches;
        }
    }

    /**
     * Creates cache of getters from template (condition) object and its values returned during one-pass
     * invocation. Method isMet() will use its keys to introspect getters of passed pojo object, and values
     * from map in comparison.
     * 
     * @return template (condition) object getters mapped to their non-null values
     */
    private Map<String, Object> getGettersAndValues() {
        if (getters2values == null) {
            getters2values = new HashMap<String, Object>();
            beanspector = new Beanspector<T>(condition);
            for (String getter : beanspector.getGettersNames()) {
                Object value = getValue(getter, condition);
                getters2values.put(getter, value);
            }
            //we do not need compare class objects
            getters2values.keySet().remove("class");
        }
        return getters2values;
    }

    private Object getValue(String getter, T pojo) {
        try {
            return beanspector.swap(pojo).getValue(getter);
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean isPrimitive(T pojo) {
        return pojo.getClass().getName().startsWith("java.lang");
    }

    @SuppressWarnings("unchecked")
    private boolean compare(Object lval, ConditionType cond, Object rval) {
        boolean compares = true;
        if (cond == ConditionType.EQUALS || cond == ConditionType.NOT_EQUALS) {
            if (rval == null) {
                compares = true;
            } else if (lval == null) {
                compares = false;
            } else {
                if (lval instanceof String) {
                    compares = textCompare((String)lval, (String)rval);
                } else {
                    compares = lval.equals(rval);
                }
                if (cond == ConditionType.NOT_EQUALS) {
                    compares = !compares;
                }
            }
        } else {
            if (lval instanceof Comparable && rval instanceof Comparable) {
                Comparable lcomp = (Comparable)lval;
                Comparable rcomp = (Comparable)rval;
                int comp = lcomp.compareTo(rcomp);
                switch (cond) {
                case GREATER_THAN:
                    compares = comp > 0;
                    break;
                case GREATER_OR_EQUALS:
                    compares = comp >= 0;
                    break;
                case LESS_THAN:
                    compares = comp < 0;
                    break;
                case LESS_OR_EQUALS:
                    compares = comp <= 0;
                    break;
                default:
                    String msg = String.format("Condition type %s is not supported", cond.name());
                    throw new RuntimeException(msg);
                }
            }
        }
        return compares;
    }

    private boolean textCompare(String lval, String rval) {
        // check wild cards
        boolean starts = false;
        boolean ends = false;
        if (rval.charAt(0) == '*') {
            starts = true;
            rval = rval.substring(1);
        }
        if (rval.charAt(rval.length() - 1) == '*') {
            ends = true;
            rval = rval.substring(0, rval.length() - 1);
        }
        if (starts || ends) {
            // wild card tests
            if (starts && !ends) {
                return lval.endsWith(rval);
            } else if (ends && !starts) {
                return lval.startsWith(rval);
            } else {
                return lval.contains(rval);
            }
        } else {
            return lval.equals(rval);
        }
    }

    public List<T> findAll(Collection<T> pojos) {
        List<T> result = new ArrayList<T>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }

}
