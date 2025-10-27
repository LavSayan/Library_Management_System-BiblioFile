/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package jframe.ManageMemebership;

import jframe.*;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.Date;

/**
 *
 * @author brizu
 */
public class OnlinePayment extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(OnlinePayment.class.getName());

    private int customerPk = 0;
    private String orderId = "";

    public OnlinePayment() {
        initComponents();
        setLocationRelativeTo(null);
    }

    private void fetchUserDetailsById() {
        String userIdStr = txt_userId.getText().trim();
        if (userIdStr.isEmpty()) {
            lbl_userError.setText("Please enter a User ID.");
            clearUserLabels();
            return;
        }

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(
                "SELECT name, email, membership_type FROM user_details WHERE user_id = ?")) {

            pst.setInt(1, Integer.parseInt(userIdStr));
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                lbl_userId.setText(userIdStr);
                lbl_userName.setText(rs.getString("name"));
                lbl_userEmail.setText(rs.getString("email"));
                lbl_membershipType.setText(rs.getString("membership_type"));
                lbl_userError.setText("");
                customerPk = Integer.parseInt(userIdStr);
            } else {
                lbl_userError.setText("User not found.");
                clearUserLabels();
            }

        } catch (Exception e) {
            lbl_userError.setText("Error fetching user.");
            clearUserLabels();
            e.printStackTrace();
        }
    }

    private void clearUserLabels() {
        lbl_userId.setText("");
        lbl_userName.setText("");
        lbl_userEmail.setText("");
        lbl_membershipType.setText("");
    }

    public boolean renewMembership(int userId) {
        try (Connection con = DBConnection.getConnection()) {

            // Step 1: Check current membership type
            String checkType = "SELECT membership_type FROM user_details WHERE user_id = ?";
            PreparedStatement pst1 = con.prepareStatement(checkType);
            pst1.setInt(1, userId);
            ResultSet rs1 = pst1.executeQuery();

            if (!rs1.next()) {
                JOptionPane.showMessageDialog(this, "User not found.");
                return false;
            }

            // Step 2: Always update user_details to Scribe
            String updateUser = "UPDATE user_details SET membership_type = 'Scribe' WHERE user_id = ?";
            PreparedStatement pst2 = con.prepareStatement(updateUser);
            pst2.setInt(1, userId);
            pst2.executeUpdate();

            // ✅ Step 3: Smart update of membership_details
            String checkStatus = "SELECT status, end_date FROM membership_details WHERE user_id = ?";
            PreparedStatement pstCheck = con.prepareStatement(checkStatus);
            pstCheck.setInt(1, userId);
            ResultSet rs = pstCheck.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                Date endDate = rs.getDate("end_date");
                Date today = new Date();

                PreparedStatement pstUpdate;

                if ("expired".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || endDate.before(today)) {
                    // Reset membership from today
                    String resetMembership = "UPDATE membership_details SET start_date = CURRENT_DATE, end_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), membership_type = 'Scribe', status = 'active' WHERE user_id = ?";
                    pstUpdate = con.prepareStatement(resetMembership);
                } else {
                    // Extend current membership
                    String extendMembership = "UPDATE membership_details SET end_date = DATE_ADD(end_date, INTERVAL 1 MONTH), membership_type = 'Scribe', status = 'active' WHERE user_id = ?";
                    pstUpdate = con.prepareStatement(extendMembership);
                }

                pstUpdate.setInt(1, userId);
                pstUpdate.executeUpdate();
            }

            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error renewing membership: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void printReferenceReceipt() {
        orderId = getUniqueId("MEM-");
        String referenceNumber = txt_referenceNumber.getText().trim();
        String filePath = InventoryUtils.billPath + orderId + ".pdf";

        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            doc.open();

            Paragraph shop = new Paragraph("BiblioFile");
            shop.setAlignment(Paragraph.ALIGN_CENTER);
            doc.add(shop);
            doc.add(new Paragraph("123 Cubao, Metro Manila, Philippines"));
            doc.add(new Paragraph("+63 123456789"));
            doc.add(new Paragraph("------------------------------------------------------------"));

            doc.add(new Paragraph("Order ID        : " + orderId));
            doc.add(new Paragraph("User ID         : " + customerPk));
            doc.add(new Paragraph("User Name       : " + lbl_userName.getText().trim()));
            doc.add(new Paragraph("Membership      : Scribe"));
            doc.add(new Paragraph("Reference Number: " + referenceNumber));
            doc.add(new Paragraph("------------------------------------------------------------"));
            doc.add(new Paragraph("Thank you for your online payment!"));

            doc.close();

            // Save to order_details
            try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO order_details (order_id, user_id, user_name, membership_type, order_date, original_price, discounted_price, cash_paid, balance, status, reference_number) "
                    + "VALUES (?, ?, ?, ?, NOW(), 0, 0, 0, 0, ?, ?)")) {

                pst.setString(1, orderId);
                pst.setInt(2, customerPk);
                pst.setString(3, lbl_userName.getText().trim());
                pst.setString(4, "Scribe");
                pst.setString(5, "completed");
                pst.setString(6, referenceNumber);
                pst.executeUpdate();

            } catch (Exception dbEx) {
                JOptionPane.showMessageDialog(null, "Error saving order: " + dbEx.getMessage());
            }

            Openpdf.OpenById(orderId);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error generating receipt: " + e.getMessage());
        }
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

        lbl_balance = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        btnRenewMembership = new rojerusan.RSMaterialButtonCircle();
        lbl_discount = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        jLabel91 = new javax.swing.JLabel();
        jLabel94 = new javax.swing.JLabel();
        jLabel97 = new javax.swing.JLabel();
        lbl_userId = new javax.swing.JLabel();
        lbl_userName = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jLabel102 = new javax.swing.JLabel();
        jPanel21 = new javax.swing.JPanel();
        jLabel103 = new javax.swing.JLabel();
        jLabel104 = new javax.swing.JLabel();
        jLabel105 = new javax.swing.JLabel();
        jLabel106 = new javax.swing.JLabel();
        jLabel107 = new javax.swing.JLabel();
        jLabel108 = new javax.swing.JLabel();
        jLabel109 = new javax.swing.JLabel();
        jLabel110 = new javax.swing.JLabel();
        jLabel111 = new javax.swing.JLabel();
        jLabel112 = new javax.swing.JLabel();
        jPanel22 = new javax.swing.JPanel();
        jLabel113 = new javax.swing.JLabel();
        jPanel23 = new javax.swing.JPanel();
        jLabel114 = new javax.swing.JLabel();
        jLabel115 = new javax.swing.JLabel();
        jLabel116 = new javax.swing.JLabel();
        jLabel117 = new javax.swing.JLabel();
        jLabel118 = new javax.swing.JLabel();
        jLabel119 = new javax.swing.JLabel();
        jLabel120 = new javax.swing.JLabel();
        jLabel121 = new javax.swing.JLabel();
        jLabel122 = new javax.swing.JLabel();
        jLabel123 = new javax.swing.JLabel();
        jPanel24 = new javax.swing.JPanel();
        jLabel124 = new javax.swing.JLabel();
        jPanel25 = new javax.swing.JPanel();
        jLabel125 = new javax.swing.JLabel();
        jLabel126 = new javax.swing.JLabel();
        jLabel127 = new javax.swing.JLabel();
        jLabel128 = new javax.swing.JLabel();
        jLabel129 = new javax.swing.JLabel();
        jLabel130 = new javax.swing.JLabel();
        jLabel131 = new javax.swing.JLabel();
        jLabel132 = new javax.swing.JLabel();
        jLabel133 = new javax.swing.JLabel();
        jLabel134 = new javax.swing.JLabel();
        jPanel26 = new javax.swing.JPanel();
        jLabel135 = new javax.swing.JLabel();
        jPanel27 = new javax.swing.JPanel();
        jLabel136 = new javax.swing.JLabel();
        jLabel137 = new javax.swing.JLabel();
        jLabel138 = new javax.swing.JLabel();
        jLabel139 = new javax.swing.JLabel();
        jLabel140 = new javax.swing.JLabel();
        jLabel141 = new javax.swing.JLabel();
        jLabel142 = new javax.swing.JLabel();
        jLabel143 = new javax.swing.JLabel();
        jLabel144 = new javax.swing.JLabel();
        jLabel145 = new javax.swing.JLabel();
        jPanel28 = new javax.swing.JPanel();
        jLabel146 = new javax.swing.JLabel();
        jPanel29 = new javax.swing.JPanel();
        jLabel147 = new javax.swing.JLabel();
        jLabel148 = new javax.swing.JLabel();
        jLabel149 = new javax.swing.JLabel();
        jLabel150 = new javax.swing.JLabel();
        jLabel151 = new javax.swing.JLabel();
        jLabel152 = new javax.swing.JLabel();
        jLabel153 = new javax.swing.JLabel();
        jLabel154 = new javax.swing.JLabel();
        jLabel155 = new javax.swing.JLabel();
        jLabel156 = new javax.swing.JLabel();
        jPanel30 = new javax.swing.JPanel();
        jLabel157 = new javax.swing.JLabel();
        jPanel31 = new javax.swing.JPanel();
        jLabel158 = new javax.swing.JLabel();
        jLabel159 = new javax.swing.JLabel();
        jLabel160 = new javax.swing.JLabel();
        jLabel161 = new javax.swing.JLabel();
        jLabel162 = new javax.swing.JLabel();
        jLabel163 = new javax.swing.JLabel();
        jLabel164 = new javax.swing.JLabel();
        jLabel165 = new javax.swing.JLabel();
        jLabel166 = new javax.swing.JLabel();
        jLabel167 = new javax.swing.JLabel();
        jPanel32 = new javax.swing.JPanel();
        jLabel168 = new javax.swing.JLabel();
        jPanel33 = new javax.swing.JPanel();
        jLabel169 = new javax.swing.JLabel();
        jLabel170 = new javax.swing.JLabel();
        jLabel171 = new javax.swing.JLabel();
        jLabel172 = new javax.swing.JLabel();
        jLabel173 = new javax.swing.JLabel();
        jLabel174 = new javax.swing.JLabel();
        jLabel175 = new javax.swing.JLabel();
        jLabel176 = new javax.swing.JLabel();
        jLabel177 = new javax.swing.JLabel();
        jLabel178 = new javax.swing.JLabel();
        jLabel95 = new javax.swing.JLabel();
        lbl_userEmail = new javax.swing.JLabel();
        jLabel98 = new javax.swing.JLabel();
        lbl_membershipType = new javax.swing.JLabel();
        lbl_userError = new javax.swing.JLabel();
        jLabel96 = new javax.swing.JLabel();
        txt_userId = new app.bolivia.swing.JCTextField();
        jLabel28 = new javax.swing.JLabel();
        jLabel99 = new javax.swing.JLabel();
        txt_referenceNumber = new app.bolivia.swing.JCTextField();
        txt_userName1 = new javax.swing.JPanel();
        lbl_fee = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbl_balance.setFont(new java.awt.Font("Serif", 1, 24)); // NOI18N
        lbl_balance.setForeground(new java.awt.Color(255, 255, 255));
        lbl_balance.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lbl_balance, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 30));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel19.setFont(new java.awt.Font("Serif", 1, 30)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(120, 27, 27));
        jLabel19.setText("Select User");
        jPanel2.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 30, 160, 60));

        btnRenewMembership.setBackground(new java.awt.Color(120, 27, 27));
        btnRenewMembership.setBorder(new javax.swing.border.MatteBorder(null));
        btnRenewMembership.setText("RENEW");
        btnRenewMembership.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRenewMembershipActionPerformed(evt);
            }
        });
        jPanel2.add(btnRenewMembership, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 360, 160, 50));

        lbl_discount.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        lbl_discount.setForeground(new java.awt.Color(120, 27, 27));
        jPanel2.add(lbl_discount, new org.netbeans.lib.awtextra.AbsoluteConstraints(1250, 390, 110, 60));

        jPanel18.setBackground(new java.awt.Color(120, 27, 27));
        jPanel18.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel91.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel91.setForeground(new java.awt.Color(255, 255, 255));
        jLabel91.setText("    User Details");
        jPanel18.add(jLabel91, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 20, 180, -1));

        jLabel94.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel94.setForeground(new java.awt.Color(255, 255, 255));
        jLabel94.setText("User Name : ");
        jPanel18.add(jLabel94, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        jLabel97.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel97.setForeground(new java.awt.Color(255, 255, 255));
        jLabel97.setText("User Id : ");
        jPanel18.add(jLabel97, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, -1, -1));

        lbl_userId.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        lbl_userId.setForeground(new java.awt.Color(255, 255, 255));
        jPanel18.add(lbl_userId, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 90, 230, 30));

        lbl_userName.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        lbl_userName.setForeground(new java.awt.Color(255, 255, 255));
        jPanel18.add(lbl_userName, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 150, 230, 30));

        jPanel20.setBackground(new java.awt.Color(120, 27, 27));
        jPanel20.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel102.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel102.setForeground(new java.awt.Color(255, 255, 255));
        jLabel102.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel102.setText("    Book Details");
        jPanel20.add(jLabel102, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel21.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel20.add(jPanel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel103.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel103.setForeground(new java.awt.Color(255, 255, 255));
        jLabel103.setText("Price : ");
        jPanel20.add(jLabel103, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel104.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel104.setForeground(new java.awt.Color(255, 255, 255));
        jPanel20.add(jLabel104, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel105.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel105.setForeground(new java.awt.Color(255, 255, 255));
        jLabel105.setText("Book Name : ");
        jPanel20.add(jLabel105, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel106.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel106.setForeground(new java.awt.Color(255, 255, 255));
        jLabel106.setText("Author : ");
        jPanel20.add(jLabel106, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel107.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel107.setForeground(new java.awt.Color(255, 255, 255));
        jLabel107.setText("Quantity :");
        jPanel20.add(jLabel107, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel108.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel108.setForeground(new java.awt.Color(255, 255, 255));
        jLabel108.setText("Book Id : ");
        jPanel20.add(jLabel108, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel109.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel109.setForeground(new java.awt.Color(255, 255, 255));
        jPanel20.add(jLabel109, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel110.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel110.setForeground(new java.awt.Color(255, 255, 255));
        jPanel20.add(jLabel110, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel111.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel111.setForeground(new java.awt.Color(255, 255, 255));
        jPanel20.add(jLabel111, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel112.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel112.setForeground(new java.awt.Color(255, 255, 255));
        jPanel20.add(jLabel112, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel18.add(jPanel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel22.setBackground(new java.awt.Color(120, 27, 27));
        jPanel22.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel113.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel113.setForeground(new java.awt.Color(255, 255, 255));
        jLabel113.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel113.setText("    Book Details");
        jPanel22.add(jLabel113, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel23.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel22.add(jPanel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel114.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel114.setForeground(new java.awt.Color(255, 255, 255));
        jLabel114.setText("Price : ");
        jPanel22.add(jLabel114, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel115.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel115.setForeground(new java.awt.Color(255, 255, 255));
        jPanel22.add(jLabel115, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel116.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel116.setForeground(new java.awt.Color(255, 255, 255));
        jLabel116.setText("Book Name : ");
        jPanel22.add(jLabel116, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel117.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel117.setForeground(new java.awt.Color(255, 255, 255));
        jLabel117.setText("Author : ");
        jPanel22.add(jLabel117, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel118.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel118.setForeground(new java.awt.Color(255, 255, 255));
        jLabel118.setText("Quantity :");
        jPanel22.add(jLabel118, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel119.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel119.setForeground(new java.awt.Color(255, 255, 255));
        jLabel119.setText("Book Id : ");
        jPanel22.add(jLabel119, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel120.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel120.setForeground(new java.awt.Color(255, 255, 255));
        jPanel22.add(jLabel120, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel121.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel121.setForeground(new java.awt.Color(255, 255, 255));
        jPanel22.add(jLabel121, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel122.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel122.setForeground(new java.awt.Color(255, 255, 255));
        jPanel22.add(jLabel122, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel123.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel123.setForeground(new java.awt.Color(255, 255, 255));
        jPanel22.add(jLabel123, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel24.setBackground(new java.awt.Color(120, 27, 27));
        jPanel24.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel124.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel124.setForeground(new java.awt.Color(255, 255, 255));
        jLabel124.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel124.setText("    Book Details");
        jPanel24.add(jLabel124, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel25.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel24.add(jPanel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel125.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel125.setForeground(new java.awt.Color(255, 255, 255));
        jLabel125.setText("Price : ");
        jPanel24.add(jLabel125, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel126.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel126.setForeground(new java.awt.Color(255, 255, 255));
        jPanel24.add(jLabel126, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel127.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel127.setForeground(new java.awt.Color(255, 255, 255));
        jLabel127.setText("Book Name : ");
        jPanel24.add(jLabel127, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel128.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel128.setForeground(new java.awt.Color(255, 255, 255));
        jLabel128.setText("Author : ");
        jPanel24.add(jLabel128, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel129.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel129.setForeground(new java.awt.Color(255, 255, 255));
        jLabel129.setText("Quantity :");
        jPanel24.add(jLabel129, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel130.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel130.setForeground(new java.awt.Color(255, 255, 255));
        jLabel130.setText("Book Id : ");
        jPanel24.add(jLabel130, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel131.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel131.setForeground(new java.awt.Color(255, 255, 255));
        jPanel24.add(jLabel131, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel132.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel132.setForeground(new java.awt.Color(255, 255, 255));
        jPanel24.add(jLabel132, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel133.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel133.setForeground(new java.awt.Color(255, 255, 255));
        jPanel24.add(jLabel133, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel134.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel134.setForeground(new java.awt.Color(255, 255, 255));
        jPanel24.add(jLabel134, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel22.add(jPanel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel18.add(jPanel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel26.setBackground(new java.awt.Color(120, 27, 27));
        jPanel26.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel135.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel135.setForeground(new java.awt.Color(255, 255, 255));
        jLabel135.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel135.setText("    Book Details");
        jPanel26.add(jLabel135, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel27.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel26.add(jPanel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel136.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel136.setForeground(new java.awt.Color(255, 255, 255));
        jLabel136.setText("Price : ");
        jPanel26.add(jLabel136, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel137.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel137.setForeground(new java.awt.Color(255, 255, 255));
        jPanel26.add(jLabel137, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel138.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel138.setForeground(new java.awt.Color(255, 255, 255));
        jLabel138.setText("Book Name : ");
        jPanel26.add(jLabel138, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel139.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel139.setForeground(new java.awt.Color(255, 255, 255));
        jLabel139.setText("Author : ");
        jPanel26.add(jLabel139, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel140.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel140.setForeground(new java.awt.Color(255, 255, 255));
        jLabel140.setText("Quantity :");
        jPanel26.add(jLabel140, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel141.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel141.setForeground(new java.awt.Color(255, 255, 255));
        jLabel141.setText("Book Id : ");
        jPanel26.add(jLabel141, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel142.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel142.setForeground(new java.awt.Color(255, 255, 255));
        jPanel26.add(jLabel142, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel143.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel143.setForeground(new java.awt.Color(255, 255, 255));
        jPanel26.add(jLabel143, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel144.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel144.setForeground(new java.awt.Color(255, 255, 255));
        jPanel26.add(jLabel144, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel145.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel145.setForeground(new java.awt.Color(255, 255, 255));
        jPanel26.add(jLabel145, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel28.setBackground(new java.awt.Color(120, 27, 27));
        jPanel28.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel146.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel146.setForeground(new java.awt.Color(255, 255, 255));
        jLabel146.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel146.setText("    Book Details");
        jPanel28.add(jLabel146, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel29.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel29Layout.setVerticalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel28.add(jPanel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel147.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel147.setForeground(new java.awt.Color(255, 255, 255));
        jLabel147.setText("Price : ");
        jPanel28.add(jLabel147, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel148.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel148.setForeground(new java.awt.Color(255, 255, 255));
        jPanel28.add(jLabel148, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel149.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel149.setForeground(new java.awt.Color(255, 255, 255));
        jLabel149.setText("Book Name : ");
        jPanel28.add(jLabel149, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel150.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel150.setForeground(new java.awt.Color(255, 255, 255));
        jLabel150.setText("Author : ");
        jPanel28.add(jLabel150, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel151.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel151.setForeground(new java.awt.Color(255, 255, 255));
        jLabel151.setText("Quantity :");
        jPanel28.add(jLabel151, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel152.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel152.setForeground(new java.awt.Color(255, 255, 255));
        jLabel152.setText("Book Id : ");
        jPanel28.add(jLabel152, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel153.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel153.setForeground(new java.awt.Color(255, 255, 255));
        jPanel28.add(jLabel153, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel154.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel154.setForeground(new java.awt.Color(255, 255, 255));
        jPanel28.add(jLabel154, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel155.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel155.setForeground(new java.awt.Color(255, 255, 255));
        jPanel28.add(jLabel155, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel156.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel156.setForeground(new java.awt.Color(255, 255, 255));
        jPanel28.add(jLabel156, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel26.add(jPanel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel30.setBackground(new java.awt.Color(120, 27, 27));
        jPanel30.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel157.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel157.setForeground(new java.awt.Color(255, 255, 255));
        jLabel157.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel157.setText("    Book Details");
        jPanel30.add(jLabel157, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel31.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel31Layout = new javax.swing.GroupLayout(jPanel31);
        jPanel31.setLayout(jPanel31Layout);
        jPanel31Layout.setHorizontalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel31Layout.setVerticalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel30.add(jPanel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel158.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel158.setForeground(new java.awt.Color(255, 255, 255));
        jLabel158.setText("Price : ");
        jPanel30.add(jLabel158, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel159.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel159.setForeground(new java.awt.Color(255, 255, 255));
        jPanel30.add(jLabel159, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel160.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel160.setForeground(new java.awt.Color(255, 255, 255));
        jLabel160.setText("Book Name : ");
        jPanel30.add(jLabel160, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel161.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel161.setForeground(new java.awt.Color(255, 255, 255));
        jLabel161.setText("Author : ");
        jPanel30.add(jLabel161, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel162.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel162.setForeground(new java.awt.Color(255, 255, 255));
        jLabel162.setText("Quantity :");
        jPanel30.add(jLabel162, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel163.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel163.setForeground(new java.awt.Color(255, 255, 255));
        jLabel163.setText("Book Id : ");
        jPanel30.add(jLabel163, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel164.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel164.setForeground(new java.awt.Color(255, 255, 255));
        jPanel30.add(jLabel164, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel165.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel165.setForeground(new java.awt.Color(255, 255, 255));
        jPanel30.add(jLabel165, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel166.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel166.setForeground(new java.awt.Color(255, 255, 255));
        jPanel30.add(jLabel166, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel167.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel167.setForeground(new java.awt.Color(255, 255, 255));
        jPanel30.add(jLabel167, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel32.setBackground(new java.awt.Color(120, 27, 27));
        jPanel32.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel168.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel168.setForeground(new java.awt.Color(255, 255, 255));
        jLabel168.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel168.setText("    Book Details");
        jPanel32.add(jLabel168, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 150, 280, -1));

        jPanel33.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel33Layout = new javax.swing.GroupLayout(jPanel33);
        jPanel33.setLayout(jPanel33Layout);
        jPanel33Layout.setHorizontalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel33Layout.setVerticalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel32.add(jPanel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, 340, 5));

        jLabel169.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel169.setForeground(new java.awt.Color(255, 255, 255));
        jLabel169.setText("Price : ");
        jPanel32.add(jLabel169, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, -1));

        jLabel170.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel170.setForeground(new java.awt.Color(255, 255, 255));
        jPanel32.add(jLabel170, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 230, 30));

        jLabel171.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel171.setForeground(new java.awt.Color(255, 255, 255));
        jLabel171.setText("Book Name : ");
        jPanel32.add(jLabel171, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, -1, -1));

        jLabel172.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel172.setForeground(new java.awt.Color(255, 255, 255));
        jLabel172.setText("Author : ");
        jPanel32.add(jLabel172, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, -1, -1));

        jLabel173.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel173.setForeground(new java.awt.Color(255, 255, 255));
        jLabel173.setText("Quantity :");
        jPanel32.add(jLabel173, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, -1, -1));

        jLabel174.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel174.setForeground(new java.awt.Color(255, 255, 255));
        jLabel174.setText("Book Id : ");
        jPanel32.add(jLabel174, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 360, -1, -1));

        jLabel175.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel175.setForeground(new java.awt.Color(255, 255, 255));
        jPanel32.add(jLabel175, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 360, 230, 30));

        jLabel176.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel176.setForeground(new java.awt.Color(255, 255, 255));
        jPanel32.add(jLabel176, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 420, 230, 30));

        jLabel177.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel177.setForeground(new java.awt.Color(255, 255, 255));
        jPanel32.add(jLabel177, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 480, 230, 30));

        jLabel178.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel178.setForeground(new java.awt.Color(255, 255, 255));
        jPanel32.add(jLabel178, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 540, 230, 30));

        jPanel30.add(jPanel32, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel26.add(jPanel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jPanel18.add(jPanel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 0, 420, 810));

        jLabel95.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel95.setForeground(new java.awt.Color(255, 255, 255));
        jLabel95.setText("Email :");
        jPanel18.add(jLabel95, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, -1, -1));

        lbl_userEmail.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        lbl_userEmail.setForeground(new java.awt.Color(255, 255, 255));
        jPanel18.add(lbl_userEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 210, 230, 30));

        jLabel98.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jLabel98.setForeground(new java.awt.Color(255, 255, 255));
        jLabel98.setText("Membership Type : ");
        jPanel18.add(jLabel98, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, -1, -1));

        lbl_membershipType.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        lbl_membershipType.setForeground(new java.awt.Color(255, 255, 255));
        jPanel18.add(lbl_membershipType, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 270, 230, 30));

        lbl_userError.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        lbl_userError.setForeground(new java.awt.Color(255, 255, 0));
        jPanel18.add(lbl_userError, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 320, 150, 40));

        jPanel2.add(jPanel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 40, 490, 380));

        jLabel96.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel96.setForeground(new java.awt.Color(120, 27, 27));
        jLabel96.setText("User Id : ");
        jPanel2.add(jLabel96, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, 80, -1));

        txt_userId.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(120, 27, 27)));
        txt_userId.setForeground(new java.awt.Color(120, 27, 27));
        txt_userId.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_userId.setPhColor(new java.awt.Color(120, 27, 27));
        txt_userId.setPlaceholder("Enter User Id....");
        txt_userId.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_userIdFocusLost(evt);
            }
        });
        txt_userId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_userIdActionPerformed(evt);
            }
        });
        jPanel2.add(txt_userId, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 100, 220, 40));

        jLabel28.setFont(new java.awt.Font("Serif", 1, 17)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(120, 27, 27));
        jLabel28.setText("Fee :");
        jPanel2.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 230, 120, 60));

        jLabel99.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel99.setForeground(new java.awt.Color(120, 27, 27));
        jLabel99.setText("Refererence Number : ");
        jPanel2.add(jLabel99, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, 170, -1));

        txt_referenceNumber.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(120, 27, 27)));
        txt_referenceNumber.setForeground(new java.awt.Color(120, 27, 27));
        txt_referenceNumber.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_referenceNumber.setPhColor(new java.awt.Color(120, 27, 27));
        txt_referenceNumber.setPlaceholder("Enter Reference Number....");
        txt_referenceNumber.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_referenceNumberFocusLost(evt);
            }
        });
        txt_referenceNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_referenceNumberActionPerformed(evt);
            }
        });
        jPanel2.add(txt_referenceNumber, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 170, 220, 40));

        txt_userName1.setBackground(new java.awt.Color(120, 27, 27));
        txt_userName1.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(0, 0, 0)));
        txt_userName1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbl_fee.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        lbl_fee.setForeground(new java.awt.Color(255, 255, 255));
        lbl_fee.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lbl_fee.setText("200");
        txt_userName1.add(lbl_fee, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 110, 30));

        jPanel2.add(txt_userName1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 250, 150, -1));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 130, 1030, 470));

        jPanel1.setBackground(new java.awt.Color(120, 27, 27));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/purchase-order-xxl.png"))); // NOI18N
        jLabel2.setText("    Manage Membership");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 0, 370, 130));

        jLabel72.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel72.setForeground(new java.awt.Color(255, 255, 255));
        jLabel72.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Rewind_48px.png"))); // NOI18N
        jLabel72.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel72MouseClicked(evt);
            }
        });
        jPanel1.add(jLabel72, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 50, 70));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1030, 130));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRenewMembershipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRenewMembershipActionPerformed
        String userId = txt_userId.getText().trim();
        String referenceNumber = txt_referenceNumber.getText().trim();

        if (userId.isEmpty() || referenceNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both User ID and Reference Number before renewing.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        fetchUserDetailsById(); // optional: refresh user info

        if (renewMembership(customerPk)) {
            printReferenceReceipt();
        }
    }//GEN-LAST:event_btnRenewMembershipActionPerformed

    private void jLabel72MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel72MouseClicked
        MembershipPayment payment = new MembershipPayment();
        payment.setVisible(true);
        dispose();
    }//GEN-LAST:event_jLabel72MouseClicked

    private void txt_userIdFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_userIdFocusLost
        if (!txt_userId.getText().trim().isEmpty()) {
            fetchUserDetailsById();
        }
    }//GEN-LAST:event_txt_userIdFocusLost

    private void txt_userIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_userIdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_userIdActionPerformed

    private void txt_referenceNumberFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_referenceNumberFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_referenceNumberFocusLost

    private void txt_referenceNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_referenceNumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_referenceNumberActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new OnlinePayment().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private rojerusan.RSMaterialButtonCircle btnRenewMembership;
    private javax.swing.JLabel jLabel102;
    private javax.swing.JLabel jLabel103;
    private javax.swing.JLabel jLabel104;
    private javax.swing.JLabel jLabel105;
    private javax.swing.JLabel jLabel106;
    private javax.swing.JLabel jLabel107;
    private javax.swing.JLabel jLabel108;
    private javax.swing.JLabel jLabel109;
    private javax.swing.JLabel jLabel110;
    private javax.swing.JLabel jLabel111;
    private javax.swing.JLabel jLabel112;
    private javax.swing.JLabel jLabel113;
    private javax.swing.JLabel jLabel114;
    private javax.swing.JLabel jLabel115;
    private javax.swing.JLabel jLabel116;
    private javax.swing.JLabel jLabel117;
    private javax.swing.JLabel jLabel118;
    private javax.swing.JLabel jLabel119;
    private javax.swing.JLabel jLabel120;
    private javax.swing.JLabel jLabel121;
    private javax.swing.JLabel jLabel122;
    private javax.swing.JLabel jLabel123;
    private javax.swing.JLabel jLabel124;
    private javax.swing.JLabel jLabel125;
    private javax.swing.JLabel jLabel126;
    private javax.swing.JLabel jLabel127;
    private javax.swing.JLabel jLabel128;
    private javax.swing.JLabel jLabel129;
    private javax.swing.JLabel jLabel130;
    private javax.swing.JLabel jLabel131;
    private javax.swing.JLabel jLabel132;
    private javax.swing.JLabel jLabel133;
    private javax.swing.JLabel jLabel134;
    private javax.swing.JLabel jLabel135;
    private javax.swing.JLabel jLabel136;
    private javax.swing.JLabel jLabel137;
    private javax.swing.JLabel jLabel138;
    private javax.swing.JLabel jLabel139;
    private javax.swing.JLabel jLabel140;
    private javax.swing.JLabel jLabel141;
    private javax.swing.JLabel jLabel142;
    private javax.swing.JLabel jLabel143;
    private javax.swing.JLabel jLabel144;
    private javax.swing.JLabel jLabel145;
    private javax.swing.JLabel jLabel146;
    private javax.swing.JLabel jLabel147;
    private javax.swing.JLabel jLabel148;
    private javax.swing.JLabel jLabel149;
    private javax.swing.JLabel jLabel150;
    private javax.swing.JLabel jLabel151;
    private javax.swing.JLabel jLabel152;
    private javax.swing.JLabel jLabel153;
    private javax.swing.JLabel jLabel154;
    private javax.swing.JLabel jLabel155;
    private javax.swing.JLabel jLabel156;
    private javax.swing.JLabel jLabel157;
    private javax.swing.JLabel jLabel158;
    private javax.swing.JLabel jLabel159;
    private javax.swing.JLabel jLabel160;
    private javax.swing.JLabel jLabel161;
    private javax.swing.JLabel jLabel162;
    private javax.swing.JLabel jLabel163;
    private javax.swing.JLabel jLabel164;
    private javax.swing.JLabel jLabel165;
    private javax.swing.JLabel jLabel166;
    private javax.swing.JLabel jLabel167;
    private javax.swing.JLabel jLabel168;
    private javax.swing.JLabel jLabel169;
    private javax.swing.JLabel jLabel170;
    private javax.swing.JLabel jLabel171;
    private javax.swing.JLabel jLabel172;
    private javax.swing.JLabel jLabel173;
    private javax.swing.JLabel jLabel174;
    private javax.swing.JLabel jLabel175;
    private javax.swing.JLabel jLabel176;
    private javax.swing.JLabel jLabel177;
    private javax.swing.JLabel jLabel178;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel91;
    private javax.swing.JLabel jLabel94;
    private javax.swing.JLabel jLabel95;
    private javax.swing.JLabel jLabel96;
    private javax.swing.JLabel jLabel97;
    private javax.swing.JLabel jLabel98;
    private javax.swing.JLabel jLabel99;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JLabel lbl_balance;
    private javax.swing.JLabel lbl_discount;
    private javax.swing.JLabel lbl_fee;
    private javax.swing.JLabel lbl_membershipType;
    private javax.swing.JLabel lbl_userEmail;
    private javax.swing.JLabel lbl_userError;
    private javax.swing.JLabel lbl_userId;
    private javax.swing.JLabel lbl_userName;
    private app.bolivia.swing.JCTextField txt_referenceNumber;
    private app.bolivia.swing.JCTextField txt_userId;
    private javax.swing.JPanel txt_userName1;
    // End of variables declaration//GEN-END:variables
}
