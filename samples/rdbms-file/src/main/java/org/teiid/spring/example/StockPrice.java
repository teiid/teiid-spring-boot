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

import org.teiid.spring.annotations.TextTable;

/**
 * This entity is showing the data from a text file. Needs {@link TextTable}
 * annotation.
 */
@Entity
@TextTable(file = "marketdata-price.txt") /* Also see property: spring.teiid.file.parent-directory=src/main/resources */
public class StockPrice {

    @Id
    String symbol;

    double price;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "StockPrice [symbol=" + symbol + ", price=" + price + "]";
    }
}
