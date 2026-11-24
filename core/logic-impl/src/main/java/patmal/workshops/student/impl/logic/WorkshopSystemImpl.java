package patmal.workshops.student.impl.logic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.InstructorImpl;
import patmal.workshops.student.impl.model.StudentImpl;
import patmal.workshops.student.impl.model.TeamImpl;
import patmal.workshops.student.impl.model.WorkshopImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory implementation of WorkshopSystem.
 * Unknown keys: getX(id) returns null; getStudentsOfX(id) returns an empty list.
 * Every rejected operation throws IllegalArgumentException, is logged at warn level, and leaves
 * the system exactly as it was (all checks run before anything is changed).
 */
public class WorkshopSystemImpl implements WorkshopSystem {
    private static final Logger LOG = LogManager.getLogger(WorkshopSystemImpl.class);

    private final Map<String, InstructorImpl> instructors = new LinkedHashMap<>();
    private final Map<String, StudentImpl> students = new LinkedHashMap<>();
    private final Map<Integer, WorkshopImpl> workshops = new LinkedHashMap<>();
    private final Map<Integer, TeamImpl> teams = new LinkedHashMap<>();
    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    public WorkshopSystemImpl() { }

    // ------------------------------------------------------------------ creation
    @Override
    public Instructor createInstructor(String id, String name, String email) {
        if (id == null) throw reject("instructor id is null");
        if (instructors.containsKey(id)) throw reject("duplicate instructor id: " + id);
        if (!Emails.isValid(email)) throw reject("invalid e-mail address for instructor " + id + ": " + email);
        InstructorImpl i = new InstructorImpl(id, name, email);
        instructors.put(id, i);
        LOG.info("Created instructor {} ({})", id, name);
        return i;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        WorkshopImpl w = new WorkshopImpl(nextWorkshopId, name, description);
        workshops.put(nextWorkshopId, w);
        nextWorkshopId++;
        LOG.info("Created workshop {} ({})", w.getId(), name);
        return w;
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        if (instructor == null || workshop == null) throw reject("instructor and workshop must not be null");
        InstructorImpl i = instructors.get(instructor.getId());
        WorkshopImpl w = workshops.get(workshop.getId());
        if (i == null) throw reject("unknown instructor: " + instructor.getId());
        if (w == null) throw reject("unknown workshop: " + workshop.getId());
        if (w.getInstructor() != null && w.getInstructor() != i) {
            ((InstructorImpl) w.getInstructor()).removeWorkshop(w); // keep both sides consistent
        }
        w.setInstructor(i);
        i.addWorkshop(w);
        LOG.info("Workshop {} is now taught by instructor {}", w.getId(), i.getId());
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        if (id == null) throw reject("student id is null");
        if (students.containsKey(id)) throw reject("duplicate student id: " + id);
        if (!Emails.isValid(email)) throw reject("invalid e-mail address for student " + id + ": " + email);
        StudentImpl s = new StudentImpl(id, name, email);
        students.put(id, s);
        LOG.info("Created student {} ({})", id, name);
        return s;
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        // ---- validate everything first; nothing is changed until all checks pass ----
        if (workshop == null || workshops.get(workshop.getId()) == null) throw reject("unknown workshop");
        if (members == null) throw reject("members list is null");
        WorkshopImpl w = workshops.get(workshop.getId());
        Set<String> seen = new HashSet<>();
        List<StudentImpl> toAdd = new ArrayList<>();
        for (String sid : members) {
            if (!seen.add(sid)) throw reject("student listed twice: " + sid);
            StudentImpl s = students.get(sid);
            if (s == null) throw reject("unknown student: " + sid);
            if (s.getTeam() != null) throw reject("student already in a team: " + sid);
            toAdd.add(s);
        }
        // ---- apply (the id is consumed only here, so a rejected call never uses one) ----
        TeamImpl t = new TeamImpl(nextTeamId, name, w);
        for (StudentImpl s : toAdd) {
            t.addStudent(s);
            s.setTeam(t);
        }
        teams.put(nextTeamId, t);
        nextTeamId++;
        LOG.info("Created team {} ({}) in workshop {} with {} student(s)", t.getId(), name, w.getId(), toAdd.size());
        return t;
    }

    // ------------------------------------------------------------------ queries
    @Override public List<Team> getTeams() { return new ArrayList<>(teams.values()); }
    @Override public Team getTeam(Integer id) { return teams.get(id); }
    @Override public List<Student> getStudents() { return new ArrayList<>(students.values()); }
    @Override public Student getStudent(String id) { return students.get(id); }
    @Override public List<Workshop> getWorkshops() { return new ArrayList<>(workshops.values()); }
    @Override public Workshop getWorkshop(Integer id) { return workshops.get(id); }
    @Override public List<Instructor> getInstructors() { return new ArrayList<>(instructors.values()); }
    @Override public Instructor getInstructor(String id) { return instructors.get(id); }

    @Override
    public List<Student> getFreeStudents() {
        List<Student> free = new ArrayList<>();
        for (StudentImpl s : students.values()) if (s.getTeam() == null) free.add(s);
        return free;
    }

    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        List<Student> result = new ArrayList<>();
        InstructorImpl i = instructors.get(instructorId);
        if (i == null) return result;
        for (StudentImpl s : students.values()) {
            if (s.getInstructor() == i) result.add(s);
        }
        return result;
    }

    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        List<Student> result = new ArrayList<>();
        WorkshopImpl w = workshops.get(workshopId);
        if (w == null) return result;
        for (StudentImpl s : students.values()) {
            if (s.getTeam() != null && s.getTeam().getWorkshop() == w) result.add(s);
        }
        return result;
    }

    // ------------------------------------------------------------------ files (API 2.0)
    @Override
    public void exportInstructorsDataToJson(File targetFileName) {
        JsonExporter.exportInstructors(this, targetFileName);
        LOG.info("Exported {} instructor(s) to {}", instructors.size(), targetFileName);
    }

    @Override
    public void exportStudentsDataToJson(File targetFileName) {
        JsonExporter.exportStudents(this, targetFileName);
        LOG.info("Exported {} student(s) to {}", students.size(), targetFileName);
    }

    @Override
    public void exportTeamsDataToJson(File targetFileName) {
        JsonExporter.exportTeams(this, targetFileName);
        LOG.info("Exported {} team(s) to {}", teams.size(), targetFileName);
    }

    @Override
    public List<Student> importStudentDataFromFile(File source) {
        List<Student> imported = StudentCsvImporter.importStudents(this, source);
        LOG.info("Imported {} student(s) from {}", imported.size(), source);
        return imported;
    }

    // ------------------------------------------------------------------ helpers
    /** Logs the rejection at warn level and returns the exception for the caller to throw. */
    private static IllegalArgumentException reject(String message) {
        LOG.warn("Rejected operation: {}", message);
        return new IllegalArgumentException(message);
    }
}
