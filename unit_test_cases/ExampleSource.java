/** Comment item 0
 * Java source file for JUnit testing 
 * 
 * Do NOT change
 */

/* package declaration */
package unit_test_cases;


/* #1 import to the checker.sourceparser package */
import checker.sourceparser.*;
/* #2 import to the checker.Reference class */
import checker.Reference;
/* #3 import standard java package */
import java.io.*;
/* #4 import test_sources */
import test_sources.HelloWorld;
/* #5 import class from the same package*/
import unit_test_cases.somePackage.SomeClass;
/* #6 import class that do not exist */
import invisibleWorld;


public class ExampleSource {
	
	//Single line comment test
	//				Single line test with tabs
	
	/**The first block comment
	 * 1.block: Stars * ** *** inside block comment
	 * 1.block: empty line with star follows (just empty line)
	 * 
	 * 1.block: empty line with two stars follow (one visible)
	 * *
	 * 1. block: short line follows (just a)
	 * a
	 * 1.block: long line follows
	 * what ever key words there are like import or package don't matter at all
	 * 1.block: line without star follows (there should not be next line..)

	 * The last line of block comment (empty line follows )
	 */
	
	/* Block comment at same line: before */ int number1 = 0;
	int number2 = 0; /* Block comment at same line: after */ 
	// Comment at same line: before (followed decl. should appear) int number3 = 0;
	int number4 = 0; // Comment line at same line: after 
	
	/* Testing special parsing cases 
	 * All of them should be bypassed 
	 * because otherwise they will confuse parsing */
	
	/* a) Testing key words and marks inside quotation */
	String importQuoTest = "import ";
	String commentStartTest = "blaa /* blaa ";
	String commentEndTest = "blaa */ blaa ";
	/* b) Testing quotation inside quotation */
	String quoInQuoTest = "foo\"bar";
	/* c) Testing special marks inside apostrophe */
	char apoTest = ';';
	char quoTestStart = '"'; /* d) This should be visible */ char quoTestEnd = '"';
	/* e) Testing apostrophe inside apostrophe */
	char apoInApo = '\'';
	/* f) Testing apostrophe inside quotation */
	String apoInQuo = "foo'";
	
	
	/* Last line of test class */
}
