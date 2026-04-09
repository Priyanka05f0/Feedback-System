package com.edupulse.service;

import com.edupulse.model.Faculty;
import com.edupulse.model.SubjectAssignment;
import com.edupulse.repository.FacultyRepository;
import com.edupulse.repository.SubjectAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataInitializerService implements CommandLineRunner {

    @Autowired private FacultyRepository facultyRepo;
    @Autowired private SubjectAssignmentRepository assignmentRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String[] BRANCHES = {"CSE", "ECE", "MECH", "CIVIL", "EEE", "IT"};
    private static final String[] SECTIONS = {"A", "B", "C"};

    private static final Object[][] FACULTY_DATA = {
        {"Rajesh Kumar",  "RK", "rajesh",    "pass123",  Faculty.Role.STAFF,     "Computer Science",     "Professor"},
        {"Suresh Rao",    "SR", "suresh",    "pass123",  Faculty.Role.STAFF,     "Electronics",          "Associate Professor"},
        {"Amit Patel",    "AP", "amit",      "pass123",  Faculty.Role.STAFF,     "Mechanical",           "Professor"},
        {"Neha Sharma",   "NS", "neha",      "pass123",  Faculty.Role.STAFF,     "Civil",                "Assistant Professor"},
        {"Meera Nair",    "MN", "meera",     "pass123",  Faculty.Role.STAFF,     "Electrical",           "Professor"},
        {"Vikram Singh",  "VS", "vikram",    "pass123",  Faculty.Role.STAFF,     "Information Tech",     "Associate Professor"},
        {"James Wilson",  "JW", "principal", "admin123", Faculty.Role.PRINCIPAL, "Administration",       "Principal"}
    };

    private static final String[][] SUBJECTS_BY_SEM = {
        {"Mathematics I",       "Data Structures",   "Algorithms",        "Operating Systems",
         "Database Systems",    "Compiler Design",   "Software Eng",      "Advanced Algorithms"},
        {"Physics",             "Chemistry",         "Network Theory",    "Signals & Systems",
         "Digital Electronics", "Microprocessors",   "VLSI Design",       "Embedded Systems"},
        {"Engineering Drawing", "Thermodynamics I",  "Fluid Mechanics",   "Manufacturing",
         "Machine Design",      "Heat Transfer",     "Industrial Eng",    "Robotics"},
        {"Surveying I",         "Surveying II",      "Structural Analysis","Concrete Technology",
         "Geotechnical Eng",    "Transportation Eng","Environmental Eng",  "Bridge Engineering"},
        {"Electrical Circuits", "Elect. Circuits II","Electrical Machines","Power Systems I",
         "Control Systems",     "Power Electronics", "Power Systems II",  "High Voltage Eng"},
        {"IT Fundamentals",     "Web Technologies",  "Software Engineering","Computer Networks",
         "Info Security",       "Cloud Computing",   "Big Data Analytics","AI & Machine Learning"}
    };

    @Override
    public void run(String... args) {
        if (facultyRepo.count() > 0) {
            System.out.println("✅ Data already initialized, skipping.");
            return;
        }
        initData();
    }

    private void initData() {
        Faculty[] saved = new Faculty[FACULTY_DATA.length];

        for (int i = 0; i < FACULTY_DATA.length; i++) {
            Object[] f = FACULTY_DATA[i];
            Faculty faculty = new Faculty();
            faculty.setName((String) f[0]);
            faculty.setShortName((String) f[1]);
            faculty.setUsername((String) f[2]);
            faculty.setPassword(passwordEncoder.encode((String) f[3]));
            faculty.setRole((Faculty.Role) f[4]);
            faculty.setDepartment((String) f[5]);
            faculty.setDesignation((String) f[6]);
            saved[i] = facultyRepo.save(faculty);
        }

        // Assign each of 6 staff faculty to all semester/branch/section combos
        for (int fi = 0; fi < 6; fi++) {
            for (int sem = 1; sem <= 8; sem++) {
                for (String branch : BRANCHES) {
                    for (String section : SECTIONS) {
                        SubjectAssignment sa = new SubjectAssignment();
                        sa.setFaculty(saved[fi]);
                        sa.setSemester(sem);
                        sa.setBranch(branch);
                        sa.setSection(section);
                        sa.setSubjectName(SUBJECTS_BY_SEM[fi][sem - 1]);
                        sa.setSubjectType(fi % 2 == 0 ? "THEORY" : "LAB");
                        assignmentRepo.save(sa);
                    }
                }
            }
        }

        System.out.println("\n========================================");
        System.out.println("✅ EduPulse Data Initialized!");
        System.out.println("========================================");
        System.out.println("STAFF LOGINS (Staff tab):");
        System.out.println("  rajesh / pass123   | suresh / pass123");
        System.out.println("  amit   / pass123   | neha   / pass123");
        System.out.println("  meera  / pass123   | vikram / pass123");
        System.out.println("PRINCIPAL LOGIN:");
        System.out.println("  principal / admin123");
        System.out.println("========================================\n");
    }
}
