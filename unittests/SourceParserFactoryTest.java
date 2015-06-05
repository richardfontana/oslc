/**
 * JUnit test for SourceParserFactory
 */


package unittests;

import static org.junit.Assert.*;

import checker.FileID;
import org.junit.*;
import checker.sourceparser.*;

/**
 * @author mika 26.11.2006
 *
 */
public class SourceParserFactoryTest {
		
	FileID file1;
	FileID file2;
	FileID file3;
	FileID file4;
	FileID file5;
	SourceParser parser;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* Normal java file */
		file1 = new FileID("test_sources", "Xml.java");
		/* Java file with upper and lower case characters in file extension */
		file2 = new FileID("test_sources", "Xml.JaVa");
		/* Text file - not a source code */
		file3 = new FileID("test_sources", "Xml.txt");
		/* Normal cpp source code */
		file4 = new FileID("test_sources", "Xml.cpp");
		/* File name is null - java.lang.NullPointerException */
		file5 = new FileID("test_sources", null);
		
	}

	/**
	 * Test method for {@link checker.sourceparser.SourceParserFactory#createSourceParser(checker.FileID)}.
	 */
	@Test
	public void testCreateSourceParser() {
		
		try{
			parser = SourceParserFactory.createSourceParser(file1);
		} catch(Exception e) {
			System.out.println("Error during createSourceParser");
		}
		assertTrue( "Parser created for java file", parser.isSourceFile(file1));
		
		try{
			parser = SourceParserFactory.createSourceParser(file3);
		} catch(Exception e) {
			System.out.println("Error during createSourceParser");
		}
		assertTrue( "Parser NOT created for txt file", parser==null);
		
		try{
			parser = SourceParserFactory.createSourceParser(file4);
		} catch(Exception e) {
			System.out.println("Error during createSourceParser");
		}
		assertTrue( "Parser created for cpp file", parser.isSourceFile(file4));
				
	}
	/**
	 * Test method for {@link checker.sourceparser.SourceParserFactory#createSourceParser(checker.FileID)}.
	 * 
	 * expected NullPointerException
	 */
	
	@Test	
	public void testCreateSourceParserWithNull() {
		
		try{
			parser = SourceParserFactory.createSourceParser(file5);
		} catch ( NullPointerException n){
			assertTrue("Null pointer caught", true);
		}
		catch ( Exception e){			
			System.err.println("Error during createSourceParser");
		}

		
	}

	/**
	 * Test method for {@link checker.sourceparser.SourceParserFactory#isSourceFile(checker.FileID)}.
	 */
	@Test
	public void testIsSourceFile() {
		assertTrue( "File 1 detected", SourceParserFactory.isSourceFile(file1));
		assertTrue( "File 2 detected", SourceParserFactory.isSourceFile(file2));
		assertTrue( "File 3 not detected", !(SourceParserFactory.isSourceFile(file3)) );
		assertTrue( "File 4 detected", SourceParserFactory.isSourceFile(file4) );
	}
	
	/**
	 * Test method for {@link checker.sourceparser.SourceParserFactory#isSourceFile(checker.FileID)}.
	 * 
	 * expected=java.lang.NullPointerException.class
	 */
	
	@Test (expected=java.lang.NullPointerException.class)	
	public void testIsSourceFileWithNull() {
		
		SourceParserFactory.isSourceFile(file5);
	}
	
	
	/**
	 * This is for Ant because it uses JUnit version 3
	 * 
	 * @return
	 */
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(JavaSourceParserTest.class);
    }

}
