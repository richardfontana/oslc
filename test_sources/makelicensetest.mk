src = ../checker/ErrorManager.java ../checker/Pair.java ../checker/Reference.java ../checker/FileID.java 
src += ../checker/license/*.java ../checker/Log.java ../checker/LogEntry.java ../checker/Reference.java

license_database_test_src = ../unittests/LicenseDatabaseTest.java
license_test_src = ../unittests/LicenseTest.java

classes = classes
classpath = classes:../junit-4.1.jar

license_database_test = unittests/LicenseDatabaseTest.class
license_test = unittests/LicenseTest.class

$(license_test): $(src) $(license_test_src)
	@if [ ! -e $(classes) ]; then mkdir $(classes); fi
	@javac -d $(classes) -classpath $(classpath) -Xlint:unchecked $^

$(license_database_test): $(src) $(license_database_test_src)
	@if [ ! -e $(classes) ]; then mkdir $(classes); fi
	@javac -d $(classes) -classpath $(classpath) -Xlint:unchecked $^

.PHONY:

test: $(license_database_test) $(license_test) .PHONY
	@echo "Run the unit tests now."
	@java -classpath $(classpath) $(basename $(license_test))
	@java -classpath $(classpath) $(basename $(license_database_test))

