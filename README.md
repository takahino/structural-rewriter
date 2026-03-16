# comby-java

[comby](https://comby.dev/) の Java 実装。構造的コード検索・置換ツール。

## 概要

comby は括弧バランス・文字列リテラル・コメントを認識しながらソースコードを検索・置換する構造的マッチングツールです。正規表現と異なり、`:[変数名]` という穴あきテンプレートでコードの「構造」にマッチします。

本実装は ANTLR4 Lexer Grammar によって文字列/コメント境界を解析し、Python のトリプルクォートやエスケープ済み引用符など手書きスキャナでは困難なケースも正確に処理します。

## 特徴

- 18言語対応（Java, Python, Go, C, C++, Rust, Scala, Kotlin, Swift, C#, JS, TS, PHP, Ruby, Bash, SQL, OCaml, Generic）
- ANTLR4 Lexer Grammar による文字列・コメント境界の正確な認識
  - Python トリプルクォート (`"""..."""`) の確実な優先処理
  - C/C++ エスケープ済み引用符 (`"\""`) の正確な処理
  - Rust/Scala/OCaml のネストコメント (`/* /* */ */`) 対応
- comby テンプレート構文のフルサポート (`:[var]`, `:[[alphanum]]`, `:[var~regex]` など)
- ルールエンジン (`where` 句) によるマッチフィルタリング
- JSON Lines / diff / インタラクティブレビュー出力フォーマット

---

## ビルド

```bash
# テストをスキップしてビルド
mvn package -DskipTests

# テスト込みでビルド（103テスト）
mvn package
```

実行可能 JAR は `target/comby-0.1.0-SNAPSHOT-executable.jar` に生成されます。

---

## 基本的な使い方

```
java -jar comby.jar <マッチテンプレート> <書き換えテンプレート> [オプション] [ディレクトリ]
```

### 最小例

```bash
# カレントディレクトリの .java ファイルで foo(...) を bar(...) に書き換え（確認表示）
java -jar comby.jar 'foo(:[args])' 'bar(:[args])' -f .java

# 実際にファイルを書き換える（-i でインプレース編集）
java -jar comby.jar 'foo(:[args])' 'bar(:[args])' -f .java -i

# 標準入力から読み込む
echo 'foo(x, y)' | java -jar comby.jar 'foo(:[a], :[b])' ':[b], :[a]' -stdin
```

---

## オプション一覧

### 入力・対象指定

| オプション | 説明 |
|---|---|
| `-d <ディレクトリ>` | 検索対象ディレクトリ（デフォルト: `.`） |
| `-f <拡張子>` | 対象ファイル拡張子（カンマ区切り例: `.java,.kt`） |
| `-stdin` | 標準入力からソースを読み込む |
| `-matcher <名前>` / `-m <名前>` | 使用する言語マッチャーを明示指定（例: `java`, `python`, `go`） |

### 動作モード

| オプション | 説明 |
|---|---|
| `-match-only` | マッチのみ表示。書き換えは行わない |
| `-i` / `-in-place` | ファイルをインプレースで書き換える |
| `-stdout` | 書き換え結果を標準出力へ出力する |
| `-count` | マッチ件数のみ表示する |
| `-rule <ルール>` | `where` ルールでマッチ結果をフィルタリング（後述） |

### 出力フォーマット

| オプション | 説明 |
|---|---|
| `-diff` | unified diff 形式で変更内容を表示 |
| `-json-lines` | JSON Lines（JSONL）形式で出力。CI/パイプラインに便利 |
| `-review` | インタラクティブに変更を確認（y/n で適用・スキップ） |

### その他

| オプション | 説明 |
|---|---|
| `-j <数>` / `-jobs <数>` | 並列処理ジョブ数（デフォルト: CPU コア数） |
| `-config <ファイル>` | TOML 設定ファイルからルールを読み込む（後述） |
| `-templates <ディレクトリ>` | TOML テンプレートファイルのディレクトリ |
| `-h` / `--help` | ヘルプ表示 |
| `--version` | バージョン表示 |

---

## テンプレート構文

`:[変数名]` がホール（穴）で、コードの任意部分にマッチします。

| 構文 | 名前 | 説明 |
|---|---|---|
| `:[x]` | EVERYTHING | 括弧バランスを保ちながら任意文字列にマッチ |
| `:[[x]]` | ALPHANUM | 英数字・アンダースコアのみにマッチ |
| `:[x.]` | NON_SPACE | 空白を含まない文字列にマッチ |
| `:[x\n]` | LINE | 1行（改行まで）にマッチ |
| `:[x~regex]` | REGEX | 正規表現でマッチ |
| `:[_]` / `...` | 匿名 | キャプチャしない（捨て変数） |
| `:[x:e]` | ESCAPABLE_STRING | エスケープ可能な文字列リテラル内にマッチ |
| `:[x:c]` | COMMENT | コメント内にマッチ |

### テンプレート例

```bash
# メソッド呼び出しの引数を入れ替え
java -jar comby.jar 'swap(:[a], :[b])' 'swap(:[b], :[a])' -f .java

# if 条件を抽出（1行マッチ）
java -jar comby.jar 'if (:[cond\n])' 'condition: :[cond]' -f .java -match-only

# 英数字識別子のみにマッチ
java -jar comby.jar 'new :[[ClassName]]()' 'create(:[ClassName].class)' -f .java

# 正規表現で数値リテラルにマッチ
java -jar comby.jar ':[n~[0-9]+]L' ':[n]' -f .java
```

---

## ルールフィルタリング（`-rule`）

`where` キーワードと条件式でマッチ結果を絞り込めます。

```bash
# :[x] が "foo" と等しいマッチのみ
java -jar comby.jar 'call(:[x])' 'call(:[x])' \
  -rule 'where :[x] == "foo"' -f .java -match-only

# :[x] が "tmp" を含むマッチのみ
java -jar comby.jar ':[x] = new :[T]()' ':[x]' \
  -rule 'where :[x] == "tmp"' -f .java -match-only
```

---

## TOML 設定ファイル（`-config`）

複数のルールをまとめて管理できます。

```toml
# rules.toml

[[rules]]
match   = "System.out.println(:[args])"
rewrite = "log.info(:[args])"

[[rules]]
match   = "e.printStackTrace()"
rewrite = "log.error(\"error\", e)"

[[rules]]
match   = "new ArrayList<>()"
rewrite = "new ArrayList<>()"
rule    = "where :[_] == :[_]"   # 常にマッチ（フィルタなし）
```

```bash
java -jar comby.jar -config rules.toml -f .java -diff
```

---

## 対応言語と自動検出

ファイル拡張子から言語を自動判定します。`-matcher` で明示上書きも可能。

| 言語 | 拡張子 | マッチャー名 |
|---|---|---|
| Java | `.java` | `java` |
| Python | `.py`, `.pyi` | `python` |
| Go | `.go` | `go` |
| C | `.c`, `.h` | `c` |
| C++ | `.cpp`, `.cc`, `.cxx`, `.hpp` | `cpp` |
| JavaScript | `.js`, `.mjs`, `.cjs` | `javascript` |
| TypeScript | `.ts`, `.tsx` | `typescript` |
| Kotlin | `.kt`, `.kts` | `kotlin` |
| Rust | `.rs` | `rust` |
| Scala | `.scala`, `.sc` | `scala` |
| C# | `.cs` | `csharp` |
| Swift | `.swift` | `swift` |
| PHP | `.php` | `php` |
| Ruby | `.rb` | `ruby` |
| Bash | `.sh`, `.bash` | `bash` |
| SQL | `.sql` | `sql` |
| OCaml | `.ml`, `.mli` | `ocaml` |
| Generic | `.generic`, `.txt` | `generic` |

---

## 実行例集

```bash
# Java: System.out.println を logger に置換（差分表示）
java -jar comby.jar \
  'System.out.println(:[msg])' \
  'logger.info(:[msg])' \
  -f .java -diff

# Python: 古いスタイルの print 文を関数呼び出しに変換
java -jar comby.jar \
  'print :[msg]' \
  'print(:[msg])' \
  -f .py -i

# Go: select 単一ケースを簡略化
java -jar comby.jar \
  'select { case :[v] := :[ch]: :[body] }' \
  ':[v] := :[ch]
:[body]' \
  -f .go -diff

# 標準入力でワンライナー変換
echo 'result := compute(a, b)' | \
  java -jar comby.jar 'compute(:[x], :[y])' 'add(:[x], :[y])' -stdin -stdout

# マッチ件数のみカウント
java -jar comby.jar 'TODO' '' -f .java -count

# JSON Lines 出力（CI パイプライン向け）
java -jar comby.jar 'foo(:[x])' 'bar(:[x])' -f .java -json-lines
```

---

## Java ライブラリとして使う

`Comby` クラスの static メソッドを呼び出すだけで、アプリケーションから構造的コード変換を利用できます。

### Maven 依存関係

[JitPack](https://jitpack.io/#takahino/structural-rewriter) 経由で利用できます。

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.takahino</groupId>
  <artifactId>structural-rewriter</artifactId>
  <version>v0.1.1</version>
</dependency>
```

### rewrite — ソース文字列の書き換え

```java
import io.github.takahino.comby.Comby;

// generic マッチャー（言語指定なし）
String result = Comby.rewrite(source, "foo(:[args])", "bar(:[args])");

// 言語を指定
String result = Comby.rewrite(source, "System.out.println(:[msg])", "log.info(:[msg])", "java");

// where ルール付き
String result = Comby.rewrite(source, "call(:[x])", "call(:[x])", "java",
                               "where :[x] == \"foo\"");

// ファイルパスから言語を自動判定
String result = Comby.rewriteFile(source, "foo(:[args])", "bar(:[args])", "src/Main.java");
```

### matches — マッチ一覧の取得

```java
import io.github.takahino.comby.Comby;
import io.github.takahino.comby.core.model.Match;
import java.util.List;

// 全マッチを取得
List<Match> matches = Comby.matches(source, "System.out.println(:[msg])", "java");

for (Match m : matches) {
    // キャプチャした変数の値を取得
    String msg = m.environment().get("msg").get().value();
    // マッチ位置（行・列・オフセット）
    int line   = m.range().start().line();
    int col    = m.range().start().column();
    System.out.printf("Line %d: %s%n", line, msg);
}

// ファイルパスから言語を自動判定してマッチ取得
List<Match> matches = Comby.matchesFile(source, "TODO: :[msg\n]", "src/Main.java");
```

### メソッド一覧

| メソッド | 説明 |
|---|---|
| `rewrite(source, match, rewrite)` | generic マッチャーで書き換え |
| `rewrite(source, match, rewrite, language)` | 言語指定で書き換え |
| `rewrite(source, match, rewrite, language, rule)` | 言語＋ルール指定で書き換え |
| `matches(source, match)` | generic マッチャーでマッチ一覧取得 |
| `matches(source, match, language)` | 言語指定でマッチ一覧取得 |
| `matches(source, match, language, rule)` | 言語＋ルール指定でマッチ一覧取得 |
| `rewriteFile(source, match, rewrite, filePath)` | ファイルパスで言語自動判定して書き換え |
| `rewriteFile(source, match, rewrite, filePath, rule)` | ファイルパス＋ルール指定で書き換え |
| `matchesFile(source, match, filePath)` | ファイルパスで言語自動判定してマッチ取得 |

`language` パラメータに使用できる値は[対応言語と自動検出](#対応言語と自動検出)の「マッチャー名」列を参照。

---

## テスト

```bash
mvn test
# → 102 tests, 0 failures, 0 skipped
```

---

## アーキテクチャ

```
MatchEngine          コアマッチングエンジン（stateless）
  ├── StructuralTokenizer   ANTLR Lexer ラッパー（文字列/コメント境界認識）
  │     ├── CombyCLikeTokenizer    C/Java/JS/TS/PHP/Kotlin/Swift/C#/Generic
  │     ├── CombyGoTokenizer       Go（バックティック raw string 対応）
  │     ├── CombyNestedBlockTokenizer  Rust/Scala（ネストコメント対応）
  │     ├── CombyHashTokenizer     Bash/Ruby（# コメント）
  │     ├── CombyPythonTokenizer   Python（トリプルクォート優先）
  │     ├── CombySqlTokenizer      SQL（-- / /* */ コメント）
  │     └── CombyOcamlTokenizer    OCaml（(* *) ネストコメント）
  └── TemplateParser        comby テンプレートのパース
```

### 実装メモ

ANTLR Lexer の「長いルールが優先（最長一致）」原則により、`"""` は必ず `"` より前に一致します。`consumeEverything` 内では `skippedOffsets`（文字列/コメント内部のオフセット集合）を参照し、文字列内部での括弧深度変化を抑制することで、`match "\"(\""` のようなパターンも正確に動作します。
