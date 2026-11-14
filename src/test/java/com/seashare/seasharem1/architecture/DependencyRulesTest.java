package com.seashare.seasharem1.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Regla de dependencias de Clean Architecture: las capas internas no conocen a las externas. */
@AnalyzeClasses(
    packages = "com.seashare.seasharem1",
    importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyRulesTest {

  @ArchTest
  static final ArchRule domainDoesNotDependOnFrameworksOrOuterLayers =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "..application..",
              "..infrastructure..",
              "..configuration..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule applicationDoesNotDependOnFrameworksOrInfrastructure =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "..infrastructure..",
              "..configuration..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule domainAndApplicationDoNotUseJsonOrBeanValidation =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..", "..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.fasterxml.jackson..", "tools.jackson..", "jakarta.validation..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule infrastructureDoesNotDependOnConfiguration =
      noClasses()
          .that()
          .resideInAPackage("..infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..configuration..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule webAdaptersDoNotKnowPersistenceAdapters =
      noClasses()
          .that()
          .resideInAPackage("..infrastructure.adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infrastructure.adapter.out..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule persistenceAdaptersDoNotKnowWebAdapters =
      noClasses()
          .that()
          .resideInAPackage("..infrastructure.adapter.out..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infrastructure.adapter.in..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule useCasesAreReachedThroughInputPorts =
      noClasses()
          .that()
          .resideInAPackage("..infrastructure.adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..application.usecase..")
          .allowEmptyShould(true);
}
