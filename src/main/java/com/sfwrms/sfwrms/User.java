package com.sfwrms.sfwrms;


public abstract class User {

    protected int    id;
    protected String name;
    protected String email;

    public abstract String getRole();
    public abstract boolean authenticate(String email, String passwordHash);

    public int    getId()    { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
}
