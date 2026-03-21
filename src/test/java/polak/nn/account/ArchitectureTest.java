package polak.nn.account;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "polak.nn", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_api = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure = noClasses().that()
            .resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_api = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_api = noClasses().that()
            .resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule account_should_not_depend_on_exchange_infrastructure = noClasses().that()
            .resideInAPackage("..account..")
            .should().dependOnClassesThat().resideInAPackage("..exchange.infrastructure..");

    @ArchTest
    static final ArchRule exchange_domain_should_not_depend_on_account = noClasses().that()
            .resideInAPackage("..exchange.domain..")
            .should().dependOnClassesThat().resideInAPackage("..account..");

    @ArchTest
    static final ArchRule exchange_infrastructure_should_not_depend_on_account_application = noClasses().that()
            .resideInAPackage("..exchange.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..account.application..");

    @ArchTest
    static final ArchRule exchange_infrastructure_should_not_depend_on_account_infrastructure = noClasses().that()
            .resideInAPackage("..exchange.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..account.infrastructure..");

    @ArchTest
    static final ArchRule exchange_infrastructure_should_not_depend_on_account_api = noClasses().that()
            .resideInAPackage("..exchange.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..account.api..");

    @ArchTest
    static final ArchRule exchange_infrastructure_should_not_depend_on_account_domain_model = noClasses().that()
            .resideInAPackage("..exchange.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..account.domain.model..");

    @ArchTest
    static final ArchRule account_domain_should_not_depend_on_exchange = noClasses().that()
            .resideInAPackage("..account.domain..")
            .should().dependOnClassesThat().resideInAPackage("..exchange..");

    @ArchTest
    static final ArchRule domain_should_be_independent_of_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule account_should_only_access_exchange_via_infrastructure_adapter = noClasses().that()
            .resideInAPackage("..account.domain..")
            .or().resideInAPackage("..account.application..")
            .should().dependOnClassesThat().resideInAPackage("..exchange..");

    @ArchTest
    static final ArchRule account_infrastructure_is_the_only_gate_to_exchange = classes().that()
            .resideInAPackage("..exchange..")
            .should().onlyBeAccessed().byAnyPackage(
                    "..exchange..",
                    "..account.infrastructure.exchange..",
                    "..shared..");
}
