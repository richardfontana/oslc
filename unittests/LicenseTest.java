package unittests;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import checker.license.License;
import checker.license.LicenseDatabase;
import checker.license.Tag;

public class LicenseTest {
    static LicenseDatabase db;
    static String licenseDirectory = "../licenses_development2";

    @Test
    public static void testCompareTo() {

        License license, anotherLicense;

	license = db.getLicense("gpl-2.0-l");
	anotherLicense = db.getLicense("php-3.0-l");

	assertTrue(license.compareTo(anotherLicense) < 0);
	assertTrue(anotherLicense.compareTo(license) > 0);

	assertTrue(license.compareTo("foo") > 0);
	assertTrue(license.compareTo("zoo") < 0);
	assertTrue(license.compareTo("gpl-2.0-l") == 0);

        System.out.println("LicenseTest: Testing CompareTo() is successful.");
    }

    @Test
    public static void testGetSisterLicense() {
        HashSet<License> licenses;
        Iterator<License> iterator;
        License license;

        licenses = db.getLicenses();
        for (iterator = licenses.iterator(); iterator.hasNext();) {
            license = iterator.next();
            String id = license.getId();
            if (id.endsWith("-l")) {
                assertTrue(license.getSisterLicense() == null);
            } else if (id.endsWith("-s")) {
                assertTrue(license.getSisterLicense() == db.getLicense(id.substring(0, id.lastIndexOf("-s")) + "-l"));
            } else {
                assertTrue(license.getSisterLicense() == null);
            }
        }
        System.out.println("LicenseTest: Testing getSisterLicense() is successful.");
    }

    @Test
    public static void testGetTags() {
        HashSet<License> licenses;
        Iterator<License> iterator;
        License license;

        licenses = db.getLicenses();
        for (iterator = licenses.iterator(); iterator.hasNext();) {
            license = iterator.next();
            String id = license.getId();
            ArrayList<Tag> tags = license.getTags();

            if (id.equals("apache-1.0") || id.equals("apache-1.1") || id.equals("gpl-2.0-l") ||
                id.equals("ipl-1.0.txt") || id.equals("lgpl-2.1-l")) {
                tags = license.getTags();
                assertTrue(tags.size() == 1);
                assertTrue(tags.get(0).getId().equals("<year>"));
            }
        }
        System.out.println("LicenseTest: Testing getTags() is successful.");
    }
    

    public static void main(String args[]) {
        db = new LicenseDatabase(licenseDirectory);
        db.buildLicenseDatabase();
        testCompareTo();
        testGetSisterLicense();
        testGetTags();
    }

}
