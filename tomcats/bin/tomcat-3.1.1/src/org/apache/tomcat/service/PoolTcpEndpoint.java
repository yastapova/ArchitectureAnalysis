/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/tomcat/service/Attic/PoolTcpEndpoint.java,v 1.2.2.1 2000/04/15 14:48:39 shachor Exp $
 * $Revision: 1.2.2.1 $
 * $Date: 2000/04/15 14:48:39 $
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


package org.apache.tomcat.service;

import org.apache.tomcat.util.*;
import org.apache.tomcat.net.*;
import java.io.*;
import java.net.*;
import java.util.*;

/* Similar with MPM module in Apache2.0. Handles all the details related with
   "tcp server" functionality - thread management, accept policy, etc.
   It should do nothing more - as soon as it get a socket ( and all socket options
   are set, etc), it just handle the stream to ConnectionHandler.processConnection. (costin)
*/



/**
 * Handle incoming TCP connections.
 *
 * This class implement a simple server model: one listener thread accepts on a socket and
 * creates a new worker thread for each incoming connection.
 *
 * More advanced Endpoints will reuse the threads, use queues, etc.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author Jason Hunter [jch@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Costin@eng.sun.com
 * @author Gal Shachor [shachor@il.ibm.com]
 */
public class PoolTcpEndpoint extends TcpEndpoint  { // implements Endpoint {

    private StringManager sm = StringManager.getManager("org.apache.tomcat.service");

    private static final int BACKLOG = 100;
    private static final int TIMEOUT = 1000;

    private boolean isPool = true;

    private int backlog = BACKLOG;
    private int timeout = TIMEOUT;

    TcpConnectionHandler handler;

    private InetAddress inet;
    private int port;

    private ServerSocketFactory factory;
    private ServerSocket serverSocket;

    Runnable listener;
    boolean running = true;

    ThreadPool tp;

    public PoolTcpEndpoint() {
	tp = new ThreadPool();
    }

    // -------------------- Configuration --------------------

    public void setPoolOn(boolean isPool) {
        this.isPool = isPool;
    }

    public boolean isPoolOn() {
        return isPool;
    }

    public void setMaxThreads(int maxThreads) {
        if(1 == maxThreads) {
            /*
             * We use one thread to listen and one to service, so we need
             * at least two threads!!! GS
			 */
            maxThreads = 2;
        }
        tp.setMaxThreads(maxThreads);
    }

    public int getMaxThreads() {
        return tp.getMaxThreads();
    }

    public void setMaxSpareThreads(int maxThreads) {
        tp.setMaxSpareThreads(maxThreads);
    }

    public int getMaxSpareThreads() {
        return tp.getMaxSpareThreads();
    }

    public void setMinSpareThreads(int minThreads) {
        tp.setMinSpareThreads(minThreads);
    }

    public int getMinSpareThreads() {
        return tp.getMinSpareThreads();
    }

    public int getPort() {
	    return port;
    }

    public void setPort(int port ) {
	    this.port=port;
    }

    public InetAddress getAddress() {
	    return inet;
    }

    public void setAddress(InetAddress inet) {
	    this.inet=inet;
    }

    public void setServerSocket(ServerSocket ss) {
	    serverSocket = ss;
    }

    public void setServerSocketFactory(  ServerSocketFactory factory ) {
	    this.factory=factory;
    }

    public void setConnectionHandler( TcpConnectionHandler handler ) {
    	this.handler=handler;
    }

    public TcpConnectionHandler getConnectionHandler() {
	    return handler;
    }

    /**
     * Allows the server developer to specify the backlog that
     * should be used for server sockets. By default, this value
     * is 100.
     */
    public void setBacklog(int backlog) {
	    this.backlog = backlog;
    }

    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the timeout in ms of the server sockets created by this
     * server. This method allows the developer to make servers
     * more or less responsive to having their server sockets
     * shut down.
     *
     * <p>By default this value is 1000ms.
     */
    public void setTimeout(int timeout) {
	    this.timeout = timeout;
    }

    // -------------------- Public methods --------------------

    public void startEndpoint() throws IOException, InstantiationException {
	    try {
	        if(factory==null)
		        factory=ServerSocketFactory.getDefault();
	        if(serverSocket==null) {
		        if (inet == null) {
		            serverSocket = factory.createSocket(port, backlog);
    		    } else {
	    	        serverSocket = factory.createSocket(port, backlog, inet);
		        }
	        }
	        if(isPool) {
		    tp.start();
		}
	    } catch( IOException ex ) {
	        // throw?
	        // ex.printStackTrace();
	        running=false;
            throw ex;
	        // throw new HttpServerException(msg);
	    } catch( InstantiationException ex1 ) {
	        // throw?
	        // ex1.printStackTrace();
	        running=false;
            throw ex1;
	        // throw new HttpServerException(msg);
	    }
	    running=true;
        if(isPool) {
    	    listener = new TcpWorkerThread(this);
            tp.runIt(listener);
        } else {
    	    System.out.println("XXX Error - need pool !");
	}
    }

    public void stopEndpoint() {
        tp.shutdown();
	running=false;
	try {
	    serverSocket.close(); // XXX?
	} catch(Exception e) {
	}
	serverSocket = null;
    }

    // -------------------- Private methods

    Socket acceptSocket() {
        Socket accepted = null;
    	try {
    	    if(running == true) {
        	    if(null!= serverSocket) {
            		accepted = serverSocket.accept();
    	        	if(running == false) {
    	        	    if(null != accepted) {
        		            accepted.close();  // rude, but unlikely!
        		            accepted = null;
        		        }
    		        }
    	        }
    	    }
    	} catch(InterruptedIOException iioe) {
    	    // normal part -- should happen regularly so
    	    // that the endpoint can release if the server
    	    // is shutdown.
    	    // you know, i really wish that there was a
    	    // way for the socket to timeout without
    	    // tripping an exception. Exceptions are so
    	    // 'spensive.
    	} catch (SocketException e) {
    	    if (running != false) {
        		running = false;
        		String msg = sm.getString("endpoint.err.fatal",
    					                  serverSocket, e);
    	    	e.printStackTrace(); // something very wrong happened - better know what
    		    System.err.println(msg);
    	    }
    	} catch(Throwable e) {
    	    running = false;
    	    String msg = sm.getString("endpoint.err.fatal",
    				                  serverSocket, e);
    	    e.printStackTrace(); // something very wrong happened - better know what
    	    System.err.println(msg);
    	}

    	return accepted;
    }
}

// -------------------- Threads --------------------
// XXX add a more efficient model - use thread pools, use a Queue, etc

// Keep the thread model in one place !

/*
 * I switched the threading model here.
 *
 * We used to have a "listener" thread and a "connection"
 * thread, this results in code simplicity but also a needless
 * thread switch.
 *
 * Instead I am now using a pool of threads, all the threads are
 * simmetric in their execution and no thread switch is needed.
 */
class TcpWorkerThread implements Runnable {
    PoolTcpEndpoint endpoint;
    Vector connectionCache;

    public TcpWorkerThread(PoolTcpEndpoint endpoint) {
	    this.endpoint = endpoint;
	    connectionCache = new Vector(endpoint.getMaxThreads());
	    for(int i = 0 ; i < endpoint.getMaxThreads()/2 ; i++) {
	        connectionCache.addElement(new TcpConnection());
	    }
    }

    public void run() {
	    while(endpoint.running) {
		//		System.out.println("XXX accept socket");
	        Socket s = endpoint.acceptSocket();
		//		System.out.print("Ac");
		//		System.out.println("XXX accepted " + s );
	        if(null != s) {
	            // Continue accepting on another thread...
		    //		    System.out.print("Ar");
	            endpoint.tp.runIt(this);
		    //  System.out.print("Ri");
	            TcpConnection con = null;
	            try {
                	// XXX set socket options
                	// 	s.setSoLinger( true, 100);
                	//	s.setSoTimeout( 1000 );
			try {
			    con = (TcpConnection)connectionCache.lastElement();
			    connectionCache.removeElementAt(connectionCache.size() - 1);
			} catch(Throwable t) {
			    con = new TcpConnection();
			}

                	con.setEndpoint(endpoint);
                	con.setSocket(s);
                	endpoint.getConnectionHandler().processConnection(con, null);
                } finally {
                    con.recycle();
                    connectionCache.addElement(con);
                }
		    //		System.out.println("XXX done " + s  );
                break;
	        }
	    }
	    //	    System.out.println("End thread "   );
    }
}
