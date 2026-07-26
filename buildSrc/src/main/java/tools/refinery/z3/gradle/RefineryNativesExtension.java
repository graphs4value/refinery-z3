/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class RefineryNativesExtension {
	public abstract Property<String> getClassifier();

	public abstract ListProperty<String> getIncludePatterns();

	public abstract ListProperty<String> getExcludePatterns();
}
