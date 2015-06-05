package unittests;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import checker.FileID;
import checker.gui.tree.LicenseTree;
import checker.gui.tree.LicenseTreeNode;

public class LicenseTreeTest {

	File file = new File("/tmp/temp.zip");
	ArrayList<FileID> files = new ArrayList<FileID>();
	
	/**
	 * Functions marked as "@Before" are run before any tests.
	 */
	@Before
	public void setUp() {
		FileID file1 = new FileID("level1/level2/level3", "file1231.java");
		FileID file2 = new FileID("level1/level2/level3", "file1232.java");
		FileID file3 = new FileID("level1/level2/level3", "file1233.java");
		FileID file4 = new FileID("level1/level2", "file121.java");
		FileID file5 = new FileID("level1/level2", "file122.java");
		FileID file6 = new FileID("level1/level3", "file131.java");
		FileID file7 = new FileID("level1/level3", "file132.java");
		FileID file8 = new FileID("level1", "file1.java");
		FileID file9 = new FileID(null, "file0.java");
		
		files.add(file1);
		files.add(file2);
		files.add(file3);
		files.add(file4);
		files.add(file5);
		files.add(file6);
		files.add(file7);
		files.add(file8);
		files.add(file9);
	}
	
	/**
	 * This is a basic test case template. Rename the function when using
	 * this template!
	 */ 
	@Test
	public void testCreateTree() throws Exception {

		/* LicenseTreeNode node = LicenseJTree.getLicenseGraph(file, files);
		LicenseJTree.printTree(node, ""); */
		/* JUnit defines many assert() functions that can be used to define
		 * the test result. A single failed assertion fails the whole test,
		 * even if the other assertions are true. 
		 */
		assertTrue(true);
	}

	/**
	 * This is a basic test case template. Rename the function when using
	 * this template!
	 */ 
	@Test
	public void testTwo() throws Exception {

		/* JUnit defines many assert() functions that can be used to define
		 * the test result. A single failed assertion fails the whole test,
		 * even if the other assertions are true. 
		 */
		assertTrue(true);
	}

	/**
	 * Produce a test suite. This is required by Ant because it has a JUnit 3.x runner.
	 * Change the class name!
	 */
	public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(LicenseTreeTest.class);
    }
}
