/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/tomcat/service/connector/Attic/JNIConnectionHandler.java,v 1.2.2.1 2000/04/13 11:44:13 shachor Exp $
 * $Revision: 1.2.2.1 $
 * $Date: 2000/04/13 11:44:13 $
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

package org.apache.tomcat.service.connector;

import java.io.IOException;
import org.apache.tomcat.core.*;
import org.apache.tomcat.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class JNIConnectionHandler {

    ContextManager contextM;

    public JNIConnectionHandler() {
    }

    public void setContextManager(ContextManager contextM) {
    	this.contextM=contextM;
    }

    public void setNativeLibrary(String lib) {

        // First try to load from the library path
        try {
            System.loadLibrary(lib);
            System.out.println("Library " + lib + " was loaded from the lib path");
            return;
        } catch(UnsatisfiedLinkError usl) {
            //usl.printStackTrace();
        }
        // Loading from the library path failed
        // Try to load assuming lib is a complete pathname.
        System.load(lib);
        System.out.println("Library " + lib + " loaded");
    }

    public void processConnection(long s, long l) {

        try {
    	    JNIRequestAdapter reqA = new JNIRequestAdapter(contextM, this);
	    reqA.setContextManager( contextM );
    	    JNIResponseAdapter resA =new JNIResponseAdapter(this);

    	    reqA.setResponse(resA);
    	    resA.setRequest(reqA);

            resA.setRequestAttr(s, l);
    	    reqA.readNextRequest(s, l);

    	    if(reqA.shutdown )
        		return;
    	    if(resA.getStatus() >= 400) {
        		resA.finish();
    		    return;
    	    }

    	    int contentLength = reqA.getFacade().getIntHeader("content-length");
    	    if (contentLength != -1) {
    		    BufferedServletInputStream sis =
    		        (BufferedServletInputStream)reqA.getInputStream();
    		    sis.setLimit(contentLength);
    	    }

    	    contextM.service( reqA, resA );

    	    if(resA.getStatus() > 0) {
    	        resA.finish();
    	    }
    	} catch(Exception ex) {
    	    ex.printStackTrace();
    	}
    }

    native int readEnvironment(long s, long l, String []env);

    native int getNumberOfHeaders(long s, long l);

    native int readHeaders(long s,
                           long l,
                           String []names,
                           String []values);

    native int read(long s,
                    long l,
                    byte []buf,
                    int from,
                    int cnt);

    native int startReasponse(long s,
                              long l,
                              int sc,
                              String msg,
                              String []headerNames,
                              String []headerValues,
                              int headerCnt);

    native int write(long s,
                     long l,
                     byte []buf,
                     int from,
                     int cnt);
}

class JNIRequestAdapter extends RequestImpl {
    StringManager sm = StringManager.getManager("org.apache.tomcat.service");
    ContextManager contextM;
    boolean shutdown=false;

    JNIConnectionHandler h;
    long s;
    long l;

    public int doRead() throws IOException {
        byte []b = new byte[1];
        int rc = doRead(b, 0, 1);

        if(rc <= 0) {
            return -1;
        }

        return ((int)b[0]) & 0x000000FF;
    }

    public  int doRead(byte b[], int off, int len) throws IOException {
        int rc = 0;

        while(0 == rc) {
	        rc = h.read(s, l, b, off, len);
	        if(0 == rc) {
	            Thread.currentThread().yield();
	        }
	    }
	    return rc;
    }

    public JNIRequestAdapter(ContextManager cm,
                             JNIConnectionHandler h) {
    	this.contextM = cm;
    	this.h = h;
    }

    protected void readNextRequest(long s, long l) throws IOException {
        String []env = new String[12];
        int i = 0;

    	this.s = s;
    	this.l = l;

        for(i = 0 ; i < 12 ; i++) {
            env[i] = null;
        }

        /*
         * Read the environment
         */
        if(h.readEnvironment(s, l, env) > 0) {
    		method      = env[0];
    		requestURI  = env[1];
    		queryString = env[2];
    		remoteAddr  = env[3];
    		remoteHost  = env[4];
    		serverName  = env[5];
            serverPort  = Integer.parseInt(env[6]);
            authType    = env[7];
            remoteUser  = env[8];
            scheme      = env[9];
            protocol    = env[10];
            // response.setServerHeader(env[11]);
        } else {
            throw new IOException("Error: JNI implementation error");
        }

        /*
         * Read the headers
         */
        int nheaders = h.getNumberOfHeaders(s, l);
        if(nheaders > 0) {
            String []names = new String[nheaders];
            String []values = new String[nheaders];
            if(h.readHeaders(s, l, names, values) > 0) {
                for(i = 0 ; i < nheaders ; i++) {
                    headers.putHeader(names[i].toLowerCase(), values[i]);
                }
            } else {
                throw new IOException("Error: JNI implementation error");
            }
        }

	    // REQUEST_URI may include a query string
	    int idQ= requestURI.indexOf("?");
	    if ( idQ > -1) {
    	    requestURI = requestURI.substring(0, idQ);
        }

	    contentLength = headers.getIntHeader("content-length");
	    contentType = headers.getHeader("content-type");
    }

    public ServletInputStream getInputStream() throws IOException {
        if(contentLength <= 0) {
            throw new IOException("Empty input stream");
        }

        in = new BufferedServletInputStream(this);
    	return in;
    }
}


// Ajp use Status: instead of Status
class JNIResponseAdapter extends ResponseImpl {

    JNIConnectionHandler h;
    long s;
    long l;

    public JNIResponseAdapter(JNIConnectionHandler h) {
    	this.h = h;
    }

    protected void setRequestAttr(long s, long l) throws IOException {
    	this.s = s;
    	this.l = l;
    }

    public void endHeaders() throws IOException {

    	if(request.getProtocol()==null) // HTTP/0.9
	        return;

        super.endHeaders();

        int    hcnt = 0;
        String []headerNames = null;
        String []headerValues = null;

        headers.removeHeader("Status");

        hcnt = headers.size();
        headerNames = new String[hcnt];
        headerValues = new String[hcnt];

        for(int i = 0; i < hcnt; i++) {
            MimeHeaderField h = headers.getField(i);
            headerNames[i] = h.getName();
            headerValues[i] = h.getValue();
        }

        if(h.startReasponse(s, l, getStatus(), getMessage(getStatus()), headerNames, headerValues, hcnt) <= 0) {
            throw new IOException("Error: JNI startReasponse implementation error");
        }
    }

    public void doWrite(byte buf[], int pos, int count) throws IOException {
        if(h.write(s, l, buf, pos, count) <= 0) {
            throw new IOException("Error: JNI implementation error");
        }
    }
}

