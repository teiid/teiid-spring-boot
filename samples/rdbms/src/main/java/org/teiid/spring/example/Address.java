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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.teiid.spring.annotations.SelectQuery;

@Entity
@SelectQuery("SELECT id, street, zip, customer_id FROM mydb.address")
public class Address {

    // @TableGenerator(name = "address",
    // table = "id_generator",
    // pkColumnName = "idkey",
    // valueColumnName = "idvalue",
    // pkColumnValue = "address",
    // allocationSize = 1)
    // @GeneratedValue(strategy = GenerationType.TABLE, generator = "address")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_generator")
    @SequenceGenerator(name = "address_generator", sequenceName = "mydb.addr_seq")
    @Id
    private Long id;

    private String street;

    private String zip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    @Override
    public String toString() {
        return "Address [id=" + id + ", street=" + street + ", zip=" + zip + "]";
    }
}
