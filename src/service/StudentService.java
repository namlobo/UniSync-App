package service;

import dao.interfaces.StudentDAO;
import dao.impl.StudentDAOImpl;
import model.user.Student;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
// StudentService manages registration and login rules, following SRP and Service Layer.
public class StudentService {

    private final StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAOImpl();
    }

    // Prevents duplicate registrations before saving, keeping validation out of the UI layer.
    public void registerStudent(Student student) {

        Optional<Student> existing = studentDAO.findByEmail(student.getEmail());

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Student already registered with this email.");
        }

        studentDAO.save(student);
    }

    // Authenticates a student by coordinating lookup and password checks, following low coupling.
    public Student login(String email, String password) {

        Optional<Student> studentOptional = studentDAO.findByEmail(email);

        if (studentOptional.isEmpty()) {
            studentOptional = studentDAO.findById(email);
        }

        if (studentOptional.isEmpty()) {
            throw new IllegalArgumentException("Student not found.");
        }

        Student student = studentOptional.get();

        // Check if student is suspended BEFORE checking password
        if (student.isSuspended()) {
            throw new IllegalArgumentException("Your account has been suspended. Please contact the administrator.");
        }

        if (!student.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid password.");
        }

        // Check if student is suspended
        if (student.isSuspended()) {
            throw new IllegalArgumentException("Your account has been suspended. Please contact the administrator.");
        }

        return student;
    }

    // Returns all students for admin views while centralizing use-case logic in one class.
    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    // Retrieves a student by ID for operations like checking suspension status.
    public Student getStudentById(String studentId) {
        Optional<Student> studentOptional = studentDAO.findById(studentId);

        if (studentOptional.isEmpty()) {
            throw new IllegalArgumentException("Student not found.");
        }

        return studentOptional.get();
    }

    // Sends student updates through the service layer to preserve separation of concerns.
    public void updateStudent(Student student) {
        studentDAO.update(student);
    }

    public void removeStudent(String studentId) {
        studentDAO.delete(studentId);
    }
}
