import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class HostelManagementSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hostel?useSSL=false";
    private static final String DB_USER = "root"; //required fields
    private static final String DB_PASSWORD = "PASS";
    private static final String ADMIN_PASSWORD = "PASS";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to database");

            Scanner scanner = new Scanner(System.in);

            System.out.println("Welcome to the CGU Hostel Management System");
            System.out.print("Enter your user ID (student ID or admin password): ");
            String userId = scanner.nextLine();

            if (userId.equals(ADMIN_PASSWORD)) {
                System.out.println("Admin authentication successful. Accessing Admin functions...");
                runAdminMenu(conn, scanner);
            } else {
                try {
                    int studentId = Integer.parseInt(userId);
                    System.out.println("Student authentication successful. Accessing Student functions...");
                    runStudentMenu(conn, studentId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid user ID or password. Exiting...");
                }
            }

            scanner.close();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        System.out.println("Exiting Hostel Management System");
    }

    private static void runAdminMenu(Connection conn, Scanner scanner) {
        createStudentsTable(conn); // Create students table if not exists

        boolean exit = false;
        while (!exit) {
            System.out.println("\nAdmin Menu:");
            System.out.println("1. Allocate Room for a Student");
            System.out.println("2. View Room Occupancy Status");
            System.out.println("3. Generate Billing Details");
            System.out.println("4. Generate Reports");
            System.out.println("5. Mark Bill Paid");
            System.out.println("6. Delete Student Record");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline after reading int
            int sid;
            switch (choice) {
                case 1:
                    allocateRoom(conn, scanner);
                    break;
                case 2:
                    viewRoomOccupancy(conn);
                    break;
                case 3:
                    generateBilling(conn);
                    break;
                case 4:
                    generateReports(conn);
                    break;
                case 5:
                    System.out.print("Enter student ID : ");
                    sid = scanner.nextInt();
                    scanner.nextLine();
                    markBillAsPaid(conn, sid);
                    break;
                case 6:
                    System.out.print("Enter student ID : ");
                    sid = scanner.nextInt();
                    scanner.nextLine();
                    deleteStudentRecord(conn, sid);
                    break;
                case 7:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 7.");
            }
        }
    }

    private static void runStudentMenu(Connection conn, int studentId) {
        generateStudentReport(conn, studentId);
    }

    private static void createStudentsTable(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(20) NOT NULL, " +
                    "room_number INT, " +
                    "bill_paid VARCHAR(3) DEFAULT 'NO', " +
                    "entry_date DATE)";
            stmt.executeUpdate(sql);
            System.out.println("Students table created");
        } catch (SQLException e) {
            System.err.println("Error creating students table: " + e.getMessage());
        }
    }

    private static void allocateRoom(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter student name: ");
            String name = scanner.nextLine();

            System.out.print("Enter room number to allocate: ");
            int roomNumber = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter date (yyyy-mm-dd): ");
            String dateString = scanner.nextLine();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date parsedDate = dateFormat.parse(dateString);
            java.sql.Date date = new java.sql.Date(parsedDate.getTime());

            String insertStudentSQL = "INSERT INTO students (name, room_number, entry_date) VALUES (?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertStudentSQL)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, roomNumber);
                pstmt.setDate(3, date);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Student inserted successfully");
                } else {
                    System.out.println("Failed to insert student");
                }
            }
        } catch (SQLException | ParseException e) {
            System.err.println("Error allocating room: " + e.getMessage());
        }
    }

    private static void viewRoomOccupancy(Connection conn) {
        String occupancySQL = "SELECT room_number, COUNT(*) AS occupied_count FROM students WHERE room_number IS NOT NULL GROUP BY room_number ORDER BY room_number";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(occupancySQL)) {
            System.out.println("\nRoom Occupancy Status:");
            while (rs.next()) {
                int roomNumber = rs.getInt("room_number");
                int occupiedCount = rs.getInt("occupied_count");
                System.out.println("Room " + roomNumber + ": Occupied " + occupiedCount + " students");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing room occupancy: " + e.getMessage());
        }
    }

    private static void generateBilling(Connection conn) {
        String billingSQL = "SELECT id, name, room_number, bill_paid FROM students";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(billingSQL)) {
            System.out.println("\nBilling Details:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int roomNumber = rs.getInt("room_number");
                String billPaid = rs.getString("bill_paid");
                System.out.println("ID: " + id + ", Name: " + name + ", Room: " + roomNumber + ", Bill Paid: " + billPaid);
            }
        } catch (SQLException e) {
            System.err.println("Error generating billing details: " + e.getMessage());
        }
    }

    private static void generateReports(Connection conn) {
        String reportSQL = "SELECT id, name, room_number, bill_paid, entry_date FROM students ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(reportSQL)) {
            System.out.println("\nHostel Management System Reports:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int roomNumber = rs.getInt("room_number");
                String billPaid = rs.getString("bill_paid");
                Date entryDate = rs.getDate("entry_date");
                System.out.println("ID: " + id + ", Name: " + name + ", Room: " + roomNumber +
                        ", Bill Paid: " + billPaid + ", Entry Date: " + entryDate);
            }
        } catch (SQLException e) {
            System.err.println("Error generating reports: " + e.getMessage());
        }
    }

    private static void generateStudentReport(Connection conn, int studentId) {
        String reportSQL = "SELECT id, name, room_number, bill_paid, entry_date FROM students WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(reportSQL)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int roomNum = rs.getInt("room_number");
                String billPaid = rs.getString("bill_paid");
                Date entryDate = rs.getDate("entry_date");

                System.out.println("\nStudent Report:");
                System.out.println("ID: " + id + ", Name: " + name + ", Room: " + roomNum +
                        ", Bill Paid: " + billPaid + ", Entry Date: " + entryDate);
            } else {
                System.out.println("Student record not found with provided ID.");
            }
        } catch (SQLException e) {
            System.err.println("Error generating student report: " + e.getMessage());
        }
    }

    private static void markBillAsPaid(Connection conn, int studentId) {
        try {
            String updateBillSQL = "UPDATE students SET bill_paid = 'YES' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBillSQL)) {
                pstmt.setInt(1, studentId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Bill marked as paid for student with ID " + studentId);
                } else {
                    System.out.println("Failed to mark bill as paid. Student not found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error marking bill as paid: " + e.getMessage());
        }
    }

    private static void deleteStudentRecord(Connection conn, int studentId) {
        try {
            String deleteSQL = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                pstmt.setInt(1, studentId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Student record with ID " + studentId + " deleted successfully");
                } else {
                    System.out.println("Failed to delete student record. Student not found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting student record: " + e.getMessage());
        }
    }
}
