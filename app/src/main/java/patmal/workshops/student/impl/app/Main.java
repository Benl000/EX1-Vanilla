package patmal.workshops.student.impl.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * workshop-cli: fills a WorkshopSystem with demo data and exports it to JSON files.
 * Usage: java -jar workshop-cli.jar [students.csv]
 * Without an argument the students.csv that is bundled in the jar is used.
 */
public final class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    private Main() { }

    public static void main(String[] args) {
        WorkshopSystem system = new WorkshopSystemImpl();

        // one instructor with two workshops, one with one workshop, one workshop without an instructor
        Instructor lola = system.createInstructor("i1", "Madame Lola Lux", "lola.lux@univ.edu");
        Instructor max = system.createInstructor("i2", "Professor Max Velvet", "max.velvet@univ.edu");
        Workshop makeup = system.createWorkshop("Makeup Artistry", "Contouring, color and drama");
        Workshop runway = system.createWorkshop("Runway and Fashion", "Walks, poses and costume design");
        Workshop comedy = system.createWorkshop("Stand-up Comedy", "Timing, roasts and stage presence");
        system.createWorkshop("Lip Sync Performance", "Showmanship (no instructor yet)");
        system.associateWorkshopWithInstructor(lola, makeup);
        system.associateWorkshopWithInstructor(lola, runway);
        system.associateWorkshopWithInstructor(max, comedy);

        // students come from a CSV file
        File csv = args.length > 0 ? new File(args[0]) : bundledCsv();
        List<Student> students = system.importStudentDataFromFile(csv);
        LOG.info("Loaded {} students from {}", students.size(), csv);

        // teams: the students that are not listed here stay free (s6 and s7)
        system.createTeam("Glam Squad", makeup, List.of("s1", "s2"));
        system.createTeam("Runway Royals", runway, List.of("s3"));
        system.createTeam("Comedy Queens", comedy, List.of("s4", "s5"));
        LOG.info("Free students: {}", system.getFreeStudents().size());

        // export
        File instructorsFile = new File("instructors.json");
        File studentsFile = new File("students.json");
        File teamsFile = new File("teams.json");
        system.exportInstructorsDataToJson(instructorsFile);
        system.exportStudentsDataToJson(studentsFile);
        system.exportTeamsDataToJson(teamsFile);
        LOG.info("JSON files written to {}", instructorsFile.getAbsoluteFile().getParent());
    }

    /** Copies the students.csv of the jar to a temporary file (the API needs a File). */
    private static File bundledCsv() {
        try (InputStream in = Main.class.getResourceAsStream("/students.csv")) {
            if (in == null) throw new IllegalStateException("students.csv is missing from the jar");
            Path tmp = Files.createTempFile("students", ".csv");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
