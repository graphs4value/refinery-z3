/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	alias(libs.plugins.versions)
}

val mavenRepositoryDir = layout.buildDirectory.map { it.dir("repo") }

val cleanMavenRepository = tasks.register<Delete>("cleanMavenRepository") {
	delete(mavenRepositoryDir)
	description = "Clean Maven repository output files"
}

val mavenRepository = tasks.register<Task>("mavenRepository") {
	dependsOn(cleanMavenRepository)
	description = "Build Maven repository output"
}

gradle.projectsEvaluated {
	mavenRepository.configure {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(MavenPublishPlugin::class)) {
				val publishTask = subproject.tasks.named("publishMavenJavaPublicationToFileRepository")
				publishTask.configure {
					mustRunAfter(cleanMavenRepository)
				}
				dependsOn(publishTask)
			}
		}
	}
}
