/*
 * $Header: /home/cvs/jakarta-tomcat/src/share/org/apache/jasper/JspC.java,v 1.10 2000/03/30 04:28:06 shemnon Exp $
 * $Revision: 1.10 $
 * $Date: 2000/03/30 04:28:06 $
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
 */ 

package org.apache.jasper;

import java.io.*;
import java.util.*;

import org.apache.jasper.compiler.JspReader;
import org.apache.jasper.compiler.ServletWriter;
import org.apache.jasper.compiler.TagLibraries;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.CommandLineCompiler;

import org.apache.jasper.runtime.JspLoader;

import org.apache.tomcat.logging.Logger;
import org.apache.tomcat.logging.TomcatLogger;

/**
 * Shell for the jspc compiler.  Handles all options associated with the 
 * command line and creates compilation contexts which it then compiles
 * according to the specified options.
 * @author Danno Ferrin
 */
public class JspC implements Options { //, JspCompilationContext {

    public static final String DEFAULT_IE_CLASS_ID = 
            "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
    
    public static final String SWITCH_VERBOSE = "-v";
    public static final String SWITCH_QUIET = "-q";
    public static final String SWITCH_OUTPUT_DIR = "-d";
    public static final String SWITCH_OUTPUT_SIMPLE_DIR = "-dd";
    public static final String SWITCH_IE_CLASS_ID = "-ieplugin";
    public static final String SWITCH_PACKAGE_NAME = "-p";
    public static final String SWITCH_CLASS_NAME = "-c";
    public static final String SWITCH_FULL_STOP = "--";
    public static final String SWITCH_URI_BASE = "-uribase";
    public static final String SWITCH_URI_ROOT = "-uriroot";
    public static final String SWITCH_FILE_WEBAPP = "-webapp";
    public static final String SWITCH_WEBAPP_INC = "-webinc";
    public static final String SWITCH_WEBAPP_XML = "-webxml";
    public static final String SWITCH_MAPPED = "-mapped";
    public static final String SWITCH_DIE = "-die";

    public static final int NO_WEBXML = 0;
    public static final int INC_WEBXML = 10;
    public static final int ALL_WEBXML = 20;

    public static final int DEFAULT_DIE_LEVEL = 1;
    public static final int NO_DIE_LEVEL = 0;

    // future direction
    //public static final String SWITCH_XML_OUTPUT = "-xml";
  
    
    boolean largeFile = false;
    boolean mappedFile = false;

    int jspVerbosityLevel = Logger.INFORMATION;

    File scratchDir;

    String ieClassId = DEFAULT_IE_CLASS_ID;

    //String classPath;
    
    String targetPackage;
    
    String targetClassName;

    String uriBase;

    String uriRoot;

    String webxmlFile;

    int webxmlLevel;

    int dieLevel;
    static int die; // I realize it is duplication, but this is for
                    // the static main catch

    //JspLoader loader;

    boolean dirset;

    Vector extensions;

    public boolean getKeepGenerated() {
        // isn't this why we are running jspc?
        return true;
    }
    
    public boolean getLargeFile() {
        return largeFile;
    }

    /**
     * Are we supporting HTML mapped servlets?
     */
    public boolean getMappedFile() {
		return mappedFile;
	}
    
    public boolean getSendErrorToClient() {
        // implied send to System.err
        return true;
    }
 
    public String getIeClassId() {
        return ieClassId;
    }
    
    public int getJspVerbosityLevel() {
        return jspVerbosityLevel;
    }

    public File getScratchDir() {
        return scratchDir;
    }

    //public String getClassPath() {
    //    return classpath;
    //}

    public Class getJspCompilerPlugin() {
       // we don't compile, so this is meanlingless
        return null;
    }

    public String getJspCompilerPath() {
       // we don't compile, so this is meanlingless
        return null;
    }

    public String getClassPath() {
        return System.getProperty("java.class.path");
    }
    
    int argPos;
    // value set by beutifully obsfucscated java
    boolean fullstop = false;
    String args[];

    private void pushBackArg() {
        if (!fullstop) {
            argPos--;
        };
    };

    private String nextArg() {
        if ((argPos >= args.length)
            || (fullstop = SWITCH_FULL_STOP.equals(args[argPos]))) {
            return null;
        } else {
            return args[argPos++];
        }
    };
        
    private String nextFile() {
        if (fullstop) argPos++;
        if (argPos >= args.length) {
            return null;
        } else {
            return args[argPos++];
        }
    };

    public JspC(String[] arg, PrintStream log) {
        args = arg;
        String tok;

        int verbosityLevel = Logger.WARNING;
        dieLevel = NO_DIE_LEVEL;
        die = dieLevel;

        while ((tok = nextArg()) != null) {
            if (tok.equals(SWITCH_QUIET)) {
                verbosityLevel = Logger.WARNING;
            } else if (tok.equals(SWITCH_VERBOSE)) {
                verbosityLevel = Logger.INFORMATION;
            } else if (tok.startsWith(SWITCH_VERBOSE)) {
                try {
                    verbosityLevel
                     = Integer.parseInt(tok.substring(SWITCH_VERBOSE.length()));
                } catch (NumberFormatException nfe) {
                    log.println(
                        "Verbosity level " 
                        + tok.substring(SWITCH_VERBOSE.length()) 
                        + " is not valid.  Option ignored.");
                }
            } else if (tok.equals(SWITCH_OUTPUT_DIR)) {
                tok = nextArg();
                if (tok != null) {
                    scratchDir = new File(new File(tok).getAbsolutePath());
                    dirset = true;
                } else {
                    // either an in-java call with an explicit null
                    // or a "-d --" sequence should cause this,
                    // which would mean default handling
                    /* no-op */
                    scratchDir = null;
                }
            } else if (tok.equals(SWITCH_OUTPUT_SIMPLE_DIR)) {
                tok = nextArg();
                if (tok != null) {
                    scratchDir = new File(new File(tok).getAbsolutePath());
                } else {
                    // either an in-java call with an explicit null
                    // or a "-d --" sequence should cause this,
                    // which would mean default handling
                    /* no-op */
                    scratchDir = null;
                }
            } else if (tok.equals(SWITCH_PACKAGE_NAME)) {
                targetPackage = nextArg();
            } else if (tok.equals(SWITCH_CLASS_NAME)) {
                targetClassName = nextArg();
            } else if (tok.equals(SWITCH_URI_BASE)) {
                uriBase = nextArg();
            } else if (tok.equals(SWITCH_URI_ROOT)) {
                uriRoot = nextArg();
            } else if (tok.equals(SWITCH_WEBAPP_INC)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = INC_WEBXML;
                };
            } else if (tok.equals(SWITCH_WEBAPP_XML)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = ALL_WEBXML;
                };
            } else if (tok.equals(SWITCH_MAPPED)) {
                mappedFile = true;
            } else if (tok.startsWith(SWITCH_DIE)) {
                try {
                    dieLevel = Integer.parseInt(
                        tok.substring(SWITCH_DIE.length()));
                } catch (NumberFormatException nfe) {
                    dieLevel = DEFAULT_DIE_LEVEL;
                };
                die = dieLevel;
            } else {
                pushBackArg();
                // Not a recognized Option?  Start treting them as JSP Pages
                break;
            }
        }

        Constants.jasperLog = new TomcatLogger();
        Constants.jasperLog.setVerbosityLevel(verbosityLevel);

    };
    
  public boolean parseFile(PrintStream log, String file, Writer mapout)
    {
        try {
            JspLoader loader =
                    new JspLoader(getClass().getClassLoader(), this);
            CommandLineContext clctxt = new CommandLineContext(
                    loader, getClassPath(), file, uriBase, uriRoot, false,
                    this);
            if ((targetClassName != null) && (targetClassName.length() > 0)) {
                clctxt.setServletClassName(targetClassName);
                clctxt.lockClassName();
            }
            if (targetPackage != null) {
                clctxt.setServletPackageName(targetPackage);
                clctxt.lockPackageName();
            }
            if (dirset) {
                clctxt.setOutputInDirs(true);
            };
            File uriDir = new File(clctxt.getRealPath("/"));
            if (uriDir.exists()) {
                if ((new File(uriDir, "WEB-INF/classes")).exists()) {
                    loader.addJar(clctxt.getRealPath("/WEB-INF/classes"));
                }
                File lib = new File(clctxt.getRealPath("WEB-INF/lib"));
                if (lib.exists() && lib.isDirectory()) {
                    String[] libs = lib.list();
                    for (int i = 0; i < libs.length; i++) {
                        try {
                            loader.addJar(lib.getCanonicalPath()
                                    + File.separator
                                    + libs[i]);
                        } catch (IOException ioe) {
                            // failing a toCanonicalPath on a file that
                            // exists() should be a JVM regression test,
                            // therefore we have permission to freak out
                            throw new RuntimeException(ioe.toString());
                        }
                    }
                }
            };
            CommandLineCompiler clc = new CommandLineCompiler(clctxt);

            clc.compile();

            targetClassName = null;
            if (mapout != null) {
                String thisServletName;
		if  (clc.getPackageName() == null) {
		    thisServletName = clc.getClassName();
		 } else {
		    thisServletName = clc.getPackageName()
                        + '.' + clc.getClassName();
		};
                mapout.write("\n\t<servlet>\n\t\t<servlet-name>");
                mapout.write(thisServletName);
                mapout.write("</servlet-name>\n\t\t<servlet-class>");
                mapout.write(thisServletName);
                mapout.write("</servlet-class>\n\t</servlet>\n\t<servlet-mapping>\n\t\t<url-pattern>");
                mapout.write(file);
                mapout.write("</url-pattern>\n\t\t<servlet-name>");
                mapout.write(thisServletName);
                mapout.write("</servlet-name>\n\t</servlet-mapping>\n");
            };
            return true;
        } catch (JasperException je) {
            je.printStackTrace(log);
            log.print("error:");
            log.println(je.getMessage());
            if (dieLevel != NO_DIE_LEVEL) {
                System.exit(dieLevel);
            }
        } catch (Exception e) {
            e.printStackTrace(log);
            log.print("ERROR:");
            log.println(e.toString());
            if (dieLevel != NO_DIE_LEVEL) {
                System.exit(dieLevel);
            }
        };
        return false;
    }


    public void parseFiles(PrintStream log)  throws JasperException {

        boolean scratchDirSet = (scratchDir != null);
        boolean urirootSet = (uriRoot != null);

        // set up a scratch/output dir if none is provided
        if (scratchDir == null) {
            String temp = System.getProperty("java.io.tempdir");
            if (temp == null) {
                temp = "";
            }
            scratchDir = new File(new File(temp).getAbsolutePath());
        }

        File f = new File(args[argPos]);
        if (!f.exists() && f.isDirectory() && (args.length - argPos == 1)) {
            // do web-app conversion
        } else if (uriRoot == null) {
            // set up the uri root if none is explicitly set
            String tUriBase = uriBase;
            if (tUriBase == null) {
                tUriBase = "/";
            }
            try {
                if (f.exists()) {
                    f = new File(f.getCanonicalPath());
                    while (f != null) {
                        File g = new File(f, "WEB-INF");
                        if (g.exists() && g.isDirectory()) {
                            uriRoot = f.getCanonicalPath();
                            uriBase = tUriBase;
                            Constants.message("jspc.implicit.uriRoot",
                                              new Object[] { uriRoot },
                                              Logger.INFORMATION);
                            break;
                        }
                        if (f.exists() && f.isDirectory()) {
                            tUriBase = "/" + f.getName() + "/" + tUriBase;
                        };
                        f = new File(f.getParent());

                        // If there is no acceptible candidate, uriRoot will
                        // remain null to indicate to the CompilerContext to
                        // use the current working/user dir.
                    }
                }
            } catch (IOException ioe) {
                // since this is an optional default and a null value
                // for uriRoot has a non-error meaning, we can just
                // pass straight through
            }
        }


        String file = nextFile();
        File froot = new File(uriRoot);
        String ubase = null;
        try {
            ubase = froot.getCanonicalPath();
        } catch (IOException ioe) {
            // if we cannot get the base, leave it null
        };

        while (file != null) {
            if (SWITCH_FILE_WEBAPP.equals(file)) {
                String base = nextFile();
                if (base == null) {
                    // friendly but quiet failure
                    break;
                }// else if (".".equals(base)) {
                //    base = "";
                //};
                String oldRoot = uriRoot;
                if (!urirootSet) {
                    uriRoot = base;
                };
                Vector pages = new Vector();

                Stack dirs = new Stack();
                dirs.push(base);
                if (extensions == null) {
                    extensions = new Vector();
                    extensions.addElement("jsp");
                };
                while (!dirs.isEmpty()) {
                    String s = dirs.pop().toString();
                    //System.out.println("--" + s);
                    f = new File(s);
                    if (f.exists() && f.isDirectory()) {
                        String[] files = f.list();
                        String ext;
                        for (int i = 0; i < files.length; i++) {
                            File f2 = new File(s, files[i]);
                            //System.out.println(":" + f2.getPath());
                            if (f2.isDirectory()) {
                                dirs.push(f2.getPath());
                                //System.out.println("++" + f2.getPath());
                            } else {
                                ext = files[i].substring(
                                        files[i].lastIndexOf('.') + 1);
                                if (extensions.contains(ext)) {
                                    //System.out.println(s + "?" + files[i]);
                                    pages.addElement(
                                        s + File.separatorChar + files[i]);
                                } else {
                                    //System.out.println("not done:" + ext);
                                };
                            };
                        };
                    };
                };

                String ubaseOld = ubase;
                File frootOld = froot;
                froot = new File(uriRoot);

                try {
                    ubase = froot.getCanonicalPath();
                } catch (IOException ioe) {
                    // if we cannot get the base, leave it null
                };

                //System.out.println("==" + ubase);


                Writer mapout;
                try {
                    if (webxmlLevel >= INC_WEBXML) {
                        File fmapings = new File(webxmlFile);
                        mapout = new FileWriter(fmapings);
                    } else {
                        mapout = null;
                    };
                    if (webxmlLevel >= ALL_WEBXML) {
                        mapout.write(Constants.getString("jspc.webxml.header"));
                    };
                } catch (IOException ioe) {
                    mapout = null;
                };

                Enumeration e = pages.elements();
                while (e.hasMoreElements())
                {
                    String nextjsp = e.nextElement().toString();
                    try {
                        if (ubase != null) {
                            File fjsp = new File(nextjsp);
                            String s = fjsp.getCanonicalPath();
                            //System.out.println("**" + s);
                            if (s.startsWith(ubase)) {
                                nextjsp = s.substring(ubase.length());
                            };
                        };
                    } catch (IOException ioe) {
                        // if we got problems dont change the file name
                    };

                    if (nextjsp.startsWith("." + File.separatorChar)) {
                        nextjsp = nextjsp.substring(2);
                    };

                    parseFile(log, nextjsp, mapout);
                };
                uriRoot = oldRoot;
                ubase = ubaseOld;
                froot = frootOld;

                if (mapout != null) {
                    try {
                        if (webxmlLevel >= ALL_WEBXML) {
                            mapout.write(Constants.getString("jspc.webxml.footer"));
                        };
                        mapout.close();
                    } catch (IOException ioe) {
                        // noting to do if it fails since we are done with it
                    }
                }
            } else {
                try {
                    if (ubase != null) {
                        File fjsp = new File(file);
                        String s = fjsp.getCanonicalPath();
                        if (s.startsWith(ubase)) {
                            file = s.substring(ubase.length());
                        };
                    }
                } catch (IOException ioe) {
                     // if we got problems dont change the file name
                };

                parseFile(log, file, null);
            };
            file = nextFile();
        }
    };

    public static void main(String arg[]) {
        if (arg.length == 0) {
           System.out.println(Constants.getString("jspc.usage"));
        } else {
            try {
                JspC jspc = new JspC(arg, System.out);
                jspc.parseFiles(System.out);
            } catch (JasperException je) {
                System.err.print("error:");
                System.err.println(je.getMessage());
                if (die != NO_DIE_LEVEL) {
                    System.exit(die);
                }
            }
        }
    };

}

