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

import org.teiid.spring.annotations.SelectQuery;

@Entity
@SelectQuery("SELECT IDKEY, IDVALUE FROM mydb.IDGENERATOR")
public class IdGenerator {
    @Id
    private String idkey;
    private Long idvalue;

    public String getIdkey() {
        return idkey;
    }

    public void setIdkey(String idkey) {
        this.idkey = idkey;
    }

    public Long getIdvalue() {
        return idvalue;
    }

    public void setIdvalue(Long idvalue) {
        this.idvalue = idvalue;
    }
}
