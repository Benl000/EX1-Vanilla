package patmal.workshops.student.impl.logic;

import org.apache.commons.validator.routines.EmailValidator;

/** Thin wrapper around commons-validator, so the rest of the code does not depend on it. */
final class Emails {
    private static final EmailValidator VALIDATOR = EmailValidator.getInstance();

    private Emails() { }

    /** @return true for a valid address; false for an invalid one, including null. */
    static boolean isValid(String email) {
        return email != null && VALIDATOR.isValid(email);
    }
}
