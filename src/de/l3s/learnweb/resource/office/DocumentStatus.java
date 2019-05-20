package de.l3s.learnweb.resource.office;

public enum DocumentStatus
{
    NOT_FOUND(0),
    BEING_EDITED(1),
    READY_FOR_SAVING(2),
    SAVING_ERROR(3),
    NO_CHANGES(4),
    SAVED_AND_BEING_EDITED(6),
    FORCE_SAVING_ERROR(7);

    private long status;

    DocumentStatus(long status)
    {
        this.status = status;
    }

    public long getStatus()
    {
        return status;
    }

}
