package com.skeedeye;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gui extends JFrame {

    private static final long serialVersionUID = 1064302502458098293L;
    
    private static final Logger logger = LoggerFactory.getLogger(Gui.class);
    
    private final JPanel panel;
    
    private JTextField criterium;
    
    private final JFileChooser fileChooser = new JFileChooser();
    
    public final static Map<String, Metadata> CACHE = new ConcurrentHashMap<String, Metadata>();
    
    static {
        logger.debug("Create metadata cache");
        new SeDeSerializer().deserialize(CACHE);
    }
    
    public Gui() {
        super();
        
        setSize(800, 200);
        
        panel = createPanel();
        add(panel, BorderLayout.CENTER);
        createElements(new GridBagConstraints());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                logger.debug("Shutdown metadata cache");
                new SeDeSerializer().serialize(CACHE);
            }
        }));
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createPanel() {
        JPanel result = new JPanel();
        result.setLayout(new GridBagLayout());
        return result;
    }

    private final static String SEARCH = "Search...";
    
    private void createElements(GridBagConstraints c) {
        c.ipadx = 10;
        c.ipady = 10;
        c.insets = new Insets(10, 10, 10, 10);
        
        addLabel("Criteria:", c);
        addCriterium(c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        addLabel("Select Output Folder:", c);
        addOutputFolder(c);
        addSearchButton(c);
    }
    
    private JButton search;
    
    public void setEnabledSearchButton(Boolean enable) {
        this.search.setEnabled(enable);
    }

    private final static String GRAB = "Grab!";
    private final static String STOP_GRABBING = "Stop Grabbing";
    
    private Grabber grabber;
    
    private void addSearchButton(GridBagConstraints c) {
        final JButton srch = new JButton(GRAB);
        srch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (srch.getText().equals(GRAB)) {
                    srch.setEnabled(false);
                    Gui.this.grabber = new Grabber(Gui.this, criterium.getText(), output.getText());
                    new Thread(Gui.this.grabber).start();
                    srch.setEnabled(true);
                    srch.setText(STOP_GRABBING);
                } else if (srch.getText().equals(STOP_GRABBING)) {
                    Gui.this.grabber.cancel();
                }
            }
        });
        srch.setEnabled(false);
        this.search = srch;
        c.weightx = 1.0;
        make(c, srch);
        panel.add(srch);
    }

    private JButton output;
    
    private final File DEFAULT_FILE = new File("C:/temp");
    
    private void addOutputFolder(GridBagConstraints c) {
        final JButton outputFolder = new JButton(DEFAULT_FILE.exists() ? DEFAULT_FILE.getAbsolutePath() : "Press to Select");
        outputFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setSelectedFile(new File(outputFolder.getText()));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showSaveDialog(Gui.this) == JFileChooser.APPROVE_OPTION &&
                        fileChooser.getSelectedFile().isDirectory()) {
                    outputFolder.setText(fileChooser.getSelectedFile().getAbsolutePath().replace('\\', '/'));
                    Gui.this.search.setEnabled(true);
                }
            }
        });
        output = outputFolder;
        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        make(c, outputFolder);
        panel.add(outputFolder);
    }

    private void addCriterium(GridBagConstraints c) {
        final JTextField crt = new JTextField(SEARCH);
        crt.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (crt.getText().trim().equalsIgnoreCase(SEARCH))
                    crt.setText("");
            }
            public void focusLost(FocusEvent e) {
                Gui.this.search.setEnabled(crt.getText().trim().length() > 0 && 
                        !crt.getText().trim().equalsIgnoreCase(Gui.SEARCH) &&
                        new File(Gui.this.output.getText()).exists());
            }
        });
        this.criterium = crt;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        make(c, crt);
        panel.add(crt);
    }

    private void addLabel(String text, GridBagConstraints c) {
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        final JLabel label = new JLabel(text, SwingConstants.RIGHT);
        make(c, label);
        panel.add(label);
    }

    private void make(GridBagConstraints c, final Component component) {
        ((GridBagLayout)panel.getLayout()).setConstraints(component, c);
    }

    public static void main(String[] args) {
        new Gui();
    }

    public void canceled() {
        this.search.setText(GRAB);
    }
    
}
