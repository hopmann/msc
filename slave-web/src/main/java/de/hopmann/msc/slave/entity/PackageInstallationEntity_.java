package de.hopmann.msc.slave.entity;

import de.hopmann.msc.commons.model.Version;
import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-22T18:55:03.114+0200")
@StaticMetamodel(PackageInstallationEntity.class)
public class PackageInstallationEntity_ {
	public static volatile SingularAttribute<PackageInstallationEntity, Long> id;
	public static volatile SingularAttribute<PackageInstallationEntity, String> packageName;
	public static volatile SingularAttribute<PackageInstallationEntity, Version> packageVersion;
	public static volatile SingularAttribute<PackageInstallationEntity, String> sourceType;
	public static volatile SingularAttribute<PackageInstallationEntity, String> sourceLocation;
	public static volatile SingularAttribute<PackageInstallationEntity, Version> sourceVersion;
	public static volatile SingularAttribute<PackageInstallationEntity, Boolean> isFailed;
	public static volatile SingularAttribute<PackageInstallationEntity, PackageInstallerEntity> packageInstaller;
	public static volatile SetAttribute<PackageInstallationEntity, PackageInstallationEntity> actualDependencies;
}
