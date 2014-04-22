package de.hopmann.msc.master.ejb.entity;

import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.master.ejb.entity.PackageResult.PackageResultType;
import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-22T16:52:15.196+0200")
@StaticMetamodel(PackageResult.class)
public class PackageResult_ {
	public static volatile SingularAttribute<PackageResult, CheckResult> checkResult;
	public static volatile ListAttribute<PackageResult, PackageResult> dependencies;
	public static volatile SingularAttribute<PackageResult, Long> id;
	public static volatile SingularAttribute<PackageResult, InstallationResult> installationResult;
	public static volatile SingularAttribute<PackageResult, PackageResultType> installationType;
	public static volatile SingularAttribute<PackageResult, PackageSource> packageSource;
	public static volatile SingularAttribute<PackageResult, Long> revision;
	public static volatile SingularAttribute<PackageResult, String> sourceLocation;
	public static volatile SingularAttribute<PackageResult, String> sourceType;
	public static volatile SingularAttribute<PackageResult, Version> sourceVersion;
}
