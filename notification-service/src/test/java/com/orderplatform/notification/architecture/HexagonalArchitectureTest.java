package com.orderplatform.notification.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Notification Service 헥사고날 아키텍처 의존성 규칙 검증
 */
@AnalyzeClasses(packages = "com.orderplatform.notification", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule 애플리케이션서비스는_어댑터에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..application.service..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule 인바운드어댑터는_아웃바운드어댑터에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.out..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 아웃바운드어댑터는_인바운드포트에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..adapter.out..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.port.in..")
                    .allowEmptyShould(true);
}
