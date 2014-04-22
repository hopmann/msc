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
package de.hopmann.repositories.subversion;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import de.hopmann.msc.commons.exception.PackageResolvingException;
import de.hopmann.msc.commons.model.DependencyInfo;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.PackageSource;
import de.hopmann.msc.commons.model.PackageSource.PackageAccessor;
import de.hopmann.msc.commons.model.PackageSource.PathHolder;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.util.ControlFile;
import de.hopmann.msc.commons.util.FutureHelper;
import de.hopmann.repositories.commons.entity.DependencyEntity;
import de.hopmann.repositories.subversion.entity.SVNPackageEntity;

/**
 * Provides access to package sources on a SVN repository. 
 *
 */
class SubversionPackageAccessor implements PackageAccessor {
	private SVNClientManager svnClientManager;
	private Map<DependencyType, Set<DependencyInfo>> dependenciesMapResolved;
	private SVNURL svnUrl;
	private PackageSource packageSource;
	private SVNUpdateClient svnUpdateClient;
	private Path workingDirectory;
	private Boolean checkedOut;
	// private Long revisionNumber;
	private SVNRevision currentWCRevision;
	private SVNRevision targetedRevisionNumber;
	private SVNPackageEntity packageEntityResolved;

	public SubversionPackageAccessor(String svnUrl, Long revisionNumber,
			Path workingDirectory, PackageSource source) {
		this.packageSource = source;
		this.workingDirectory = workingDirectory;
		try {
			this.svnUrl = SVNURL.parseURIEncoded(svnUrl);
		} catch (SVNException e) {
			throw new PackageResolvingException("Error in svn url", e);
		}

		FSRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		DAVRepositoryFactory.setup();
		svnClientManager = SVNClientManager.newInstance();
		svnUpdateClient = svnClientManager.getUpdateClient();

		setToRevision(revisionNumber);
	}

	private SVNPackageEntity getPackageEntity() throws IOException {
		if (packageEntityResolved == null) {
			try {
				updateTargetDirectory();
			} catch (SVNException e) {
				throw new PackageResolvingException(e);
			}

			ControlFile<SVNPackageDescriptionRecord> controlFile = new ControlFile<>(
					SVNPackageDescriptionRecord.class, new FileReader(
							workingDirectory.resolve("DESCRIPTION").toFile()));

			try {
				SVNPackageDescriptionRecord record = controlFile.readRecord();
				if (record == null || record.getPackageName() == null) {
					throw new PackageResolvingException(
							"Could not read DESCRIPTION file");
				}

				this.packageEntityResolved = record.buildEntity();
			} catch (Exception e) {
				throw new PackageResolvingException(e);
			} finally {
				controlFile.close();
			}
		}

		return packageEntityResolved;
	}

	private void getLatestRevision() {
		// TODO
		SVNRepository repository = null;
		try {
			repository = SVNRepositoryFactory.create(svnUrl);
			SVNDirEntry entry = repository.info(".", -1);
			System.out.println("Latest Rev: " + entry.getRevision());
		} catch (SVNException e) {
			throw new PackageResolvingException();
		}
	}

	public void setToRevision(Long targetRevisionNumber) {
		if (targetRevisionNumber == null) {
			this.targetedRevisionNumber = SVNRevision.HEAD;
		} else {
			this.targetedRevisionNumber = SVNRevision
					.create(targetRevisionNumber);
		}

		this.packageEntityResolved = null;
		this.dependenciesMapResolved = null;
	}

	@Override
	public Future<PathHolder> acquireSource() {
		try {
			updateTargetDirectory();
			return FutureHelper.createImmediateFuture(PathHolder
					.ofPath(workingDirectory));
		} catch (SVNException e) {
			throw new PackageResolvingException(e);
		}
	}

	private void checkoutWorkingCopy() throws SVNException {
		svnUpdateClient.setIgnoreExternals(false);
		long newRev = svnUpdateClient.doCheckout(svnUrl,
				workingDirectory.toFile(), SVNRevision.HEAD,
				targetedRevisionNumber, SVNDepth.INFINITY, true);
		setCurrentRevision(newRev);
	}

	private void setCurrentRevision(long rev) {
		currentWCRevision = SVNRevision.create(rev);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SubversionPackageAccessor)) {
			return false;
		}
		SubversionPackageAccessor other = (SubversionPackageAccessor) obj;
		return new EqualsBuilder()
				.append(targetedRevisionNumber, other.targetedRevisionNumber)
				.append(svnUrl, other.svnUrl).isEquals();
	}

	@Override
	public Map<DependencyType, Set<DependencyInfo>> getDeclaredDependenciesMap()
			throws PackageResolvingException {
		if (dependenciesMapResolved == null) {
			dependenciesMapResolved = new HashMap<>();
			List<DependencyEntity> declaredDependencies;
			try {
				declaredDependencies = getPackageEntity().getDependencies();
			} catch (IOException e) {
				throw new PackageResolvingException(e);
			}

			for (DependencyEntity dependencyEntity : declaredDependencies) {
				DependencyInfo dependencyInfo = new DependencyInfo(
						dependencyEntity.getDependingPackage(), null);// TODO
																		// version
																		// constr

				Set<DependencyInfo> dependencies = dependenciesMapResolved
						.get(dependencyEntity.getType());
				if (dependencies == null) {
					dependencies = new HashSet<DependencyInfo>();
					dependenciesMapResolved.put(dependencyEntity.getType(),
							dependencies);
				}
				dependencies.add(dependencyInfo);
			}
		}
		return dependenciesMapResolved;
	}

	@Override
	public String getOSType() throws PackageResolvingException {
		try {
			return getPackageEntity().getOsType();
		} catch (IOException e) {
			throw new PackageResolvingException(e);
		}
	};

	@Override
	public String getPackageName() throws PackageResolvingException {
		try {
			return getPackageEntity().getName();
		} catch (IOException e) {
			throw new PackageResolvingException(e);
		}
	}

	@Override
	public PackageSource getPackageSource() {
		return packageSource;
	}

	@Override
	public Version getPackageVersion() throws PackageResolvingException {
		try {
			return getPackageEntity().getVersion();
		} catch (IOException e) {
			throw new PackageResolvingException(e);
		}
	}

	@Override
	public String getSourceLocation() {
		return svnUrl.toString();
	}

	@Override
	public Version getSourceVersion() throws PackageResolvingException {
		try {
			updateTargetDirectory();
		} catch (SVNException e) {
			throw new PackageResolvingException(e);
		}
		return Version.fromVersionNumber(currentWCRevision.getNumber());
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(targetedRevisionNumber)
				.append(svnUrl).toHashCode();
	}

	@Override
	public boolean isAvailable() {
		return true;// XXX
	}

	private boolean isCheckedOut() {
		if (checkedOut == null
				&& Files.notExists(workingDirectory.resolve(".svn"))) {
			checkedOut = false;
		} else {
			checkedOut = true;
		}

		return checkedOut;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("svnUrl", svnUrl)
				.append("targetedRevision", targetedRevisionNumber)
				.append("currentWCRevision", currentWCRevision).toString();
	}

	private void updateTargetDirectory() throws SVNException {
		if (currentWCRevision != null && targetedRevisionNumber != null) {
			if (currentWCRevision.getNumber() == targetedRevisionNumber
					.getNumber()) {
				// WC is on right revision
				return;
			}
		}

		if (!isCheckedOut()) {
			checkoutWorkingCopy();
		} else {
			updateWorkingCopy();
		}

		if (targetedRevisionNumber.getNumber() == -1) {
			targetedRevisionNumber = currentWCRevision;
		}
	}

	private void updateWorkingCopy() throws SVNException {
		svnUpdateClient.setIgnoreExternals(false);
		long newRev = svnUpdateClient.doUpdate(workingDirectory.toFile(),
				targetedRevisionNumber, SVNDepth.INFINITY, true, true);
		setCurrentRevision(newRev);
	}

}