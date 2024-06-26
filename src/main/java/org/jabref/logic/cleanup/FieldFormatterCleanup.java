package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;

/**
 * Formats a given entry field with the specified formatter.
 */
public class FieldFormatterCleanup implements CleanupJob {

    private final Field field;
    private final Formatter formatter;

    public FieldFormatterCleanup(Field field, Formatter formatter) {
        this.field = field;
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if (InternalField.INTERNAL_ALL_FIELD == field) {
            return cleanupAllFields(entry);
        } else if (InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD == field) {
            return cleanupAllTextFields(entry);
        } else {
            return cleanupSingleField(field, entry);
        }
    }

    /**
     * Runs the formatter on the specified field in the given entry.
     * <p>
     * If the formatter returns an empty string, then the field is removed.
     *
     * @param fieldKey the field on which to run the formatter
     * @param entry    the entry to be cleaned up
     * @return a list of changes of the entry
     */
    private List<FieldChange> cleanupSingleField(Field fieldKey, BibEntry entry) {
        if (!entry.hasField(fieldKey)) {
            // Not set -> nothing to do
            return List.of();
        }

        String oldValue = entry.getField(fieldKey).orElse(null);
        String newValue = formatter.format(oldValue);

        if (newValue.equals(oldValue)) {
            return List.of();
        } else {
            if (newValue.isEmpty()) {
                entry.clearField(fieldKey);
                newValue = null;
            } else {
                entry.setField(fieldKey, newValue, EntriesEventSource.SAVE_ACTION);
            }
            FieldChange change = new FieldChange(entry, fieldKey, oldValue, newValue);
            return List.of(change);
        }
    }

    private List<FieldChange> cleanupAllFields(BibEntry entry) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        for (Field fieldKey : entry.getFields()) {
            if (!fieldKey.equals(InternalField.KEY_FIELD)) {
                fieldChanges.addAll(cleanupSingleField(fieldKey, entry));
            }
        }

        return fieldChanges;
    }

    private List<FieldChange> cleanupAllTextFields(BibEntry entry) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        Set<Field> fields = new HashSet<>(entry.getFields());
        FieldFactory.getNotTextFields().forEach(fields::remove);
        for (Field fieldKey : fields) {
            if (!fieldKey.equals(InternalField.KEY_FIELD)) {
                fieldChanges.addAll(cleanupSingleField(fieldKey, entry));
            }
        }

        return fieldChanges;
    }

    public Field getField() {
        return field;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldFormatterCleanup that) {
            return Objects.equals(field, that.field) && Objects.equals(formatter, that.formatter);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, formatter);
    }

    @Override
    public String toString() {
        return field + ": " + formatter.getName();
    }
}
