/**
 * 
 *   Copyright (C) <2006> <Sakari Kääriäinen, Mika Rajanen>
 *
 *   This program is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU General Public License as published by the Free Software Foundation; either version 2
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *   without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *   See the GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along with this program;
 *   if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *   MA 02111-1307 USA
 *
 *   Also add information on how to contact you by electronic and paper mail.
 *
 */

package checker.sourceparser;

import checker.FileID;

/**
 * Class for creating SourceParser instances.
 * 
 * @Classname 	SourceParserFactory.java	
 * @Version 	1.18
 * @Date 	15.2.2007
 * @author 	Group 14, Mika Rajanen
 */
public class SourceParserFactory {

    /**
     * Creates a new SourceParser object that represents the given source type.
     * Detection of the source file is done based on the filename (extension or
     * the full name).
     * <li>It's assumed that there are no conflicts in filenames of different
     * source types.
     * <li>Only one SourceParser object is created per source type, so it's
     * permitted to use this object to store information required by reference
     * detection.
     * 
     * @param FileID
     * 				file is the source file candidate
     * @return SourceParser
     * 				parser for parsing supported source code
     */
    public static SourceParser createSourceParser(FileID file) throws Exception  {

	/* Creates New SourceParser Objects */
	SourceParser parser = null;


	try{
	    if( isSourceFile(file) ){
		/* Test the type of source code*/
		parser = new JavaSourceParser();	

		if(!parser.isSourceFile(file)){
		    /* That was not java source code
		     * Try cpp
		     */
		    parser = new CppSourceParser();
		    if(!parser.isSourceFile(file)){
			/* That was not cpp source code
			 * Try php
			 */

			/* Add new source code parser here if needed */

			parser = new PHPSourceParser();
			if(!parser.isSourceFile(file)){
			    /* Special case:
			     * That was not any type of source code
			     * Set parser NULL
			     */
			    parser = null;							
			}											
		    }					
		}								
	    } 

	}catch(Exception e){
	    /* Exception  */
	    throw e;
	}
	/*
	 * Parser is null if file was not supported source file,
	 * otherwise method returns appropriate parser
	 */
	return parser;

    }

    /**
     * Is the given file a source file? (any type)
     * 
     * @param file
     *            Source file candidate.
     * @return true if the file is a source file.
     */
    public static boolean isSourceFile(FileID file) {

	boolean foundSourceFile = false;

	/* Make sure that all letters are lower case */
	String fName = file.name.toLowerCase();

	if( fName.endsWith(".java")
		|| fName.endsWith(".cpp") 
		|| fName.endsWith(".c")
		|| fName.endsWith(".cc")
		|| fName.endsWith(".h")
		|| fName.endsWith(".hpp")

		/* Add new file name extension here for new source code parser */

		|| fName.endsWith(".php")){ 
	    /* Found supported source file */
	    foundSourceFile=true;
	}

	return foundSourceFile;
    }

}	
