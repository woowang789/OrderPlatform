package com.orderplatform.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 헥사고날 아키텍처 의존성 규칙 검증 테스트.
 *
 * 규칙 1: domain → application.service, adapter 의존 금지
 * 규칙 2: application.port.in → domain에만 의존
 * 규칙 3: adapter.in → domain.model 직접 접근 금지
 */
@AnalyzeClasses(packages = "com.orderplatform", importOptions = ImportOption.DoNotIncludeTests.class)
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
                    .resideInAnyPackage("..domain..", "java..", "jakarta..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 웹어댑터는_도메인모델에_직접_접근하지_않는다 =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..domain.model..")
                    .allowEmptyShould(true);
}
