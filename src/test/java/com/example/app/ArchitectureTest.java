package com.example.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

/**
 * アーキテクチャルールをテストとして表現するクラス。
 *
 * <p>{@code @AnalyzeClasses} で解析対象のパッケージを指定します。 テストクラス自身を対象から外すため、{@code
 * ImportOption.DoNotIncludeTests} を付けています。
 */
@AnalyzeClasses(
        packages = "com.example.app",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** ルール1: Controller は Repository に直接依存してはならない。 */
    @ArchTest
    static final ArchRule controllerShouldNotDependOnRepository =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .because("Controller は Service を経由してデータにアクセスする");

    /** ルール2: レイヤー間の依存方向をまとめて定義する。 */
    @ArchTest
    static final ArchRule layersShouldBeRespected =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                    .because("依存は Controller → Service → Repository の一方向にする");

    /** ルール3: {@code @Service} が付いたクラスは service パッケージに置き、名前は Service で終わる。 */
    @ArchTest
    static final ArchRule serviceClassesShouldResideInServicePackage =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().resideInAPackage("..service..")
                    .andShould().haveSimpleNameEndingWith("Service")
                    .because("役割（アノテーション）と置き場所・クラス名を揃える");
}
