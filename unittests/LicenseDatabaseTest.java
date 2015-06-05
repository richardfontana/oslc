package unittests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

// Some imports to test the import detection
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import java.io.FileInputStream;
import java.io.*;


import checker.license.LicenseDatabase; 
import checker.license.License; 
import checker.license.ForbiddenPhrase;
import checker.license.LicenseException;
import checker.Pair;
import checker.Reference;


/**
 * This is the first target source file used for testing.
 */
public class LicenseDatabaseTest {

   private static LicenseDatabase db;
   private static String licenseDirectory = "../licenses_development2";

    @Test
    public static void testLicenseText() {
        File licenseTextFile;
        License license;
        ArrayList<String> licenseText;
        HashSet<License> licenses = db.getLicenses();
        Iterator<String> txtIterator;
        Iterator<License> it;

        for (it = licenses.iterator(); it.hasNext();) {
            BufferedReader reader;
            String str, line;
            license = it.next();

            licenseTextFile = new File(licenseDirectory + "/" + license.getId() + ".txt");

            if (!licenseTextFile.exists()) {
                System.err.println(license.getId() + " should exist but it doesn't!");
                return;
            }

            licenseText = license.getLicenseText();

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(licenseTextFile)));
                txtIterator = licenseText.iterator();

                while ((str = reader.readLine()) != null) {
                    assertTrue(txtIterator.hasNext());

                    line = txtIterator.next();
                    assertTrue(line != null);
                    assertTrue(line.equals(str));
                }
                reader.close();
            } catch(Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
        System.out.println("LicenseDatabaseTest: License text test is successful.");
    }

    @Test
    public static void testCompatibility() {
        License license;
        Iterator<License> it;
        HashSet<License> licenses = db.getLicenses();

        for (it = licenses.iterator(); it.hasNext();) {
            Pair<Reference.ReferenceType, License> compatibleLicense;
            license = it.next();

            /* Test the contents from the meta file. */
            if (license.getId().equals("gpl-2.0-l")) {
                assertTrue(license.isCompatible(db.getLicense("gpl-2.0-s"), null));
                assertTrue(license.isCompatible(db.getLicense("bsd"), null));
                assertTrue(license.isCompatible(db.getLicense("lgpl-2.1-l"), null));
                assertTrue(license.isCompatible(db.getLicense("lgpl-2.1-s"), null));
                assertTrue(license.isCompatible(db.getLicense("ms-pl"), null));

            } else if (license.getId().equals("artistic")) {
		/* bsd php-3.0-l php-3.0-s apache-1.0 apache-1.1 apache-2.0-l apache-2.0-s ipl-1.0 ms-pl */

                assertTrue(license.isCompatible(db.getLicense("bsd"), null));
                assertTrue(license.isCompatible(db.getLicense("php-3.0-l"), null));
                assertTrue(license.isCompatible(db.getLicense("apache-2.0-l"), null));

            } else if (license.getId().equals("mpl-1.1-s")) {
                /* bsd mpl-1.0-l mpl-1.0-s mpl-1.1-l cddl-1.0 ms-pl */

                assertTrue(license.isCompatible(db.getLicense("bsd"), null));
                assertTrue(license.isCompatible(db.getLicense("mpl-1.1-l"), null));
                assertTrue(license.isCompatible(db.getLicense("cddl-1.0"), null));

            } else if (license.getId().equals("php-3.0-s")) {
                /* bsd php-3.0-l apache-1.0 apache-1.1 apache-2.0-l apache-2.0-s ipl-1.0 artistic ms-pl */
                assertTrue(license.isCompatible(db.getLicense("apache-1.0"), null));
                assertTrue(license.isCompatible(db.getLicense("apache-2.0-s"), null));
                assertTrue(license.isCompatible(db.getLicense("ipl-1.0"), null));

            }
        }
        System.out.println("LicenseDatabaseTest: Compatibility test is successful.");
    }

    @Test
    public static void testForbiddenPhrases() {
        ForbiddenPhrase forbiddenPhrase = db.getForbiddenPhrase("all_rights_reserved-f");
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("apache-1.0"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("bsd"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("ipl-1.0"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("mpl-1.0-s"), null));

        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("sleepycat-bdbje"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("sleepycat-bdb"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("sleepycat-bdbxml"), null));
        assertTrue(forbiddenPhrase.isCompatible(db.getLicense("sleepycat"), null));
        System.out.println("LicenseDatabaseTest: Forbidden phrases test is successful.");
    }

    @Test
    public static void testLicenseExceptions() {
        HashSet<LicenseException> licenseExceptions = db.getLicenseExceptions();
        LicenseException licenseException;

        assertTrue(licenseExceptions != null);
        assertTrue(licenseExceptions.contains(licenseException = db.getLicenseException("gpl-2.0-m-classpath")));

        assertTrue(!licenseException.getParentLicenses().contains(db.getLicense("gpl-2.0-m-classpath")));
        assertTrue(licenseException.getParentLicenses().contains(db.getLicense("gpl-2.0-l")));
        assertTrue(licenseException.getParentLicenses().contains(db.getLicense("gpl-2.0-s")));
        
        System.out.println("LicenseDatabaseTest: License exceptions test is successful.");
    }

    public static void main(String args[]) {
        db = new LicenseDatabase(licenseDirectory);
        db.buildLicenseDatabase();
        testCompatibility();
        testLicenseText();
        testForbiddenPhrases();
        testLicenseExceptions();
    }
}
