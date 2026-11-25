package util;

import models.Staff;

/**
 * One rule for every "update when authorized" operation:
 * the staff member who owns the record, or any Doctor, may change it.
 */
public final class Authorization {

    private Authorization() { }

    public static boolean canEdit(Staff editor, long ownerStaffId) {
        return editor != null && (editor.getId() == ownerStaffId || editor.isDoctor());
    }

    public static String deniedMessage(String ownerName) {
        return "Not authorized: only " + ownerName + " (who owns this record) or a Doctor can update it.";
    }
}
