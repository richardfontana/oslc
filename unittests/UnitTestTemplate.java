package unittests;
import static org.junit.Assert.*;

import org.junit.*;

public class UnitTestTemplate {

	private int myNumber = 0; 
	
	/**
	 * Functions marked as "@Before" are run before any tests.
	 */
	@Before
	public void setUp() {
		myNumber = 1024;		
	}
	
	/**
	 * This is a basic test case template. Rename the function when using
	 * this template!
	 */ 
	@Test
	public void testOne() throws Exception {

		boolean testResult = true;

		/* JUnit defines many assert() functions that can be used to define
		 * the test result. A single failed assertion fails the whole test,
		 * even if the other assertions are true. 
		 */
		assertTrue(testResult);
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
		assertTrue(myNumber > 500);
		assertTrue(myNumber < 2000);
	}

	/**
	 * Produce a test suite. This is required by Ant because it has a JUnit 3.x runner.
	 * Change the class name!
	 */
	public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(UnitTestTemplate.class);
    }
}
