package org.springaicommunity.bench.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture rules for agent-bench-core. Enforces module layering and package
 * dependencies.
 */
@AnalyzeClasses(packages = "org.springaicommunity.bench.core", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule core_does_not_depend_on_agents_module = noClasses()
		.that()
		.resideInAPackage("org.springaicommunity.bench.core..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("org.springaicommunity.bench.agents..")
		.because("core must not depend on the agents module — agents depends on core, not the reverse");

	@ArchTest
	static final ArchRule result_package_does_not_depend_on_cli = noClasses()
		.that()
		.resideInAPackage("..result..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..cli..")
		.because("result records are pure data — they must not depend on CLI infrastructure");

	@ArchTest
	static final ArchRule benchmark_package_does_not_depend_on_cli = noClasses()
		.that()
		.resideInAPackage("..benchmark..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..cli..")
		.because("benchmark discovery/loading must not depend on CLI infrastructure");

	@ArchTest
	static final ArchRule no_circular_dependencies_between_packages = slices()
		.matching("org.springaicommunity.bench.core.(*)..")
		.should()
		.beFreeOfCycles()
		.because("packages within core must not have circular dependencies");

}
