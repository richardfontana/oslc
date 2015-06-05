/**
 * 
 *   Copyright (C) 2006 Lauri Koponen
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

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import checker.CommentLine;
import checker.license.License;
import checker.matching.LicenseMatch;
import checker.matching.LicenseMatcher;
import checker.matching.MatchPosition;
import checker.matching.LicenseMatcher.MatchAlgorithm;

/**
 * @author Lauri Koponen
 * 
 */
public class LicenseMatcherTest {

	/**
	 * A simple test case for LicenseMatcher.match().
	 * 
	 */
	@Test
	public void testMatch() throws Exception {

		class TestLicense extends License {

			TestLicense() {
				name = "TEST";

				ArrayList<String> text = new ArrayList<String>();
				text.add("bar baz foo");
				setLicenseText(text);
				
			}
		}

		License license = new TestLicense();

		ArrayList<CommentLine> comments = new ArrayList<CommentLine>();
		comments.add(new CommentLine("foo bar baz", 0, 0));
		comments.add(new CommentLine("Foo baf bag", 1, 0));

		ArrayList<MatchPosition> expectedPositions = new ArrayList<MatchPosition>();
		expectedPositions.add(new MatchPosition(0, 4, 1, 2, 0, 0, 0, 10, 0, 3));

		
		LicenseMatch match = LicenseMatcher.match(comments, license,
				MatchAlgorithm.PARTIAL);

		
		assertTrue(match.getMatchPositions().get(0).equals(
				expectedPositions.get(0)));
	}

	
	/**
	 * Test method for
	 * {@link checker.matching.LicenseMatcher#matchForbiddenPhrases(java.util.ArrayList, java.util.ArrayList)}.
	 */
	/*
	 * @Test public void testMatchForbiddenPhrases() { fail("Not yet
	 * implemented"); }
	 */

	/**
	 * Produce a test suite. This is required by Ant because it has a JUnit 3.x runner.
	 */
	public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(LicenseMatcherTest.class);
    }

}
