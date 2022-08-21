package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CallbackData {
    // private List<Action> actions;
    /**
     * Defines the link to the file with the document editing data used to track and display the document changes history.
     * The link is present when the status value is equal to 2 or 3 only. The file must be saved and its address
     * must be sent as changesUrl parameter using the setHistoryData method to show the changes corresponding
     * to the specific document version.
     */
    @SerializedName("changesurl")
    private String changesUrl;
    /**
     * Defines the type of initiator when the force saving request is performed. Can have the following values:
     *     0 - the force saving request is performed to the command service,
     *     1 - the force saving request is performed each time the saving is done (e.g. the Save button is clicked),
     *         which is only available when the forcesave option is set to true.
     *     2 - the force saving request is performed by timer with the settings from the server config.
     * The type is present when the status value is equal to 6 or 7 only.
     */
    @SerializedName("forcesavetype")
    private int forceSaveType;
    /**
     * Defines the object with the document changes history. The object is present when the status value is equal
     * to 2 or 3 only.It contains the object serverVersion and changes, which must be sent as properties serverVersion
     * and changes of the object sent as the argument to the refreshHistory method.
     */
    private History history;
    /**
     * Defines the edited document identifier.
     */
    private String key;
    /**
     * Defines the status of the document. Can have the following values:
     *     1 - document is being edited,
     *     2 - document is ready for saving,
     *     3 - document saving error has occurred,
     *     4 - document is closed with no changes,
     *     6 - document is being edited, but the current document state is saved,
     *     7 - error has occurred while force saving the document.
     */
    private int status;
    /**
     * Defines the link to the edited document to be saved with the document storage service.
     * The link is present when the status value is equal to 2 or 3 only.
     */
    private String url;
    /**
     * Defines the custom information sent to the command service in case it was present in the request.
     */
    @SerializedName("userdata")
    private String userData;
    /**
     * Defines the list of the identifiers of the users who opened the document for editing;
     * when the document has been changed the users will return the identifier of the user who
     * was the last to edit the document (for status 2 and status 6 replies).
     */
    private List<Integer> users;

    public String getChangesUrl() {
        return changesUrl;
    }

    public void setChangesUrl(final String changesUrl) {
        this.changesUrl = changesUrl;
    }

    public int getForceSaveType() {
        return forceSaveType;
    }

    public void setForceSaveType(final int forceSaveType) {
        this.forceSaveType = forceSaveType;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(final History history) {
        this.history = history;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(final String userData) {
        this.userData = userData;
    }

    public List<Integer> getUsers() {
        return users;
    }

    public void setUsers(final List<Integer> users) {
        this.users = users;
    }
}
