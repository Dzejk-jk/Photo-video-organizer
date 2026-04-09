import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Main application window.
 * All file/media logic is delegated to MediaOrganizer and FileUtils.
 * File operations run on a background SwingWorker thread to keep the UI responsive.
 */
public class OrganizerGUI extends JFrame {

    // ── Color palette (Tailwind-inspired) ──────────────────────────────────
    private static final Color C_PRIMARY       = new Color(37,  99, 235);
    private static final Color C_PRIMARY_HOVER = new Color(29,  78, 216);
    private static final Color C_BG            = new Color(248, 250, 252);
    private static final Color C_CARD          = Color.WHITE;
    private static final Color C_BORDER        = new Color(226, 232, 240);
    private static final Color C_TEXT          = new Color(15,  23,  42);
    private static final Color C_MUTED         = new Color(100, 116, 139);
    private static final Color C_HEADER_SUB    = new Color(147, 197, 253);

    private final MediaOrganizer organizer = new MediaOrganizer();

    private JLabel       pathLabel;
    private JButton      selectButton, flattenButton, organizeButton;
    private JProgressBar progressBar;
    private JLabel       statusLabel;
    private Path         selectedPath;

    public OrganizerGUI() {
        super("Media Organizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        applyLookAndFeel();
        setContentPane(buildRoot());
        pack();
        setLocationRelativeTo(null);
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) { /* fallback to default */ }
    }

    // ── Root layout ─────────────────────────────────────────────────────────

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.setPreferredSize(new Dimension(520, 420));
        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new GridBagLayout());
        hdr.setBackground(C_PRIMARY);
        hdr.setPreferredSize(new Dimension(0, 84));
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Media Organizer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Organizuje fotky a videa podle data pořízení");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(C_HEADER_SUB);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        hdr.add(text, g);
        return hdr;
    }

    // ── Center ──────────────────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx   = 0;

        g.gridy  = 0; g.insets = new Insets(0,  0, 0, 0);
        p.add(buildFolderCard(), g);

        g.gridy  = 1; g.insets = new Insets(16, 0, 0, 0);
        p.add(buildActionsCard(), g);

        g.gridy  = 2; g.insets = new Insets(12, 0, 0, 0);
        p.add(buildProgressPanel(), g);

        // push everything to the top
        g.gridy  = 3; g.weighty = 1.0; g.fill = GridBagConstraints.BOTH;
        p.add(Box.createGlue(), g);

        return p;
    }

    private JPanel buildFolderCard() {
        JPanel card = card(new BorderLayout(16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel("VYBRANÁ SLOŽKA");
        caption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        caption.setForeground(C_MUTED);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        pathLabel = new JLabel("Žádná složka nevybrána");
        pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathLabel.setForeground(C_MUTED);
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(caption);
        left.add(Box.createVerticalStrut(4));
        left.add(pathLabel);

        selectButton = btn("Vybrat", C_PRIMARY, C_PRIMARY_HOVER, Color.WHITE, false);
        selectButton.addActionListener(e -> chooseFolder());

        card.add(left,         BorderLayout.CENTER);
        card.add(selectButton, BorderLayout.EAST);
        return card;
    }

    private JPanel buildActionsCard() {
        JPanel card = card(new GridLayout(1, 2, 12, 0));

        flattenButton  = btn("Přemístit do jedné složky", C_CARD, C_BG, C_TEXT, true);
        organizeButton = btn("Organizovat média",         C_PRIMARY, C_PRIMARY_HOVER, Color.WHITE, false);

        flattenButton.addActionListener(e  -> runFlatten());
        organizeButton.addActionListener(e -> runOrganize());

        card.add(flattenButton);
        card.add(organizeButton);
        return card;
    }

    private JPanel buildProgressPanel() {
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(progressBar);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(241, 245, 249));
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        statusLabel = new JLabel("Připraven");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(C_MUTED);
        bar.add(statusLabel);
        return bar;
    }

    // ── Component factories ──────────────────────────────────────────────────

    private static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(C_CARD);
        p.setBorder(new CompoundBorder(
            new LineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        return p;
    }

    /**
     * Returns a custom-painted button with rounded corners and hover effect.
     *
     * @param bordered draw a subtle border around the button (secondary style)
     */
    private static JButton btn(String text, Color bg, Color bgHover, Color fg, boolean bordered) {
        JButton b = new JButton(text) {
            boolean over;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { over = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { over = false; repaint(); }
                });
            }
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                if (!enabled) over = false;
                repaint();
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background
                Color fill = isEnabled() ? (over ? bgHover : bg) : new Color(220, 220, 225);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Optional border for secondary style
                if (bordered) {
                    g2.setColor(isEnabled() ? C_BORDER : new Color(200, 200, 205));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }

                // Label
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? fg : C_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", bordered ? Font.PLAIN : Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return b;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Vyber složku s fotkami a videi");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedPath = chooser.getSelectedFile().toPath();
            pathLabel.setText(selectedPath.toString());
            pathLabel.setForeground(C_TEXT);
            pathLabel.setToolTipText(selectedPath.toString());
            status("Složka: " + selectedPath.getFileName());
        }
    }

    private boolean requireFolder() {
        if (selectedPath == null) {
            err("Nejprve vyberte složku.");
            return false;
        }
        return true;
    }

    /** Runs FileUtils.flattenDirectory on a background thread. */
    private void runFlatten() {
        if (!requireFolder()) return;
        busy(true, "Přesouvám soubory do kořenové složky…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                FileUtils.flattenDirectory(selectedPath);
                return null;
            }
            @Override
            protected void done() {
                busy(false, null);
                try {
                    get();
                    status("Přesun dokončen.");
                    info("Přesun dokončen.", "Hotovo");
                } catch (ExecutionException ex) {
                    status("Chyba: " + ex.getCause().getMessage());
                    err("Chyba při přesunu: " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Runs organizeVideos + organizeImages on a background thread. */
    private void runOrganize() {
        if (!requireFolder()) return;
        busy(true, "Organizuji média podle data…");

        new SwingWorker<List<File>, Void>() {
            @Override
            protected List<File> doInBackground() {
                organizer.organizeVideos(selectedPath);
                return organizer.organizeImages(selectedPath);
            }
            @Override
            protected void done() {
                busy(false, null);
                try {
                    List<File> undated = get();
                    String msg = "Organizace dokončena.";
                    if (!undated.isEmpty())
                        msg += "\n\nSoubory bez datumu (ponechány ve složce): " + undated.size();
                    status("Hotovo. Bez datumu: " + undated.size() + " souborů.");
                    info(msg, "Hotovo");
                } catch (ExecutionException ex) {
                    status("Chyba při organizaci.");
                    err("Chyba: " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ── UI state helpers ─────────────────────────────────────────────────────

    private void busy(boolean on, String msg) {
        selectButton.setEnabled(!on);
        flattenButton.setEnabled(!on);
        organizeButton.setEnabled(!on);
        progressBar.setVisible(on);
        if (msg != null) status(msg);
        revalidate();
        repaint();
    }

    private void status(String text)              { statusLabel.setText(text); }
    private void info(String msg, String title)   { JOptionPane.showMessageDialog(this, msg, title,  JOptionPane.INFORMATION_MESSAGE); }
    private void err(String msg)                  { JOptionPane.showMessageDialog(this, msg, "Chyba", JOptionPane.ERROR_MESSAGE); }
}
