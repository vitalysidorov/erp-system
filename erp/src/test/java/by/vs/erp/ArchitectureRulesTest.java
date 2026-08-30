package by.vs.erp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void init() {
        importedClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("by.vs.erp");
    }

    @Test
    @DisplayName("Layered Architecture: Соблюдение классических правил изоляции слоев")
    void shouldAdhereToLayeredArchitecture() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyBeAccessed().byAnyPackage("..controller..")
                .check(importedClasses);

        classes()
                .that().resideInAPackage("..repository..")
                .should().onlyBeAccessed().byAnyPackage("..service..", "..controller..", "..listener..",
                        "..repository..", "..config..", "..security..")
                .check(importedClasses);

        noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(importedClasses);

        noClasses()
                .that().resideInAPackage("..mapper..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Repository Isolation: Репозитории не должны зависеть от веб-контроллеров или сервисов")
    void repositoriesShouldBeIndependent() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Naming Conventions: Классы в пакете controller должны иметь суффикс Controller")
    void controllersShouldBeNamedCorrectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .should().haveSimpleNameEndingWith("Controller")
                .orShould().haveSimpleNameEndingWith("Advice"); // Для GlobalExceptionHandler

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Naming Conventions: Классы в пакете service должны иметь суффикс Service")
    void servicesShouldBeNamedCorrectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("Publisher")
                .orShould().haveSimpleNameEndingWith("Listener")
                .orShould().haveSimpleNameEndingWith("Executor");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Naming Conventions: Интерфейсы репозиториев должны иметь суффикс Repository")
    void repositoriesShouldBeNamedCorrectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .should().haveSimpleNameEndingWith("Repository");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Spring Validation: Аннотация @NotBlank должна применяться только к строковым полям")
    void dtoValidationShouldBeCorrectForNumericTypes() {
        ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields()
                .that().areAnnotatedWith(jakarta.validation.constraints.NotBlank.class)
                .should().haveRawType(String.class)
                .as("Аннотация @NotBlank может применяться исключительно к полям типа String");

        rule.check(importedClasses);
    }
}
