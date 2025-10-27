/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package jframe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author brizu
 */
public class ManageUsers extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManageUsers.class.getName());

    /**
     * Creates new form ManageBooks
     */
    String userName, membershipType;
    int userId;
    DefaultTableModel model;

    public ManageUsers() {
        initComponents();
        setUserDetails();
    }

    //inputs the user details in the table
    public void setUserDetails() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM user_details")) {

            model = (DefaultTableModel) tbl_usersDetails.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                String userId = rs.getString("user_id");
                String userName = rs.getString("name");
                String userEmail = rs.getString("email");
                String membershipType = rs.getString("membership_type");

                Object[] obj = {userId, userName, userEmail, membershipType};
                model.addRow(obj);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //to add user to user_details table
    public boolean addUser() {
        String userIdText = txt_userId.getText().trim();
        String userName = txt_userName.getText().trim();
        String userEmail = txt_userEmail.getText().trim();
        String membershipType = combo_membershipType.getSelectedItem().toString();

        if (userIdText.isEmpty() || userName.isEmpty() || userEmail.isEmpty() || membershipType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter user ID, name, email, and membership type.");
            return false;
        }

        if (!isValidEmail(userEmail)) {
            JOptionPane.showMessageDialog(this,
                    "Invalid email format.\nPlease enter a valid email like: user@example.com",
                    "Email Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.");
            return false;
        }

        try (Connection con = DBConnection.getConnection()) {
            String userSql = "INSERT INTO user_details (user_id, name, email, membership_type) VALUES (?, ?, ?, ?)";
            PreparedStatement userPst = con.prepareStatement(userSql);
            userPst.setInt(1, userId);
            userPst.setString(2, userName);
            userPst.setString(3, userEmail);
            userPst.setString(4, membershipType);
            int rows = userPst.executeUpdate();

            if (rows > 0) {
                java.sql.Date startDate = new java.sql.Date(System.currentTimeMillis());
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(Calendar.MONTH, 1);
                java.sql.Date endDate = new java.sql.Date(cal.getTimeInMillis());

                String membershipSql = "INSERT INTO membership_details (user_id, user_name, membership_type, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement memPst = con.prepareStatement(membershipSql);
                memPst.setInt(1, userId);
                memPst.setString(2, userName);
                memPst.setString(3, membershipType);
                memPst.setDate(4, startDate);
                memPst.setDate(5, endDate);
                memPst.setString(6, "active");
                memPst.executeUpdate();

                return true;
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        return false;
    }

    //to update user details
    public boolean updateUser() {
        userId = Integer.parseInt(txt_userId.getText().trim());
        userName = txt_userName.getText().trim();
        String userEmail = txt_userEmail.getText().trim();
        String selectedMembershipType = combo_membershipType.getSelectedItem().toString();

        if (userName.isEmpty() || userEmail.isEmpty() || selectedMembershipType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter name, email, and membership type.");
            return false;
        }

        if (!isValidEmail(userEmail)) {
            JOptionPane.showMessageDialog(this,
                    "Invalid email format.\nPlease enter a valid email like: user@example.com",
                    "Email Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Check current membership type from DB
            String checkSql = "SELECT membership_type FROM user_details WHERE user_id = ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setInt(1, userId);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next()) {
                String currentMembershipType = rs.getString("membership_type");

                if (!currentMembershipType.equals(selectedMembershipType)) {
                    JOptionPane.showMessageDialog(this,
                            "You cannot update the user's membership type here.\nPlease go to Manage Membership to make changes.",
                            "Membership Type Restricted",
                            JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }

            // Proceed with update
            String sql = "UPDATE user_details SET name = ?, email = ? WHERE user_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, userName);
            pst.setString(2, userEmail);
            pst.setInt(3, userId);

            return pst.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //to delete user
    public boolean deleteUser(int userId) {
        try (Connection con = DBConnection.getConnection()) {
            // Step 1: Delete membership record
            String deleteMembership = "DELETE FROM membership_details WHERE user_id = ?";
            PreparedStatement pst1 = con.prepareStatement(deleteMembership);
            pst1.setInt(1, userId);
            pst1.executeUpdate();

            // Step 2: Delete user record
            String deleteUser = "DELETE FROM user_details WHERE user_id = ?";
            PreparedStatement pst2 = con.prepareStatement(deleteUser);
            pst2.setInt(1, userId);
            int rows = pst2.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting user: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    //to clear table 
    public void clearTable() {
        DefaultTableModel model = (DefaultTableModel) tbl_usersDetails.getModel();
        model.setRowCount(0);
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
        txt_userId = new app.bolivia.swing.JCTextField();
        jLabel12 = new javax.swing.JLabel();
        txt_userName = new app.bolivia.swing.JCTextField();
        jLabel13 = new javax.swing.JLabel();
        rSMaterialButtonCircle2 = new rojerusan.RSMaterialButtonCircle();
        rSMaterialButtonCircle3 = new rojerusan.RSMaterialButtonCircle();
        rSMaterialButtonCircle4 = new rojerusan.RSMaterialButtonCircle();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        combo_membershipType = new javax.swing.JComboBox<>();
        jLabel14 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        txt_userEmail = new app.bolivia.swing.JCTextField();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_usersDetails = new rojeru_san.complementos.RSTableMetro();
        jLabel16 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(120, 27, 27));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txt_userId.setBackground(new java.awt.Color(120, 27, 27));
        txt_userId.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(255, 255, 255)));
        txt_userId.setForeground(new java.awt.Color(255, 255, 255));
        txt_userId.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_userId.setPhColor(new java.awt.Color(255, 255, 255));
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
        jPanel1.add(txt_userId, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 180, 360, 40));

        jLabel12.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Name");
        jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 260, 170, -1));

        txt_userName.setBackground(new java.awt.Color(120, 27, 27));
        txt_userName.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(255, 255, 255)));
        txt_userName.setForeground(new java.awt.Color(255, 255, 255));
        txt_userName.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_userName.setPhColor(new java.awt.Color(255, 255, 255));
        txt_userName.setPlaceholder("Enter User Name....");
        txt_userName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_userNameFocusLost(evt);
            }
        });
        txt_userName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_userNameActionPerformed(evt);
            }
        });
        jPanel1.add(txt_userName, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 290, 360, 40));

        jLabel13.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Select Membership Type");
        jPanel1.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 480, 170, -1));

        rSMaterialButtonCircle2.setBackground(new java.awt.Color(255, 255, 255));
        rSMaterialButtonCircle2.setBorder(new javax.swing.border.MatteBorder(null));
        rSMaterialButtonCircle2.setForeground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle2.setText("DELETE");
        rSMaterialButtonCircle2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle2ActionPerformed(evt);
            }
        });
        jPanel1.add(rSMaterialButtonCircle2, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 660, 130, 60));

        rSMaterialButtonCircle3.setBackground(new java.awt.Color(255, 255, 255));
        rSMaterialButtonCircle3.setBorder(new javax.swing.border.MatteBorder(null));
        rSMaterialButtonCircle3.setForeground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle3.setText("ADD");
        rSMaterialButtonCircle3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle3ActionPerformed(evt);
            }
        });
        jPanel1.add(rSMaterialButtonCircle3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 660, 130, 60));

        rSMaterialButtonCircle4.setBackground(new java.awt.Color(255, 255, 255));
        rSMaterialButtonCircle4.setBorder(new javax.swing.border.MatteBorder(null));
        rSMaterialButtonCircle4.setForeground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle4.setText("UPDATE");
        rSMaterialButtonCircle4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle4ActionPerformed(evt);
            }
        });
        jPanel1.add(rSMaterialButtonCircle4, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 660, 130, 60));

        jLabel34.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel34.setForeground(new java.awt.Color(255, 255, 255));
        jLabel34.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Collaborator_Male_35px.png"))); // NOI18N
        jPanel1.add(jLabel34, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 290, 50, 40));

        jLabel35.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel35.setForeground(new java.awt.Color(255, 255, 255));
        jLabel35.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Contact_26px.png"))); // NOI18N
        jPanel1.add(jLabel35, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 180, 50, 40));

        jLabel36.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel36.setForeground(new java.awt.Color(255, 255, 255));
        jLabel36.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Unit_26px.png"))); // NOI18N
        jPanel1.add(jLabel36, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 500, 40, 50));

        jLabel72.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel72.setForeground(new java.awt.Color(255, 255, 255));
        jLabel72.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Rewind_48px.png"))); // NOI18N
        jLabel72.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel72MouseClicked(evt);
            }
        });
        jPanel1.add(jLabel72, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 50, 70));

        jLabel15.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("User Id");
        jPanel1.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 150, 170, -1));

        combo_membershipType.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        combo_membershipType.setForeground(new java.awt.Color(120, 27, 27));
        combo_membershipType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Normal", "Scribe" }));
        jPanel1.add(combo_membershipType, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 510, 360, 40));

        jLabel14.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("Email");
        jPanel1.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 370, 170, -1));

        jLabel37.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel37.setForeground(new java.awt.Color(255, 255, 255));
        jLabel37.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Moleskine_26px.png"))); // NOI18N
        jPanel1.add(jLabel37, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 400, 50, 40));

        txt_userEmail.setBackground(new java.awt.Color(120, 27, 27));
        txt_userEmail.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(255, 255, 255)));
        txt_userEmail.setForeground(new java.awt.Color(255, 255, 255));
        txt_userEmail.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        txt_userEmail.setPhColor(new java.awt.Color(255, 255, 255));
        txt_userEmail.setPlaceholder("Enter Email....");
        txt_userEmail.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_userEmailFocusLost(evt);
            }
        });
        txt_userEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_userEmailActionPerformed(evt);
            }
        });
        jPanel1.add(txt_userEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 400, 360, 40));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 580, 830));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tbl_usersDetails.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "User Id", "Name", "Email", "Membership Type"
            }
        ));
        tbl_usersDetails.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tbl_usersDetails.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tbl_usersDetails.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tbl_usersDetails.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tbl_usersDetails.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        tbl_usersDetails.setFuenteFilas(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tbl_usersDetails.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 18)); // NOI18N
        tbl_usersDetails.setFuenteHead(new java.awt.Font("Serif", 1, 20)); // NOI18N
        tbl_usersDetails.setRowHeight(40);
        tbl_usersDetails.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_usersDetailsMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tbl_usersDetailsMouseEntered(evt);
            }
        });
        jScrollPane1.setViewportView(tbl_usersDetails);

        jPanel3.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 290, 850, 320));

        jLabel16.setFont(new java.awt.Font("Serif", 0, 30)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(120, 27, 27));
        jLabel16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/userIcon - Copy.png"))); // NOI18N
        jLabel16.setText("    Manage Users");
        jPanel3.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 110, 310, 110));

        jPanel2.setBackground(new java.awt.Color(120, 27, 27));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 420, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel3.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 235, 420, 5));

        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, -10, 1160, 850));

        setSize(new java.awt.Dimension(1724, 824));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void txt_userIdFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_userIdFocusLost

    }//GEN-LAST:event_txt_userIdFocusLost

    private void txt_userIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_userIdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_userIdActionPerformed

    private void txt_userNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_userNameFocusLost

    }//GEN-LAST:event_txt_userNameFocusLost

    private void txt_userNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_userNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_userNameActionPerformed

    private void rSMaterialButtonCircle2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle2ActionPerformed
        try {
            int userId = Integer.parseInt(txt_userId.getText().trim());
            if (deleteUser(userId)) {
                JOptionPane.showMessageDialog(this, "User Deleted");
                clearTable();
                setUserDetails();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to Delete the User");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid or missing User ID.");
        }
    }//GEN-LAST:event_rSMaterialButtonCircle2ActionPerformed

    private void rSMaterialButtonCircle3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle3ActionPerformed
        if (addUser()) {
            JOptionPane.showMessageDialog(this, "User added successfully.");
            clearTable();
            setUserDetails();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add the user.");
        }
    }//GEN-LAST:event_rSMaterialButtonCircle3ActionPerformed

    private void rSMaterialButtonCircle4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle4ActionPerformed
        if (updateUser() == true) {
            JOptionPane.showMessageDialog(this, "User Updated");
            clearTable();
            setUserDetails();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to Update the User");
        }
    }//GEN-LAST:event_rSMaterialButtonCircle4ActionPerformed

    private void jLabel72MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel72MouseClicked
        // TODO add your handling code here:
        HomePage home = new HomePage();
        home.setVisible(true);
        dispose();
    }//GEN-LAST:event_jLabel72MouseClicked

    private void tbl_usersDetailsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tbl_usersDetailsMouseClicked
        int rowNo = tbl_usersDetails.getSelectedRow();
        TableModel model = tbl_usersDetails.getModel();

        txt_userId.setText(model.getValueAt(rowNo, 0).toString());
        txt_userName.setText(model.getValueAt(rowNo, 1).toString());
        txt_userEmail.setText(model.getValueAt(rowNo, 2).toString());
        combo_membershipType.setSelectedItem(model.getValueAt(rowNo, 3).toString());

    }//GEN-LAST:event_tbl_usersDetailsMouseClicked

    private void tbl_usersDetailsMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tbl_usersDetailsMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tbl_usersDetailsMouseEntered

    private void txt_userEmailFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_userEmailFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_userEmailFocusLost

    private void txt_userEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_userEmailActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_userEmailActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new ManageUsers().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> combo_membershipType;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle2;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle3;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle4;
    private rojeru_san.complementos.RSTableMetro tbl_usersDetails;
    private app.bolivia.swing.JCTextField txt_userEmail;
    private app.bolivia.swing.JCTextField txt_userId;
    private app.bolivia.swing.JCTextField txt_userName;
    // End of variables declaration//GEN-END:variables

}
