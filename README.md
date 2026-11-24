# Workshop Registration System - EX2 (Maven Evolution)

Multi-module Maven project (groupId `patmal.workshops.student.impl`, version `1.0-SNAPSHOT`):

```
root                 aggregator + top parent (all versions live here)
|-- core             aggregator + parent of the implementation modules
|   |-- model-impl   implementation of Person, Instructor, Student, Workshop, Team
|   `-- logic-impl   WorkshopSystemImpl (+ e-mail validation, JSON export, CSV import)
`-- app              workshop-cli: Main + the uber jar (target/workshop-cli.jar)
libs/                the lecturer's jars (API 2.0 and tester 2.0), not on Maven Central
```

## Requirements
- JDK 25 (`java -version`)
- Maven 3.9 or newer (`mvn -version`)

## 1. Install the lecturer's jars into the local repository (once per computer)
The jars are not on Maven Central, so they are installed by hand (run from the root folder).
The coordinates are the ones required by the exercise.

```
mvn install:install-file "-Dfile=libs/model-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=model" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
mvn install:install-file "-Dfile=libs/logic-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=logic" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
mvn install:install-file "-Dfile=libs/workshop-tester-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=workshop-tester" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
```

(The quotes are needed in PowerShell, and are harmless in other shells.)
`install-libs.bat` (Windows) and `install-libs.sh` (Linux / macOS) run the three commands.

## 2. Build
```
mvn clean install
```
This builds everything in the right order and creates `app/target/workshop-cli.jar`.

## 3. Run the application
```
java -jar app/target/workshop-cli.jar            # uses the students.csv bundled in the jar
java -jar app/target/workshop-cli.jar my.csv     # or your own CSV file
```
It writes `instructors.json`, `students.json` and `teams.json` to the current folder.

## 4. Run the tester
```
java -cp "libs/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main
```
(On Linux / macOS use `:` instead of `;`.) Expected last line: `RESULT: ALL OK`.

## Chosen libraries
| Need | Library | Where |
| --- | --- | --- |
| Logging | Log4j 2: `log4j-api` (compile, logic-impl and app), `log4j-core` (runtime, app) | config: `app/src/main/resources/log4j2.xml` |
| E-mail validation | commons-validator | logic-impl |
| JSON export | Jackson (`jackson-databind`) | logic-impl |
| CSV import (bonus) | commons-csv | logic-impl |

All versions are properties of the root pom. The uber jar is built by `maven-assembly-plugin`
(`jar-with-dependencies`, `finalName` = `workshop-cli`, `appendAssemblyId` = false, main class in the manifest, bound to the `package` phase).
