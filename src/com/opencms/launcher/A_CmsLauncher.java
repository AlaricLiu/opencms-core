/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/launcher/Attic/A_CmsLauncher.java,v $
 * Date   : $Date: 2000/06/27 16:47:16 $
 * Version: $Revision: 1.16 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.opencms.launcher;

import com.opencms.template.*;
import com.opencms.file.*;
import com.opencms.core.*;

import java.util.*;
import java.io.*;

import javax.servlet.http.*;

/**
 * Abstract OpenCms launcher class.
 * <P>
 * This class implements basic functionality for all OpenCms launchers.
 * For each relevant file type (e.g. XML control files, plain text files,
 * JavaScript files,...) a customized launcher has to be implemented.
 * <P>
 * Every extending class has to implement the abstract methods
 * <UL>
 * <LI>getLauncherId() to indicate the type of the launcher</LI>
 * <LI>launch() to be called by initlaunch</LI>
 * </UL>
 * <P>
 * The functionality of this class is 
 * <UL>
 * <LI>provide a global cache for template class results</LI>
 * <LI>receive the system's launcher call, do some relevant initial
 * things and call the launch() method</LI>
 * <LI>provide some utility methods</LI>
 * </UL>
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.16 $ $Date: 2000/06/27 16:47:16 $
 */
abstract class A_CmsLauncher implements I_CmsLauncher, I_CmsLogChannels, I_CmsConstants  {
        
    /** Boolean for additional debug output control */
    private static final boolean C_DEBUG = false;

    /** Value of the filesystem counter, when the last template clear cache was done. */
    private static long m_lastFsCounterTemplate = 0;

    /** Value of the filesystem counter, when the last XML file clear cache was done. */
    private static long m_lastFsCounterFile = 0;

    /** The template cache that holds all cached templates */
	protected static I_CmsTemplateCache m_templateCache = new CmsTemplateCache();
    
    /** Default constructor to create a new launcher */
    /*public A_CmsLauncher() {
        if(A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "Initialized successfully.");
        }
    }*/

    /**
     * Gets the ID that indicates the type of the launcher.
     * @return launcher ID
     */
	public abstract int getLauncherId();    

    /**
     * Gets a reference to the global template cache
     * @return Template cache
     */
    public static I_CmsTemplateCache getTemplateCache() {
        return m_templateCache;
    }
    
    /**
     * Start method called by the OpenCms system to show a resource.
     * <P>
     * In this method initial values valid for all launchers can be set
     * and the _clearcache parameter is checked.
     * After this the abstract method launch(...) is called to
     * invoke the customized part of the launcher.
     * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param file CmsFile Object with the selected resource to be shown.
	 * @param startTemplateClass Name of the template class to start with.
	 * @param openCms a instance of A_OpenCms for redirect-needs
     * @exception CmsException
     */
    public void initlaunch(CmsObject cms, CmsFile file, String startTemplateClass, A_OpenCms openCms) throws CmsException {
        
        // First some debugging output.
        if(C_DEBUG && A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Launcher started for " + file.getName());
        }
        
        // Check all values to be valid        
        String errorMessage = null;
        
        if(file==null) {
            errorMessage = "Got \"null\" CmsFile object. :-(";
        }
        if(cms==null) {
            errorMessage = "Actual cms object missing";
        }                
        if(errorMessage != null) {
            if(A_OpenCms.isLogging()) {
                A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + errorMessage);                    
            }        
            throw new CmsException(errorMessage, CmsException.C_LAUNCH_ERROR);
        }        
        
        // Check the clearcache parameter        
        String clearcache = cms.getRequestContext().getRequest().getParameter("_clearcache");
        long currentFsCounter = cms.getFileSystemChanges();
        
        if((clearcache != null) && ("all".equals(clearcache) || "class".equals(clearcache))) {
            CmsTemplateClassManager.clearCache();
        }        
        
        if(((clearcache != null) && ("all".equals(clearcache) || "file".equals(clearcache)))
                || (currentFsCounter > m_lastFsCounterFile )) {
            A_CmsXmlContent.clearFileCache();                
            m_lastFsCounterFile = currentFsCounter;
        }
                
        if(((clearcache != null) && ("all".equals(clearcache) || "template".equals(clearcache)))
                || (currentFsCounter > m_lastFsCounterTemplate )) {
            m_templateCache.clearCache();
            m_lastFsCounterTemplate = currentFsCounter;
        }        
                
        launch(cms, file, startTemplateClass, openCms);
    }

    /**
 	 * Unitary method to start generating the output.
 	 * Every launcher has to implement this method.
 	 * In it possibly the selected file will be analyzed, and the
 	 * Canonical Root will be called with the appropriate 
 	 * template class, template file and parameters. At least the 
 	 * canonical root's output must be written to the HttpServletResponse.
 	 * 
	 * @param cms CmsObject Object for accessing system resources
	 * @param file CmsFile Object with the selected resource to be shown
	 * @param startTemplateClass Name of the template class to start with.
	 * @param openCms a instance of A_OpenCms for redirect-needs
     * @exception CmsException
	 */	
	protected abstract void launch(CmsObject cms, CmsFile file, String startTemplateClass, A_OpenCms openCms) throws CmsException;
    
	/**
	 * Utility method used by the launcher implementation to give control
	 * to the CanonicalRoot.
	 * The CanonicalRoot will call the master template and return a byte array of the 
	 * generated output.
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param templateClass Class that should generate the output of the master template.
	 * @param masterTemplate CmsFile Object with masterTemplate for the output.
	 * @param parameters Hashtable with all parameters for the template class.
     * @return byte array with the generated output or null if there were errors.
     * @exception CmsException
	 * 
	 */
	protected byte[] callCanonicalRoot(CmsObject cms, I_CmsTemplate templateClass, CmsFile masterTemplate, Hashtable parameters) throws CmsException {
        try {
            com.opencms.template.CmsRootTemplate root = (CmsRootTemplate)CmsTemplateClassManager.getClassInstance(cms, "com.opencms.template.CmsRootTemplate");
            return root.getMasterTemplate(cms, templateClass, masterTemplate, m_templateCache, parameters);
        } catch(Exception e) {
            // There is no document we could show.
            handleException(cms, e, "Received error while calling canonical root for requested file " + masterTemplate.getName() + ". ");
        }
        return null;
    }	

    /**
     * Utility method to handle any occurence of an execption.
     * <P>
     * If the Exception is NO CmsException (i.e. it was not detected previously)
     * it will be written to the logfile.
     * <P>
     * If the current user is the anonymous user, no further execption will
     * be thrown, but a server error will be sent
     * (we want to prevent the user from seeing any exeptions).
     * Otherwise a new Exception will be thrown.
     * 
     * @param cms CmsObject Object for accessing system resources.
     * @param e Exception that should be handled.
     * @param errorText Error message that should be shown.
     * @exception CmsException
     */
    public void handleException(CmsObject cms, Exception e, String errorText) throws CmsException {
        
        // Print out some error messages
        if(A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + errorText);
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "--> Exception: "+ e);
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "--> Cannot create output for this file. Must send error. Sorry.");
        }        

        // If the user is "Guest", we send an servlet error.
        // Otherwise we try to throw an exception.
        CmsRequestContext reqContext = cms.getRequestContext();        
        if((! C_DEBUG) && cms.anonymousUser().equals(reqContext.currentUser())) {
            throw new CmsException(errorText, CmsException.C_SERVICE_UNAVAILABLE, e);
        } else {                        
            if(e instanceof CmsException) {
                throw (CmsException)e;
            } else {
                e.printStackTrace();
                throw new CmsException(errorText, CmsException.C_LAUNCH_ERROR, e);
            }
        }
    }
    
    /**
     * Writes a given byte array to the HttpServletRespose output stream.
     * @param result byte array that should be written.
     * @param mimeType MIME type that should be set for the output.
     * @exception CmsException
     */
    protected void writeBytesToResponse(CmsObject cms, byte[] result) 
            throws CmsException {
        try {              
            I_CmsResponse resp = cms.getRequestContext().getResponse();
            OutputStream out = resp.getOutputStream();
            out.write(result);
            out.flush();
            out.close();
        } catch(Exception e) {
            String errorMessage = "Cannot write output to HTTP response stream";
            handleException(cms, e, errorMessage);
        }
    }
    
    /**
     * Calls the CmsClassManager to get an instance of the given template class.
     * The returned object is checked to be an implementing class of the interface
     * I_CmsTemplate.
     * If the template cache of the template class is not yet setted, this will
     * be done, too.
     * @param cms CmsObject object for accessing system resources.
     * @param classname Name of the requested template class.
     * @return Instance of the template class.
     * @exception CmsException.
     */
    protected I_CmsTemplate getTemplateClass(CmsObject cms, String classname) throws CmsException {
   
        if(C_DEBUG && A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "Getting start template class " + classname + ". ");
        }
        Object o = CmsTemplateClassManager.getClassInstance(cms, classname);
        
        // Check, if the loaded class really is a OpenCms template class.
        // This is done be checking the implemented interface.
            
        if(! (o instanceof I_CmsTemplate)) {
            String errorMessage = "Class " + classname + " is no OpenCms template class.";
            if(A_OpenCms.isLogging()) {
                A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsTemplateClassManager] " + errorMessage);
            }
            throw new CmsException(errorMessage, CmsException.C_XML_NO_TEMPLATE_CLASS);
        }

        I_CmsTemplate cmsTemplate = (I_CmsTemplate)o;
        
        if(!cmsTemplate.isTemplateCacheSet()) {
            cmsTemplate.setTemplateCache(m_templateCache);
        }        
        return cmsTemplate;        
    }

    /**
     * Gets the name of the class in the form "[ClassName] "
     * This can be used for error logging purposes.
     * @return name of this class
     */
    protected String getClassName() {
        String name = getClass().getName();
        return "[" + name.substring(name.lastIndexOf(".") + 1) + "] ";
    }
}
