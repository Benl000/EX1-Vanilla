package patmal.workshops.student.impl.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * Builds the three JSON exports as trees of Jackson nodes (never by concatenating strings).
 * It only reads the system through the public API, so an export never changes anything.
 * Every document is a tree: a child never points back to one of its ancestors.
 */
final class JsonExporter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonExporter() { }

    // ------------------------------------------------------------------ instructors
    static void exportInstructors(WorkshopSystem system, File target) {
        Map<Integer, List<Team>> teamsByWorkshop = new HashMap<>();
        for (Team t : system.getTeams()) {
            teamsByWorkshop.computeIfAbsent(t.getWorkshop().getId(), k -> new ArrayList<>()).add(t);
        }
        ArrayNode root = MAPPER.createArrayNode();
        for (Instructor i : system.getInstructors()) {
            ObjectNode in = person(i);
            ArrayNode workshops = in.putArray("workshops");
            List<Workshop> sorted = new ArrayList<>(i.getWorkshops());
            sorted.sort(Comparator.comparing(Workshop::getId));          // creation order
            for (Workshop w : sorted) {
                ObjectNode wn = workshopDetails(w);
                ArrayNode teams = wn.putArray("teams");
                for (Team t : teamsByWorkshop.getOrDefault(w.getId(), List.of())) {
                    ObjectNode tn = MAPPER.createObjectNode();
                    tn.put("id", t.getId());
                    tn.put("name", t.getName());
                    ArrayNode students = tn.putArray("students");
                    for (Student s : t.getStudents()) students.add(person(s));
                    teams.add(tn);
                }
                workshops.add(wn);
            }
            root.add(in);
        }
        write(target, root);
    }

    // ------------------------------------------------------------------ students
    static void exportStudents(WorkshopSystem system, File target) {
        Map<String, Team> teamOfStudent = new HashMap<>();
        for (Team t : system.getTeams()) {
            for (Student s : t.getStudents()) teamOfStudent.put(s.getId(), t);
        }
        ArrayNode root = MAPPER.createArrayNode();
        for (Student s : system.getStudents()) {
            ObjectNode sn = person(s);
            Team t = teamOfStudent.get(s.getId());
            if (t == null) {
                sn.putNull("team");                                       // free student
            } else {
                ObjectNode tn = sn.putObject("team");
                tn.put("id", t.getId());
                tn.put("name", t.getName());
                ArrayNode ids = tn.putArray("studentIds");                // ids only: no cycle
                for (Student m : t.getStudents()) ids.add(m.getId());
                tn.set("workshop", workshopWithInstructor(t.getWorkshop()));
            }
            root.add(sn);
        }
        write(target, root);
    }

    // ------------------------------------------------------------------ teams
    static void exportTeams(WorkshopSystem system, File target) {
        ArrayNode root = MAPPER.createArrayNode();
        for (Team t : system.getTeams()) {
            ObjectNode tn = MAPPER.createObjectNode();
            tn.put("id", t.getId());
            tn.put("name", t.getName());
            ArrayNode students = tn.putArray("students");
            for (Student s : t.getStudents()) students.add(person(s));
            tn.set("workshop", workshopWithInstructor(t.getWorkshop()));
            root.add(tn);
        }
        write(target, root);
    }

    // ------------------------------------------------------------------ helpers
    /** id, name, email: the only details of a person. */
    private static ObjectNode person(Person p) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("id", p.getId());
        n.put("name", p.getName());
        n.put("email", p.getEmail());
        return n;
    }

    /** id, name, description: the details of a workshop. */
    private static ObjectNode workshopDetails(Workshop w) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("id", w.getId());
        n.put("name", w.getName());
        n.put("description", w.getDescription());
        return n;
    }

    private static ObjectNode workshopWithInstructor(Workshop w) {
        ObjectNode n = workshopDetails(w);
        Instructor i = w.getInstructor();
        if (i == null) n.putNull("instructor");
        else n.set("instructor", person(i));
        return n;
    }

    private static void write(File target, JsonNode root) {
        if (target == null) throw new IllegalArgumentException("target file is null");
        try {
            // Jackson writes UTF-8, overwrites an existing file and fails if the folder is missing.
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(target, root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write JSON file: " + target, e);
        }
    }
}
