package com.github.mozvip.romm.ui;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.SyncArchivesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class RommJFrame extends JFrame {

    public static final Logger log = LoggerFactory.getLogger(RommJFrame.class);

    private final Romm romm;
    private final ArchiveFileRepository archiveFileRepository;
    private final RommArchiveRepository rommArchiveRepository;
    private final OutputFolderRepository outputFolderRepository;
    private final SyncArchivesService syncArchivesService;

    private JPanel contentPane;
    private JTextField textDatFolder;
    private JTextField textInputFolder;
    private JButton buttonBrowseDatFolder;
    private JButton buttonBrowseInputFolder;
    private JTextField textOutputFolder;
    private JButton buttonBrowseOutput;
    private JTextField textUnknownFilesFolder;
    private JButton buttonBrowseUnknownFiles;
    private JTree treeArchives;
    private JTable tableArchives;
    private JComboBox comboBoxFilter;
    private JTextField textFieldFilter;
    private JTable tableFiles;
    private JButton buttonSyncCache;

    public RommJFrame(Romm romm, ArchiveFileRepository archiveFileRepository, RommArchiveRepository rommArchiveRepository, OutputFolderRepository outputFolderRepository, SyncArchivesService syncArchivesService) {
        this.romm = romm;
        this.archiveFileRepository = archiveFileRepository;
        this.rommArchiveRepository = rommArchiveRepository;
        this.outputFolderRepository = outputFolderRepository;
        this.syncArchivesService = syncArchivesService;

        setTitle(String.format("Romm %s", Romm.ROMM_VERSION));
        setContentPane(contentPane);
        setResizable(true);
        getRootPane().setDefaultButton(buttonSyncCache);

        textDatFolder.setText(romm.getDatFolderArg().toAbsolutePath().toString());
        buttonBrowseDatFolder.addActionListener(e -> {
            selectFolder(textDatFolder);
        });

        textInputFolder.setText(romm.getInputFolderArg().toAbsolutePath().toString());
        buttonBrowseInputFolder.addActionListener(e -> {
            selectFolder(textInputFolder);
        });

        textOutputFolder.setText(romm.getOutputFolderArg().toAbsolutePath().toString());
        buttonBrowseOutput.addActionListener(e -> {
            selectFolder(textOutputFolder);
        });

        textUnknownFilesFolder.setText(romm.getUnknownFolderArg().toAbsolutePath().toString());
        buttonBrowseUnknownFiles.addActionListener(e -> {
            selectFolder(textUnknownFilesFolder);
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        refreshData();

        tableArchives.setAutoCreateRowSorter(true);
        tableArchives.setRowSelectionAllowed(true);

        ListSelectionModel selectionModel = tableArchives.getSelectionModel();
        selectionModel.addListSelectionListener(this::selectArchive);

        treeArchives.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
            final List<RommArchive> rommArchives = rommArchiveRepository.findByDatPath((String)lastPathComponent.getUserObject());

            tableArchives.setModel(new TableModel() {

                final String[] columns = new String[] {"Archive Path", "Status"};

                @Override
                public int getRowCount() {
                    return rommArchives.size();
                }

                @Override
                public int getColumnCount() {
                    return columns.length;
                }

                @Override
                public String getColumnName(int columnIndex) {
                    return columns[columnIndex];
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? String.class : ArchiveStatus.class;
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    final RommArchive rommArchive = rommArchives.get(rowIndex);
                    if (columnIndex == 0) {
                        String archivePath = rommArchive.getArchivePath();
                        archivePath = archivePath.substring(archivePath.lastIndexOf(File.separator) + 1);
                        return archivePath;
                    } else {
                        return rommArchive.getStatus();
                    }
                }

                @Override
                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

                }

                @Override
                public void addTableModelListener(TableModelListener l) {

                }

                @Override
                public void removeTableModelListener(TableModelListener l) {

                }
            });

            tableArchives.setRowSorter(new TableRowSorter<>(tableArchives.getModel()));
            updateRowFilter();

            tableArchives.getColumnModel().getColumn(0).setPreferredWidth(200);
            tableArchives.getColumnModel().getColumn(1).setPreferredWidth(20);

            tableArchives.getColumnModel().getColumn(1).setCellRenderer(new ArchiveStatusRenderer());
        });
        comboBoxFilter.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            updateRowFilter();
        });

        textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRowFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRowFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRowFilter();
            }
        });
        buttonSyncCache.addActionListener(e -> syncArchivesService.fixCacheForArchiveNames(null));
    }

    private void selectArchive(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        final ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
        final TableModel model = tableArchives.getModel();
        System.out.println(model.getValueAt(listSelectionModel.getMinSelectionIndex(), 0));
    }

    public void updateRowFilter() {
        final TableRowSorter<? extends TableModel> rowSorter = (TableRowSorter<? extends TableModel>) tableArchives.getRowSorter();
        List<RowFilter<TableModel, Object>> filters = new ArrayList<>();
        switch ((String)comboBoxFilter.getSelectedItem()) {
            case "All":
                break;
            case "Missing Only":
                filters.add(RowFilter.regexFilter("MISSING", 1));
                break;
            case "Partial Only":
                filters.add(RowFilter.regexFilter("PARTIAL", 1));
                break;
            case "Complete Only":
                filters.add(RowFilter.regexFilter("COMPLETE", 1));
                break;
        }
        if (StringUtils.hasText(textFieldFilter.getText())) {
            filters.add(RowFilter.regexFilter(textFieldFilter.getText(), 0));
        }
        if (filters.size() > 0) {
            rowSorter.setRowFilter(filters.size() > 1 ? RowFilter.andFilter(filters) : filters.get(0));
        } else {
            rowSorter.setRowFilter(null);
        }
    }

    public void refreshData() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.setUserObject("");
        Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();
        TreeModel model = new DefaultTreeModel(root);

        final Set<String> datPaths = rommArchiveRepository.findDistinctDatPath();
        for (String datPath : datPaths) {
            if (StringUtils.hasText(datPath)) {
                final String[] strings = datPath.split("[/\\\\]+");
                DefaultMutableTreeNode parentNode = root;
                String currentIndentifier = "";
                for (int i=0; i<strings.length; i++) {
                    if (currentIndentifier.length() > 0) {
                        currentIndentifier += File.separator;
                    }
                    currentIndentifier += strings[i];
                    DefaultMutableTreeNode finalParentNode = parentNode;
                    DefaultMutableTreeNode currentNode = nodes.computeIfAbsent(currentIndentifier, s -> {
                        final DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                        node.setUserObject(s);
                        finalParentNode.add(node);
                        return node;
                    });
                    parentNode = currentNode;
                }
            }
        }
        treeArchives.setModel(model);
    }

    public void saveSettings() throws IOException {

        final Path homeFolder = Paths.get(System.getProperty("user.home"));
        final Path configFolder = homeFolder.resolve(".romm");
        Files.createDirectories(configFolder);
        final Path configFile = configFolder.resolve("config.json");

    }

    private void selectFolder(JTextComponent target) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

}
