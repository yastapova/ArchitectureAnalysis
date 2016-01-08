/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/jasper/compiler/JikesJavaCompiler.java,v 1.3 2000/01/23 01:37:46 bergsten Exp $
 * $Revision: 1.3 $
 * $Date: 2000/01/23 01:37:46 $
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
 *	 "This product includes software developed by the
 *	  Apache Software Foundation (http://www.apache.org/)."
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
 * DISCLAIMED.	IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
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
 */

package org.apache.jasper.compiler;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.ByteArrayOutputStream;

/**
  * A Plug-in class for specifying a 'jikes' compile.
  *
  * @author Jeffrey Chiu
  * @author Hans Bergsten <hans@gefionsoftware.com>
  */
public class JikesJavaCompiler implements JavaCompiler {

    static final int OUTPUT_BUFFER_SIZE = 1024;
    static final int BUFFER_SIZE = 512;

    String encoding;
    String classpath;
    String compilerPath = "jikes";
    String outdir;
    OutputStream out;

    /**
     * Specify where the compiler can be found
     */ 
    public void setCompilerPath(String compilerPath) {
	this.compilerPath = compilerPath;
    }

    /**
     * Set the encoding (character set) of the source
     */ 
    public void setEncoding(String encoding) {
      this.encoding = encoding;
    }

    /**
     * Set the class path for the compiler
     */ 
    public void setClasspath(String classpath) {
      this.classpath = classpath;
    }

    /**
     * Set the output directory
     */ 
    public void setOutputDir(String outdir) {
      this.outdir = outdir;
    }

    /**
     * Set where you want the compiler output (messages) to go 
     */ 
    public void setMsgOutput(OutputStream out) {
      this.out = out;
    }

    /**
     * Execute the compiler
     * @param source - file name of the source to be compiled
     */ 
    public boolean compile(String source) {
	Process p;
	int exitValue = -1;

	String[] compilerCmd = new String[] {
	  compilerPath,
          //XXX - add encoding once Jikes supports it
          "-classpath", classpath,
          "-d", outdir,
          // Only report errors, to be able to test on output in addition to exit code
          "-nowarn",
          source
        };

        ByteArrayOutputStream tmpErr = new ByteArrayOutputStream(OUTPUT_BUFFER_SIZE);
	try {
	    p = Runtime.getRuntime().exec(compilerCmd);
	    
	    BufferedInputStream compilerErr = new
		BufferedInputStream(p.getErrorStream());

	    StreamPumper errPumper = new StreamPumper(compilerErr, tmpErr);

	    errPumper.start();

            p.waitFor();
            exitValue = p.exitValue();

	    // Wait until the complete error stream has been read
            errPumper.join();
	    compilerErr.close();

	    p.destroy();

            // Write the compiler error messages, if any, to the real stream 
            tmpErr.close();
            tmpErr.writeTo(out);
            
	} catch (IOException ioe) {
	    return false;

	} catch (InterruptedException ie) {
	    return false;
	}

        boolean isOkay = exitValue == 0;
        // Jikes returns 0 even when there are some types of errors. 
        // Check if any error output as well
        if (tmpErr.size() > 0) {
            isOkay = false;
        }
        return isOkay;
    }

    // Inner class for continually pumping the input stream during
    // Process's runtime.
    class StreamPumper extends Thread {
	private BufferedInputStream stream;
	private boolean endOfStream = false;
	private boolean stopSignal  = false;
	private int SLEEP_TIME = 5;
	private OutputStream out;

	public StreamPumper(BufferedInputStream is, OutputStream out) {
	    this.stream = is;
	    this.out = out;
	}

	public void pumpStream()
	    throws IOException
	{
	    byte[] buf = new byte[BUFFER_SIZE];
	    if (!endOfStream) {
		int bytesRead=stream.read(buf, 0, BUFFER_SIZE);

		if (bytesRead > 0) {
		    out.write(buf, 0, bytesRead);
		} else if (bytesRead==-1) {
		    endOfStream=true;
		}
	    }
	}

	public void run() {
	    try {
		//while (!endOfStream || !stopSignal) {
		while (!endOfStream) {
		    pumpStream();
		    sleep(SLEEP_TIME);
		}
	    } catch (InterruptedException ie) {
	    } catch (IOException ioe) {
	    }
	}
    }
}


