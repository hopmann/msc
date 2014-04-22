/**
 * Copyright (C) 2014 Holger Hopmann (h.hopmann@uni-muenster.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hopmann.repositories.commons.entity;

import de.hopmann.msc.commons.model.DependencyType;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-02T10:42:49.769+0200")
@StaticMetamodel(DependencyEntity.class)
public class DependencyEntity_ {
	public static volatile SingularAttribute<DependencyEntity, Integer> id;
	public static volatile SingularAttribute<DependencyEntity, String> dependingPackageName;
	public static volatile SingularAttribute<DependencyEntity, VersionConstraintEntity> versionConstraint;
	public static volatile SingularAttribute<DependencyEntity, DependencyType> type;
}
