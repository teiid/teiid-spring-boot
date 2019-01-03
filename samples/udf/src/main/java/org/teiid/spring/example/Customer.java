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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.teiid.spring.annotations.SelectQuery;

@Entity
@SelectQuery("SELECT id, addSalutation(name), ssn FROM mydb.customer")
public class Customer {
    @Id
    Long id;

    @Column
    String name;

    @Column
    String ssn;

    public Customer() {
    }

    public Customer(Long id, String name, String ssn) {
        this.id = id;
        this.name = name;
        this.ssn = ssn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    @Override
    public String toString() {
        return "Customer [id=" + id + ", name=" + name + ", ssn=" + ssn + "]";
    }
}
