import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application window. Handles only UI concerns —
 * all file/media logic is delegated to MediaOrganizer and FileUtils.
 */
public class OrganizerGUI extends JFrame implements ActionListener {

    private final MediaOrganizer organizer = new MediaOrganizer();

    private JButton allToOneFolderButton;
    private JButton organizeButton;
    private JLabel pathLabel;
    private JFileChooser chooser;

    public OrganizerGUI() {
        super("Photo Cleaner");
        setSize(350, 250);
        setLayout(new FlowLayout());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        addGuiComponents();
    }

    // -------------------------------------------------------------------------
    // UI setup
    // -------------------------------------------------------------------------

    private void addGuiComponents() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Složka");
        JMenuItem openItem = new JMenuItem("Otevřít");
        openItem.setActionCommand(Action.FOLDER.name());
        openItem.addActionListener(this);
        fileMenu.add(openItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        pathLabel = new JLabel("Žádná složka nevybrána");
        add(pathLabel);

        allToOneFolderButton = new JButton("Přemístit do jedné složky");
        allToOneFolderButton.setActionCommand(Action.ONEFOLDER.name());
        allToOneFolderButton.addActionListener(this);
        add(allToOneFolderButton);

        organizeButton = new JButton("Organizace médií");
        organizeButton.setActionCommand(Action.ORGANIZE.name());
        organizeButton.addActionListener(this);
        add(organizeButton);
    }

    // -------------------------------------------------------------------------
    // Action dispatch
    // -------------------------------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals(Action.FOLDER.name())) {
            chooseFolder();

        } else if (command.equals(Action.ONEFOLDER.name())) {
            if (!validateFolderSelected()) return;
            try {
                FileUtils.flattenDirectory(getSelectedPath());
                JOptionPane.showMessageDialog(this, "Přesun dokončen.", "Hotovo", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                showError("Chyba při přesunu souborů: " + ex.getMessage());
            }

        } else if (command.equals(Action.ORGANIZE.name())) {
            if (!validateFolderSelected()) return;
            runOrganize();
        }
    }

    // -------------------------------------------------------------------------
    // Organize flow
    // -------------------------------------------------------------------------

    private void runOrganize() {
        Path root = getSelectedPath();

        // 1. Organize images with dates
        List<File> undated = organizer.organizeImages(root);

        // 2. Organize videos
        organizer.organizeVideos(root);

        // 3. Report result
        String message = "Organizace dokončena.";
        if (!undated.isEmpty()) {
            message += "\n\nSoubory bez datumu (ponechány v původní složce): " + undated.size();
        }
        JOptionPane.showMessageDialog(this, message, "Hotovo", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    private void chooseFolder() {
        chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Vyber složku s fotkami a videi");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathLabel.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private boolean validateFolderSelected() {
        if (pathLabel.getText().equals("Žádná složka nevybrána")) {
            showError("Musí se vybrat cesta ke složce.");
            return false;
        }
        return true;
    }

    private Path getSelectedPath() {
        return Path.of(pathLabel.getText());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Chyba", JOptionPane.ERROR_MESSAGE);
    }


}