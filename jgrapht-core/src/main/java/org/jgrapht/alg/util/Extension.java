/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* -----------------
 * MaximumFlowAlgorithmBase.java
 * -----------------
 * (C) Copyright 2015-2015, by Alexey Kudinkin and Contributors.
 *
 * Original Author:  Alexey Kudinkin
 * Contributor(s):
 *
 * $Id$
 *
 * Changes
 * -------
 */
package org.jgrapht.alg.util;

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract extension/encapsulation manager.
 * This class creates and manages objects extensions and encapsulations. An object, from here on denoted as prototype,
 * can be encapsulated in or extended by another object. An example would be the relation between an edge (prototype) and an annotated edge. The
 * annotated edge encapsulates/extends an edge, thereby augmenting it with additional data.
 * In symbolic form, if b is the prototype class, than a(b) would be its extension. This concept is similar to java's extension where
 * one class is derived from (extends) another class (prototype).
 *
 * @param <T> class-type to be extended (class-type of prototype)
 * @param <E> class-type of extension
 *
 *
 *
 * NOTE JK: This seems to be a poor name. This class is more of an ExtensionManager than an Extension by itself
 * */
public class Extension<T, E>
{
    /* Factory class to create new extensions */
    private ExtensionFactory<E> extensionFactory;
    /* Mapping of prototypes to their extensions */
    private Map<T, E> prototypeToExtensionMap = new HashMap<>();

    public Extension(ExtensionFactory<E> factory)
    {
        this.extensionFactory = factory;
    }

    /**
     * Creates and returns an extension object.
     * JK: Renamed this function to 'getInstance' for consistency with the getSingletonInstance function
     * @return Extension object
     */
    @Deprecated
    public E createInstance()
    {
        return extensionFactory.create();
    }

    /**
     * Creates and returns an extension object.
     * @return Extension object
     */
    public E getInstance()
    {
        return extensionFactory.create();
    }

    /**
     * For a given prototype t, this function returns t's extension. If no encapsulation/extension
     * for t exists, a new one is created and returned.
     *
     * NOTE: JK - Do we need want/this? There is no equivalent set(T t). Furthermore, it gets quite ambiguous if in some cases
     * an Extension is created without a prototype (createInstance()) and sometimes an Extension is created through the get(T t) function.
     * Only Extentions associated with a prototype t will be stored by the Manager. This leads to some very hard to track behavior. At the
     * very least, give the function a proper name such as getSingletonInstance (as in contrast to the createInstance function above.
     *
     * @param t prototype
     * @return Extension of prototype
     */
    @Deprecated
    public E get(T t)
    {
        if (prototypeToExtensionMap.containsKey(t)) {
            return prototypeToExtensionMap.get(t);
        }

        E x = getInstance();
        prototypeToExtensionMap.put(t, x);
        return x;
    }

    /**
     * Creates a new extension object for prototype t if no such object exists, returns the old one otherwise.
     */
    public E getSingletonInstance(T t)
    {
        if (prototypeToExtensionMap.containsKey(t)) {
            return prototypeToExtensionMap.get(t);
        }

        E x = getInstance();
        prototypeToExtensionMap.put(t, x);
        return x;
    }

    /**
     * Factory capable of producing extension objects of the given
     * class-type
     *
     * @param <E> class-type of extension
     *
     * NOTE: JK - Why is this class a subclass of the Extension/ExtensionManager class, instead of a class on its own.
     */
    public interface ExtensionFactory<E>
    {
        E create();
    }

    /**
     * Comments/Description missing
     *
     * NOTE: JK - Why do we want or need this? The only function this would offer is that all extensions have some common
     * super type. However, none of the code requires that E extends BaseExtension. Furthermore, I'm not sure why this
     * class is a subclass of the Extension/ExtensionManager class, instead of a class on its own.
     */
    public static abstract class BaseExtension
    {
        public BaseExtension()
        {
        }
    }

    /**
     * Comments/Description missing
     *
     * NOTE: JK - Do we really need this? This seems well beyond what any user would require. Better to keep the library as concise as possible.
     */
    public static class ExtensionManagerInstantiationException
        extends RuntimeException
    {
        Exception exception;

        public ExtensionManagerInstantiationException(Exception e)
        {
            exception = e;
        }
    }
}

// End Extension.java
