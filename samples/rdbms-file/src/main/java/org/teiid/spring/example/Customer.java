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

@Entity(name="CUSTOMER")
public class Customer {
    
    @Id
    @Column(name="SSN")
    private String ssn;
    
    @Column(name="FIRSTNAME")
    private String firstName;
    
    @Column(name="LASTNAME")
    private String lastName;
    
    @Column(name="ST_ADDRESS")
    private String streetAddress;
    
    @Column(name="APT_NUMBER")
    private String apartmentNumber;
    
    @Column(name="CITY")
    private String city;
    
    @Column(name="STATE")
    private String state;
    
    @Column(name="ZIPCODE")
    private String zipCode;

    @Column(name="PHONE")
    private String phone;
    
    public Customer() {
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "Customer [ssn=" + ssn + ", firstName=" + firstName + ", lastName=" + lastName + ", streetAddress="
                + streetAddress + ", apartmentNumber=" + apartmentNumber + ", city=" + city + ", state=" + state
                + ", zipCode=" + zipCode + ", phone=" + phone + "]";
    }
}