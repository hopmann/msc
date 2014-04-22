package de.hopmann.msc.slave.entity;

import de.hopmann.msc.commons.model.Version;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-22T18:55:03.129+0200")
@StaticMetamodel(PackageInstallerEntity.class)
public class PackageInstallerEntity_ {
	public static volatile SingularAttribute<PackageInstallerEntity, Version> version;
	public static volatile SingularAttribute<PackageInstallerEntity, String> flavor;
	public static volatile SingularAttribute<PackageInstallerEntity, String> architecture;
}
