/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/tomcat/service/connector/Attic/ConnectorResponse.java,v 1.4 2000/02/01 07:37:39 costin Exp $
 * $Revision: 1.4 $
 * $Date: 2000/02/01 07:37:39 $
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

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.tomcat.core.*;
import org.apache.tomcat.util.*;
//import org.apache.tomcat.server.*;
import javax.servlet.*;
import javax.servlet.http.*;


public class ConnectorResponse extends ResponseImpl {
    public static final int SEND_HEADERS=2;
    public static final int END_RESPONSE=5;

    MsgConnector con;
    ConnectorServletOS rout;

    public ConnectorResponse(MsgConnector con) {
        super();
	this.con=con;
	rout=new ConnectorServletOS(con);
	rout.setResponse( this );
	this.out=rout;
    }

    public void recycle() {
	super.recycle();
	rout.recycle();

	rout=new ConnectorServletOS(con);
	rout.setResponse( this );

	out=rout;
    }

    // XXX if more headers that MAX_SIZE, send 2 packets!   
    public void endHeaders() throws IOException {
	super.endHeaders();
	
        if (request.getProtocol() == null) {
            return;
        }
	
	MsgBuffer msg=con.getMsgBuffer();
	msg.reset();
	msg.appendInt( SEND_HEADERS );
	msg.appendInt( headers.size() );

	Enumeration e = headers.names();
	while (e.hasMoreElements()) {
	    String headerName = (String)e.nextElement();
	    String headerValue = headers.getHeader(headerName);
	    msg.appendString( headerName);
	    msg.appendString( headerValue);
	}

	msg.end();
	con.send( msg );
     }


    public void finish() throws IOException {
	super.finish();
	MsgBuffer msg=con.getMsgBuffer();
	msg.reset();
	msg.appendInt( END_RESPONSE );
	msg.end();
	con.send( msg );
    }


}
