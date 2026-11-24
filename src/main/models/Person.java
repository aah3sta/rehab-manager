package main.models;

public class Person {
    private long id;
    private String fullName;
    private String contact;

    public Person() {}

    public Person(long id, String fullName, String contact) {
        this.id = id;
        this.fullName = fullName;
        this.contact = contact;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
