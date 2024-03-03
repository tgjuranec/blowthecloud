package com.conform.blowcloud;

public class BackupStates {
    public enum state{
        BACKUP_APPROVED,
        BACKUP_APPROVED_DISSECT_DIR,
        BACKUP_APPROVED_MEDIASTORE,
        BACKUP_APPROVED_SAF,
        NO_DESTINATION,
        TWO_DESTINATIONS_POSSIBLE,
        DESTINATION_SPACE_NOT_SUFFICIENT,
        NO_FILES_FOR_BACKUP,
        SUSPICIOUS_SYSTEM_STATE,
        BACKUP_CANCELED,
    }
    public state current;
}
