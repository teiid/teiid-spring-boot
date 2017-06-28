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
package org.teiid.spring.views;

import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.spring.annotations.DeleteQuery;
import org.teiid.spring.annotations.InsertQuery;
import org.teiid.spring.annotations.SelectQuery;
import org.teiid.spring.annotations.UpdateQuery;

public class SimpleView extends ViewBuilder<SelectQuery> {

    @Override
    void onFinish(Table view, MetadataFactory mf, Class<?> entityClazz, SelectQuery annotation) {
        view.setSelectTransformation(annotation.value());
        
        InsertQuery insertAnnotation = entityClazz.getAnnotation(InsertQuery.class);
        if (insertAnnotation != null) {
            view.setInsertPlan(insertAnnotation.value());
        }
        
        UpdateQuery updateAnnotation = entityClazz.getAnnotation(UpdateQuery.class);
        if (updateAnnotation != null) {
            view.setUpdatePlan(updateAnnotation.value());
        }

        DeleteQuery deleteAnnotation = entityClazz.getAnnotation(DeleteQuery.class);
        if (deleteAnnotation != null) {
            view.setDeletePlan(deleteAnnotation.value());
        }        
    }
}
