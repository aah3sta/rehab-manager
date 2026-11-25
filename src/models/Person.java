package models;

public abstract class Person {
    private long id;
    private String fullName;
    private String contact;

    public Person(long id, String fullName, String contact) {
        this.id = id;
        setFullName(fullName);
        setContact(contact);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        String name = fullName.trim();
        if (name.length() > 100) {
            throw new IllegalArgumentException("Name cannot be longer than 100 characters.");
        }
        if (!name.matches("[A-Za-z][A-Za-z .'-]*")) {
            throw new IllegalArgumentException("Name may only contain letters, spaces, apostrophes, dots and hyphens.");
        }
        this.fullName = name;
    }

    public String getContact() { return contact; }
    public void setContact(String contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contact cannot be empty.");
        }
        // Allow people to type "0712 345 678" or "0712-345-678", store digits only
        String cleaned = contact.trim().replaceAll("[\\s-]", "");
        if (!cleaned.matches("\\+?\\d{10,15}")) {
            throw new IllegalArgumentException("Contact must be 10 to 15 digits (an optional leading + is allowed).");
        }
        this.contact = cleaned;
    }

    public abstract String getDetails();

    @Override
    public String toString() { return getDetails(); }
}
