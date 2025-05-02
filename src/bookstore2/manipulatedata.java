/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bookstore2;

/**
 *
 * @author Delluser
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.table.DefaultTableModel;


public class manipulatedata extends JFrame {
    static final String URL = "jdbc:mysql://localhost/bookstore";
    static final String USER = "root";
    static final String PASSWORD = "";

    private JTable BookTable;
    private DefaultTableModel tableModel;

    public manipulatedata() {
        initComponents();
        setVisible(true);
        loadData(); // Load data when form is initialized
        
    }

    private void initComponents() {
        setTitle("Manipulate Data");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create table to display products
        tableModel = new DefaultTableModel(new String[]{"BookID", "BookName", "Author", "Price", "Added Date", "Updated Date"}, 0);
        BookTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(BookTable);
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton loadButton = new JButton("Load Data");
        loadButton.addActionListener(e -> loadData());

        JButton addButton = new JButton("Add Product");
        addButton.addActionListener(e -> addProduct());

        JButton updateButton = new JButton("Update Product");
        updateButton.addActionListener(e -> updateProduct());

        JButton deleteButton = new JButton("Delete Product");
        deleteButton.addActionListener(e -> deleteProduct());

        JButton viewImageButton = new JButton("View Image");
        viewImageButton.addActionListener(e -> viewImageById());

        buttonPanel.add(loadButton);
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewImageButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadData() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books")) {

            tableModel.setRowCount(0); // Clear existing data

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("BookID"));
                row.add(rs.getString("BookName"));
                row.add(rs.getString("Author"));
                row.add(rs.getDouble("Price"));
                row.add(rs.getTimestamp("AddedDate"));
                row.add(rs.getTimestamp("UpdatedDate"));
                tableModel.addRow(row);
            }

            System.out.println("Data loaded successfully. Rows count: " + tableModel.getRowCount());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "SQL Exception: " + e.getMessage());
        }
    }

    private void addProduct() {
        String BookIDStr = JOptionPane.showInputDialog(this, "Enter Book ID:");
        if (BookIDStr == null) return; // Cancelled

        String BookName = JOptionPane.showInputDialog(this, "Enter Book Name:");
        if (BookName == null) return; // Cancelled

        String Author = JOptionPane.showInputDialog(this, "Enter Author:");
        if (Author == null) return; // Cancelled

        String priceStr = JOptionPane.showInputDialog(this, "Enter Price:");
        if (priceStr == null) return; // Cancelled

        // Validate inputs
        try {
            double price = Double.parseDouble(priceStr);

            // Select image
            byte[] imageBytes = selectImage();
            if (imageBytes == null) {
                JOptionPane.showMessageDialog(this, "No image selected. Book will be added without an image.");
            }

            // Prepare SQL statement
            String sql = "INSERT INTO books (BookID, BookName, Author, Price, BookImage) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, BookIDStr);
                pstmt.setString(2, BookName);
                pstmt.setString(3, Author);
                pstmt.setDouble(4, price);
                pstmt.setBytes(5, imageBytes); // Set the image bytes (can be null)

                // Execute the update
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Book added successfully.");
                loadData(); // Refresh the table data after adding
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input for Book ID or Price.");
        }
    }

    private byte[] selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an Image");
        int userSelection = fileChooser.showOpenDialog(this);

        // Check if the user selected a file
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToUpload = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(fileToUpload);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                return bos.toByteArray(); // Return the image as a byte array
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading image: " + e.getMessage());
            }
        }
        return null; // Return null if no image is selected or an error occurs
    }

    private boolean productExists(String productId) {
        String sql = "SELECT COUNT(*) FROM books WHERE BookID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if product exists
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error checking Book existence: " + e.getMessage());
        }
        return false; // Return false if an error occurs or if product does not exist
    }

    private void updateProduct() {
        String BookIDStr = JOptionPane.showInputDialog(this, "Enter Book ID to Update:");
        if (BookIDStr == null) return; // Cancelled

        // Check if product exists
        if (!productExists(BookIDStr)) {
            JOptionPane.showMessageDialog(this, "No Book found with the given ID.");
            return;
        }

        String BookName = JOptionPane.showInputDialog(this, "Enter New Book Name:");
        if (BookName == null) return; // Cancelled

        String Author = JOptionPane.showInputDialog(this, "Enter New Author:");
        if (Author == null) return; // Cancelled

        String priceStr = JOptionPane.showInputDialog(this, "Enter New Price:");
        if (priceStr == null) return; // Cancelled

        // Validate price input
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input for Price.");
            return;
        }

        String sql = "UPDATE books SET BookName = ?, Author = ?, Price = ? WHERE BookID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BookName);
            pstmt.setString(2, Author);
            pstmt.setDouble(3, price);
            pstmt.setString(4, BookIDStr);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Book updated successfully.");
                loadData(); // Refresh the table data after updating
            } else {
                JOptionPane.showMessageDialog(this, "No Book found with the given ID.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating Book: " + e.getMessage());
        }
    }

    private void deleteProduct() {
        String BookIDStr = JOptionPane.showInputDialog(this, "Enter Book ID to Delete:");
        if (BookIDStr == null) return; // Cancelled

        // Check if product exists
        if (!productExists(BookIDStr)) {
            JOptionPane.showMessageDialog(this, "No Book found with the given ID.");
            return;
        }

        String sql = "DELETE FROM books WHERE BookID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BookIDStr);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Book deleted successfully.");
                loadData(); // Refresh the table data after deletion
            } else {
                JOptionPane.showMessageDialog(this, "No Book found with the given ID.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting Book: " + e.getMessage());
        }
    }

    private void viewImageById() {
        String BookID = JOptionPane.showInputDialog(this, "Enter Book ID to View Image:");
        if (BookID == null) return; // Cancelled

        // Check if product exists
        if (!productExists(BookID)) {
            JOptionPane.showMessageDialog(this, "No product found with the given ID.");
            return;
        }

        String sql = "SELECT BookImage FROM books WHERE BookID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BookID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                byte[] imageBytes = rs.getBytes("BookImage");
                if (imageBytes != null && imageBytes.length > 0) {
                    displayImage(imageBytes);
                } else {
                    JOptionPane.showMessageDialog(this, "No image available for this Book.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "No Book found with the given ID.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving image: " + e.getMessage());
        }
    }

    private void displayImage(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img != null) {
                ImageIcon icon = new ImageIcon(img);
                JLabel imageLabel = new JLabel(icon);
                JOptionPane.showMessageDialog(this, imageLabel, "Book Image", JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error reading image data.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error displaying image: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            manipulatedata form = new manipulatedata();
            form.setVisible(true);
  });
}
}
