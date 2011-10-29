package net.wohlfart.changerequest.entities;

public enum Priority {
    HIGHEST("priority.highest"), HIGH("priority.high"), NORMAL("priority.normal"), LOW("priority.low") /*
                                                                                                        * ,
                                                                                                        * LOWEST
                                                                                                        * (
                                                                                                        * "priority.lowest"
                                                                                                        * )
                                                                                                        */;

    // the message code must be configured in some message properties
    private final String msgCode;

    private Priority(final String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgCode() {
        return msgCode;
    }
}
