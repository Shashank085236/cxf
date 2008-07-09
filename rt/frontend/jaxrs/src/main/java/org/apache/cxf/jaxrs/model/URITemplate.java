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

package org.apache.cxf.jaxrs.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;

public final class URITemplate {
    
    public static final String TEMPLATE_PARAMETERS = "jaxrs.template.parameters";
    
    public static final String LIMITED_REGEX_SUFFIX = "(/.*)?";
    public static final String UNLIMITED_REGEX_SUFFIX = "(/)?";
    public static final String FINAL_MATCH_GROUP = "FINAL_MATCH_GROUP";
    
    /**
     * The regular expression for matching URI templates and names.
     */
    private static final Pattern TEMPLATE_NAMES_PATTERN = Pattern.compile("\\{([a-zA-Z0-9][-\\w.]*)\\}");

    /**
     * A URI template is converted into a regular expression by substituting
     * (.*?) for each occurrence of {\([w- 14 \. ]+?\)} within the URL
     * template
     */
    private static final String PATH_VARIABLE_REGEX = "([^/]+?)";
    private static final String PATH_UNLIMITED_VARIABLE_REGEX = "(.*?)";

    private final String template;
    private final boolean limited;
    private final boolean encoded;
    private final List<String> templateVariables;
    private final Pattern templateRegexPattern;
    private final String literals;

    public URITemplate(String theTemplate) {
        this(theTemplate, true, false);
    }
    
    public URITemplate(String theTemplate, boolean limited, boolean encoded) {
        this.template = theTemplate;
        this.limited = limited;
        this.encoded = encoded;
        
        StringBuilder literalChars = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        List<String> names = new ArrayList<String>();

        // compute a regular expression from URI template
        Matcher matcher = TEMPLATE_NAMES_PATTERN.matcher(template);
        int i = 0;
        while (matcher.find()) {
            literalChars.append(template.substring(i, matcher.start()));
            copyURITemplateCharacters(template, i, matcher.start(), stringBuilder);
            i = matcher.end();
            if (!limited && i == template.length()) {
                stringBuilder.append(PATH_UNLIMITED_VARIABLE_REGEX);
            } else {
                stringBuilder.append(PATH_VARIABLE_REGEX);
            }
            names.add(matcher.group(1));
        }
        literalChars.append(template.substring(i, template.length()));
        copyURITemplateCharacters(template, i, template.length(), stringBuilder);

        literals = literalChars.toString();
        templateVariables = Collections.unmodifiableList(names);

        int endPos = stringBuilder.length() - 1;
        boolean endsWithSlash = (endPos >= 0) ? stringBuilder.charAt(endPos) == '/' : false;
        
        if (endsWithSlash) {
            stringBuilder.deleteCharAt(endPos);
        }
        stringBuilder.append(limited ? LIMITED_REGEX_SUFFIX : UNLIMITED_REGEX_SUFFIX);
        
        templateRegexPattern = Pattern.compile(stringBuilder.toString());
    }

    public boolean isLimited() {
        return limited;
    }
    
    public boolean encode() {
        return encoded;
    }
    
    public String getLiteralChars() {
        return literals;
    }
    
    public String getValue() {
        return template;
    }
    
    public int getNumberOfGroups() {
        return templateVariables.size();
    }
    
    private void copyURITemplateCharacters(String templateValue, int start, int end, StringBuilder b) {
        for (int i = start; i < end; i++) {
            char c = templateValue.charAt(i);
            if (c == '?') {
                b.append("\\?");
            } else {
                b.append(c);
            }
        }
    }

    public boolean match(String uri, MultivaluedMap<String, String> templateVariableToValue) {

        if (uri == null) {
            return (templateRegexPattern == null) ? true : false;
        }

        if (templateRegexPattern == null) {
            return false;
        }

        Matcher m = templateRegexPattern.matcher(uri);
        if (!m.matches()) {
            return false;
        }

        // Assign the matched template values to template variables
        int i = 1;
        for (String name : templateVariables) {
            String value = m.group(i++);
            templateVariableToValue.add(name, value);
        }

        // The right hand side value, might be used to further resolve sub-resources.
        
        String finalGroup = m.group(i);
        templateVariableToValue.putSingle(FINAL_MATCH_GROUP, finalGroup == null ? "/" : finalGroup);
        

        return true;
    }
    
    public static URITemplate createTemplate(ClassResourceInfo cri,
                                             Path path) {
        
        if (path == null) {
            return new URITemplate("/");
        }
        
        String pathValue = path.value();
        if (!pathValue.startsWith("/")) {
            pathValue = "/" + pathValue;
        }
        
        return new URITemplate(pathValue, path.limited(), path.encode());
    }
}
