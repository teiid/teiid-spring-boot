--
-- Copyright (C) 2016 Red Hat, Inc.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
INSERT INTO Category (id,name) VALUES (1, 'Cats');
INSERT INTO Category (id,name) VALUES (2, 'Dogs');
INSERT INTO Category (id,name) VALUES (3, 'Birds');

INSERT INTO pet (id, name, category_id, status) VALUES (1,'rocky', 2,  'sold');
INSERT INTO pet (id, name, category_id, status) VALUES (2,'nikky', 1,  'available');
INSERT INTO pet (id, name, category_id, status) VALUES (3,'micky', 3,  'pending');

INSERT INTO Tag (id, name) VALUES (1, 'cute');
INSERT INTO Tag (id, name) VALUES (2, 'puppy');
INSERT INTO Tag (id, name) VALUES (3, 'heavy');
INSERT INTO Tag (id, name) VALUES (4, 'colorful');

INSERT INTO PetTag (id, pet_id) VALUES (1, 1);
INSERT INTO PetTag (id, pet_id) VALUES (2, 1);
INSERT INTO PetTag (id, pet_id) VALUES (3, 2);
INSERT INTO PetTag (id, pet_id) VALUES (1, 2);

commit;

