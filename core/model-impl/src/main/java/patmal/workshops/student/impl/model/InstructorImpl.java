package patmal.workshops.student.impl.model;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;

import java.util.ArrayList;
import java.util.List;

public class InstructorImpl extends PersonImpl implements Instructor {
    private final List<Workshop> workshops = new ArrayList<>();

    public InstructorImpl(String id, String name, String email) {
        super(id, name, email);
    }

    /** Returns a copy, so callers cannot change the internal list. */
    @Override public List<Workshop> getWorkshops() { return new ArrayList<>(workshops); }

    /** Internal: keeps the two-way relation; never adds the same workshop twice. */
    public void addWorkshop(Workshop w) {
        if (!workshops.contains(w)) workshops.add(w);
    }

    public void removeWorkshop(Workshop w) { workshops.remove(w); }
}
