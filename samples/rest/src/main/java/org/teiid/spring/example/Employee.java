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
import javax.persistence.Id;

import org.teiid.spring.annotations.JsonTable;

@JsonTable(endpoint = "webCallBean", source = "rest", root = "/data")
@Entity
public class Employee {

    @Id
    private Integer id;
    private String employee_name;

    public Employee() {
    }

    public Employee(int id, String name) {
        this.id = id;
        this.employee_name = name;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.employee_name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String quote) {
        this.employee_name = quote;
    }

    @Override
    public String toString() {
        return "Value{" + "id=" + id + ", Name='" + employee_name + '\'' + '}';
    }
}
