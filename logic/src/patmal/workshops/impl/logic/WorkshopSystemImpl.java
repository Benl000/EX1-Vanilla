package patmal.workshops.impl.logic;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.*;
import patmal.workshops.impl.model.*;

import java.util.*;

/**
 * In-memory implementation of WorkshopSystem.
 * Unknown keys: getX(id) returns null; getStudentsOfX(id) returns an empty list.
 */
public class WorkshopSystemImpl implements WorkshopSystem {
    private final Map<String, InstructorImpl> instructors = new LinkedHashMap<>();
    private final Map<String, StudentImpl> students = new LinkedHashMap<>();
    private final Map<Integer, WorkshopImpl> workshops = new LinkedHashMap<>();
    private final Map<Integer, TeamImpl> teams = new LinkedHashMap<>();
    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    public WorkshopSystemImpl() { }

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        if (id == null) throw new IllegalArgumentException("instructor id is null");
        if (instructors.containsKey(id)) throw new IllegalArgumentException("duplicate instructor id: " + id);
        InstructorImpl i = new InstructorImpl(id, name, email);
        instructors.put(id, i);
        return i;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        WorkshopImpl w = new WorkshopImpl(nextWorkshopId, name, description);
        workshops.put(nextWorkshopId, w);
        nextWorkshopId++;
        return w;
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        if (instructor == null || workshop == null) throw new IllegalArgumentException("null argument");
        InstructorImpl i = instructors.get(instructor.getId());
        WorkshopImpl w = workshops.get(workshop.getId());
        if (i == null) throw new IllegalArgumentException("unknown instructor: " + instructor.getId());
        if (w == null) throw new IllegalArgumentException("unknown workshop: " + workshop.getId());
        if (w.getInstructor() != null && w.getInstructor() != i) {
            ((InstructorImpl) w.getInstructor()).removeWorkshop(w); // re-assignment keeps both sides consistent
        }
        w.setInstructor(i);
        i.addWorkshop(w);
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        if (id == null) throw new IllegalArgumentException("student id is null");
        if (students.containsKey(id)) throw new IllegalArgumentException("duplicate student id: " + id);
        StudentImpl s = new StudentImpl(id, name, email);
        students.put(id, s);
        return s;
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        // ---- validate everything first; nothing is changed until all checks pass ----
        if (workshop == null || workshops.get(workshop.getId()) == null)
            throw new IllegalArgumentException("unknown workshop");
        if (members == null) throw new IllegalArgumentException("members is null");
        WorkshopImpl w = workshops.get(workshop.getId());
        Set<String> seen = new HashSet<>();
        List<StudentImpl> toAdd = new ArrayList<>();
        for (String sid : members) {
            if (!seen.add(sid)) throw new IllegalArgumentException("student listed twice: " + sid);
            StudentImpl s = students.get(sid);
            if (s == null) throw new IllegalArgumentException("unknown student: " + sid);
            if (s.getTeam() != null) throw new IllegalArgumentException("student already in a team: " + sid);
            toAdd.add(s);
        }
        // ---- apply ----
        TeamImpl t = new TeamImpl(nextTeamId, name, w);
        for (StudentImpl s : toAdd) {
            t.addStudent(s);
            s.setTeam(t);
        }
        teams.put(nextTeamId, t);
        nextTeamId++;
        return t;
    }

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
}
