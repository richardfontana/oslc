
/**
 * 
 *   Copyright (C) <2007> <Mika Rajanen>
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

package unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import checker.CommentLine;
import checker.FileID;
import checker.Reference;
import checker.sourceparser.PHPSourceParser;

/**
 * @author mika 15.2.2007
 * This test requires:
 * 			1) unit_test_cases/ExampleSource.php for candidate file
 * 			2) unit_test_cases/ParsedExamplePHPSource.txt for comment comparing
 */

/**
 * JUnit test for PHPSourceFileParser
 */

public class PHPSourceParserTest {
	ArrayList<CommentLine> comments;
	ArrayList<Reference> references;
	ArrayList<Reference> candidateReferences;
	ArrayList<String> fileContent;
	ArrayList<String> candidateFileContent;
	FileID file;
	PHPSourceParser parser;


	/**
	 * Functions marked as "@Before" are run before any tests.
	 */
	@Before
	public void setUp() {

		BufferedReader inputStream;								
		String line;

		/* Candidate file */
		file = new FileID("unit_test_cases", "ExampleSource.php");;

		candidateReferences = new ArrayList<Reference>();
		references = new ArrayList<Reference>();
		fileContent = new ArrayList<String> ();
		candidateFileContent = new ArrayList<String> ();


		/* Read source file to string array */
		try{
			inputStream = new BufferedReader(new FileReader
					(file.path + File.separatorChar + file.name) );

			while ((line = inputStream.readLine()) != null){

				candidateFileContent.add(line);
			}
			inputStream.close();

		}catch (IOException e){
			System.err.println("Candidate file error");
		}
		parser = new PHPSourceParser();	

		/* Read already parsed (Example) comments to compare it 
		 * to candidate comments afterwards */
		try{
			inputStream = new BufferedReader(new FileReader("unit_test_cases" + 
					File.separatorChar + "ParsedExamplePHPSource.txt") );

			while ((line = inputStream.readLine()) != null){

				fileContent.add(line);
			}
			inputStream.close();

		}catch (IOException e){
			System.err.println("Reference file error");
		}

		/* Parse candidate file for comments and references */
		try{
			parser.scanFile(file, candidateFileContent);

		} catch (Exception e){
			System.out.println("Error during candidate file parsing");
		}

	}

	/**
	 * Test method for {@link checker.sourceparser.
	 * PHPSourceParser#scanFile(checker.FileID, java.util.ArrayList)}.
	 */
	@Test
	public void testScanFile() {
		/*
		 * This was executed during setup
		 */
	}

	/**
	 * Test method for {@link checker.sourceparser.PHPSourceParser#getComments()}.
	 */
	@Test
	public void testGetComments() {

		comments = parser.getComments();
		/* Verify that size of the parsed comments equals reference comments */
		assertTrue( comments.size() == fileContent.size() );
		for(int n=0; n < fileContent.size(); n++){
		    	
			String candidateLine = comments.get(n).getContent();
			/* Compare reference comments (from ParsedExampleSource.txt) 
			 * to just parsed comments */
			assertEquals( fileContent.get(n), candidateLine );
		}

	}

	/**
	 * Test method for {@link checker.sourceparser.PHPSourceParser#PHPSourceParser()}.
	 */
	@Test
	public void testPHPSourceParser() {
		/* See SourceParserFactory
		 * for initalization
		 */		
	}

	/**
	 * Test method for {@link checker.sourceparser.PHPSourceParser#getReferences()}.
	 */
	@Test
	public void testGetReferences() {
		Reference reference;
		FileID targetFile;
		
		targetFile = new FileID("unit_test_cases", "test.php");
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.STATIC_INCLUDE;
		references.add(reference);
		
		targetFile = new FileID(null, null);
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.UNPARSABLE;
		reference.information = "Reference to www document";
		references.add(reference);
		
		targetFile = new FileID("unit_test_cases", "once.php");
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.STATIC_INCLUDE;
		references.add(reference);
		
		targetFile = new FileID("unit_test_cases", "twice.php");
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.STATIC_INCLUDE;
		references.add(reference);
		
		targetFile = new FileID("unit_test_cases", "req_file1.php");
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.STATIC_INCLUDE;
		references.add(reference);
		
		targetFile = new FileID("unit_test_cases", "req_file2.php");
		reference = new Reference(file, targetFile);
		reference.referenceType = Reference.ReferenceType.UNPARSABLE;
		reference.information = "Reference include variables " +
    					"e.g. path";
		references.add(reference);
		
		/* Get candidate references */
		candidateReferences = parser.getReferences();

		assertTrue( references.size() == candidateReferences.size() );
		for(int n=0; n < references.size(); n++){

			Reference cRef = candidateReferences.get(n);
			Reference ref = references.get(n);
			/* Compare reference 
			 * to just parsed reference
			 */
			assertEquals( ref.targetFile.path, cRef.targetFile.path);
			assertEquals( ref.targetFile.name, cRef.targetFile.name);
			assertEquals( ref.referenceType, cRef.referenceType);
			assertEquals( ref.information, cRef.information);
		}




	}


	/**
	 * Test method for {@link checker.sourceparser.PHPSourceParser#isSourceFile(checker.FileID)}.
	 */
	@Test
	public void testIsSourceFile() {
		assertTrue( "Source code detected", parser.isSourceFile(file));

	}
	/**
	 * This is for Ant because it uses JUnit version 3
	 * 
	 * @return
	 */
	public static junit.framework.Test suite() {
		return new junit.framework.JUnit4TestAdapter(PHPSourceParserTest.class);
	}
   
}

