<?php
/** Comment item 0
 * PHP source file for JUnit testing 
 * 
 * Do NOT change
 */

define('NL', "\r\n");
include 'test.php';
include 'http://test_web_page.php';
include_once 'once.php';
require('twice.php');
require_once "req_file1.php";
require_once( 'ROOT_PATH' + "req_file2.php" ) ;

	
	//Single line comment test
	//				Single line test with tabs
	
	# unix test whole one row


echo 'before:'.NL; # unix test end of the row
echo 'v1=' . $v1 . NL; # block end now
echo 'v2=' . $v2 . NL; // new line comment
echo 'v3=' . $v3 . NL;
echo 'v4=' . $v4 . NL;
	
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
	char[] importQuoTest = "require ";
	char[] commentStartTest = "blaa /* blaa ";
	char[] commentEndTest = "blaa */ blaa ";
	/* b) Testing quotation inside quotation */
	char[] quoInQuoTest = "foo\"bar";
	/* c) Testing special marks inside apostrophe */
	char apoTest = ';';
	char quoTestStart = '"'; /* d) This should be visible */ char quoTestEnd = '"';
	/* e) Testing apostrophe inside apostrophe */
	char apoInApo = '\'';
	/* f) Testing apostrophe inside quotation */
	char[] apoInQuo = "foo'";
	
	
	/* Last line of test class */
?>
