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

package org.apache.cxf.jaxrs.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Response.Builder;
import javax.ws.rs.ext.EntityProvider;
import javax.ws.rs.ext.HeaderProvider;
import javax.ws.rs.ext.ProviderFactory;

import org.apache.cxf.jaxrs.JAXRSUtils;



//NOTE: ProviderFactory should provide a method that can pass in media types
public class ProviderFactoryImpl extends ProviderFactory {
    protected List<EntityProvider> defaultEntityProviders = new ArrayList<EntityProvider>();
    protected List<EntityProvider> userEntityProviders = new ArrayList<EntityProvider>();
    protected List<HeaderProvider> headerProviders = new ArrayList<HeaderProvider>();    

    public ProviderFactoryImpl() {
        //TODO: search for EntityProviders from classpath or config file.
        defaultEntityProviders.add(new JAXBElementProvider());
        defaultEntityProviders.add(new JSONProvider());
        defaultEntityProviders.add(new StringProvider());
        defaultEntityProviders.add(new DOMSourceProvider());
        defaultEntityProviders.add(new AtomFeedProvider());
        defaultEntityProviders.add(new AtomEntryProvider());
        sort(defaultEntityProviders);
    }
    
    public <T> T createInstance(Class<T> type) {
        if (type.isAssignableFrom(Builder.class)) {
            return type.cast(new BuilderImpl());
        } 
        return null;
    }
   
    @SuppressWarnings("unchecked")
    public <T> EntityProvider<T> createEntityProvider(Class<T> type) {
        
        //Try user provided providers
        EntityProvider<T> ep = chooseEntityProvider(userEntityProviders, type);
        
        //If none found try the default ones
        if (ep == null) {
            ep = chooseEntityProvider(defaultEntityProviders, type);
        }     
        
        return ep;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityProvider<T> createEntityProvider(Class<T> type, String[] requestedMimeTypes,
                                                      boolean isConsumeMime) {

      //Try user defined providers
        EntityProvider<T> ep = chooseEntityProvider(userEntityProviders, 
                                                    type, requestedMimeTypes, isConsumeMime);
        
        //If none found try the default ones
        if (ep == null) {
            ep = chooseEntityProvider(defaultEntityProviders, type, requestedMimeTypes, isConsumeMime);
        }     
        
        return ep;
    }
    
    @SuppressWarnings("unchecked")
    public <T> HeaderProvider<T> createHeaderProvider(Class<T> type) {
        for (HeaderProvider<T> hp : headerProviders) {
            if (hp.supports(type)) {
                return hp;
            }
        }     
        
        return null;
    }
    
    public boolean registerUserEntityProvider(EntityProvider e) {
        userEntityProviders.add(e);
        sort(userEntityProviders);
        return true;
    }
    
    public boolean deregisterUserEntityProvider(EntityProvider e) {
        return userEntityProviders.remove(e);
    }
   
    /*
     * sorts the available providers according to the media types they declare
     * support for. Sorting of media types follows the general rule: x/y < * x < *,
     * i.e. a provider that explicitly lists a media types is sorted before a
     * provider that lists *. Quality parameter values are also used such that
     * x/y;q=1.0 < x/y;q=0.7.
     */    
    private void sort(List<EntityProvider> entityProviders) {
        Collections.sort(entityProviders, new EntityProviderComparator());
    }
    
    
    private static class EntityProviderComparator implements Comparator<EntityProvider> {
        public int compare(EntityProvider e1, EntityProvider e2) {
            ConsumeMime c = e1.getClass().getAnnotation(ConsumeMime.class);
            String[] mimeType1 = {"*/*"};
            if (c != null) {
                mimeType1 = c.value();               
            }
            
            ConsumeMime c2 = e2.getClass().getAnnotation(ConsumeMime.class);
            String[] mimeType2 = {"*/*"};
            if (c2 != null) {
                mimeType2 = c2.value();               
            }

            return compareString(mimeType1[0], mimeType2[0]);
            
        }
        
        private int compareString(String str1, String str2) {
            if (!str1.startsWith("*/") && str2.startsWith("*/")) {
                return -1;
            } else if (str1.startsWith("*/") && !str2.startsWith("*/")) {
                return 1;
            } 
            
            return str1.compareTo(str2);
        }
    }

    /**
     * Choose the first Entity provider that matches the requestedMimeTypes 
     * for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param entityProviders
     * @param type
     * @param requestedMimeTypes
     * @param isConsumeMime
     * @return
     */
    private EntityProvider chooseEntityProvider(List<EntityProvider> entityProviders, Class<?> type, 
                                                 String[] requestedMimeTypes, boolean isConsumeMime) {
        for (EntityProvider<?> ep : entityProviders) {
            String[] supportedMimeTypes = {"*/*"};            
            if (isConsumeMime) {
                ConsumeMime c = ep.getClass().getAnnotation(ConsumeMime.class);
                if (c != null) {
                    supportedMimeTypes = c.value();               
                }           
            } else {
                ProduceMime c = ep.getClass().getAnnotation(ProduceMime.class);
                if (c != null) {
                    supportedMimeTypes = c.value();               
                }                  
            }
            
            String[] availableMimeTypes = JAXRSUtils.intersectMimeTypes(requestedMimeTypes,
                                                                        supportedMimeTypes);

            if (availableMimeTypes.length != 0 && ep.supports(type)) {
                return ep;
            }
        }     
        
        return null;
        
    }
    
    /**
     * Choose the first Entity provider that matches the type for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param entityProviders
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> EntityProvider<T> chooseEntityProvider(List<EntityProvider> entityProviders, Class<T> type) {
        
        for (EntityProvider<T> ep : entityProviders) {
            if (ep.supports(type)) {
                return ep;
            }
        }           
        return null;
    }
    
    
    public List<EntityProvider> getDefaultEntityProviders() {
        return defaultEntityProviders;
    }


    public List<EntityProvider> getUserEntityProviders() {
        return userEntityProviders;
    }

    /**
     * Use for injection of entityProviders
     * @param entityProviders the entityProviders to set
     */
    public void setUserEntityProviders(List<EntityProvider> userEntityProviders) {
        this.userEntityProviders = userEntityProviders;
        sort(this.userEntityProviders);
    }

}
