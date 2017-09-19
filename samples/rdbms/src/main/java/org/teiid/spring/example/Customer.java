/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teiid.spring.example;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.teiid.spring.annotations.DeleteQuery;
import org.teiid.spring.annotations.InsertQuery;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.UpdateQuery;

@Entity
@Table(name="all_customers")
@SelectQuery("SELECT id, name, ssn FROM accountsDS.Customers UNION ALL SELECT id, name, ssn FROM customerDS.Customer")

@InsertQuery("FOR EACH ROW \n"+
             "BEGIN ATOMIC \n" +
		     "INSERT INTO customerDS.Customer(name, ssn) values (NEW.name, NEW.ssn);\n" +
             "END")

@UpdateQuery("FOR EACH ROW \n"+
        	 "BEGIN ATOMIC \n" +
			 "UPDATE customerDS.Customer SET name=NEW.name, ssn=NEW.ssn WHERE id = OLD.id;\n"+
        	 "END")

@DeleteQuery("FOR EACH ROW \n"+
   	 		"BEGIN ATOMIC \n" +
			"DELETE FROM customerDS.Customer where id = OLD.id;\n"+
   	 		"END")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    
    @Column
    String name;
    
    @Column
    String ssn;
    
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="customer_id")
    private List<Address> address;

    public Customer() {
    }
    
    public Customer(int id, String name, String ssn) {
        this.id = id;
        this.name = name;
        this.ssn= ssn;
    }

    @Override
    public String toString() {
        return "Customer [id=" + id + ", name=" + name + ", ssn=" + ssn + ", address=" + address + "]";
    }
    
    public long getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSsn() {
        return ssn;
    }
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

	public List<Address> getAddress() {
		return address;
	}

	public void setAddress(List<Address> address) {
		this.address = address;
	}
}