package de.hopmann.msc.master.ejb.entity;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-22T16:52:15.294+0200")
@StaticMetamodel(PackageSource.class)
public class PackageSource_ {
	public static volatile SingularAttribute<PackageSource, Long> id;
	public static volatile SingularAttribute<PackageSource, String> sourceType;
	public static volatile SingularAttribute<PackageSource, String> sourceLocation;
	public static volatile SingularAttribute<PackageSource, Long> maxRevisionNumber;
	public static volatile SingularAttribute<PackageSource, String> packageName;
	public static volatile SingularAttribute<PackageSource, PackageContext> repositoryEntity;
	public static volatile ListAttribute<PackageSource, PackageResult> packageResults;
}
