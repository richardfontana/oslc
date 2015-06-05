Contents:

- Introduction
- System Requirements & Installation
- How to use the application
- OSLC 2.0 Documentation
- Bug Reports and Feedback

1. INTRODUCTION
	Open Source License Checker 2.0 is a risk management tool for analyzing open 
	source software licenses. It is developed in Java, and is platform 
	independent.
	
	Supported Features:
	- opening a single source file, or a source directory from the file system
	- opening compression packages: zip, jar, tar, tar.gz, tgz
	- identifying open source licenses from:
		* Java, PHP, and C/C++ source files 
		* Linux kernel source support
		* LICENSE.txt and COPYING.txt
	- Indicating the license matching condifence comparing to the original license
	  text.
	- highlighting the matching license text
	- displaying the license conflicts:
		* Local/reference conflicts: source file A cannot import or include source 
		  file B due to license reference restriction. (e.g GLP license source file
		  cannot import or include PHP licensed source file.)
		* Global conflicts: TODO
	- filtering source files
        - print support
	- showing found tags (author name, years, etc)
	- identificating license exceptions
	- identificating forbidden phrases
	- Summary and report on the source files in the package
	
2. SYSTEM REQUIREMENTS AND INSTALLATION
	The minimun requirement is: JRE version 1.5 or above
	
3. HOW TO USE THE APPLICATION

Quick start:
    java -jar oslc2.jar   
           to run the GUI version and
    java -jar oslc2.jar [arguments]
           to run the CLI version.

Run the program with the "-h" argument to see the CLI help screen.

When run without arguments, the GUI is started.

Script files for Unix and Windows:
   ./oslc2cli [arguments]       (Unix, CLI)
   ./oslc2gui                   (Unix, GUI)
   oslc2cli [arguments]         (Windows, CLI)
   oslc2gui                     (Windows, GUI)


For example:
   ./oslc2cli -r test_sources.zip	
	
4. OSLC 2.0 DOCUMENTATION
	https://sourceforge.net/projects/oslc/ in Documentation section
	
5. BUGS AND FEEDBACKS
	Bugs and feedback can be reported to us in sourceforge: 
	https://sourceforge.net/projects/oslc/

	