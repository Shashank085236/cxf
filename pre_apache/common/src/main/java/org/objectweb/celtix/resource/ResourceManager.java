package org.objectweb.celtix.resource;


import java.io.InputStream;
import java.util.List;


/**
 * Locates resources that are used at runtime.  The
 * <code>ResourceManager</code> queries registered
 * <code>ResourceResolver</code> to find resources.
 */
public interface ResourceManager {
    
    /**
     * Resolve a resource.  The ResourceManager will query all of the
     * registered <code>ResourceResovler</code> objects until one
     * manages to resolve the resource
     * 
     * @param name name of resource to resolve.
     * @param type type of resource to resolve.
     * @return the resolved resource or null if nothing found.
     */
    <T> T resolveResource(String name, Class<T> type);

    /** 
     * Resolve a resource with via a specified list of resovlers.  This allows 
     * resources to be specified with a locally defined list of resolvers.
     * 
     * @param name name of resource to resolve.
     * @param type type of resource to resolve.
     * @param resolvers list of <code>ResourceResolvers</codea> to search.
     * @return the resolved resource or null if nothing found.
     */
    <T> T resolveResource(String name, Class<T> type, List<ResourceResolver> resolvers);

    /**
     * Open stream to resource.  
     *
     * @param name name of resource to resolve. 
     * @return the InputStream to the resource or null if the resource
     * cannot be found.
     */
    InputStream getResourceAsStream(String name);

    /** 
     * Add a <code>ResourceResolver</code>.  The newly added resolver
     * is added at the head of the list so the most recently added
     * will be queried first.
     * @param resolver the <code>ResourceResolver</code> to
     * add. Duplicates will be ignored.
     */
    void addResourceResolver(ResourceResolver resolver);

    /** 
     * Remove a <code>ResourceResolver</code>.
     * @param resolver the <code>ResourceResolver</code> to remove.
     * If not previously registered, it is ignored.
     */
    void removeResourceResolver(ResourceResolver resolver);


    /**
     * Get all the currently registered resolvers.  This method should return 
     * a copy of the list of resolvers so that resolvers added after this method 
     * has been called will alter the list returned.
     */
    List<ResourceResolver> getResourceResolvers();
}
