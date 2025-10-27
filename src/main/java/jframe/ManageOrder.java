/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package jframe;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.*;
import javax.swing.table.TableModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author brizu
 */
public class ManageOrder extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManageOrder.class.getName());

    private int customerPk = 0;
    private int productPk = 0;
    private int finalTotalPrice = 0;
    private String orderId = "";

    /**
     * Creates new form ManageOrder
     */
    public ManageOrder() {
        initComponents();
        setLocationRelativeTo(null);
        setUserDetailsToTable();
        setBookDetailsToTable();
    }

    private void clearProductFields() {
        productPk = 0;
        txt_bookName.setText("");
        txt_price.setText("");
        txt_orderQuantity.setText("");
    }

    private void clearCustomerFields() {
        customerPk = 0;
        txt_userName.setText("");
        txt_membershipType.setText("");
    }

    // to set customer details to table from database
    private void setUserDetailsToTable() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT user_id, name, membership_type FROM user_details")) {

            DefaultTableModel model = (DefaultTableModel) tableUser.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                String userId = rs.getString("user_id");
                String userName = rs.getString("name");
                String membershipType = rs.getString("membership_type");
                Object[] obj = {userId, userName, membershipType};
                model.addRow(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // to set product details to table from database
    private void setBookDetailsToTable() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT book_id, book_name, author, quantity, book_price FROM book_details")) {

            DefaultTableModel model = (DefaultTableModel) tableBook.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                String bookId = rs.getString("book_id");
                String bookName = rs.getString("book_name");
                String author = rs.getString("author");
                int quantity = rs.getInt("quantity");
                int price = rs.getInt("book_price");
                Object[] obj = {bookId, bookName, author, quantity, price};
                model.addRow(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchUser() {
        String name = txt_userName.getText();
        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement("SELECT user_id, name, membership_type FROM user_details WHERE name LIKE ?")) {

            pst.setString(1, "%" + name + "%");
            try (ResultSet rs = pst.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) tableUser.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    String userName = rs.getString("name");
                    String membershipType = rs.getString("membership_type");
                    Object[] row = {userId, userName, membershipType};
                    model.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchBook() {
        String name = txt_bookName.getText();
        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement("SELECT book_id, book_name, author, quantity, book_price FROM book_details WHERE book_name LIKE ?")) {

            pst.setString(1, "%" + name + "%");
            try (ResultSet rs = pst.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) tableBook.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    String bookId = rs.getString("book_id");
                    String bookName = rs.getString("book_name");
                    String author = rs.getString("author");
                    int quantity = rs.getInt("quantity");
                    int price = rs.getInt("book_price");
                    Object[] row = {bookId, bookName, author, quantity, price};
                    model.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addProductToCart() {
        if (productPk == 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first.");
            return;
        }
        if (txt_orderQuantity.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter order quantity.");
            return;
        }

        int quantityRequested;
        int price;
        try {
            quantityRequested = Integer.parseInt(txt_orderQuantity.getText().trim());
            price = Integer.parseInt(txt_price.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format for quantity or price.");
            return;
        }

        // check stock using book_details.quantity
        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement("SELECT quantity FROM book_details WHERE book_id = ?")) {

            pst.setInt(1, productPk);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int available = rs.getInt("quantity");
                    if (available < quantityRequested) {
                        JOptionPane.showMessageDialog(this, "Product is out of stock. Only " + available + " remaining.");
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Selected book not found in database.");
                    return;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e);
            return;
        }

        //applying 20% discount if the user is a Scribe
        String membershipType = txt_membershipType.getText().trim();
        int total;

        if ("Scribe".equalsIgnoreCase(membershipType)) {
            double discountedPrice = price * 0.8; // 20% off
            total = (int) Math.round(quantityRequested * discountedPrice);
        } else {
            total = quantityRequested * price;
        }
        DefaultTableModel model = (DefaultTableModel) tableCart.getModel();

        // check product already in cart
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Integer.parseInt(model.getValueAt(i, 0).toString()) == productPk) {
                JOptionPane.showMessageDialog(this, "Product already exists in cart.");
                clearProductFields();
                return;
            }
        }

        if ("Scribe".equalsIgnoreCase(membershipType)) {
            lbl_discount.setText("20% discounted");
        } else {
            lbl_discount.setText("No discount");
        }

        model.addRow(new Object[]{productPk, txt_bookName.getText(), quantityRequested, price, total});
        finalTotalPrice += total;
        lbl_finalPrice.setText(String.valueOf(finalTotalPrice));
        clearProductFields();
    }

    public void print() {
        try {
            String orderId = getUniqueId("ORD-"); // or use your existing orderId variable
            String userId = String.valueOf(customerPk); // assuming customerPk is set when user is selected
            String membership = txt_membershipType.getText().trim();

            b.setText("                                   BiblioFile \n");
            b.setText(b.getText() + "                                   123/ Cubao, \n");
            b.setText(b.getText() + "                                   Metro Manila, Philippines, \n");
            b.setText(b.getText() + "                                  +63 123456789 \n");
            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");
            b.setText(b.getText() + "Order ID     : " + orderId + "\n");
            b.setText(b.getText() + "User ID      : " + userId + "\n");
            b.setText(b.getText() + "Membership   : " + membership + "\n");
            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");
            b.setText(b.getText() + "Item\t\tQty\tUnit Price\tTotal\n");
            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");

            DefaultTableModel df = (DefaultTableModel) tableCart.getModel();
            int originalTotal = 0;

            for (int i = 0; i < df.getRowCount(); i++) {
                String name = df.getValueAt(i, 1).toString();
                int qty = Integer.parseInt(df.getValueAt(i, 2).toString());
                int unitPrice = Integer.parseInt(df.getValueAt(i, 3).toString());
                int total = qty * unitPrice;
                originalTotal += total;

                b.setText(b.getText() + name + "\t\t" + qty + "\t" + unitPrice + "\t" + total + "\n");
            }

            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");
            b.setText(b.getText() + "Original Total : " + originalTotal + "\n");

            int discountedTotal = finalTotalPrice;
            if ("Scribe".equalsIgnoreCase(membership)) {
                b.setText(b.getText() + "Discount (20%) : -" + (originalTotal - discountedTotal) + "\n");
            } else {
                b.setText(b.getText() + "Discount       : 0\n");
            }

            b.setText(b.getText() + "Final Total    : " + discountedTotal + "\n");
            b.setText(b.getText() + "Cash           : " + lbl_cash.getText() + "\n");
            b.setText(b.getText() + "Balance        : " + lbl_balance.getText() + "\n");
            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");
            b.setText(b.getText() + "                              Thanks For Your Business...!\n");
            b.setText(b.getText() + "--------------------------------------------------------------------------------------------------\n");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearCart() {
        DefaultTableModel model = (DefaultTableModel) tableCart.getModel();
        model.setRowCount(0);
        finalTotalPrice = 0;
        lbl_finalPrice.setText("0");
        lbl_discount.setText("");
    }

    private String getUniqueId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        sdadadad = new javax.swing.JPanel();
        txt_userName = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableCart = new rojeru_san.complementos.RSTableMetro();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableUser = new rojeru_san.complementos.RSTableMetro();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableBook = new rojeru_san.complementos.RSTableMetro();
        jLabel16 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        txt_orderQuantity = new app.bolivia.swing.JCTextField();
        rSMaterialButtonCircle4 = new rojerusan.RSMaterialButtonCircle();
        lblPrintReciept = new rojerusan.RSMaterialButtonCircle();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        rSMaterialButtonCircle7 = new rojerusan.RSMaterialButtonCircle();
        btnPay = new rojerusan.RSMaterialButtonCircle();
        jLabel29 = new javax.swing.JLabel();
        lbl_cash = new app.bolivia.swing.JCTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        b = new javax.swing.JTextArea();
        jLabel17 = new javax.swing.JLabel();
        lbl_discount = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        lbl_balance = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        lbl_finalPrice = new javax.swing.JLabel();
        sdada = new javax.swing.JPanel();
        txt_membershipType = new javax.swing.JLabel();
        adada = new javax.swing.JPanel();
        txt_bookName = new javax.swing.JLabel();
        lsls = new javax.swing.JPanel();
        txt_price = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(120, 27, 27));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/purchase-order-xxl.png"))); // NOI18N
        jLabel2.setText("    Manage order");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 0, 340, 130));

        jLabel72.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel72.setForeground(new java.awt.Color(255, 255, 255));
        jLabel72.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Rewind_48px.png"))); // NOI18N
        jLabel72.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel72MouseClicked(evt);
            }
        });
        jPanel1.add(jLabel72, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 50, 70));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1770, 130));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        sdadadad.setBackground(new java.awt.Color(120, 27, 27));
        sdadadad.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        sdadadad.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txt_userName.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        txt_userName.setForeground(new java.awt.Color(255, 255, 255));
        txt_userName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        sdadadad.add(txt_userName, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 360, 30));

        jPanel2.add(sdadadad, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 400, 360, 30));

        tableCart.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Book Id", "Name", "Qty", "Orig. Price", "Disc. Price"
            }
        ));
        tableCart.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tableCart.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tableCart.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tableCart.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tableCart.setFont(new java.awt.Font("Serif", 0, 10)); // NOI18N
        tableCart.setFuenteFilas(new java.awt.Font("Serif", 1, 10)); // NOI18N
        tableCart.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 12)); // NOI18N
        tableCart.setFuenteHead(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tableCart.setRowHeight(40);
        tableCart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableCartMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tableCart);

        jPanel2.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 70, 400, 260));

        tableUser.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "User Id", "Name", "Membership Type"
            }
        ));
        tableUser.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tableUser.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tableUser.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tableUser.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tableUser.setFont(new java.awt.Font("Serif", 0, 10)); // NOI18N
        tableUser.setFuenteFilas(new java.awt.Font("Serif", 1, 10)); // NOI18N
        tableUser.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 12)); // NOI18N
        tableUser.setFuenteHead(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tableUser.setRowHeight(40);
        tableUser.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableUserMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tableUserMouseEntered(evt);
            }
        });
        jScrollPane2.setViewportView(tableUser);

        jPanel2.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 70, 410, 260));

        tableBook.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Book Id", "Name", "Author", "Quantity", "Price"
            }
        ));
        tableBook.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tableBook.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tableBook.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tableBook.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tableBook.setFont(new java.awt.Font("Serif", 0, 10)); // NOI18N
        tableBook.setFuenteFilas(new java.awt.Font("Serif", 1, 10)); // NOI18N
        tableBook.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 12)); // NOI18N
        tableBook.setFuenteHead(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tableBook.setRowHeight(40);
        tableBook.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableBookMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(tableBook);

        jPanel2.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 70, 410, 260));

        jLabel16.setFont(new java.awt.Font("Serif", 1, 30)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(120, 27, 27));
        jLabel16.setText("Cart");
        jPanel2.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(1080, 10, 70, 60));

        jLabel18.setFont(new java.awt.Font("Serif", 1, 30)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(120, 27, 27));
        jLabel18.setText("Book List");
        jPanel2.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 10, 130, 60));

        jLabel19.setFont(new java.awt.Font("Serif", 1, 30)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(120, 27, 27));
        jLabel19.setText("User List");
        jPanel2.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 10, 120, 60));

        jLabel20.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(120, 27, 27));
        jLabel20.setText("Selected User");
        jPanel2.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 330, 120, 40));

        jLabel15.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(120, 27, 27));
        jLabel15.setText("User Name");
        jPanel2.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 363, 170, 30));

        jLabel22.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(120, 27, 27));
        jLabel22.setText("Book Name");
        jPanel2.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 363, 170, 30));

        jLabel23.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(120, 27, 27));
        jLabel23.setText("Price");
        jPanel2.add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 430, 170, 30));

        jLabel24.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(120, 27, 27));
        jLabel24.setText("Order Quantity");
        jPanel2.add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 500, 170, 30));

        txt_orderQuantity.setBackground(new java.awt.Color(120, 27, 27));
        txt_orderQuantity.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        txt_orderQuantity.setForeground(new java.awt.Color(255, 255, 255));
        txt_orderQuantity.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_orderQuantity.setPhColor(new java.awt.Color(255, 255, 255));
        txt_orderQuantity.setPlaceholder("Enter Order Quantity....");
        txt_orderQuantity.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_orderQuantityFocusLost(evt);
            }
        });
        txt_orderQuantity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_orderQuantityActionPerformed(evt);
            }
        });
        jPanel2.add(txt_orderQuantity, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 540, 360, 30));

        rSMaterialButtonCircle4.setBackground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle4.setBorder(new javax.swing.border.MatteBorder(null));
        rSMaterialButtonCircle4.setText("ADD TO CART");
        rSMaterialButtonCircle4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle4ActionPerformed(evt);
            }
        });
        jPanel2.add(rSMaterialButtonCircle4, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 590, 160, 50));

        lblPrintReciept.setBackground(new java.awt.Color(120, 27, 27));
        lblPrintReciept.setBorder(new javax.swing.border.MatteBorder(null));
        lblPrintReciept.setText("PRINT");
        lblPrintReciept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblPrintRecieptActionPerformed(evt);
            }
        });
        jPanel2.add(lblPrintReciept, new org.netbeans.lib.awtextra.AbsoluteConstraints(1050, 590, 110, 50));

        jLabel25.setFont(new java.awt.Font("Serif", 1, 17)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(120, 27, 27));
        jLabel25.setText("Balance :");
        jPanel2.add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 510, 120, 60));

        jLabel26.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(120, 27, 27));
        jLabel26.setText("Selected Book");
        jPanel2.add(jLabel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 330, 120, 40));

        jLabel28.setFont(new java.awt.Font("Serif", 1, 17)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(120, 27, 27));
        jLabel28.setText("Cash :");
        jPanel2.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 450, 120, 60));

        rSMaterialButtonCircle7.setBackground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle7.setBorder(new javax.swing.border.MatteBorder(null));
        rSMaterialButtonCircle7.setText("RESET");
        rSMaterialButtonCircle7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle7ActionPerformed(evt);
            }
        });
        jPanel2.add(rSMaterialButtonCircle7, new org.netbeans.lib.awtextra.AbsoluteConstraints(1220, 330, 100, 50));

        btnPay.setBackground(new java.awt.Color(120, 27, 27));
        btnPay.setBorder(new javax.swing.border.MatteBorder(null));
        btnPay.setText("PAY");
        btnPay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPayActionPerformed(evt);
            }
        });
        jPanel2.add(btnPay, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 590, 110, 50));

        jLabel29.setFont(new java.awt.Font("Serif", 1, 17)); // NOI18N
        jLabel29.setForeground(new java.awt.Color(120, 27, 27));
        jLabel29.setText("Total Amount :");
        jPanel2.add(jLabel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 390, 120, 60));

        lbl_cash.setBackground(new java.awt.Color(120, 27, 27));
        lbl_cash.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lbl_cash.setForeground(new java.awt.Color(255, 255, 255));
        lbl_cash.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        lbl_cash.setPhColor(new java.awt.Color(255, 255, 255));
        lbl_cash.setPlaceholder("Enter Cash....");
        lbl_cash.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                lbl_cashFocusLost(evt);
            }
        });
        lbl_cash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lbl_cashActionPerformed(evt);
            }
        });
        jPanel2.add(lbl_cash, new org.netbeans.lib.awtextra.AbsoluteConstraints(1090, 470, 150, 30));

        b.setColumns(20);
        b.setRows(5);
        jScrollPane4.setViewportView(b);

        jPanel2.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1370, 70, 370, 540));

        jLabel17.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(120, 27, 27));
        jLabel17.setText("Membership Type");
        jPanel2.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 430, 170, 30));

        lbl_discount.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        lbl_discount.setForeground(new java.awt.Color(120, 27, 27));
        jPanel2.add(lbl_discount, new org.netbeans.lib.awtextra.AbsoluteConstraints(1250, 390, 110, 60));

        jPanel3.setBackground(new java.awt.Color(120, 27, 27));
        jPanel3.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbl_balance.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        lbl_balance.setForeground(new java.awt.Color(255, 255, 255));
        lbl_balance.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jPanel3.add(lbl_balance, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 30));

        jPanel2.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(1090, 530, 150, 30));

        jPanel4.setBackground(new java.awt.Color(120, 27, 27));
        jPanel4.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbl_finalPrice.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        lbl_finalPrice.setForeground(new java.awt.Color(255, 255, 255));
        lbl_finalPrice.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lbl_finalPrice.setToolTipText("");
        jPanel4.add(lbl_finalPrice, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 30));

        jPanel2.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1090, 410, 150, 30));

        sdada.setBackground(new java.awt.Color(120, 27, 27));
        sdada.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        sdada.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txt_membershipType.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        txt_membershipType.setForeground(new java.awt.Color(255, 255, 255));
        txt_membershipType.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        sdada.add(txt_membershipType, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 360, 30));

        jPanel2.add(sdada, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 470, 360, 30));

        adada.setBackground(new java.awt.Color(120, 27, 27));
        adada.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        adada.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txt_bookName.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        txt_bookName.setForeground(new java.awt.Color(255, 255, 255));
        txt_bookName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        adada.add(txt_bookName, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 360, 30));

        jPanel2.add(adada, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 400, 360, -1));

        lsls.setBackground(new java.awt.Color(120, 27, 27));
        lsls.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        lsls.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txt_price.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        txt_price.setForeground(new java.awt.Color(255, 255, 255));
        txt_price.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lsls.add(txt_price, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 360, 30));

        jPanel2.add(lsls, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 470, 360, -1));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 130, 1770, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tableCartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableCartMouseClicked
        // TODO add your handling code here:
        int index = tableCart.getSelectedRow();
        if (index >= 0) {
            int a = JOptionPane.showConfirmDialog(null, "Do you want to remove this product?", "Select", JOptionPane.YES_NO_OPTION);
            if (a == 0) {
                TableModel model = tableCart.getModel();
                // subtotal is column index 4
                String subTotalStr = model.getValueAt(index, 4).toString();
                int subTotal = 0;
                try {
                    subTotal = Integer.parseInt(subTotalStr);
                } catch (NumberFormatException ex) {
                    subTotal = 0;
                }
                finalTotalPrice = finalTotalPrice - subTotal;
                if (finalTotalPrice < 0) {
                    finalTotalPrice = 0;
                }
                lbl_finalPrice.setText(String.valueOf(finalTotalPrice));
                ((DefaultTableModel) tableCart.getModel()).removeRow(index);
            }
        }
    }//GEN-LAST:event_tableCartMouseClicked

    private void tableUserMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableUserMouseClicked
        // TODO add your handling code here:
        int rowNo = tableUser.getSelectedRow();
        TableModel model = tableUser.getModel();
        try {
            customerPk = Integer.parseInt(model.getValueAt(rowNo, 0).toString());
        } catch (NumberFormatException ex) {
            customerPk = 0;
        }
        txt_userName.setText(model.getValueAt(rowNo, 1).toString());
        txt_membershipType.setText(model.getValueAt(rowNo, 2).toString());
    }//GEN-LAST:event_tableUserMouseClicked

    private void tableUserMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableUserMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tableUserMouseEntered

    private void tableBookMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableBookMouseClicked
        // TODO add your handling code here:
        int rowNo = tableBook.getSelectedRow();
        TableModel model = tableBook.getModel();
        try {
            productPk = Integer.parseInt(model.getValueAt(rowNo, 0).toString());
        } catch (NumberFormatException ex) {
            productPk = 0;
        }
        txt_bookName.setText(model.getValueAt(rowNo, 1).toString());
        // price is now column index 4 (0-based)
        txt_price.setText(model.getValueAt(rowNo, 4).toString());
    }//GEN-LAST:event_tableBookMouseClicked

    private void txt_orderQuantityFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_orderQuantityFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_orderQuantityFocusLost

    private void txt_orderQuantityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_orderQuantityActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_orderQuantityActionPerformed

    private void rSMaterialButtonCircle4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle4ActionPerformed
        // TODO add your handling code here:
        addProductToCart();
    }//GEN-LAST:event_rSMaterialButtonCircle4ActionPerformed

    private void lblPrintRecieptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblPrintRecieptActionPerformed
        orderId = getUniqueId("ORD-"); // generate new ID
        com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
        String filePath = InventoryUtils.billPath + orderId + ".pdf";

        try {
            PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            doc.open();

            // Header
            Paragraph shop = new Paragraph("BiblioFile");
            shop.setAlignment(Paragraph.ALIGN_CENTER);
            doc.add(shop);
            doc.add(new Paragraph("123 Cubao, Metro Manila, Philippines"));
            doc.add(new Paragraph("+63 123456789"));
            doc.add(new Paragraph("------------------------------------------------------------"));

            // Order Info
            String userId = String.valueOf(customerPk);
            String membership = txt_membershipType.getText().trim();

            doc.add(new Paragraph("Order ID      : " + orderId));
            doc.add(new Paragraph("User ID       : " + userId));
            doc.add(new Paragraph("Membership    : " + membership));
            doc.add(new Paragraph("------------------------------------------------------------"));

            // Table
            PdfPTable tbl = new PdfPTable(5);
            tbl.addCell("Book Id");
            tbl.addCell("Book Name");
            tbl.addCell("Quantity");
            tbl.addCell("Unit Price");
            tbl.addCell("Total");

            DefaultTableModel model = (DefaultTableModel) tableCart.getModel();
            int originalTotal = 0;

            for (int i = 0; i < model.getRowCount(); i++) {
                int qty = Integer.parseInt(model.getValueAt(i, 2).toString());
                int unitPrice = Integer.parseInt(model.getValueAt(i, 3).toString());
                int total = qty * unitPrice;
                originalTotal += total;

                tbl.addCell(String.valueOf(model.getValueAt(i, 0))); // Book Id
                tbl.addCell(String.valueOf(model.getValueAt(i, 1))); // Book Name
                tbl.addCell(String.valueOf(qty));
                tbl.addCell(String.valueOf(unitPrice));
                tbl.addCell(String.valueOf(total));
            }

            doc.add(tbl);
            doc.add(new Paragraph(" "));

            // Pricing Summary
            int discountAmount = originalTotal - finalTotalPrice;

            doc.add(new Paragraph("Original Total : " + originalTotal));
            doc.add(new Paragraph("Discount       : " + discountAmount));
            doc.add(new Paragraph("Final Total    : " + finalTotalPrice));
            doc.add(new Paragraph("Cash Paid      : " + lbl_cash.getText()));
            doc.add(new Paragraph("Balance        : " + lbl_balance.getText()));
            doc.add(new Paragraph("------------------------------------------------------------"));
            doc.add(new Paragraph("Thank you for your business!"));

            doc.close();

            try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO order_details (order_id, user_id, user_name, membership_type, order_date, original_price, discounted_price, cash_paid, balance, status) "
                    + "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?)")) {

                pst.setString(1, orderId);
                pst.setInt(2, customerPk);
                pst.setString(3, txt_userName.getText().trim());
                pst.setString(4, membership);
                pst.setInt(5, originalTotal);
                pst.setInt(6, finalTotalPrice);
                pst.setInt(7, Integer.parseInt(lbl_cash.getText().trim()));
                pst.setInt(8, Integer.parseInt(lbl_balance.getText().trim()));
                pst.setString(9, "completed");

                pst.executeUpdate();

            } catch (Exception dbEx) {
                JOptionPane.showMessageDialog(null, "Error saving order to database: " + dbEx.getMessage());
            }

            // Try opening the PDF
            try {
                Thread.sleep(300);
                Openpdf.OpenById(orderId);
            } catch (Exception openEx) {
                JOptionPane.showMessageDialog(null,
                        "Receipt saved at: " + filePath + "\nCould not open automatically.\n" + openEx.getMessage(),
                        "PDF Saved", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error while generating PDF: " + e.getMessage());
        }


    }//GEN-LAST:event_lblPrintRecieptActionPerformed

    private void rSMaterialButtonCircle7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle7ActionPerformed
        // TODO add your handling code here:
        clearCart();
        clearCustomerFields();
        clearProductFields();
        setUserDetailsToTable();
        setBookDetailsToTable();
        b.setText("");
        lbl_cash.setText("");
        lbl_balance.setText("");
    }//GEN-LAST:event_rSMaterialButtonCircle7ActionPerformed

    private void btnPayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPayActionPerformed

        String totalStr = lbl_finalPrice.getText().trim();
        String cashStr = lbl_cash.getText().trim();

        // basic checks
        if (totalStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Total amount is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cashStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter cash amount.", "Input required", JOptionPane.WARNING_MESSAGE);
            lbl_cash.requestFocus();
            return;
        }

        // allow only digits (integers). If you want to allow negative or decimals, change the regex.
        if (!cashStr.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Invalid cash amount. Please enter digits only (integers).", "Invalid input", JOptionPane.ERROR_MESSAGE);
            lbl_cash.setText("");
            lbl_cash.requestFocus();
            return;
        }

        try {
            int finalPrice = Integer.parseInt(totalStr);
            int cash = Integer.parseInt(cashStr);

            if (cash < finalPrice) {
                JOptionPane.showMessageDialog(this, "Insufficient cash. Please enter an amount equal to or greater than the total.", "Insufficient cash", JOptionPane.WARNING_MESSAGE);
                lbl_cash.requestFocus();
                return;
            }

            int balance = cash - finalPrice;
            lbl_balance.setText(String.valueOf(balance));
        } catch (NumberFormatException ex) {
            // this covers the extremely large integer case
            JOptionPane.showMessageDialog(this, "Number too large or otherwise invalid. Please enter a valid integer.", "Invalid number", JOptionPane.ERROR_MESSAGE);
            lbl_cash.setText("");
            lbl_cash.requestFocus();
        }

        print();

    }//GEN-LAST:event_btnPayActionPerformed

    private void lbl_cashFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lbl_cashFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_lbl_cashFocusLost

    private void lbl_cashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lbl_cashActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lbl_cashActionPerformed

    private void jLabel72MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel72MouseClicked
        // TODO add your handling code here:
        HomePage home = new HomePage();
        home.setVisible(true);
        dispose();
    }//GEN-LAST:event_jLabel72MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ManageOrder().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel adada;
    private javax.swing.JTextArea b;
    private rojerusan.RSMaterialButtonCircle btnPay;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private rojerusan.RSMaterialButtonCircle lblPrintReciept;
    private javax.swing.JLabel lbl_balance;
    private app.bolivia.swing.JCTextField lbl_cash;
    private javax.swing.JLabel lbl_discount;
    private javax.swing.JLabel lbl_finalPrice;
    private javax.swing.JPanel lsls;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle4;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle7;
    private javax.swing.JPanel sdada;
    private javax.swing.JPanel sdadadad;
    private rojeru_san.complementos.RSTableMetro tableBook;
    private rojeru_san.complementos.RSTableMetro tableCart;
    private rojeru_san.complementos.RSTableMetro tableUser;
    private javax.swing.JLabel txt_bookName;
    private javax.swing.JLabel txt_membershipType;
    private app.bolivia.swing.JCTextField txt_orderQuantity;
    private javax.swing.JLabel txt_price;
    private javax.swing.JLabel txt_userName;
    // End of variables declaration//GEN-END:variables
}
