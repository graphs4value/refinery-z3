/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class RefineryZ3Extension {
	private final String z3Version;

	@Inject
	public RefineryZ3Extension(Project project) {
		z3Version = project.getVersion().toString().split("-")[0];
	}

	public String getZ3Version() {
		return z3Version;
	}

	public abstract Property<String> getNameSuffix();

	/**
	 * Commentary for the {@code pedigree} of the bundled Z3 component in the CycloneDX SBOM, describing how this
	 * artifact came to contain its copy of Z3.
	 */
	public abstract Property<String> getPedigreeNotes();
}
