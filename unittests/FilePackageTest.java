package unittests;
import static org.junit.Assert.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.*;
import checker.filepackage.*;

public class FilePackageTest {

	private int myNumber = 0; 
	
	/**
	 * Functions marked as "@Before" are run before any tests.
	 */
	@Before
	public void setUp() {
		myNumber = 1024;		
	}
	
	/**
	 * comareZipAndDir. This test compares the results from zip package and dir.
	 */ 
	@Test
	public void compareZipAndDir() throws Exception {

        File zip = new File("test_sources"+File.separator+"test_dir.zip");
        File dir = new File("test_sources"+File.separator+"test_dir");

        FilePackage zipPack = FilePackageFactory.createFilePackage(zip);
        FilePackage dirPack = FilePackageFactory.createFilePackage(dir);
        
        Iterator<PackageFile> z = zipPack.iterator();
        int zcount = 0;
        while(z.hasNext())
        {
            z.next();
            ++zcount;
        }

        Iterator<PackageFile> d = dirPack.iterator();
        int dcount = 0;
        while(d.hasNext())
        {
            d.next();
            ++dcount;
        }

        assertTrue(zcount == dcount);
        
        z = zipPack.iterator();
	    
        while (z.hasNext()) {
            PackageFile zp = z.next();
            
            ArrayList<String> zc = zp.getContents();
            ArrayList<String> dc = dirPack.readFile(zp.getFileID());
            
            
            int i=0;
            for(i=0; i<zc.size() && i < dc.size(); ++i)
            {
                assertTrue(zc.get(i) == dc.get(i));
            }

            assertTrue(i >= zc.size());
            assertTrue(i >= dc.size());
        }
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
		//assertTrue(myNumber > 500);
		//assertTrue(myNumber < 2000);
	}

	/**
	 * Produce a test suite. This is required by Ant because it has a JUnit 3.x runner.
	 * Change the class name!
	 */
	public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(FilePackageTest.class);
    }
}
