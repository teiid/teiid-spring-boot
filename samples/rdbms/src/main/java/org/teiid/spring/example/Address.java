package org.teiid.spring.example;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.teiid.spring.annotations.SelectQuery;

@Entity
@SelectQuery("SELECT id, street, customer_id FROM customerDS.address")
public class Address {

	@Id
	long id;
	
	String street;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	@Override
	public String toString() {
		return "Address [id=" + id + ", street=" + street + "]";
	}
}
