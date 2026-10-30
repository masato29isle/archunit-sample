# ハンズオン: ArchUnit でレイヤー違反を機械的に止める

所要時間の目安: 約 15 分

## このハンズオンでやること

3層構造のサンプルアプリに ArchUnit を導入し、レイヤー違反を検出するルールをテストとして書きます。
仕込まれた違反コードでテストを **赤** にしてから、コードを直して **緑** にします。

完了すると次の状態になります。

- `mvn test` でアーキテクチャルールが検証される
- Controller から Repository を直接呼ぶコードがあると、テストが失敗する

## 前提環境

作業を始める前に、以下を確認してください。

| 項目 | 要件 | 確認コマンド |
| --- | --- | --- |
| JDK | 17 以上 | `java -version` |
| ビルドツール | Maven 3.9.x | `mvn -v` |
| Git | 任意のバージョン | `git --version` |
| エディタ | IntelliJ IDEA / VS Code など任意 | - |
| ネットワーク | Maven Central に接続できること（依存のダウンロード用） | - |

サンプルプロジェクトは GitHub の [masato29isle/archunit-sample](https://github.com/masato29isle/archunit-sample) にあります。
事前に clone して、`mvn test` が通ることを確認しておいてください（この時点ではテストが 0 件のまま成功します）。

```bash
git clone https://github.com/masato29isle/archunit-sample.git
cd archunit-sample
mvn test
```

**期待される結果**: `BUILD SUCCESS` と表示される

`main` ブランチが開始状態、`solution` ブランチが完成状態です。途中で詰まったら、完成状態のファイルを取り出して見比べてください。

```bash
# 開始状態との差分を確認する
git diff main solution

# 特定のファイルだけ完成状態を取り出す（例: テストクラス）
git checkout solution -- src/test/java/com/example/app/ArchitectureTest.java
```

> このハンズオンでは API キーや外部サービスは使いません。

## 手順

### Step 1: サンプルの構造を確認する（2分）

clone したディレクトリ（`archunit-sample/`）で、パッケージ構成を眺めます。

```
src/main/java/com/example/app
├── controller/UserController.java   @RestController
├── service/UserService.java         @Service
└── repository/UserRepository.java   @Repository
```

各クラスには Spring のアノテーションが付いていて、業務でよく見る Spring Boot のコードと同じ見た目になっています。
ただし **このサンプルは Spring Boot ではないので起動しません**。ArchUnit はコンパイル済みのクラスを解析するだけなので、アプリを動かす必要がないためです。
`@RestController` や `@GetMapping` は「そのクラスが何の役割か」を示す目印だと思ってください。

`UserController.java` を開いてください。`NOTE` とコメントされた箇所が2つあります。
Controller が `UserRepository` をフィールドで持ち、`listUsers()` で直接呼んでいます。これが今日直す **レイヤー違反** です。

**期待される結果**: 違反箇所が2つ見つかる。この時点で `mvn test` は通る（違反してもビルドは通る、というのがポイント）

### Step 2: ArchUnit の依存を追加する（2分）

`pom.xml` を開き、`TODO(ハンズオン Step 2)` とあるコメント行を次の `<dependency>` に置き換えます。

```xml
    <!-- ArchUnit（JUnit 5 連携） -->
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>1.3.0</version>
      <scope>test</scope>
    </dependency>
```

置き換え後の `<dependencies>` は次のようになります。

```xml
  <dependencies>
    <!-- Spring のアノテーションを使うためだけに入れている（アプリは起動しない） -->
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-context</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>

    <!-- ArchUnit（JUnit 5 連携） -->
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>1.3.0</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
```

> Spring と JUnit のバージョンは `<dependencyManagement>` の BOM で管理しているので、個別の `<version>` は書いていません。
> `spring-context` と `spring-web` は `@Service` や `@RestController` を書くためだけに入っています。Spring Boot の依存（`spring-boot-starter-*`）は入っていないので、起動用の設定は不要です。

**期待される結果**: `mvn test` が通り、依存がダウンロードされる

### Step 3: ルール1を書いてテストを赤にする（4分）

`src/test/java/com/example/app/ArchitectureTest.java` を作成し、次の内容を書きます。

```java
package com.example.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
}
```

ポイントは3つです。

- `@AnalyzeClasses` で解析するパッケージを指定する。`DoNotIncludeTests` でテストクラス自身を対象から外す
- `@ArchTest` を付けた `static final ArchRule` フィールドが、1つのテストケースになる
- `..controller..` の `..` は「任意の階層」を表す。`com.example.app.controller` もその下のサブパッケージも対象になる

テストを実行します。

```bash
mvn test
```

**期待される結果**: テストが **失敗** し、次のようなメッセージが出る（`[ERROR]` の行に続いて表示されます）

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0 <<< FAILURE! -- in com.example.app.ArchitectureTest
[ERROR] com.example.app.ArchitectureTest.controllerShouldNotDependOnRepository <<< FAILURE!
java.lang.AssertionError:
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package
'..controller..' should depend on classes that reside in a package '..repository..',
because Controller は Service を経由してデータにアクセスする' was violated (3 times):
Constructor <com.example.app.controller.UserController.<init>(...)> has parameter of type
  <com.example.app.repository.UserRepository> in (UserController.java:0)
Field <com.example.app.controller.UserController.userRepository> has type
  <com.example.app.repository.UserRepository> in (UserController.java:0)
Method <com.example.app.controller.UserController.listUsers()> calls method
  <com.example.app.repository.UserRepository.findAll()> in (UserController.java:38)
```

メッセージを読んでみてください。**何のルール** に、**どこ** が、**なぜ** 違反しているかが全部書かれています。
`because(...)` に書いた文章がそのまま出ているのも確認してください。

### Step 4: 違反コードを直してテストを緑にする（3分）

`UserController.java` を、Service 経由でデータを取る形に直します。

```java
package com.example.app.controller;

import com.example.app.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ユーザー関連のリクエストを受け付けるクラス。 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{name}")
    public String checkUser(@PathVariable String name) {
        return userService.exists(name) ? "OK: " + name : "NOT FOUND: " + name;
    }

    // Service を経由してデータを取得する
    @GetMapping
    public List<String> listUsers() {
        return userService.listUsers();
    }
}
```

変えたのは次の2点です。

- `UserRepository` の import、フィールド、コンストラクタ引数を削除した
- `listUsers()` を `userService.listUsers()` に変えた

テストを再実行します。

```bash
mvn test
```

**期待される結果**: `BUILD SUCCESS` と表示され、テストが通る

### Step 5: ルール2でレイヤー全体の依存方向を定義する（4分）

ルール1は「Controller → Repository を禁止」という1本の禁止でした。
レイヤーが増えると禁止ルールも増えてしまうので、`layeredArchitecture()` でまとめて書きます。

`ArchitectureTest.java` に import とルールを追加します。

```java
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
```

```java
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
```

読み方は次のとおりです。

- `layer(...).definedBy(...)`: レイヤーに名前を付け、どのパッケージが属するかを決める
- `whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")`: Service を使ってよいのは Controller だけ
- `consideringOnlyDependenciesInLayers()`: レイヤー外のクラス（`java.util` など）への依存は無視する

```bash
mvn test
```

**期待される結果**: ルールが2本になり、どちらも通る

試しに Step 4 で直した `UserController.java` を Step 1 の状態に戻して `mvn test` を実行すると、今度は **2本とも** 赤になります。確認したら再度直してください。

### Step 6（時間があれば）: アノテーションを条件にしたルールを足す

ここまでのルールは「パッケージ」を条件にしていました。
Spring のプロジェクトでは `@Service` や `@RestController` といったアノテーションがクラスの役割を表すので、それを条件にしたルールも書けます。

「`@Service` が付いたクラスは `service` パッケージに置き、クラス名は `Service` で終わる」というルールを追加します。

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.springframework.stereotype.Service;
```

```java
    /** ルール3: @Service が付いたクラスは service パッケージに置き、名前は Service で終わる。 */
    @ArchTest
    static final ArchRule serviceClassesShouldResideInServicePackage =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().resideInAPackage("..service..")
                    .andShould().haveSimpleNameEndingWith("Service")
                    .because("役割（アノテーション）と置き場所・クラス名を揃える");
```

読み方は次のとおりです。

- `that().areAnnotatedWith(Service.class)`: 対象を「`@Service` が付いたクラス」に絞る
- `should().resideInAPackage(...)`: そのクラスが満たすべき条件。`andShould()` で条件を重ねられる
- `that()` が **どのクラスを見るか**、`should()` が **何を守るべきか**、という2段構えはルール1と同じ

```bash
mvn test
```

**期待される結果**: ルールが3本になり、すべて通る

試しに `controller` パッケージに次のクラスを追加して `mvn test` を実行してください。

```java
package com.example.app.controller;

import org.springframework.stereotype.Service;

@Service
public class UserHelper {}
```

**期待される結果**: ルール3だけが赤になり、「`..service..` パッケージにない」「名前が `Service` で終わらない」の2件が報告される。確認したらクラスを削除する

### 発展・自由課題

時間が余った人向けです。

- `@RestController` や `@Repository` についても、ルール3と同じ形でルールを書いてみる
- Repository が Service を呼び返す（逆方向の依存）コードを書いて、ルール2がどう反応するか見る
- ルール2のレイヤー定義をパッケージではなくアノテーションで書き換えてみる。`definedBy("..service..")` の代わりに `definedBy(annotatedWith(Service.class))` と書けます（`com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith` を static import する）。パッケージ構成が崩れているプロジェクトでも、アノテーションを手がかりにレイヤーを定義できるのが利点です
- 自分の現場のプロジェクトなら、どんなルールを最初の1本にするか考えてみる

## トラブルシューティング

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| テストが1本も実行されない（`Tests run: 0`） | `@AnalyzeClasses` または `@ArchTest` の付け忘れ、`archunit-junit5` ではなく `archunit` を入れている、または `maven-surefire-plugin` が 3.5.3 | アノテーションと依存名を確認する。surefire 3.5.3 は ArchUnit のテストを検出しない不具合があるので、`pom.xml` のとおり 3.6.0 を使う（3.5.4 以降なら問題ない） |
| Step 4 で直したのにまだ赤い | ビルド結果が古い | `mvn clean test` を実行する |
| `package org.springframework.stereotype does not exist` | `spring-context` / `spring-web` の依存が消えている | `pom.xml` の `spring-context` と `spring-web` の `<dependency>` を確認する。Step 2 で `<dependencies>` ごと置き換えた場合に起きやすい |
| `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` | JDK 24 以降で ArchUnit 内部のライブラリが出す警告 | 動作には影響しない。無視してよい |
| `SLF4J(W): No SLF4J providers were found.` | ArchUnit がログ出力に使う SLF4J の実装が入っていない | 動作には影響しない。無視してよい |
| 違反がないのにルール2が赤い | レイヤーの外にあるクラスが Controller を呼んでいる（例: `Main` クラス） | `consideringOnlyDependenciesInLayers()` が付いているか確認する。付いていれば、レイヤー外のクラスは無視される |

## 片付け

なし。ローカルのファイルを消したければ、clone したディレクトリごと削除してください。

## 参考資料

- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html): 公式ガイド。「6. The Library API」に `layeredArchitecture()` の説明がある
- [ArchUnit Examples](https://github.com/TNG/ArchUnit-Examples): 公式のサンプル集
