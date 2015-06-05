/**
 * 
 *   Copyright (C) <2006> <Veli-Jussi Raitila>
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

package checker.gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jdesktop.swingworker.SwingWorker;

import checker.ErrorManager;
import checker.FileID;
import checker.LicenseChecker;
import checker.Log;
import checker.LogEntry;
import checker.Pair;
import checker.Reference;
import checker.event.LicenseEvent;
import checker.event.LicenseProcessEvent;
import checker.event.LicenseProcessListener;
import checker.gui.combo.CriteriaBoxModel;
import checker.gui.filter.Criteria;
import checker.gui.filter.LicenseCriterion;
import checker.gui.filter.MatchCriterion;
import checker.gui.table.ConflictTableModel;
import checker.gui.table.ConflictTableRow;
import checker.gui.table.CountTableModel;
import checker.gui.tree.Directory;
import checker.gui.tree.FileAbstract;
import checker.gui.tree.FileLicense;
import checker.gui.tree.FileReference;
import checker.gui.tree.FileSource;
import checker.gui.tree.FileUnknown;
import checker.gui.tree.LicenseTreeModel;
import checker.gui.tree.LicenseTreeNode;
import checker.license.License;

/**
 * Main GUI frame for LicenseChecker
 *  
 * @author Veli-Jussi Raitila
 * 
 */
public class LicenseMain extends javax.swing.JFrame implements LicenseProcessListener {
    
    // The file separator used on this platform
    public static String fileSeparator;
    // A LicenseChecker instance
    private LicenseChecker lc;
    // Processing task (SwingWorker)
    private CheckerTask checkerTask;
    // Opening task (SwingWorker)
    private OpenTask openTask;
    // Package being analyzed, chosen by the user
    private File activePackage;
    // Open tabs, keep track of these
    private HashMap<FileAbstract, LicenseTab> openTabs;
    // Current file the user is browsing
    private ListIterator<FileAbstract> currentFile;
    // File counts
    private int 
    	srcCount, licCount, unkCount, allCount, 
    	disLicCount, confRefCount, confGblCount;
    
    /**
     * Creates new form LicenseMain
     */
    public LicenseMain() {
        initComponents();
        initHelp();
        
        /* Fix file separator for regexps in Windows */
        if ("\\".equals(File.separator)) 
            LicenseMain.fileSeparator = "\\\\";
        else
        	LicenseMain.fileSeparator = File.separator;
        
        openTabs = new HashMap<FileAbstract, LicenseTab>();
        currentFile = new LinkedList<FileAbstract>().listIterator();
    }
    
	public void fileOpenBegun(LicenseEvent e) {
		/* FIXME Prevent the user from generating events */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	statusLabel.setText("Opening file");
                statusBar.setValue(0);
                statusBar.setIndeterminate(true);
            }
        });
	}

	public void fileOpenEnded(LicenseEvent e) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	statusLabel.setText("File opened");
                statusBar.setIndeterminate(false);
            }
        });
	}

	public void processBegun(LicenseEvent e) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                processBar.setIndeterminate(false);
        		processLabel.setText("Begin processing files.");
            }
        });
	}

    public void fileProcessed(LicenseProcessEvent e) {
    	final int i = e.getFileIndex();
    	final int c = e.getFileCount();
    	final FileID f = e.getFile();

    	java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
        		processBar.setValue(i * 100 / c);
        		processLabel.setText("[" + i + "/" + c + "] " + f.name);
            }
        });
	}

	public void processEnded(LicenseEvent e) {
		java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
        		processLabel.setText("Processing finished.");
        		processDialog.setVisible(false);
            }
        });
	}

	public void processCancelled(LicenseEvent e) {
        checkerTask.cancel(false);
		java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
        		processLabel.setText("Processing cancelled.");
        		processDialog.setVisible(false);
            }
        });
	}

    /**
     * Check a particular file/dir/package for licenses
     * Display a dialog and start a worker thread.
     */
    private void runChecker() {

    	if (lc != null) {
	    	int value = JOptionPane.showConfirmDialog(
	    			this, 
	    			"Do you want to open a new package?\n" +
	    			"The current one will close.",
	    			"Confirm package open",
	    			JOptionPane.YES_NO_OPTION
	    			);
	    	
	    	if (value == JOptionPane.NO_OPTION)
	    		return;
    	}

    	/* Create a new tree */
    	licenseTree.reset();
    	
    	/* Create a new lc instance */
        lc = new LicenseChecker();
        lc.addProcessingListener(this);

        /* Close all open files */
        closeAllTabs();
        
        /* Start the processing task */
        checkerTask = new CheckerTask();
        checkerTask.execute();

        processBar.setValue(0);
        processBar.setIndeterminate(true);
		processLabel.setText("Unpacking files.");

        processDialog.pack();
        processDialog.setLocationRelativeTo(this);
        processDialog.setVisible(true);
    }

    /**
     * A worker thread that opens a file
     */
    class OpenTask extends SwingWorker<ArrayList<String>, Void> {

    	private FileAbstract fileabs;
    	private FileID file;
    	
    	public OpenTask(FileAbstract f) {
    		fileabs = f;
    		file = f.getFileID();
    	}
    	
    	@Override
        protected ArrayList<String> doInBackground() {
    		ArrayList<String> lines = null;
    		
	        try {
	        	lines = lc.readFile(file);
	        } catch (Exception e) {
	        	ErrorManager.error("File contents cannot be shown", e);
	        	/*		
	        	statusLabel.setText("File contents cannot be shown");

	        	JOptionPane.showMessageDialog(LicenseMain.this,
	        		    "File contents cannot be shown",
	        		    "File not found",
	        		    JOptionPane.ERROR_MESSAGE);
	        		    */
	        }
	        
	        return lines;
        }
        
        @Override
        protected void done() {
        	if (!isCancelled()) {
    	        try {
        	        LicenseTab tab = new LicenseTab();

        	        tab.setWordWrap(wrapBox.getState());
    	        	tab.setFile(get(), fileabs);

    	        	tabbedPane.addTab(file.name, tab);
		            tabbedPane.setSelectedComponent(tab);
		            openTabs.put(fileabs, tab);

		            while (currentFile.hasNext()) {
		            	currentFile.next();
		            	currentFile.remove();
		            }
		            currentFile.add(fileabs);

		            statusLabel.setText("Done");
    	        }
    	        catch (InterruptedException ignore) {}
		        catch (java.util.concurrent.ExecutionException e) {
		        	ErrorManager.error("Error inserting file to a tab", e);
		        }
        	}
        	
        	updateToolBar();
            // statusBar.setIndeterminate(false);
        }
    	
    }

    /**
     * A worker thread that opens a package, processes it
     * and inserts the results in a hierarchical structure
     */
    class CheckerTask extends SwingWorker<LicenseTreeNode, Void> {

        @Override
        public LicenseTreeNode doInBackground() {
            LicenseTreeNode node = null;

            /* Try opening a package */
            try {
            	lc.openPackage(activePackage);
            } catch (Exception e) {
        		ErrorManager.error("Error opening package from the GUI", e);
            }

            /* Continue processing. Exceptions occurring here
             * are caught in 'done'
             */
            lc.processPackage();
            
        	ArrayList<FileID> sourceFiles = lc.getSourceFiles();
        	ArrayList<FileID> licenseFiles = lc.getLicenseFiles();
        	ArrayList<FileID> unknownFiles = lc.getUnknownFiles();
        	File rootFile = lc.getPackage().getRootFile();

        	// Retrieve some information to be displayed in the overview
        	srcCount = sourceFiles.size();
        	licCount = licenseFiles.size();
        	unkCount = unknownFiles.size();
        	disLicCount = lc.getLicenseCounts().size();
        	confRefCount = lc.getNumReferenceConflicts(); 
        	confGblCount = lc.getGlobalLicenseConflicts().size() / 2;
        	
        	allCount = srcCount + licCount + unkCount;

        	/* Read several files.
    		 * Insert them in a hierarchical data structure
    		 * separating file types, package/dir name as root
    		 */
    		Directory root = new Directory(rootFile.getName());

    		Directory curdir = root; 

    		/* Parse through source files and add them to a 
    		 * hierarchical structure. Parsing is done
    		 * by tokenizing the file path with a file separator  
    		 */
    		for (FileID file : sourceFiles) {
    			if (file.path != null) {
    				String[] dirs = file.path.split(LicenseMain.fileSeparator);
    				for (String dir : dirs) {
    					if(!curdir.hasFile(dir)) 
    						curdir.addFile(new Directory(dir)); 
    					curdir = (Directory)curdir.getFile(dir);
    				}
    			}

    			/*
    			if(lc.getLicenseMatches(file) == null) {
    				for (Exception e : lc.getFileExceptions(file)) {
    					System.err.println(file);
    					e.printStackTrace();
    				}
    			}
    			*/

    			FileSource source = new FileSource(file, lc.getLicenseMatches(file));
    			
    			/* Add references and associate conflicts with them */
    			ArrayList<Reference> refs = lc.getReferences(file);
    			if (refs != null)
    			for (Reference ref : refs) {
        			ArrayList<Pair<License, License>> conflicts = lc.getLicenseConflicts(ref);
       				source.addReference(new FileReference(ref, conflicts));
    			}

    			/* Add a possible self-reference ie. file conflicts with itself. */
    			ArrayList<Pair<License, License>> selfconflicts = lc.getInternalLicenseConflicts(file);
    			if (selfconflicts != null) {  
					Reference self = new Reference(file, file);
    				self.referenceType = Reference.ReferenceType.IMPORT;
    				self.declaration = "(self)";
        			source.addReference(new FileReference(self, selfconflicts)); 
    			}
    			
    			curdir.addFile(source);
    			curdir = root;
    		}

    		/* Parse through license files.
    		 * As above
    		 */
    		for (FileID file : licenseFiles) {
    			if (file.path != null) {
    				String[] dirs = file.path.split(LicenseMain.fileSeparator);
    				for (String dir : dirs) {
    					if(!curdir.hasFile(dir)) 
    						curdir.addFile(new Directory(dir)); 
    					curdir = (Directory)curdir.getFile(dir);
    				}
    			}
    			curdir.addFile(new FileLicense(file, lc.getLicenseMatches(file)));
    			curdir = root;
    		}

    		/* Parse through unknown files.
    		 * As above
    		 */
    		for (FileID file : unknownFiles) {
    			if (file.path != null) {
    				String[] dirs = file.path.split(LicenseMain.fileSeparator);
    				for (String dir : dirs) {
    					if(!curdir.hasFile(dir)) 
    						curdir.addFile(new Directory(dir)); 
    					curdir = (Directory)curdir.getFile(dir);
    				}
    			}
    			curdir.addFile(new FileUnknown(file));
    			curdir = root;
    		}

    		node = root;

        	return node;
        }

        @Override
        protected void done() {
            Toolkit.getDefaultToolkit().beep();

    		/* If processing is complete and not cancelled,
    		 * Set the generated model to the tree and
             * update other views to display the results.
             */

        	licenseTree.setRootVisible(true);

        	if (!isCancelled()) {
	            try {
	            	licenseTree.setModel(new LicenseTreeModel(get()));
	                licenseTree.showReferences(referencesBox.isSelected());
	            } 
	            catch (InterruptedException ignore) {}
	            catch (java.util.concurrent.ExecutionException e) {
	            	ErrorManager.error("Could not generate a tree from the chosen file/dir", e);
	            }
            } else lc = null; // Destroy license checker if cancelled

            /* Update the overview panel */
            updateOverview();
            /* Update the tool bar */
            updateToolBar();
            /* Update filtering criteria */
            updateCriteria();
            /* Update the tree (apply filtering) */
            updateFiltering();
        }
    }

    /**
     * Update the combo box that displays filtering criteria
     */
    private void updateCriteria() {
		CriteriaBoxModel critmodel = new CriteriaBoxModel();
		criteriaDialog.setTitle("Choose Criteria");
		
		switch (filterCombo.getSelectedIndex()) {
    	case 0:
    	case 1:
    	case 2:
    		criteriaButton.setEnabled(false);
    		break;
    	case 3:
        	if (lc != null) {
		    	for (License license : lc.getLicenseCounts().keySet()) {
		    		critmodel.addCriteria(new Criteria(new LicenseCriterion(license)));
		    	}
        	}
    		criteriaDialog.setTitle("Choose License");
        	criteriaButton.setEnabled(true);
    		break;
    	case 4:
	    	for (float i = 0.2f; i <= 0.5f; i += 0.1f) {
	    		critmodel.addCriteria(new Criteria(new MatchCriterion(i)));
	    	}
			criteriaDialog.setTitle("Choose Match%");
        	criteriaButton.setEnabled(true);
    		break;
    	default:
    		// Undefined filter, do nothing
    		break;
		}

		criteriaCombo.setModel(critmodel);
    }
    
    /**
     * Apply tree filtering
     */
    private void updateFiltering() {
    	if (allCount == 1) licenseTree.setRootVisible(false);
    	
		Object selected = criteriaCombo.getSelectedItem(); 
		Criteria c = Criteria.ALL; 

		switch (filterCombo.getSelectedIndex()) {
    	case 0:
    		licenseTree.applyFilter(LicenseTreeModel.FilterType.ALL, Criteria.ALL);
    		break;
    	case 1:
    		licenseTree.applyFilter(LicenseTreeModel.FilterType.CONFLICTS, Criteria.ALL);
    		break;
    	case 2:
    		licenseTree.applyFilter(LicenseTreeModel.FilterType.MISSING, Criteria.ALL);
    		break;
    	case 3:
    		if (selected instanceof Criteria) c = (Criteria)selected;
    		licenseTree.applyFilter(LicenseTreeModel.FilterType.LICENSED, c);
    		break;
    	case 4:
    		if (selected instanceof Criteria) c = (Criteria)selected;
    		licenseTree.applyFilter(LicenseTreeModel.FilterType.UNCERTAIN, c);
    		break;
    	default:
    		// Undefined filter, do nothing
    		break;
    	}
    }
    
    /**
     * Updates the overview panel
     */
    private void updateOverview() {
		String na = "<html><font color=\"gray\">0</font></html>";
		String none = "<html><font color=\"green\">0</font></html>";
		String count = "<html>%d</html>";
		String count_red = "<html><font color=\"red\">%d</font></html>";

		if (lc == null) {
    		overviewHyperLink.setActive(false);
            overviewDialog.setVisible(false);
        	srcCountLabel.setText(na);
        	allCountLabel.setText(na);
        	disLicCountLabel.setText(na);
        	confRefCountLabel.setText(na);
        	confGblCountLabel.setText(na);
        	return;
    	}
    	
    	overviewHyperLink.setActive(true);
    	
    	/* Set global counts in the overview panel */
    	if (srcCount == 0) srcCountLabel.setText(none);
        else srcCountLabel.setText(String.format(count, srcCount));

        if (allCount == 0) allCountLabel.setText(none);
        else allCountLabel.setText(String.format(count, allCount));

        if (disLicCount == 0) disLicCountLabel.setText(none);
        else disLicCountLabel.setText(String.format(count, disLicCount));

        if (confRefCount == 0) confRefCountLabel.setText(none);
        else confRefCountLabel.setText(String.format(count_red, confRefCount));

        if (confGblCount == 0) confGblCountLabel.setText(none);
        else confGblCountLabel.setText(String.format(count_red, confGblCount));
        
        CountTableModel licmodel = new CountTableModel(lc.getLicenseCounts(), lc.getMaxMatchPrs());
        matchesTable.setModel(licmodel);

        /** 
         * Populate conflict table with global conflicts,
         * filtering duplicates.
         */
        ConflictTableModel confmodel = new ConflictTableModel();
        for (Pair<License, License> pair : lc.getGlobalLicenseConflicts()) {
        	if (pair.e1.compareTo(pair.e2) < 0)
        	confmodel.addRow(new ConflictTableRow(
        			pair.e1.getId(),
        			pair.e2.getId()));
        }
        conflictsTable.setModel(confmodel);
    }
    
    private void updateToolBar() {
    	if (lc == null) currentFile = new LinkedList<FileAbstract>().listIterator();
    	
        firstButton.setEnabled(currentFile.previousIndex() > 0);
        prevButton.setEnabled(currentFile.previousIndex() > 0);
   		nextButton.setEnabled(currentFile.hasNext());
   		lastButton.setEnabled(currentFile.hasNext());
    }
    
    /**
     * Jump to a given reference in a tree
     * if applicable.
     * 
     * @param ref
     */
    private void jumptoReference(FileReference ref) {
        try {
        	LicenseTreeNode node = licenseTree.findNode(ref);
           	if (node instanceof FileAbstract) showFile((FileAbstract)node);
        } catch (Exception e) {
        	String reason = ref.getReference().information;
        	if (reason == null) reason = "File not found";
        	JOptionPane.showMessageDialog(this,
        		    reason,
        		    "Referred file cannot be shown",
        		    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Jump to a given file in a tree
     * if applicable.
     * 
     * @param ref
     */
    private void jumptoFile(FileAbstract ref) {
        try {
        	LicenseTreeNode node = licenseTree.findNode(ref);
           	if (node instanceof FileAbstract) {
           		FileAbstract file = (FileAbstract) node;
           		if(openTabs.containsKey(file)) switchFile(file); 
           	}
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(this,
        		    "File not found",
        		    "File cannot be shown",
        		    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void switchFile(FileAbstract file) {
    	tabbedPane.setSelectedComponent(openTabs.get(file));
    }
    
    /**
     * Start an opening task and upon completion 
     * show file contents in a tab
     * 
     * @param fileabs
     */
    private void showFile(FileAbstract fileabs) {
        
        /* If the file is viewable, populate a tab and show it */
        if(!fileabs.isViewable()) {
    		JOptionPane.showMessageDialog(this, "File is not viewable");
    	} else {
            openTask = new OpenTask(fileabs);
            openTask.execute();
    	}
    }

    /* NOTE 
     * Converted into a task
     * 
    private void showFile_DEL(FileAbstract fileabs) {

        FileID file = fileabs.getFileID();
        
        // Check if tab is already open 
        if(openTabs.containsKey(file)) {
            tabbedPane.setSelectedComponent(openTabs.get(file));
        	return;
        }

        // If the file is viewable, populate a tab and show it 
        if(!fileabs.isViewable()) {
    		JOptionPane.showMessageDialog(this, "File is not viewable");
    	} else {

	        LicenseTab tab = new LicenseTab();
	        try {

	        	// Insert file contents onto the tab
	        	// and associate the FileAbstract it
	        	// represents 
	        	tab.setWordWrap(wrapBox.getState());
	        	tab.setFile(
	        			lc.readFile(file),
	        			fileabs
	        			);
	        	
	        	tabbedPane.addTab(file.name, tab);
	            tabbedPane.setSelectedComponent(tab);
	            openTabs.put(file, tab);

	        } catch (FileNotFoundException e) {
	        	JOptionPane.showMessageDialog(this,
	        		    "File contents cannot be shown",
	        		    "File not found",
	        		    JOptionPane.ERROR_MESSAGE);
	        } catch (Exception e) {
	        	ErrorManager.error("Error inserting file to a tab", e);
	        }
        }

    }
	*/

    private void closeTab(LicenseTab tab) {
    	FileAbstract fileabs = tab.getFile();
    	
    	tabbedPane.remove(tab);
    	openTabs.remove(fileabs);
    }
    
    private void closeAllTabs() {
        tabbedPane.removeAll();
        openTabs.clear();
    }
    
    /**
     * Initialize application help
     */
    private void initHelp() {
    	try {
	    	ClassLoader cl = LicenseMain.class.getClassLoader();
	    	URL url = HelpSet.findHelpSet(cl, "documentation/OSLC.hs");
	    	JHelp helpViewer = new JHelp(new HelpSet(cl, url));
	    	helpFrame.getContentPane().add(helpViewer);
    	} catch (Exception e) {
    		ErrorManager.error("HelpSet not found", e);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        fileChooser = new javax.swing.JFileChooser();
        tabPopup = new javax.swing.JPopupMenu();
        closeTabItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        printTabItem = new javax.swing.JMenuItem();
        prefsDialog = new javax.swing.JDialog(this);
        prefsPanel = new javax.swing.JPanel();
        prefsCancel = new javax.swing.JButton();
        prefsOk = new javax.swing.JButton();
        prefsLabel = new javax.swing.JLabel();
        processDialog = new javax.swing.JDialog(this);
        processLabel = new javax.swing.JLabel();
        processBar = new javax.swing.JProgressBar();
        processCancel = new javax.swing.JButton();
        overviewDialog = new javax.swing.JDialog();
        overviewDlgPanel = new javax.swing.JPanel();
        overviewDlgPane = new javax.swing.JTabbedPane();
        matchesPane = new javax.swing.JScrollPane();
        matchesTable = new javax.swing.JTable();
        conflictsPane = new javax.swing.JScrollPane();
        conflictsTable = new javax.swing.JTable();
        aboutDialog = new javax.swing.JDialog();
        aboutLabel1 = new javax.swing.JLabel();
        aboutLabel2 = new javax.swing.JLabel();
        aboutScrollPane = new javax.swing.JScrollPane();
        aboutTextPane = new javax.swing.JTextPane();
        criteriaDialog = new javax.swing.JDialog();
        criteriaCombo = new javax.swing.JComboBox();
        helpFrame = new javax.swing.JFrame();
        mainPanel = new javax.swing.JPanel();
        splitPane = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        overviewPanel = new javax.swing.JPanel();
        overviewLabel1 = new javax.swing.JLabel();
        srcCountLabel = new javax.swing.JLabel();
        overviewLabel2 = new javax.swing.JLabel();
        allCountLabel = new javax.swing.JLabel();
        overviewLabel3 = new javax.swing.JLabel();
        disLicCountLabel = new javax.swing.JLabel();
        overviewLabel4 = new javax.swing.JLabel();
        confRefCountLabel = new javax.swing.JLabel();
        overviewLabel5 = new javax.swing.JLabel();
        confGblCountLabel = new javax.swing.JLabel();
        overviewLabel6 = new javax.swing.JLabel();
        overviewHyperLink = new checker.gui.LicenseHyperLink();
        treePanel = new javax.swing.JPanel();
        filterCombo = new javax.swing.JComboBox();
        referencesBox = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        licenseTree = new checker.gui.tree.LicenseTree();
        criteriaButton = new javax.swing.JButton();
        rightPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        toolBar = new javax.swing.JToolBar();
        openButton = new javax.swing.JButton();
        firstButton = new javax.swing.JButton();
        prevButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        lastButton = new javax.swing.JButton();
        statusPanel = new javax.swing.JPanel();
        statusBar = new javax.swing.JProgressBar();
        statusLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openItem = new javax.swing.JMenuItem();
        closeItem = new javax.swing.JMenuItem();
        closeAllItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        printItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        checkoutItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        quitItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        overviewBox = new javax.swing.JCheckBoxMenuItem();
        wrapBox = new javax.swing.JCheckBoxMenuItem();
        optionsMenu = new javax.swing.JMenu();
        prefsItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        aboutItem = new javax.swing.JMenuItem();

        fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        closeTabItem.setText("Close File");
        closeTabItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeTabItemActionPerformed(evt);
            }
        });

        tabPopup.add(closeTabItem);

        tabPopup.add(jSeparator3);

        printTabItem.setText("Print File");
        printTabItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printTabItemActionPerformed(evt);
            }
        });

        tabPopup.add(printTabItem);

        prefsDialog.setTitle("Preferences");
        prefsDialog.setModal(true);
        prefsCancel.setText("Cancel");
        prefsCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prefsCancelActionPerformed(evt);
            }
        });

        prefsOk.setText("OK");
        prefsOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prefsOkActionPerformed(evt);
            }
        });

        prefsLabel.setText("Nothing here yet...");

        org.jdesktop.layout.GroupLayout prefsPanelLayout = new org.jdesktop.layout.GroupLayout(prefsPanel);
        prefsPanel.setLayout(prefsPanelLayout);
        prefsPanelLayout.setHorizontalGroup(
            prefsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, prefsPanelLayout.createSequentialGroup()
                .addContainerGap(254, Short.MAX_VALUE)
                .add(prefsCancel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(prefsOk)
                .addContainerGap())
            .add(prefsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(prefsLabel)
                .addContainerGap(275, Short.MAX_VALUE))
        );
        prefsPanelLayout.setVerticalGroup(
            prefsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, prefsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(prefsLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 236, Short.MAX_VALUE)
                .add(prefsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(prefsOk)
                    .add(prefsCancel))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout prefsDialogLayout = new org.jdesktop.layout.GroupLayout(prefsDialog.getContentPane());
        prefsDialog.getContentPane().setLayout(prefsDialogLayout);
        prefsDialogLayout.setHorizontalGroup(
            prefsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(prefsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
        prefsDialogLayout.setVerticalGroup(
            prefsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(prefsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
        processDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        processDialog.setTitle("Processing files");
        processDialog.setModal(true);
        processDialog.setResizable(false);
        processDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                processDialogWindowClosing(evt);
            }
        });

        processLabel.setText("Please wait.");

        processCancel.setText("Cancel");
        processCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processCancelActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout processDialogLayout = new org.jdesktop.layout.GroupLayout(processDialog.getContentPane());
        processDialog.getContentPane().setLayout(processDialogLayout);
        processDialogLayout.setHorizontalGroup(
            processDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(processDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(processDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(processLabel)
                    .add(processDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(processCancel)
                        .add(processBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        processDialogLayout.setVerticalGroup(
            processDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(processDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(processLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(processBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(processCancel)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        overviewDialog.setTitle("Details");
        overviewDlgPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Package Overview"));
        matchesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "License", "Count"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        matchesPane.setViewportView(matchesTable);

        overviewDlgPane.addTab("Matches", matchesPane);

        conflictsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "License 1", "Icon", "License 2"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        conflictsPane.setViewportView(conflictsTable);

        overviewDlgPane.addTab("Conflicts (global)", conflictsPane);

        org.jdesktop.layout.GroupLayout overviewDlgPanelLayout = new org.jdesktop.layout.GroupLayout(overviewDlgPanel);
        overviewDlgPanel.setLayout(overviewDlgPanelLayout);
        overviewDlgPanelLayout.setHorizontalGroup(
            overviewDlgPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(overviewDlgPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        overviewDlgPanelLayout.setVerticalGroup(
            overviewDlgPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(overviewDlgPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout overviewDialogLayout = new org.jdesktop.layout.GroupLayout(overviewDialog.getContentPane());
        overviewDialog.getContentPane().setLayout(overviewDialogLayout);
        overviewDialogLayout.setHorizontalGroup(
            overviewDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(overviewDlgPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        overviewDialogLayout.setVerticalGroup(
            overviewDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(overviewDlgPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        aboutDialog.setTitle("About OSLC");
        aboutLabel1.setText("Open Source License Checker");

        aboutLabel2.setText("version 2.0, 2007");

        aboutTextPane.setContentType("text/html");
        aboutTextPane.setEditable(false);
        //aboutTextPane.setText("<html>\n<p>This software is released under GPL 2.0 license.</p>\n<p>This application uses:</p>\n<ul>\n<li>the Silk icon set by Mark James <a href=\"http://www.famfamfam.com/\">http://www.famfamfam.com</a> released under a Creative Commons Attribution 2.5 License</li>\n<li>javatar-2.5 tar file reader by Timothy Gerard Endres <a href=\"mailto:time@gjt.org\">time@gjt.org</a> <a href=\"http://www.trustice.com\">http://www.trustice.com</a> under public domain license</li>\n</ul>\n<p>Creditors:<br>\nJing Jing-Helles<br>\nSakari K\u00e4\u00e4ri\u00e4inen<br>\nYuan Yuan<br>\nLauri Koponen<br>\nVeli-Jussi Raitila<br>\nMika Rajanen<br>\nXie Xiaolei<br>\nJussi Sirpoma<br>\n</p>\n<p>\nFor more info about the application:<br>\n<a href=\"http://sourceforge.net/projects/oslc/\">http://sourceforge.net/projects/oslc/</a><br>\n</p>\n<p>\nThis project is created for the T-76.4115 course in<br>\nSoftware Engineering and Business lab, <br>\nin Computer Science and Engineering department <br>\nof Helsinki University of Technology<br>\n</p>\n<p>\nSpecial thanks to Prof. Juha Laine, Ville Oksanen and Seppo Sahi<br>\nfor guiding us throughout the project.<br>\n</p>\n</html>\n");
        aboutTextPane.setText("<html>\n<p>This software is released under GPL 2.0 license.</p>\n<p>This application uses:</p>\n<ul>\n<li>the Silk icon set by Mark James <a href=\"http://www.famfamfam.com/\">http://www.famfamfam.com</a> released under a Creative Commons Attribution 2.5 License</li>\n<li>javatar-2.5 tar file reader by Timothy Gerard Endres <a href=\"mailto:time@gjt.org\">time@gjt.org</a> <a href=\"http://www.trustice.com\">http://www.trustice.com</a> under public domain license</li>\n<li>swing-layout-1.0.jar a part of NetBeans GUI Builder <a href=\"http://www.netbeans.org\">http://www.netbeans.org</a> under Lesser General Public License (LGPL)\n</li><li>swing-worker.jar originally published in 1998 by Hans Muller and Kathy Walrath. The last version known was published by Joseph Bowbeer. <a href=\"https://swingworker.dev.java.net\">https://swingworker.dev.java.net</a> under Lesser General Public License (LGPL)\n</li><li>easyprint.jar by Eyer Leander at Eyer IT Services, Naters.<a href=\"https://easyprint.dev.java.net/\">https://easyprint.dev.java.net/</a> under Lesser General Public License (LGPL)\n</li><li>jhbasic.jar by Sun Microsystems. <a href=\"http://java.sun.com/products/javahelp/index.jsp\">http://java.sun.com/products/javahelp/index.jsp</a> see LICENSE.txt for license description.\n</li><li>activation.jar by Sun Microsystems. <a href=\"http://java.sun.com/products/javabeans/jaf/downloads/index.html\">http://java.sun.com/products/javabeans/jaf/downloads/index.html</a> see LICENSE.txt for license description.\n</li></ul>\n<p>Creditors:<br>\nJing Jing-Helles<br>\nSakari K\u00e4\u00e4ri\u00e4inen<br>\nYuan Yuan<br>\nLauri Koponen<br>\nVeli-Jussi Raitila<br>\nMika Rajanen<br>\nXie Xiaolei<br>\nJussi Sirpoma<br>\n</p>\n<p>\nFor more info about the application:<br>\n<a href=\"http://sourceforge.net/projects/oslc/\">http://sourceforge.net/projects/oslc/</a><br>\n\nIf you want to contact the authors, you can do it through SourceForge by contacting usernames devil_moon,sjkaaria or villoks\n</p>\n<p>\nThis project is created for the T-76.4115 course in<br>\nSoftware Engineering and Business lab, <br>\nin Computer Science and Engineering department <br>\nof Helsinki University of Technology<br>\n</p>\n<p>\nSpecial thanks to Prof. Juha Laine, Ville Oksanen and Seppo Sahi<br>\nfor guiding us throughout the project.<br>\n</p>\n</html>\n");
        aboutScrollPane.setViewportView(aboutTextPane);

        org.jdesktop.layout.GroupLayout aboutDialogLayout = new org.jdesktop.layout.GroupLayout(aboutDialog.getContentPane());
        aboutDialog.getContentPane().setLayout(aboutDialogLayout);
        aboutDialogLayout.setHorizontalGroup(
            aboutDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(aboutDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(aboutDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(aboutScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .add(aboutLabel1)
                    .add(aboutLabel2))
                .addContainerGap())
        );
        aboutDialogLayout.setVerticalGroup(
            aboutDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(aboutDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(aboutLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(aboutLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(aboutScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                .addContainerGap())
        );
        criteriaDialog.setTitle("Choose Criteria");
        criteriaCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All" }));
        criteriaCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                criteriaComboActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout criteriaDialogLayout = new org.jdesktop.layout.GroupLayout(criteriaDialog.getContentPane());
        criteriaDialog.getContentPane().setLayout(criteriaDialogLayout);
        criteriaDialogLayout.setHorizontalGroup(
            criteriaDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(criteriaDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(criteriaCombo, 0, 148, Short.MAX_VALUE)
                .addContainerGap())
        );
        criteriaDialogLayout.setVerticalGroup(
            criteriaDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(criteriaDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(criteriaCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        helpFrame.setTitle("OSLC Help");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("OSLC v2.0");
        mainPanel.setLayout(new java.awt.BorderLayout());

        splitPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.25);
        overviewPanel.setLayout(new java.awt.GridLayout(6, 2, -100, 10));

        overviewPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Overview"));
        overviewLabel1.setText("Source Files");
        overviewPanel.add(overviewLabel1);

        srcCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        srcCountLabel.setText("<html><font color=\"gray\">0</font></html>");
        overviewPanel.add(srcCountLabel);

        overviewLabel2.setText("All Files");
        overviewPanel.add(overviewLabel2);

        allCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        allCountLabel.setText("<html><font color=\"gray\">0</font></html>");
        overviewPanel.add(allCountLabel);

        overviewLabel3.setText("Distinct Licenses");
        overviewPanel.add(overviewLabel3);

        disLicCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        disLicCountLabel.setText("<html><font color=\"gray\">0</font></html>");
        overviewPanel.add(disLicCountLabel);

        overviewLabel4.setText("Conflicts (reference)");
        overviewPanel.add(overviewLabel4);

        confRefCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        confRefCountLabel.setText("<html><font color=\"gray\">0</font></html>");
        overviewPanel.add(confRefCountLabel);

        overviewLabel5.setText("Conflicts (global)");
        overviewPanel.add(overviewLabel5);

        confGblCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        confGblCountLabel.setText("<html><font color=\"gray\">0</font></html>");
        overviewPanel.add(confGblCountLabel);

        overviewPanel.add(overviewLabel6);

        overviewHyperLink.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        overviewHyperLink.setText("Details...");
        overviewHyperLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                overviewHyperLinkMouseClicked(evt);
            }
        });

        overviewPanel.add(overviewHyperLink);

        treePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter"));
        filterCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All Files", "Conflicting Files", "Missing Licenses", "Licensed Files", "Uncertain Licenses" }));
        filterCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterComboActionPerformed(evt);
            }
        });

        referencesBox.setSelected(true);
        referencesBox.setText("Show References");
        referencesBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        referencesBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        referencesBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                referencesBoxItemStateChanged(evt);
            }
        });

        licenseTree.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                licenseTreeKeyPressed(evt);
            }
        });
        licenseTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                licenseTreeMouseClicked(evt);
            }
        });

        jScrollPane1.setViewportView(licenseTree);

        criteriaButton.setText("Choose...");
        criteriaButton.setEnabled(false);
        criteriaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                criteriaButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout treePanelLayout = new org.jdesktop.layout.GroupLayout(treePanel);
        treePanel.setLayout(treePanelLayout);
        treePanelLayout.setHorizontalGroup(
            treePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(treePanelLayout.createSequentialGroup()
                .add(referencesBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 168, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .add(org.jdesktop.layout.GroupLayout.TRAILING, treePanelLayout.createSequentialGroup()
                .add(filterCombo, 0, 92, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(criteriaButton))
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
        );
        treePanelLayout.setVerticalGroup(
            treePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, treePanelLayout.createSequentialGroup()
                .add(treePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(criteriaButton)
                    .add(filterCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(referencesBox))
        );

        org.jdesktop.layout.GroupLayout leftPanelLayout = new org.jdesktop.layout.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(treePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(overviewPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(leftPanelLayout.createSequentialGroup()
                .add(overviewPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(treePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        splitPane.setLeftComponent(leftPanel);

        tabbedPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        tabbedPane.setComponentPopupMenu(tabPopup);

        org.jdesktop.layout.GroupLayout rightPanelLayout = new org.jdesktop.layout.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 541, Short.MAX_VALUE)
        );
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, java.awt.BorderLayout.CENTER);

        toolBar.setFloatable(false);
        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/folder.png")));
        openButton.setBorderPainted(false);
        openButton.setFocusPainted(false);
        openButton.setFocusable(false);
        openButton.setOpaque(false);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        toolBar.add(openButton);

        toolBar.addSeparator();
        firstButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/resultset_first.png")));
        firstButton.setBorderPainted(false);
        firstButton.setEnabled(false);
        firstButton.setFocusable(false);
        firstButton.setOpaque(false);
        firstButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstButtonActionPerformed(evt);
            }
        });

        toolBar.add(firstButton);

        prevButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/resultset_previous.png")));
        prevButton.setBorderPainted(false);
        prevButton.setEnabled(false);
        prevButton.setFocusable(false);
        prevButton.setOpaque(false);
        prevButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prevButtonActionPerformed(evt);
            }
        });

        toolBar.add(prevButton);

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/resultset_next.png")));
        nextButton.setBorderPainted(false);
        nextButton.setEnabled(false);
        nextButton.setFocusable(false);
        nextButton.setOpaque(false);
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        toolBar.add(nextButton);

        lastButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/resultset_last.png")));
        lastButton.setBorderPainted(false);
        lastButton.setEnabled(false);
        lastButton.setFocusable(false);
        lastButton.setOpaque(false);
        lastButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastButtonActionPerformed(evt);
            }
        });

        toolBar.add(lastButton);

        mainPanel.add(toolBar, java.awt.BorderLayout.NORTH);

        statusPanel.setLayout(new java.awt.BorderLayout());

        statusPanel.add(statusBar, java.awt.BorderLayout.EAST);

        statusLabel.setText("Ready");
        statusPanel.add(statusLabel, java.awt.BorderLayout.WEST);

        mainPanel.add(statusPanel, java.awt.BorderLayout.SOUTH);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        openItem.setMnemonic('O');
        openItem.setText("Open...");
        openItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openItemActionPerformed(evt);
            }
        });

        fileMenu.add(openItem);

        closeItem.setMnemonic('C');
        closeItem.setText("Close");
        closeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeItemActionPerformed(evt);
            }
        });

        fileMenu.add(closeItem);

        closeAllItem.setMnemonic('A');
        closeAllItem.setText("Close All");
        closeAllItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllItemActionPerformed(evt);
            }
        });

        fileMenu.add(closeAllItem);

        fileMenu.add(jSeparator1);

        printItem.setMnemonic('P');
        printItem.setText("Print...");
        printItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printItemActionPerformed(evt);
            }
        });

        fileMenu.add(printItem);

        fileMenu.add(jSeparator2);

        checkoutItem.setMnemonic('V');
        checkoutItem.setText("Checkout From CVS...");
        checkoutItem.setEnabled(false);
        fileMenu.add(checkoutItem);

        fileMenu.add(jSeparator4);

        quitItem.setMnemonic('u');
        quitItem.setText("Quit");
        quitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitItemActionPerformed(evt);
            }
        });

        fileMenu.add(quitItem);

        menuBar.add(fileMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");
        overviewBox.setMnemonic('v');
        overviewBox.setSelected(true);
        overviewBox.setText("Show Overview");
        overviewBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overviewBoxActionPerformed(evt);
            }
        });

        viewMenu.add(overviewBox);

        wrapBox.setMnemonic('W');
        wrapBox.setText("Word Wrap");
        wrapBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wrapBoxActionPerformed(evt);
            }
        });

        viewMenu.add(wrapBox);

        menuBar.add(viewMenu);

        optionsMenu.setText("Options");
        optionsMenu.setEnabled(false);
        prefsItem.setText("Preferences");
        prefsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prefsItemActionPerformed(evt);
            }
        });

        optionsMenu.add(prefsItem);

        menuBar.add(optionsMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");
        helpItem.setText("Help Contents");
        helpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpItemActionPerformed(evt);
            }
        });

        helpMenu.add(helpItem);

        helpMenu.add(jSeparator5);

        aboutItem.setText("About OSLC");
        aboutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutItemActionPerformed(evt);
            }
        });

        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lastButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastButtonActionPerformed
    	FileAbstract file = null;
    	while (currentFile.hasNext()) file = currentFile.next();

    	jumptoFile(file);
    	updateToolBar();
    }//GEN-LAST:event_lastButtonActionPerformed

    private void firstButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstButtonActionPerformed
    	while (currentFile.hasPrevious()) currentFile.previous();

    	jumptoFile(currentFile.next());
    	updateToolBar();
    }//GEN-LAST:event_firstButtonActionPerformed

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
    	//System.err.println("before next: " + currentFile.previousIndex() + ":" + currentFile.nextIndex());

    	jumptoFile(currentFile.next());
    	updateToolBar();
    }//GEN-LAST:event_nextButtonActionPerformed

    private void prevButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prevButtonActionPerformed
    	//System.err.println("before prev: " + currentFile.previousIndex() + ":" + currentFile.nextIndex());

    	currentFile.previous();
    	if (currentFile.hasPrevious()) currentFile.previous();

    	jumptoFile(currentFile.next());
    	updateToolBar();
    }//GEN-LAST:event_prevButtonActionPerformed

    private void helpItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpItemActionPerformed
    	helpFrame.pack();
    	helpFrame.setLocationRelativeTo(this);
    	helpFrame.setVisible(true);
    }//GEN-LAST:event_helpItemActionPerformed

    private void criteriaComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_criteriaComboActionPerformed
        updateFiltering();
    }//GEN-LAST:event_criteriaComboActionPerformed

    private void criteriaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_criteriaButtonActionPerformed
      	criteriaDialog.pack();
        criteriaDialog.setLocationRelativeTo(this);
        criteriaDialog.setVisible(true);
    }//GEN-LAST:event_criteriaButtonActionPerformed

    private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
      	aboutDialog.pack();
        aboutDialog.setLocationRelativeTo(this);
        aboutDialog.setVisible(true);
    }//GEN-LAST:event_aboutItemActionPerformed

    private void licenseTreeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_licenseTreeKeyPressed
    	if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
    		Object node = licenseTree.getLastSelectedPathComponent();

    		if (node instanceof FileAbstract) {
    			FileAbstract file = (FileAbstract)node;

    			/* Check if tab is already open */
    			if(openTabs.containsKey(file)) switchFile(file);
    			else showFile(file);

    		} else if (node instanceof FileReference) {
    			FileReference ref = (FileReference)node;
    			
    			jumptoReference(ref);
    		}
    	}
    }//GEN-LAST:event_licenseTreeKeyPressed

    private void printItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printItemActionPerformed
    	Component tab = tabbedPane.getSelectedComponent(); 
    	if (tab instanceof LicenseTab) ((LicenseTab)tab).printLicense();
    }//GEN-LAST:event_printItemActionPerformed

    private void printTabItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printTabItemActionPerformed
    	Component tab = tabbedPane.getSelectedComponent(); 
    	if (tab instanceof LicenseTab) ((LicenseTab)tab).printLicense();
    }//GEN-LAST:event_printTabItemActionPerformed

    private void overviewHyperLinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_overviewHyperLinkMouseClicked
    	if(overviewHyperLink.isActive()) {
        	overviewDialog.pack();
            overviewDialog.setLocationRelativeTo(this);
            overviewDialog.setVisible(true);
    	}
    }//GEN-LAST:event_overviewHyperLinkMouseClicked

    private void closeTabItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeTabItemActionPerformed
    	Component tab = tabbedPane.getSelectedComponent(); 
    	if (tab instanceof LicenseTab) closeTab((LicenseTab)tab);
    }//GEN-LAST:event_closeTabItemActionPerformed

    private void closeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeItemActionPerformed
    	Component tab = tabbedPane.getSelectedComponent(); 
    	if (tab instanceof LicenseTab) closeTab((LicenseTab)tab);
    }//GEN-LAST:event_closeItemActionPerformed

    private void filterComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterComboActionPerformed
        updateCriteria();
        updateFiltering();
    }//GEN-LAST:event_filterComboActionPerformed

    private void referencesBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_referencesBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.DESELECTED) {
            licenseTree.showReferences(false);
        } else {
            licenseTree.showReferences(true);
        }
    }//GEN-LAST:event_referencesBoxItemStateChanged

    private void processDialogWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_processDialogWindowClosing
        lc.cancel();
    }//GEN-LAST:event_processDialogWindowClosing

    private void processCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processCancelActionPerformed
        lc.cancel();
    }//GEN-LAST:event_processCancelActionPerformed

    private void wrapBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wrapBoxActionPerformed
        int tabCount = tabbedPane.getTabCount();
        
        for (int i = 0; i < tabCount; i++) {
            LicenseTab tab = (LicenseTab) tabbedPane.getComponentAt(i);
            tab.setWordWrap(wrapBox.getState());
        }
    }//GEN-LAST:event_wrapBoxActionPerformed

    private void licenseTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_licenseTreeMouseClicked
    	Object selnode = licenseTree.getLastSelectedPathComponent();

        if (evt.getClickCount() == 2) {
        	if (selnode instanceof FileAbstract) { 
        		FileAbstract file = (FileAbstract)selnode;
        		showFile(file);
        	} else if (selnode instanceof FileReference) {
        		FileReference ref = (FileReference)selnode;
        		jumptoReference(ref);
        	}
    	}
    }//GEN-LAST:event_licenseTreeMouseClicked

    private void overviewBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overviewBoxActionPerformed
        overviewPanel.setVisible(overviewBox.getState());
    }//GEN-LAST:event_overviewBoxActionPerformed

    private void prefsCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prefsCancelActionPerformed
        prefsDialog.setVisible(false);
    }//GEN-LAST:event_prefsCancelActionPerformed

    private void prefsOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prefsOkActionPerformed
        prefsDialog.setVisible(false);
    }//GEN-LAST:event_prefsOkActionPerformed

    private void prefsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prefsItemActionPerformed
        prefsDialog.pack();
        prefsDialog.setLocationRelativeTo(this);
        prefsDialog.setVisible(true);
    }//GEN-LAST:event_prefsItemActionPerformed

    private void closeAllItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllItemActionPerformed
        closeAllTabs();
    }//GEN-LAST:event_closeAllItemActionPerformed

    private void quitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_quitItemActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        int retval = fileChooser.showOpenDialog(this);

        if (retval == JFileChooser.APPROVE_OPTION) {
            // System.out.println("You chose to open this file: " +
        	activePackage = fileChooser.getSelectedFile(); 
            if (activePackage.exists()) {
            	runChecker();
            } else {
            	JOptionPane.showMessageDialog(this,
            		    "File you selected does not exist",
            		    "File not found",
            		    JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void openItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openItemActionPerformed
        int retval = fileChooser.showOpenDialog(this);

        if (retval == JFileChooser.APPROVE_OPTION) {
            // System.out.println("You chose to open this file: " +
        	activePackage = fileChooser.getSelectedFile(); 
            if (activePackage.exists()) {
            	runChecker();
            } else {
            	JOptionPane.showMessageDialog(this,
            		    "File you selected does not exist",
            		    "File not found",
            		    JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_openItemActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    	Log.setVerbosity(LogEntry.ERROR);
        try {
        	UIManager.setLookAndFeel(
        			UIManager.getSystemLookAndFeelClassName()); 
            /*
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.setLookAndFeel(
                    "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); 
            */
        } catch(Exception e) {
        	ErrorManager.error("Could not set L&F", e);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LicenseMain().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog aboutDialog;
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JLabel aboutLabel1;
    private javax.swing.JLabel aboutLabel2;
    private javax.swing.JScrollPane aboutScrollPane;
    private javax.swing.JTextPane aboutTextPane;
    private javax.swing.JLabel allCountLabel;
    private javax.swing.JMenuItem checkoutItem;
    private javax.swing.JMenuItem closeAllItem;
    private javax.swing.JMenuItem closeItem;
    private javax.swing.JMenuItem closeTabItem;
    private javax.swing.JLabel confGblCountLabel;
    private javax.swing.JLabel confRefCountLabel;
    private javax.swing.JScrollPane conflictsPane;
    private javax.swing.JTable conflictsTable;
    private javax.swing.JButton criteriaButton;
    private javax.swing.JComboBox criteriaCombo;
    private javax.swing.JDialog criteriaDialog;
    private javax.swing.JLabel disLicCountLabel;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JComboBox filterCombo;
    private javax.swing.JButton firstButton;
    private javax.swing.JFrame helpFrame;
    private javax.swing.JMenuItem helpItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JButton lastButton;
    private javax.swing.JPanel leftPanel;
    private checker.gui.tree.LicenseTree licenseTree;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane matchesPane;
    private javax.swing.JTable matchesTable;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton openButton;
    private javax.swing.JMenuItem openItem;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JCheckBoxMenuItem overviewBox;
    private javax.swing.JDialog overviewDialog;
    private javax.swing.JTabbedPane overviewDlgPane;
    private javax.swing.JPanel overviewDlgPanel;
    private checker.gui.LicenseHyperLink overviewHyperLink;
    private javax.swing.JLabel overviewLabel1;
    private javax.swing.JLabel overviewLabel2;
    private javax.swing.JLabel overviewLabel3;
    private javax.swing.JLabel overviewLabel4;
    private javax.swing.JLabel overviewLabel5;
    private javax.swing.JLabel overviewLabel6;
    private javax.swing.JPanel overviewPanel;
    private javax.swing.JButton prefsCancel;
    private javax.swing.JDialog prefsDialog;
    private javax.swing.JMenuItem prefsItem;
    private javax.swing.JLabel prefsLabel;
    private javax.swing.JButton prefsOk;
    private javax.swing.JPanel prefsPanel;
    private javax.swing.JButton prevButton;
    private javax.swing.JMenuItem printItem;
    private javax.swing.JMenuItem printTabItem;
    private javax.swing.JProgressBar processBar;
    private javax.swing.JButton processCancel;
    private javax.swing.JDialog processDialog;
    private javax.swing.JLabel processLabel;
    private javax.swing.JMenuItem quitItem;
    private javax.swing.JCheckBox referencesBox;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JLabel srcCountLabel;
    private javax.swing.JProgressBar statusBar;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JPopupMenu tabPopup;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JPanel treePanel;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JCheckBoxMenuItem wrapBox;
    // End of variables declaration//GEN-END:variables
    
}