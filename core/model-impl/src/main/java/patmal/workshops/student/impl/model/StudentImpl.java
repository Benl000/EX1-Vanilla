package patmal.workshops.student.impl.model;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;

import java.util.ArrayList;
import java.util.List;

/** A student only stores its team; instructor and team mates are derived from it. */
public class StudentImpl extends PersonImpl implements Student {
    private Team team; // null for a free student

    public StudentImpl(String id, String name, String email) {
        super(id, name, email);
    }

    public void setTeam(Team team) { this.team = team; }
    public Team getTeam() { return team; }

    @Override public String getTeamName() { return team == null ? null : team.getName(); }

    @Override public Instructor getInstructor() {
        return (team == null || team.getWorkshop() == null) ? null : team.getWorkshop().getInstructor();
    }

    @Override public List<Student> getTeamMates() {
        List<Student> mates = new ArrayList<>();
        if (team == null) return mates;
        for (Student s : team.getStudents()) if (s != this) mates.add(s);
        return mates;
    }
}
