# archunit-sample

社内勉強会「ArchUnit でレイヤー違反を機械的に止める」のハンズオン用サンプルプロジェクトです。
Controller / Service / Repository の3層構造を持ち、Controller に **意図的なレイヤー違反** が1箇所入っています。

各クラスには Spring の `@RestController` / `@Service` / `@Repository` を付けていますが、
**Spring Boot ではないのでアプリは起動しません**。業務コードと同じ見た目にするための飾りです。
（ArchUnit はコンパイル済みクラスを解析するだけなので、起動する必要がありません）

## ハンズオン手順

手順は [docs/handson.md](docs/handson.md) を参照してください。

## ブランチ

| ブランチ | 内容 |
| --- | --- |
| `main` | 開始状態。ArchUnit の依存もテストもなく、Controller にレイヤー違反が残っている |
| `solution` | 完成状態。ArchUnit の依存とルール（`ArchitectureTest`）を追加し、レイヤー違反を直したもの |

ハンズオンでは `main` から始めます。詰まったときは `solution` ブランチを参照してください。

```bash
# 開始状態との差分を確認する
git diff main solution

# 特定のファイルだけ完成状態を取り出す（例: テストクラス）
git checkout solution -- src/test/java/com/example/app/ArchitectureTest.java
```

## 前提環境

- JDK 17 以上（`java -version` で確認）
- Maven 3.9.x（`mvn -v` で確認）
- 任意: IntelliJ IDEA や VS Code などのエディタ

## 始め方

```bash
git clone https://github.com/masato29isle/archunit-sample.git
cd archunit-sample
mvn test
```

開始状態ではテストが 0 件のまま `BUILD SUCCESS` になります。
依存ライブラリのダウンロードに時間がかかるため、勉強会の前に一度実行しておいてください。

## 構成

```
pom.xml                              Maven の設定（依存はここに足す）
docs/handson.md                      ハンズオン手順
src/main/java/com/example/app
├── controller/UserController.java   @RestController  リクエストを受け付ける
├── service/UserService.java         @Service         業務ロジック
└── repository/UserRepository.java   @Repository      データの読み書き（インメモリ）
src/test/java/com/example/app        ハンズオンでテストを追加する場所
```

ビルド成果物（`target/`）や IDE の設定ファイルは `.gitignore` で除外しています。
