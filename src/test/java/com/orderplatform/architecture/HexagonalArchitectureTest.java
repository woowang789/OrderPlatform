package com.orderplatform.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 헥사고날 아키텍처 의존성 규칙 검증 테스트.
 *
 * 규칙 1: domain → application.service, adapter 의존 금지
 * 규칙 2: application.port.in → domain에만 의존
 * 규칙 3: adapter.in → domain.model 직접 접근 금지
 * 규칙 4: application.service → adapter 의존 금지
 * 규칙 5: adapter.out → application.port.in 의존 금지
 * 규칙 6: 컨텍스트 간 domain 패키지 상호 의존 금지
 */
@AnalyzeClasses(packages = "com.orderplatform", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // === 기존 규칙 ===

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

    // === 추가 규칙 ===

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

    @ArchTest
    static final ArchRule 컨텍스트간_도메인_직접의존_금지 =
            slices().matching("com.orderplatform.(*).domain..")
                    .should().notDependOnEachOther()
                    .as("컨텍스트 간 도메인 패키지는 서로 의존하지 않는다 (common 제외)")
                    .ignoreDependency(
                            DescribedPredicate.<JavaClass>alwaysTrue(),
                            new DescribedPredicate<JavaClass>("common 패키지") {
                                @Override
                                public boolean test(JavaClass javaClass) {
                                    return javaClass.getPackageName().contains(".common.");
                                }
                            }
                    );
}
