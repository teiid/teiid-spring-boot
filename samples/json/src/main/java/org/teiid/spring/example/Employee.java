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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.teiid.hibernate.types.LongArrayType;
import org.teiid.spring.annotations.JsonTable;

@Entity
@Table(name = "employee")
@JsonTable(endpoint = "employee.json", source = "file")
@TypeDefs({ @TypeDef(name = "long-array", typeClass = LongArrayType.class) })
public class Employee {

    @Id
    private int id;
    private String name;
    private int age;
    private BigDecimal salary;
    private String designation;

    @Embedded
    private Address address;

    @OneToMany(mappedBy = "employee", fetch = FetchType.EAGER)
    private Set<Skills> skills = new HashSet<>();

    @Type(type = "long-array")
    @Column(name = "phonenumbers", columnDefinition = "long[]")
    private long[] phoneNumbers;

    public int getId() {
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public long[] getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(long[] phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public Set<Skills> getSkills() {
        return this.skills;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n----- Employee Information-----\n");
        sb.append("ID: " + getId() + "\n");
        sb.append("Name: " + getName() + "\n");
        sb.append("Age: " + getAge() + "\n");
        sb.append("Salary: $" + getSalary() + "\n");
        sb.append("Designation: " + getDesignation() + "\n");
        sb.append("Phone Numbers: " + Arrays.toString(getPhoneNumbers()) + "\n");
        sb.append("Address: " + getAddress() + "\n");
        sb.append("Skills: " + getSkills() + "\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Employee other = (Employee) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (age != other.age) {
            return false;
        }
        if (designation == null) {
            if (other.designation != null) {
                return false;
            }
        } else if (!designation.equals(other.designation)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (!Arrays.equals(phoneNumbers, other.phoneNumbers)) {
            return false;
        }
        if (salary == null) {
            if (other.salary != null) {
                return false;
            }
        } else if (!salary.equals(other.salary)) {
            return false;
        }
        return true;
    }
}
