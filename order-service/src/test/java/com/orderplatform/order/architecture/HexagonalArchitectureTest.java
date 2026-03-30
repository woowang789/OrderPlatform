package com.orderplatform.order.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Order Service 헥사고날 아키텍처 의존성 규칙 검증
 */
@AnalyzeClasses(packages = "com.orderplatform.order", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule 도메인은_애플리케이션서비스와_어댑터에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application.service..", "..adapter..");

    @ArchTest
    static final ArchRule 인바운드포트는_도메인에만_의존한다 =
            classes().that().resideInAPackage("..application.port.in..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("..domain..", "..application.port.in..", "java..", "jakarta..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 웹어댑터는_도메인모델에_직접_접근하지_않는다 =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..domain.model..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 애플리케이션서비스는_어댑터에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..application.service..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule 아웃바운드어댑터는_같은_컨텍스트의_인바운드포트에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..adapter.out.persistence..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.port.in..")
                    .allowEmptyShould(true);
}
