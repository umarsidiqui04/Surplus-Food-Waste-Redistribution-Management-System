package com.sfwrms.sfwrms;

import java.time.LocalDateTime;


public class NotificationModel {
    private int           notificationId;
    private int           recipientId;
    private int           donationId;
    private String        message;
    private String        recipientType;
    private LocalDateTime sentAt;
    private boolean       isRead;

    public NotificationModel() {}

    public int           getNotificationId()          { return notificationId; }
    public void          setNotificationId(int v)     { notificationId = v; }
    public int           getRecipientId()             { return recipientId; }
    public void          setRecipientId(int v)        { recipientId = v; }
    public int           getDonationId()              { return donationId; }
    public void          setDonationId(int v)         { donationId = v; }
    public String        getMessage()                 { return message; }
    public void          setMessage(String v)         { message = v; }
    public String        getRecipientType()           { return recipientType; }
    public void          setRecipientType(String v)   { recipientType = v; }
    public LocalDateTime getSentAt()                  { return sentAt; }
    public void          setSentAt(LocalDateTime v)   { sentAt = v; }
    public boolean       isRead()                     { return isRead; }
    public void          setRead(boolean v)           { isRead = v; }
}
