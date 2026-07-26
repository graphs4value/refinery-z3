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
}
