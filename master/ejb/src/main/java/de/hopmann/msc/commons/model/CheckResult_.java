package de.hopmann.msc.commons.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2014-04-22T16:52:15.056+0200")
@StaticMetamodel(CheckResult.class)
public class CheckResult_ {
	public static volatile SingularAttribute<CheckResult, Integer> errorCount;
	public static volatile SingularAttribute<CheckResult, Integer> noteCount;
	public static volatile SingularAttribute<CheckResult, Integer> warningCount;
	public static volatile SingularAttribute<CheckResult, Integer> skippedCount;
}
