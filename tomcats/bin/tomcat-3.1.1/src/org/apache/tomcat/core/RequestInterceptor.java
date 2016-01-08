/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/tomcat/core/Attic/RequestInterceptor.java,v 1.8 2000/02/16 05:53:33 costin Exp $
 * $Revision: 1.8 $
 * $Date: 2000/02/16 05:53:33 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */ 


package org.apache.tomcat.core;
import javax.servlet.Servlet;
import java.util.*;

/**
 * Provide a mechanism to customize the request processing.
 *
 * @author costin@dnt.ro
 */
public interface RequestInterceptor {
    public static final int OK=0;

    /** Will detect the context path for a request.
     *  It need to set: context, contextPath, lookupPath
     *
     *  A possible use for this would be a "user-home" interceptor
     *  that will implement ~costin servlets ( add and map them at run time).
     */
    public int contextMap(Request request);

    
    /** Handle mappings inside a context.
     *  You are required to respect the mappings in web.xml.
     */
    public int requestMap(Request request);

    
    /** Will extract the user ID from the request, and check the password.
     *  It will set the user only if the user/password are correct, or user
     *  will be null.
     *  XXX what should we do if the password is wrong ? 
     */
    public int authenticate(Request request, Response response);


    /** Will check if the user is authorized, by checking if it is in one
     *  of the roles defined in security constraints.
     *
     *  This will also work for "isUserInRole()".
     *
     *  If the user is not authorized, it will return an error code ( 401 ),
     *  and will set the response fields for an internal redirect.
     *  ContextManager will take care of handling that.
     *  
     */
    public int authorize(Request request, Response response);


    /** Called before service method is invoked. 
     */
    public int preService(Request request, Response response);

    
    /** Called before the first body write, and before sending
     *  the headers. The interceptor have a chance to change the
     *  output headers.
     */
    public int beforeBody( Request request, Response response);

    
    /** Called before the output buffer is commited
     */
    public int beforeCommit( Request request, Response response);

    
    /** Called after the output stream is closed ( either by servlet
     *  or automatically at end of service )
     */
    public int afterBody( Request request, Response response);

    
    /** Called after service method ends. Log is a particular use
     */
    public int postService(Request request, Response response);


    /** Will return the methods fow which this interceptor is interested
     *  in notification.
     *  This will be used by ContextManager to call only the interceptors
     *  that are interested, avoiding empty calls.
     *  ( not implemented yet )
     */
    public String[] getMethods();

    

}

