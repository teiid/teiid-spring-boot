package org.teiid.spring.example;
public class Customer {

    int id;
    String name;
    String ssn;

    public Customer(int id, String name, String ssn) {
        this.id = id;
        this.name = name;
        this.ssn= ssn;
    }

    @Override
    public String toString() {
        return "Customer [id=" + id + ", name=" + name + ", ssn=" + ssn + "]";
    }

}