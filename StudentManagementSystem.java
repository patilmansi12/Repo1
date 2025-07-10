package myproject;

import java.io.*;
import java.sql.*;
import java.util.*;

// Student class representing the data model
class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String email;
    private int age;
    private String course;
    
    // Constructors
    public Student() {}
    
    public Student(int id, String name, String email, int age, String course) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.course = course;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', email='%s', age=%d, course='%s'}", 
                           id, name, email, age, course);
    }
    
    // Method to convert to CSV format
    public String toCsv() {
        return String.format("%d,%s,%s,%d,%s", id, name, email, age, course);
    }
    
    // Static method to create Student from CSV line
    public static Student fromCsv(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length >= 5) {
            return new Student(
                Integer.parseInt(parts[0]),
                parts[1],
                parts[2],
                Integer.parseInt(parts[3]),
                parts[4]
            );
        }
        return null;
    }
}

// Abstract base class for data operations
abstract class DataOperations {
    protected List<Student> students;
    
    public DataOperations() {
        this.students = new ArrayList<>();
    }
    
    // Abstract methods to be implemented by subclasses
    public abstract void saveData() throws IOException, SQLException;
    public abstract void loadData() throws IOException, SQLException, ClassNotFoundException;
    public abstract void displayAll();
    
    // Common operations
    public void addStudent(Student student) {
        students.add(student);
        System.out.println("Student added successfully!");
    }
    
    public Student findStudentById(int id) {
        return students.stream()
                      .filter(s -> s.getId() == id)
                      .findFirst()
                      .orElse(null);
    }
    
    public boolean removeStudent(int id) {
        return students.removeIf(s -> s.getId() == id);
    }
    
    public List<Student> getAllStudents() {
        return new ArrayList<>(students);
    }
}

// File operations implementation
class FileOperations extends DataOperations {
    private String filename;
    
    public FileOperations(String filename) {
        super();
        this.filename = filename;
    }
    
    // Save students to text file (CSV format)
    public void saveToTextFile() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename + ".csv"))) {
            writer.println("ID,Name,Email,Age,Course"); // Header
            for (Student student : students) {
                writer.println(student.toCsv());
            }
        }
        System.out.println("Data saved to " + filename + ".csv");
    }
    
    // Load students from text file (CSV format)
    public void loadFromTextFile() throws IOException {
        students.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename + ".csv"))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                Student student = Student.fromCsv(line);
                if (student != null) {
                    students.add(student);
                }
            }
        }
        System.out.println("Data loaded from " + filename + ".csv");
    }
    
    // Save students to binary file (Serialization)
    public void saveToBinaryFile() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename + ".dat"))) {
            oos.writeObject(students);
        }
        System.out.println("Data saved to " + filename + ".dat");
    }
    
    // Load students from binary file (Deserialization)
    @SuppressWarnings("unchecked")
    public void loadFromBinaryFile() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename + ".dat"))) {
            students = (List<Student>) ois.readObject();
        }
        System.out.println("Data loaded from " + filename + ".dat");
    }
    
    @Override
    public void saveData() throws IOException {
        saveToTextFile();
        saveToBinaryFile();
    }
    
    @Override
    public void loadData() throws IOException, ClassNotFoundException {
        // Try loading from text file first, then binary
        try {
            loadFromTextFile();
        } catch (FileNotFoundException e) {
            try {
                loadFromBinaryFile();
            } catch (FileNotFoundException ex) {
                System.out.println("No existing data files found. Starting with empty list.");
            }
        }
    }
    
    @Override
    public void displayAll() {
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        
        System.out.println("\n--- Student Records ---");
        System.out.printf("%-5s %-15s %-25s %-5s %-15s%n", 
                         "ID", "Name", "Email", "Age", "Course");
        System.out.println("-".repeat(70));
        
        for (Student student : students) {
            System.out.printf("%-5d %-15s %-25s %-5d %-15s%n",
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            student.getAge(),
                            student.getCourse());
        }
    }
}

// Database operations implementation
class DatabaseOperations extends DataOperations {
    private String url;
    private String username;
    private String password;
    private Connection connection;
    
    public DatabaseOperations(String url, String username, String password) {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    // Establish database connection
    private void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }
    
    // Close database connection
    private void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    // Initialize database table
    public void initializeDatabase() throws SQLException {
        connect();
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(150) UNIQUE NOT NULL,
                age INTEGER NOT NULL,
                course VARCHAR(100) NOT NULL
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Database initialized successfully.");
        }
    }
    
    // Save student to database
    public void saveStudentToDb(Student student) throws SQLException {
        connect();
        String insertSQL = "INSERT OR REPLACE INTO students (id, name, email, age, course) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setInt(4, student.getAge());
            pstmt.setString(5, student.getCourse());
            
            pstmt.executeUpdate();
        }
    }
    
    @Override
    public void saveData() throws SQLException {
        connect();
        // Clear existing data and save current list
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM students");
        }
        
        for (Student student : students) {
            saveStudentToDb(student);
        }
        System.out.println("Data saved to database successfully.");
    }
    
    @Override
    public void loadData() throws SQLException {
        connect();
        students.clear();
        
        String selectSQL = "SELECT * FROM students ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                Student student = new Student(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("age"),
                    rs.getString("course")
                );
                students.add(student);
            }
        }
        System.out.println("Data loaded from database successfully.");
    }
    
    @Override
    public void displayAll() {
        if (students.isEmpty()) {
            System.out.println("No students found in database.");
            return;
        }
        
        System.out.println("\n--- Student Records (Database) ---");
        System.out.printf("%-5s %-15s %-25s %-5s %-15s%n", 
                         "ID", "Name", "Email", "Age", "Course");
        System.out.println("-".repeat(70));
        
        for (Student student : students) {
            System.out.printf("%-5d %-15s %-25s %-5d %-15s%n",
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            student.getAge(),
                            student.getCourse());
        }
    }
    
    // Additional database-specific methods
    public List<Student> findStudentsByAge(int age) throws SQLException {
        connect();
        List<Student> result = new ArrayList<>();
        String selectSQL = "SELECT * FROM students WHERE age = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setInt(1, age);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("age"),
                    rs.getString("course")
                );
                result.add(student);
            }
        }
        return result;
    }
    
    public void cleanup() throws SQLException {
        disconnect();
    }
}

// Main application class
public class StudentManagementSystem {
    private FileOperations fileOps;
    private DatabaseOperations dbOps;
    private Scanner scanner;
    
    public StudentManagementSystem() {
        this.fileOps = new FileOperations("students");
        this.dbOps = new DatabaseOperations("jdbc:sqlite:students.db", "", "");
        this.scanner = new Scanner(System.in);
    }
    
    public void initialize() {
        try {
            // Initialize database
            dbOps.initializeDatabase();
            
            // Load existing data
            fileOps.loadData();
            dbOps.loadData();
            
            System.out.println("System initialized successfully!");
        } catch (Exception e) {
            System.err.println("Error initializing system: " + e.getMessage());
        }
    }
    
    public void showMenu() {
        System.out.println("\n=== Student Management System ===");
        System.out.println("1. Add Student");
        System.out.println("2. Display All Students (File)");
        System.out.println("3. Display All Students (Database)");
        System.out.println("4. Find Student by ID");
        System.out.println("5. Remove Student");
        System.out.println("6. Save to File");
        System.out.println("7. Save to Database");
        System.out.println("8. Load from File");
        System.out.println("9. Load from Database");
        System.out.println("10. Sync File to Database");
        System.out.println("11. Exit");
        System.out.print("Choose an option: ");
    }
    
    public void run() {
        initialize();
        
        while (true) {
            showMenu();
            
            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                
                switch (choice) {
                    case 1 -> addStudent();
                    case 2 -> fileOps.displayAll();
                    case 3 -> dbOps.displayAll();
                    case 4 -> findStudent();
                    case 5 -> removeStudent();
                    case 6 -> fileOps.saveData();
                    case 7 -> dbOps.saveData();
                    case 8 -> fileOps.loadData();
                    case 9 -> dbOps.loadData();
                    case 10 -> syncFileToDatabase();
                    case 11 -> {
                        System.out.println("Goodbye!");
                        dbOps.cleanup();
                        return;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Clear invalid input
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    private void addStudent() {
        try {
            System.out.print("Enter Student ID: ");
            int id = scanner.nextInt();
            scanner.nextLine();
            
            System.out.print("Enter Name: ");
            String name = scanner.nextLine();
            
            System.out.print("Enter Email: ");
            String email = scanner.nextLine();
            
            System.out.print("Enter Age: ");
            int age = scanner.nextInt();
            scanner.nextLine();
            
            System.out.print("Enter Course: ");
            String course = scanner.nextLine();
            
            Student student = new Student(id, name, email, age, course);
            fileOps.addStudent(student);
            dbOps.addStudent(student);
            
        } catch (InputMismatchException e) {
            System.out.println("Invalid input format. Please try again.");
            scanner.nextLine();
        }
    }
    
    private void findStudent() {
        System.out.print("Enter Student ID to find: ");
        int id = scanner.nextInt();
        
        Student student = fileOps.findStudentById(id);
        if (student != null) {
            System.out.println("Student found: " + student);
        } else {
            System.out.println("Student not found.");
        }
    }
    
    private void removeStudent() {
        System.out.print("Enter Student ID to remove: ");
        int id = scanner.nextInt();
        
        boolean removed = fileOps.removeStudent(id);
        if (removed) {
            dbOps.removeStudent(id);
            System.out.println("Student removed successfully.");
        } else {
            System.out.println("Student not found.");
        }
    }
    
    private void syncFileToDatabase() {
        try {
            dbOps.students.clear();
            dbOps.students.addAll(fileOps.getAllStudents());
            dbOps.saveData();
            System.out.println("File data synchronized to database.");
        } catch (SQLException e) {
            System.err.println("Error syncing to database: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        StudentManagementSystem system = new StudentManagementSystem();
        system.run();
    }
}