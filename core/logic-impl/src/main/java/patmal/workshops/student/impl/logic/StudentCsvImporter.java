package patmal.workshops.student.impl.logic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Student;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads students from a UTF-8 CSV file (header: id,name,email). Every line is registered through
 * createStudent, so all the usual rules apply. Stops at the first rejected line; students of the
 * lines before it stay registered.
 */
final class StudentCsvImporter {
    private StudentCsvImporter() { }

    static List<Student> importStudents(WorkshopSystem system, File source) {
        if (source == null) throw new IllegalArgumentException("source file is null");
        List<Student> registered = new ArrayList<>();
        boolean headerSeen = false;
        try (Reader reader = Files.newBufferedReader(source.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {   // DEFAULT ignores empty lines
            for (CSVRecord record : parser) {
                if (!headerSeen) {
                    checkHeader(record);
                    headerSeen = true;
                    continue;
                }
                if (record.size() != 3) {
                    throw new IllegalArgumentException("CSV line " + record.getRecordNumber()
                            + " must have 3 values but has " + record.size());
                }
                registered.add(system.createStudent(record.get(0), record.get(1), record.get(2)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read CSV file: " + source, e);
        } catch (UncheckedIOException | IllegalStateException e) {
            // thrown by the CSV parser while iterating, for example a malformed quoted value
            throw new IllegalArgumentException("Malformed CSV file: " + source + " (" + e.getMessage() + ")", e);
        }
        if (!headerSeen) throw new IllegalArgumentException("CSV file has no header line: " + source);
        return registered;
    }

    private static void checkHeader(CSVRecord header) {
        boolean ok = header.size() == 3
                && "id".equals(stripBom(header.get(0)))
                && "name".equals(header.get(1))
                && "email".equals(header.get(2));
        if (!ok) throw new IllegalArgumentException("CSV header must be exactly: id,name,email");
    }

    private static String stripBom(String s) {
        return s.startsWith("﻿") ? s.substring(1) : s;
    }
}
