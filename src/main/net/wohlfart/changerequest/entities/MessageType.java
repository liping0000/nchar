package net.wohlfart.changerequest.entities;

public enum MessageType {
    FORWARD_DRAFT("changerequest.messagetype.forward_draft", "simpleMessage", "forward.png"), 
    FORWARD("changerequest.messagetype.forward", "simpleMessage",  "forward.png"),

    REVIEW("changerequest.messagetype.review", "simpleMessage", "request.png"), 
    REVIEW_REPLY("changerequest.messagetype.review_reply", "simpleMessage", "respond.png"),

    HANDLE("changerequest.messagetype.handle", "simpleMessage", "forward.png"), 
    HANDLE_REPLY("changerequest.messagetype.handle_reply", "simpleMessage", "respond.png"),

    IMPLEMENT("changerequest.messagetype.implement", "simpleMessage", "forward.png"), 
    IMPLEMENT_REPLY("changerequest.messagetype.implement_reply", "simpleMessage", "respond.png"),

    EXPERT_REVIEW("changerequest.messagetype.expert_review", "simpleMessage", "respond.png"), 
    INITIAL_SAVE("changerequest.messagetype.initial_save", "redMessage", "save.png"),
    // PROCESS("changerequest.messagetype.process", "simpleMessage"),
    REALIZE("changerequest.messagetype.realize", "redMessage", "thumb_up.png"), 
    SUBMIT("changerequest.messagetype.submit", "redMessage", "page.png"), 
    // the so called "Quereinstieg"
    SUBMIT2("changerequest.messagetype.submit2", "redMessage", "page.png"), 
    FINISH("changerequest.messagetype.finish", "redMessage", "implement.png"), 
    TRANSFER("changerequest.messagetype.transfer", "redMessage", "forward.png"), 
    CANCEL("changerequest.messagetype.cancel", "redMessage", "thumb_down.png"),
    // soft delete a draft
    DISCARD("changerequest.messagetype.cancel", "redMessage", "request.png"),
    // take is used by a member of the TQM group taking the task from the group
    TAKE("changerequest.messagetype.take", "simpleMessage", "implement.png"),
    // "process" was renamed to "assign" for the TQM action after taking the
    // task
    ASSIGN("changerequest.messagetype.assign", "simpleMessage", "request.png"),
    //
    PROCESS("changerequest.messagetype.process", "simpleMessage", "forward.png"),

    ;

    // the message code must be configured in some message properties
    private final String msgCode;
    private final String styleClass;
    private final String image;

    private MessageType(final String msgCode, final String styleClass, final String image) {
        this.msgCode = msgCode;
        this.styleClass = styleClass;
        this.image = image;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getImage() {
        return image;
    }
}
