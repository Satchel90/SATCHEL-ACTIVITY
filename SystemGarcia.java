import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.ArrayList;

class Garcia {
    String category;
    String id;
    String title;
    String author;
    boolean borrowed;

    public Garcia(String category, String title, String author, String id, boolean borrowed) {
        this.category = category;
        this.title = title;
        this.author = author;
        this.id = id;
        this.borrowed = borrowed;
    }
}

public class SystemGarcia extends JFrame {
    private static final long serialVersionUID = 1L;

    private final ArrayList<Garcia> books = new ArrayList<>();

    private JTextField txtId;
    private JTextField txtTitle;
    private JTextField txtAuthor;
    private JTextField txtCategory;

    private JLabel lblStatus;
    private JLabel lblCount;

    private JTable table;
    private DefaultTableModel tableModel;
    private int displayedBookCount;
    private Timer countAnimation;

    private static final String FILE_NAME = "nowBookFile.txt";
    private boolean dataFileValid = true;
    private String loadError;

    private final Color BG_LIGHT = new Color(248, 250, 252);
    private final Color PANEL_LIGHT = new Color(255, 255, 255);
    private final Color TEXT_DARK = new Color(15, 23, 42);
    private final Color FIELD_BG = new Color(248, 250, 252);
    private final Color BUTTON_BG = new Color(124, 58, 237);
    private final Color BUTTON_HOVER = new Color(91, 33, 182);
    private final Color TABLE_GRID = new Color(203, 213, 225);
    private final Color BORDER_SOFT = new Color(191, 219, 254);
    private final Color ACCENT_BLUE = new Color(124, 58, 237);
    private final Color ACCENT_LIGHT = new Color(167, 139, 250);
    private final Color SUCCESS_BG = new Color(220, 252, 231);
    private final Color SUCCESS_TEXT = new Color(22, 101, 52);
    private final Color GOLD = new Color(251, 191, 36);
    private final Color NIGHT = new Color(11, 16, 32);
    private final Color MIDNIGHT = new Color(30, 41, 59);

    private static class GradientPanel extends JPanel {
        private final Color startColor;
        private final Color endColor;
        private float animationPhase;
        private final Timer animationTimer;

        GradientPanel(Color startColor, Color endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            setOpaque(false);
            animationTimer = new Timer(40, e -> {
                animationPhase += 0.012f;
                if (animationPhase >= 1f) {
                    animationPhase -= 1f;
                }
                repaint();
            });
        }

        @Override
        public void addNotify() {
            super.addNotify();
            animationTimer.start();
        }

        @Override
        public void removeNotify() {
            animationTimer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int width = getWidth();
            int height = getHeight();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float shift = (float) Math.sin(animationPhase * Math.PI * 2) * 0.18f;
            Color movingColor = blendColors(startColor, endColor, 0.5f + shift);
            GradientPaint gradient = new GradientPaint(0, 0, startColor, width, height, movingColor);
            g2.setPaint(gradient);
            g2.fillRect(0, 0, width, height);

            g2.setColor(new Color(255, 255, 255, 22));
            int drift = (int) (animationPhase * 42);
            for (int i = 0; i < 7; i++) {
                int x = width - 210 + ((i * 53 + drift) % 230);
                int y = 12 + (i * 29) % Math.max(1, height - 24);
                int size = i % 3 == 0 ? 4 : 2;
                g2.fillOval(x, y, size, size);
                if (i < 6) {
                    int nextX = width - 210 + (((i + 1) * 53 + drift) % 230);
                    int nextY = 12 + ((i + 1) * 29) % Math.max(1, height - 24);
                    g2.drawLine(x + size / 2, y + size / 2, nextX + size / 2, nextY + size / 2);
                }
            }

            int glowX = (int) (width * (0.72 + 0.12 * Math.sin(animationPhase * Math.PI * 2)));
            g2.setColor(new Color(251, 191, 36, 34));
            g2.fillOval(glowX, height / 2 - 3, 6, 6);
            g2.dispose();
        }

        private Color blendColors(Color first, Color second, float amount) {
            float clampedAmount = Math.max(0f, Math.min(1f, amount));
            int red = Math.round(first.getRed() + (second.getRed() - first.getRed()) * clampedAmount);
            int green = Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * clampedAmount);
            int blue = Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * clampedAmount);
            return new Color(red, green, blue);
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 8, 8, 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = 8;
            insets.right = 8;
            insets.top = 8;
            insets.bottom = 8;
            return insets;
        }
    }

    public SystemGarcia() {
        setTitle("Garcia Vault Library");
        setSize(980, 620);
        setMinimumSize(new Dimension(980, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(14, 14));
        getContentPane().setBackground(BG_LIGHT);
        setResizable(false);

        loadBooksFromFile();

        JPanel headerPanel = new GradientPanel(new Color(11, 16, 32), new Color(124, 58, 237));
        headerPanel.setLayout(new BorderLayout(12, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel brandBadge = new JLabel("GV");
        brandBadge.setHorizontalAlignment(SwingConstants.CENTER);
        brandBadge.setFont(new Font("Arial", Font.BOLD, 24));
        brandBadge.setForeground(new Color(11, 16, 32));
        brandBadge.setOpaque(true);
        brandBadge.setBackground(new Color(251, 191, 36));
        brandBadge.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 3));
        titlePanel.setOpaque(false);
        JLabel lblHeader = new JLabel("GARCIA VAULT LIBRARY", JLabel.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 22));
        lblHeader.setForeground(Color.WHITE);
        JLabel lblSubtitle = new JLabel("Curated stories. Trusted loans. Timeless access.", JLabel.CENTER);
        lblSubtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(232, 234, 255));
        titlePanel.add(lblHeader);
        titlePanel.add(lblSubtitle);

        headerPanel.add(brandBadge, BorderLayout.WEST);
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(PANEL_LIGHT);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_SOFT, 20),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        leftPanel.setPreferredSize(new Dimension(330, 0));

        txtId = createStyledTextField();
        txtTitle = createStyledTextField();
        txtAuthor = createStyledTextField();
        txtCategory = createStyledTextField();

        leftPanel.add(createFieldLabel("Category:"));
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(txtCategory);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createFieldLabel("Book ID:"));
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(txtId);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createFieldLabel("Book Title:"));
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(txtTitle);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createFieldLabel("Author:"));
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(txtAuthor);
        leftPanel.add(Box.createVerticalStrut(16));

        JButton btnAdd = createStyledButton("Add Book");
        JButton btnUpdate = createStyledButton("Update Book");
        JButton btnDisplay = createStyledButton("Refresh");
        JButton btnBorrow = createStyledButton("Borrow");
        JButton btnReturn = createStyledButton("Return");
        JButton btnSearch = createStyledButton("Search");
        JButton btnClear = createStyledButton("Clear");

        JPanel buttonGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        buttonGrid.setBackground(PANEL_LIGHT);
        buttonGrid.add(btnAdd);
        buttonGrid.add(btnUpdate);
        buttonGrid.add(btnDisplay);
        buttonGrid.add(btnBorrow);
        buttonGrid.add(btnReturn);
        buttonGrid.add(btnSearch);
        buttonGrid.add(btnClear);

        add(leftPanel, BorderLayout.WEST);

        String[] columnNames = { "Category", "Title", "Author", "Book ID", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setBackground(FIELD_BG);
        table.setForeground(TEXT_DARK);
        table.setGridColor(TABLE_GRID);
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(32);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(30, 64, 175));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(241, 245, 249));
                    cell.setForeground(TEXT_DARK);
                }
                setHorizontalAlignment(column == 4 ? JLabel.CENTER : JLabel.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (column == 4 && value instanceof String) {
                    String status = ((String) value).toLowerCase();
                    if (status.contains("borrowed")) {
                        setForeground(new Color(153, 27, 27));
                        setBackground(new Color(254, 226, 226));
                    } else if (status.contains("available")) {
                        setForeground(new Color(21, 128, 61));
                        setBackground(new Color(220, 252, 231));
                    }
                }
                return cell;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(BG_LIGHT);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_SOFT, 18),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        buttonPanel.setBackground(BG_LIGHT);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDisplay);
        buttonPanel.add(btnBorrow);
        buttonPanel.add(btnReturn);
        buttonPanel.add(btnSearch);
        buttonPanel.add(btnClear);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(BG_LIGHT);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(PANEL_LIGHT);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_SOFT),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        lblStatus = new JLabel("Status: System Ready");
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(239, 246, 255));
        lblStatus.setForeground(new Color(30, 64, 175));
        lblStatus.setFont(new Font("Arial", Font.BOLD, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblCount = new JLabel("Total Books: 0");
        lblCount.setForeground(ACCENT_BLUE);
        lblCount.setFont(new Font("Arial", Font.BOLD, 12));
        lblCount.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        bottomPanel.add(lblStatus, BorderLayout.WEST);
        bottomPanel.add(lblCount, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshTable();
        if (loadError != null) {
            SwingUtilities.invokeLater(() -> showError("Data File Error", loadError));
        }

        btnAdd.addActionListener(e -> addBook());
        btnUpdate.addActionListener(e -> updateBook());
        btnDisplay.addActionListener(e -> {
            refreshTable();
            lblStatus.setText("Status: Displaying all books.");
        });
        btnBorrow.addActionListener(e -> borrowBook());
        btnReturn.addActionListener(e -> returnBook());
        btnSearch.addActionListener(e -> searchBook());
        btnClear.addActionListener(e -> clearFields());

        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                txtCategory.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtTitle.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtAuthor.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtId.setText(tableModel.getValueAt(selectedRow, 3).toString());
            }
        });

        setVisible(true);
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setBackground(FIELD_BG);
        tf.setForeground(TEXT_DARK);
        tf.setCaretColor(TEXT_DARK);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(TABLE_GRID, 12),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
        return tf;
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        return lbl;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BUTTON_HOVER, 12),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color[] targetColor = { BUTTON_BG };
        Timer animation = new Timer(15, null);
        animation.addActionListener(e -> {
            Color currentColor = btn.getBackground();
            Color nextColor = blendColors(currentColor, targetColor[0], 0.25f);
            btn.setBackground(nextColor);

            if (Math.abs(nextColor.getRed() - targetColor[0].getRed()) < 3
                    && Math.abs(nextColor.getGreen() - targetColor[0].getGreen()) < 3
                    && Math.abs(nextColor.getBlue() - targetColor[0].getBlue()) < 3) {
                btn.setBackground(targetColor[0]);
                animation.stop();
            }
        });

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                targetColor[0] = BUTTON_HOVER;
                animation.restart();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                targetColor[0] = BUTTON_BG;
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(BUTTON_HOVER, 12),
                        BorderFactory.createEmptyBorder(8, 6, 8, 6)));
                animation.restart();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                targetColor[0] = new Color(23, 65, 180);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(BUTTON_HOVER, 12),
                        BorderFactory.createEmptyBorder(10, 6, 6, 6)));
                animation.restart();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                targetColor[0] = btn.contains(evt.getPoint()) ? BUTTON_HOVER : BUTTON_BG;
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(BUTTON_HOVER, 12),
                        BorderFactory.createEmptyBorder(8, 6, 8, 6)));
                animation.restart();
            }
        });

        return btn;
    }

    private Color blendColors(Color current, Color target, float amount) {
        int red = Math.round(current.getRed() + (target.getRed() - current.getRed()) * amount);
        int green = Math.round(current.getGreen() + (target.getGreen() - current.getGreen()) * amount);
        int blue = Math.round(current.getBlue() + (target.getBlue() - current.getBlue()) * amount);
        return new Color(red, green, blue);
    }

    private void loadBooksFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split("/", -1);
                if (parts.length != 5
                        || parts[0].trim().isEmpty()
                        || parts[1].trim().isEmpty()
                        || parts[2].trim().isEmpty()
                        || parts[3].trim().isEmpty()
                        || !(parts[4].equalsIgnoreCase("true") || parts[4].equalsIgnoreCase("false"))) {
                    throw new IOException("Invalid book data on line " + lineNumber
                            + ". Expected Category/Title/Author/ID/true-or-false.");
                }

                books.add(new Garcia(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        Boolean.parseBoolean(parts[4].trim())));
            }
        } catch (IOException e) {
            dataFileValid = false;
            loadError = "Could not read " + FILE_NAME + ":\n" + e.getMessage()
                    + "\nThe file was not changed.";
        }
    }

    private boolean saveBooksToFile() {
        if (!dataFileValid) {
            showError("Save Error", "Fix the invalid data in " + FILE_NAME
                    + " before changing books. The file was not overwritten.");
            return false;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Garcia book : books) {
                writer.println(book.category + "/" + book.title + "/" + book.author
                        + "/" + book.id + "/" + book.borrowed);
            }
            if (writer.checkError()) {
                showError("Save Error", "Could not save the book data to " + FILE_NAME + ".");
                return false;
            }
            return true;
        } catch (IOException e) {
            showError("Save Error", "Could not save the book data:\n" + e.getMessage());
            return false;
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Garcia book : books) {
            tableModel.addRow(new Object[] {
                    book.category,
                    book.title,
                    book.author,
                    book.id,
                    book.borrowed ? "Borrowed" : "Available"
            });
        }
        updateCount();
    }

    private void addBook() {
        String category = txtCategory.getText().trim();
        String id = txtId.getText().trim();
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();

        if (category.isEmpty() || id.isEmpty() || title.isEmpty() || author.isEmpty()) {
            showError("Missing Information", "Enter a category, book ID, title, and author.");
            return;
        }
        if (containsFileDelimiter(category, title, author, id)) {
            showError("Invalid Character", "Book details cannot contain a slash (/).");
            return;
        }

        if (findBook(id) != null) {
            showError("Duplicate Book ID", "That book ID is already in use.");
            return;
        }

        Garcia newBook = new Garcia(category, title, author, id, false);
        books.add(newBook);
        if (!saveBooksToFile()) {
            books.remove(newBook);
            return;
        }
        refreshTable();
        clearFields();
        lblStatus.setText("Status: Book added successfully!");
    }

    private void updateBook() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showError("No Book Selected", "Select a book from the table before updating.");
            return;
        }

        String originalId = tableModel.getValueAt(selectedRow, 3).toString();
        Garcia book = findBook(originalId);
        if (book == null) {
            showError("Book Not Found", "The selected book could not be found.");
            return;
        }

        String category = txtCategory.getText().trim();
        String id = txtId.getText().trim();
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();

        if (category.isEmpty() || id.isEmpty() || title.isEmpty() || author.isEmpty()) {
            showError("Missing Information", "Enter a category, book ID, title, and author.");
            return;
        }
        if (containsFileDelimiter(category, title, author, id)) {
            showError("Invalid Character", "Book details cannot contain a slash (/).");
            return;
        }

        Garcia duplicate = findBook(id);
        if (duplicate != null && duplicate != book) {
            showError("Duplicate Book ID", "That book ID is already in use.");
            return;
        }

        String oldId = book.id;
        String oldCategory = book.category;
        String oldTitle = book.title;
        String oldAuthor = book.author;

        book.category = category;
        book.id = id;
        book.title = title;
        book.author = author;

        if (!saveBooksToFile()) {
            book.category = oldCategory;
            book.id = oldId;
            book.title = oldTitle;
            book.author = oldAuthor;
            return;
        }

        refreshTable();
        clearFields();
        lblStatus.setText("Status: Book updated successfully!");
    }

    private void borrowBook() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            showError("Missing Book ID", "Enter a book ID or select a book from the table.");
            return;
        }

        Garcia book = findBook(id);
        if (book == null) {
            showError("Book Not Found", "No book was found with ID \"" + id + "\".");
        } else if (book.borrowed) {
            showError("Book Unavailable", "This book is already borrowed.");
        } else {
            book.borrowed = true;
            if (!saveBooksToFile()) {
                book.borrowed = false;
                return;
            }
            refreshTable();
            lblStatus.setText("Status: Book borrowed successfully!");
        }
    }

    private void returnBook() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            showError("Missing Book ID", "Enter a book ID or select a book from the table.");
            return;
        }

        Garcia book = findBook(id);
        if (book == null) {
            showError("Book Not Found", "No book was found with ID \"" + id + "\".");
        } else if (!book.borrowed) {
            showError("Book Already Available", "This book has not been borrowed.");
        } else {
            book.borrowed = false;
            if (!saveBooksToFile()) {
                book.borrowed = true;
                return;
            }
            refreshTable();
            lblStatus.setText("Status: Book returned successfully!");
        }
    }

    private void searchBook() {
        String categorySearch = txtCategory.getText().trim();
        String idSearch = txtId.getText() == null ? "" : txtId.getText().trim();
        String titleSearch = txtTitle.getText() == null ? "" : txtTitle.getText().trim();
        String authorSearch = txtAuthor.getText() == null ? "" : txtAuthor.getText().trim();

        if (categorySearch.isEmpty() && idSearch.isEmpty()
                && titleSearch.isEmpty() && authorSearch.isEmpty()) {
            showError("Missing Search Text", "Enter a category, book ID, title, or author to search.");
            refreshTable();
            return;
        }

        tableModel.setRowCount(0);
        boolean found = false;

        for (Garcia book : books) {
            if (containsIgnoreCase(book.category, categorySearch)
                    || containsIgnoreCase(book.id, idSearch)
                    || containsIgnoreCase(book.title, titleSearch)
                    || containsIgnoreCase(book.author, authorSearch)) {
                tableModel.addRow(new Object[] {
                        book.category,
                        book.title,
                        book.author,
                        book.id,
                        book.borrowed ? "Borrowed" : "Available"
                });
                found = true;
            }
        }

        if (found) {
            lblStatus.setText("Status: Search results displayed.");
        } else {
            showError("No Results", "No books matched your search.");
        }

        updateCount();
    }

    private void showError(String title, String message) {
        lblStatus.setText("Status: " + message.replace('\n', ' '));
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return !search.isEmpty() && value != null
                && value.toLowerCase().contains(search.toLowerCase());
    }

    private boolean containsFileDelimiter(String... values) {
        for (String value : values) {
            if (value.contains("/") || value.contains("\n") || value.contains("\r")) {
                return true;
            }
        }
        return false;
    }

    private Garcia findBook(String id) {
        for (Garcia book : books) {
            if (book.id != null && book.id.equalsIgnoreCase(id)) {
                return book;
            }
        }
        return null;
    }

    private void clearFields() {
        txtCategory.setText("");
        txtId.setText("");
        txtTitle.setText("");
        txtAuthor.setText("");
        table.clearSelection();
    }

    private void updateCount() {
        int targetCount = books.size();
        if (countAnimation != null) {
            countAnimation.stop();
        }

        if (displayedBookCount == targetCount) {
            lblCount.setText("Total Books: " + targetCount);
            return;
        }

        countAnimation = new Timer(25, e -> {
            int difference = targetCount - displayedBookCount;
            int step = Math.max(1, Math.abs(difference) / 8);
            displayedBookCount += difference > 0 ? step : -step;

            if ((difference > 0 && displayedBookCount >= targetCount)
                    || (difference < 0 && displayedBookCount <= targetCount)) {
                displayedBookCount = targetCount;
                countAnimation.stop();
            }

            lblCount.setText("Total Books: " + displayedBookCount);
        });
        countAnimation.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JWindow splash = new JWindow();
            JPanel panel = new GradientPanel(new Color(11, 16, 32), new Color(124, 58, 237));
            panel.setLayout(new BorderLayout(0, 14));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            JLabel badge = new JLabel("GV", SwingConstants.CENTER);
            badge.setFont(new Font("Arial", Font.BOLD, 34));
            badge.setForeground(new Color(11, 16, 32));
            badge.setOpaque(true);
            badge.setBackground(new Color(251, 191, 36));
            badge.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

            JLabel title = new JLabel("GARCIA VAULT LIBRARY", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setForeground(Color.WHITE);
            JLabel loading = new JLabel("Opening the vault...", SwingConstants.CENTER);
            loading.setFont(new Font("Arial", Font.PLAIN, 13));
            loading.setForeground(new Color(220, 230, 255));
            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setForeground(new Color(251, 191, 36));
            progress.setBackground(new Color(255, 255, 255));

            JPanel messagePanel = new JPanel(new GridLayout(2, 1, 0, 6));
            messagePanel.setOpaque(false);
            messagePanel.add(title);
            messagePanel.add(loading);

            panel.add(badge, BorderLayout.NORTH);
            panel.add(messagePanel, BorderLayout.CENTER);
            panel.add(progress, BorderLayout.SOUTH);
            splash.setContentPane(panel);
            splash.setSize(420, 190);
            splash.setLocationRelativeTo(null);
            splash.setVisible(true);

            Timer timer = new Timer(900, e -> {
                splash.dispose();
                new SystemGarcia();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}