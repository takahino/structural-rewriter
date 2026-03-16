# structural-rewriter リファレンス

---

## 目次

1. [CLIリファレンス](#cliリファレンス)
   - [基本構文](#基本構文)
   - [入力モード](#入力モード)
   - [テンプレート構文](#テンプレート構文)
   - [リライトテンプレートの属性変換](#リライトテンプレートの属性変換)
   - [オプション詳細](#オプション詳細)
   - [ルール構文（`-rule`）](#ルール構文-rule)
   - [TOML 設定ファイル（`-config` / `-templates`）](#toml-設定ファイル-config---templates)
   - [言語マッチャー一覧](#言語マッチャー一覧)
   - [CLI 使用例集](#cli-使用例集)
2. [Java API リファレンス](#java-api-リファレンス)
   - [セットアップ](#セットアップ)
   - [Comby クラス メソッド一覧](#comby-クラス-メソッド一覧)
   - [Match モデル API](#match-モデル-api)
   - [API 使用例集](#api-使用例集)

---

## CLIリファレンス

### 基本構文

```
java -jar structural-rewriter-0.1.0-executable.jar <マッチテンプレート> <リライトテンプレート> [オプション]
```

| 位置引数 | 必須 | 説明 |
|---|---|---|
| `<マッチテンプレート>` | ✓ | ホールを含むマッチパターン |
| `<リライトテンプレート>` | ✓（`-match-only` 時は省略可） | 置換後のテンプレート。マッチと同じにすると変化しない |

```bash
# 最小構成（カレントディレクトリを検索）
java -jar structural-rewriter-0.1.0-executable.jar 'foo(:[x])' 'bar(:[x])'

# パイプ入力（-d 未指定・非インタラクティブ環境では自動的に stdin モードになる）
echo 'foo(x)' | java -jar structural-rewriter-0.1.0-executable.jar 'foo(:[x])' 'bar(:[x])'
```

---

### 入力モード

ツールは以下の優先順で入力を決定します。

#### 1. stdin 自動検出（推奨）

`-d` を指定せず、かつパイプ・リダイレクト環境（非インタラクティブ）の場合、
`-stdin` フラグなしで自動的に標準入力から読み込みます。

```bash
# bash / sh
echo 'if (x > 0) {}' | java -jar structural-rewriter-0.1.0-executable.jar \
  'if (:[c])' 'if ((:[c]).check())'

# PowerShell
'if (x > 0) {}' | java -jar structural-rewriter-0.1.0-executable.jar \
  'if (:[c])' 'if ((:[c]).check())'
```

#### 2. `-stdin` フラグ（明示指定）

インタラクティブ環境でも強制的に stdin から読み込む場合に使います。

```bash
java -jar structural-rewriter-0.1.0-executable.jar 'foo(:[x])' 'bar(:[x])' -stdin <<< 'foo(1)'
```

#### 3. ディレクトリ検索（`-d`）

`-d` を指定するとディレクトリを再帰検索します。未指定時のデフォルトはカレントディレクトリ（`.`）です。

```bash
# src/ 以下の .java ファイルを検索
java -jar structural-rewriter-0.1.0-executable.jar 'foo(:[x])' 'bar(:[x])' -d src -f .java
```

> **注意**: stdin 自動検出は `-d` が未指定の場合のみ働きます。
> `-d` を指定した場合はディレクトリ検索モードになります。

---

### テンプレート構文

`:[変数名]` がホール（穴）です。ホールはコードの任意部分にマッチし、
変数名でキャプチャした値を後から参照できます。

#### ホール種別

| 構文 | 種別 | 説明 |
|---|---|---|
| `:[x]` | EVERYTHING | **最も一般的**。括弧バランスを保ちながら停止条件まで消費。ネストした括弧・文字列・コメントを透過 |
| `:[[x]]` | ALPHANUM | 英字・数字・アンダースコア（`[A-Za-z0-9_]`）のみにマッチ |
| `:[x.]` | NON_SPACE | 空白を含まない文字列にマッチ |
| `:[x\n]` | LINE | 改行まで（改行文字は含まない）1行にマッチ |
| `:[ x]` | BLANK | 空白文字（スペース・タブ・改行）のみにマッチ |
| `:[x~regex]` | REGEX | 指定した正規表現にマッチ |
| `:[_]` または `...` | 匿名 | キャプチャしない（捨て変数）。同じ名前を複数回使う一貫性チェックが不要な場合に利用 |

#### 変数の再利用（一致制約）

同じ変数名を複数箇所に使うと、**すべて同じ値**にマッチすることを要求します。

```
:[x] + :[x]   →   a + a にマッチ（a + b にはマッチしない）
```

#### スコープ修飾子

| 構文 | 説明 |
|---|---|
| `:[x:e]` | エスケープ可能な文字列リテラルの内部にマッチ |
| `:[x:r]` | raw 文字列リテラルの内部にマッチ（バックティックなど） |
| `:[x:c]` | コメントの内部にマッチ |

#### EVERYTHING の停止条件

EVERYTHING ホール（`:[x]`）の直後にある定数が**自動的に停止条件**になります。

```
foo(:[args])      → :[args] は ) の直前まで消費
:[a] + :[b]       → :[a] はスペースの直前まで、:[b] は末尾まで
if (:[c]) { ... } → :[c] は ) の直前まで
```

括弧が深くネストしていても、対応する閉じ括弧の手前で止まります。

```
foo(bar(:[x]))    → :[x] は内側の ) の直前まで（深度0を保つ）
```

#### テンプレート例

```bash
# メソッド呼び出し全体にマッチ
'System.out.println(:[msg])'

# 任意の if 文
'if (:[cond]) { :[body] }'

# 英数字識別子のみ
'new :[[Type]]()'

# 1行マッチ（改行を越えない）
'// TODO: :[msg\n]'

# 正規表現で数値リテラル
':[n~[0-9]+]L'

# 括弧内の任意引数（0個以上）
'assert(:[_])'
```

---

### リライトテンプレートの属性変換

リライトテンプレート内で `:[変数名.属性]` と書くと、キャプチャした値を変換できます。

| 属性 | 説明 | 例（入力 → 出力） |
|---|---|---|
| `UPPERCASE` | すべて大文字 | `hello` → `HELLO` |
| `lowercase` | すべて小文字 | `HELLO` → `hello` |
| `Capitalize` | 先頭のみ大文字、残り小文字 | `helloWorld` → `Helloworld` |
| `UpperCamelCase` | アンダースコア区切り → UpperCamelCase | `my_method` → `MyMethod` |
| `lowerCamelCase` | アンダースコア区切り → lowerCamelCase | `my_method` → `myMethod` |
| `UPPER_SNAKE_CASE` | CamelCase → UPPER_SNAKE_CASE | `MyMethod` → `MY_METHOD` |
| `length` | 文字列の文字数（数値として展開） | `hello` → `5` |
| `lines` | キャプチャ内の改行数（数値として展開） | `a\nb\nc` → `2` |
| `line` | マッチ開始行番号（数値として展開） | — → `42` |
| `file` | 処理中のファイルパス | — → `src/Main.java` |

#### 特殊ホール

| 構文 | 説明 |
|---|---|
| `:[id()]` | UUID v4 ベースの8文字ランダムID（例: `a3f9c12b`） |

#### 属性変換例

```bash
# メソッド名を lowerCamelCase に変換
java -jar structural-rewriter-0.1.0-executable.jar \
  'def :[[name]]():' 'def :[name.lowerCamelCase]():' -f .py

# フィールド名から定数名を生成
java -jar structural-rewriter-0.1.0-executable.jar \
  'private String :[[field]];' \
  'private static final String :[field.UPPER_SNAKE_CASE] = "";' -f .java

# 一意IDを挿入
java -jar structural-rewriter-0.1.0-executable.jar \
  'createButton()' 'createButton(":[id()]")' -f .java
```

---

### オプション詳細

#### 入力・対象指定

| オプション | 説明 |
|---|---|
| `-d <ディレクトリ>` | 検索対象ディレクトリを指定（デフォルト: `.`）。指定するとディレクトリ検索モードになり stdin 自動検出が無効になる |
| `-f <拡張子>` | 対象ファイルの拡張子をカンマ区切りで指定（例: `.java,.kt`）。未指定時はすべてのファイルが対象 |
| `-stdin` | 標準入力からソースを強制的に読み込む。パイプ環境では自動検出されるためほぼ不要 |
| `-matcher <名前>` または `-m <名前>` | 言語マッチャーを明示指定（例: `java`, `python`）。未指定時はファイル拡張子から自動判定 |

#### 動作モード

| オプション | 説明 |
|---|---|
| `-match-only` | マッチ箇所の表示のみ行い、書き換えは実行しない |
| `-i` または `-in-place` | マッチしたファイルをその場で書き換える。`-diff` と同時に指定不可 |
| `-stdout` | 書き換え後のソースを標準出力に出力する。ファイルは変更しない |
| `-count` | マッチした件数のみ数値として出力する |
| `-rule <ルール>` | `where` 句でマッチ結果をフィルタリング（[ルール構文](#ルール構文-rule)参照） |
| `-j <数>` または `-jobs <数>` | 並列処理のジョブ数を指定（デフォルト: CPUコア数） |

#### 出力フォーマット

| オプション | 説明 |
|---|---|
| （なし） | マッチしたファイルの書き換え後テキストを出力（変更がある場合のみ） |
| `-diff` | unified diff 形式で変更内容を表示。実際のファイルは書き換えない |
| `-json-lines` | JSON Lines（JSONL）形式で全マッチ情報を出力。CI やパイプライン処理に適している |
| `-review` | インタラクティブに変更を確認。`y` で適用、`n` でスキップ |

#### 設定ファイル

| オプション | 説明 |
|---|---|
| `-config <ファイル>` | TOML 設定ファイルからルールセットを読み込む |
| `-templates <ディレクトリ>` | 指定ディレクトリ内のすべての `.toml` ファイルをルールとして読み込む |

#### その他

| オプション | 説明 |
|---|---|
| `-h` または `--help` | ヘルプを表示して終了 |
| `--version` | バージョン情報を表示して終了 |

---

### ルール構文（`-rule`）

`where` キーワードから始まる条件式でマッチ結果を絞り込みます。

#### 基本構文

```
where <条件> [, <条件> ...]
```

複数条件はカンマで区切ります（AND 結合）。

#### 条件式

| 構文 | 説明 |
|---|---|
| `:[x] == "文字列"` | 変数 x がリテラルと等しい |
| `:[x] != "文字列"` | 変数 x がリテラルと等しくない |
| `:[x] == :[y]` | 変数 x と変数 y が等しい |
| `:[x] != :[y]` | 変数 x と変数 y が等しくない |
| `"文字列" == :[x]` | 左辺にリテラルを置くことも可能 |
| `rewrite :[x] { "テンプレート" }` | 変数 x の値をインプレースでリライト |
| `true` / `false` | 常に真 / 常に偽 |

#### 使用例

```bash
# x が "null" の場合だけマッチ
-rule 'where :[x] == "null"'

# x と y が異なる場合だけマッチ
-rule 'where :[x] != :[y]'

# 複数条件（AND）: x が "foo" かつ y が "bar" 以外
-rule 'where :[x] == "foo", :[y] != "bar"'

# 変数の値をルール内でリライトしてから使う
-rule 'rewrite :[body] { "transformed :[body]" }'
```

#### CLI での指定例

```bash
# System.out.println の引数が "debug" のものだけ置換
java -jar structural-rewriter-0.1.0-executable.jar \
  'System.out.println(:[msg])' \
  'logger.debug(:[msg])' \
  -rule 'where :[msg] == "debug"' \
  -f .java -diff

# null チェックがある代入のみ検出
java -jar structural-rewriter-0.1.0-executable.jar \
  ':[x] = :[y]' '' \
  -rule 'where :[y] == "null"' \
  -f .java -match-only
```

---

### TOML 設定ファイル（`-config` / `-templates`）

複数のルールをファイルにまとめて管理できます。

#### ファイル形式

```toml
# rules.toml

[[rules]]
match   = "System.out.println(:[args])"
rewrite = "logger.info(:[args])"

[[rules]]
match   = "e.printStackTrace()"
rewrite = "logger.error(\"exception\", e)"

[[rules]]
match   = ":[x] == null"
rewrite = ":[x] == null"
rule    = "where :[x] != \"this\""   # this == null は除外
```

#### フィールド

| フィールド | 必須 | 説明 |
|---|---|---|
| `match` | ✓ | マッチテンプレート |
| `rewrite` | | リライトテンプレート（省略時は match と同じ） |
| `rule` | | where ルール文字列 |

#### 使用例

```bash
# 単一ファイル
java -jar structural-rewriter-0.1.0-executable.jar -config rules.toml -f .java -diff

# ディレクトリ内の全 .toml ファイルを適用
java -jar structural-rewriter-0.1.0-executable.jar -templates ./rules/ -f .java -i
```

---

### 言語マッチャー一覧

ファイル拡張子から自動判定されます。`-matcher` で明示指定することも可能です。

| `-matcher` 名 | 対応拡張子 | 特徴 |
|---|---|---|
| `java` | `.java` | `//` `/* */` コメント、`"` `'` 文字列 |
| `python` | `.py` `.pyi` | `"""..."""` `'''...'''` トリプルクォート優先、`#` コメント |
| `go` | `.go` | バックティック raw string、`//` `/* */` コメント |
| `c` | `.c` `.h` | `//` `/* */` コメント |
| `cpp` | `.cpp` `.cc` `.cxx` `.hpp` | C と同じ |
| `javascript` | `.js` `.mjs` `.cjs` | C スタイル |
| `typescript` | `.ts` `.tsx` | C スタイル |
| `kotlin` | `.kt` `.kts` | C スタイル |
| `csharp` | `.cs` | C スタイル |
| `swift` | `.swift` | C スタイル |
| `php` | `.php` | C スタイル |
| `rust` | `.rs` | `/* /* */ */` ネストブロックコメント |
| `scala` | `.scala` `.sc` | ネストブロックコメント |
| `ruby` | `.rb` | `#` コメント |
| `bash` | `.sh` `.bash` | `#` コメント |
| `sql` | `.sql` | `--` 行コメント、`/* */` ブロックコメント |
| `ocaml` | `.ml` `.mli` | `(* (* *) *)` ネストコメント |
| `generic` | その他 | `"` `'` 文字列のみ、コメント認識なし |

---

### CLI 使用例集

#### 書き換え・確認

```bash
# 差分確認（ファイルは変更しない）
java -jar structural-rewriter-0.1.0-executable.jar \
  'System.out.println(:[msg])' 'logger.info(:[msg])' -f .java -diff

# インプレース書き換え
java -jar structural-rewriter-0.1.0-executable.jar \
  'System.out.println(:[msg])' 'logger.info(:[msg])' -f .java -i

# インタラクティブレビュー（y/n で個別適用）
java -jar structural-rewriter-0.1.0-executable.jar \
  'TODO: :[msg\n]' 'FIXME: :[msg]' -f .java -review
```

#### マッチ検索

```bash
# マッチのみ表示（書き換えなし）
java -jar structural-rewriter-0.1.0-executable.jar \
  'catch (:[_]) { :[_] }' '' -f .java -match-only

# マッチ件数をカウント
java -jar structural-rewriter-0.1.0-executable.jar \
  'new Thread(' '' -f .java -count

# JSON Lines 出力（各マッチの位置・キャプチャ値を含む）
java -jar structural-rewriter-0.1.0-executable.jar \
  ':[x] + :[y]' '' -f .java -json-lines
```

#### stdin / stdout

```bash
# パイプで変換（ファイルに書かず stdout に出力）
echo 'foo(x, y)' | java -jar structural-rewriter-0.1.0-executable.jar \
  'foo(:[a], :[b])' 'foo(:[b], :[a])'

# 変換して別ファイルへ
cat input.java | java -jar structural-rewriter-0.1.0-executable.jar \
  'Assert.assertEquals(:[a], :[b])' 'assertEquals(:[a], :[b])' > output.java
```

#### 属性変換

```bash
# スネークケース → キャメルケース
java -jar structural-rewriter-0.1.0-executable.jar \
  'def :[[name]]' 'def :[name.lowerCamelCase]' -f .py -diff

# 定数名を生成
java -jar structural-rewriter-0.1.0-executable.jar \
  'String :[[field]] =' 'static final String :[field.UPPER_SNAKE_CASE] =' -f .java -diff
```

#### 言語指定

```bash
# 拡張子によらず Python マッチャーを使用
java -jar structural-rewriter-0.1.0-executable.jar \
  'print :[x]' 'print(:[x])' -matcher python -f .txt -i
```

---

## Java API リファレンス

### セットアップ

#### Maven

```xml
<dependency>
  <groupId>io.github.takahino</groupId>
  <artifactId>structural-rewriter</artifactId>
  <version>0.1.0</version>
</dependency>
```

#### import

```java
import io.github.takahino.comby.Comby;
import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.core.model.CapturedValue;
import io.github.takahino.comby.core.model.Range;
import io.github.takahino.comby.core.model.Location;

import java.util.List;
```

---

### Comby クラス メソッド一覧

すべて `static` メソッドです。インスタンス生成は不要です。

#### rewrite — ソースの書き換え

| メソッドシグネチャ | 説明 |
|---|---|
| `rewrite(source, match, rewrite)` | generic マッチャーで書き換え |
| `rewrite(source, match, rewrite, language)` | 言語を指定して書き換え |
| `rewrite(source, match, rewrite, language, rule)` | 言語 + where ルールを指定して書き換え |
| `rewriteFile(source, match, rewrite, filePath)` | ファイルパスから言語を自動判定して書き換え |
| `rewriteFile(source, match, rewrite, filePath, rule)` | ファイルパス + where ルールを指定して書き換え |

- マッチがなければ `source` をそのまま返す
- `language` に使用できる値は[言語マッチャー一覧](#言語マッチャー一覧)の「`-matcher` 名」列を参照
- `rule` が不要な場合は `null` を渡す

#### matches — マッチ一覧の取得

| メソッドシグネチャ | 説明 |
|---|---|
| `matches(source, match)` | generic マッチャーで全マッチを取得 |
| `matches(source, match, language)` | 言語を指定して全マッチを取得 |
| `matches(source, match, language, rule)` | 言語 + where ルールを指定して全マッチを取得 |
| `matchesFile(source, match, filePath)` | ファイルパスから言語を自動判定して全マッチを取得 |

- マッチがなければ空のリストを返す

---

### Match モデル API

`Comby.matches()` が返す `Match` オブジェクトの構造です。

```
Match
├── matchedText()   : String          マッチした文字列全体
├── range()         : Range           マッチ位置（行・列・オフセット）
├── environment()   : MatchEnvironment  キャプチャした変数のマップ
└── filePath()      : String | null   処理元ファイルパス（stdin 時は "<stdin>"）

Range
├── start() : Location   開始位置
└── end()   : Location   終了位置（exclusive）

Location
├── offset() : int   ソース先頭からのバイトオフセット
├── line()   : int   行番号（1始まり）
└── column() : int   列番号（1始まり）

MatchEnvironment
└── get(name) : Optional<CapturedValue>   変数名でキャプチャ値を取得

CapturedValue
├── name()  : String   変数名
├── value() : String   キャプチャした文字列
└── range() : Range    キャプチャ位置
```

#### キャプチャ値の取得

```java
List<Match> matches = Comby.matches(source, "foo(:[a], :[b])", "java");

for (Match m : matches) {
    // キャプチャ変数の値
    String a = m.environment().get("a").get().value();
    String b = m.environment().get("b").get().value();

    // 変数が存在するか確認してから取得
    m.environment().get("a").ifPresent(cv -> System.out.println(cv.value()));

    // マッチ全体の文字列
    String matched = m.matchedText();

    // 位置情報
    int startLine   = m.range().start().line();
    int startCol    = m.range().start().column();
    int startOffset = m.range().start().offset();
    int endOffset   = m.range().end().offset();
}
```

---

### API 使用例集

#### 基本的な書き換え

```java
String source = "System.out.println(\"hello\");\nSystem.out.println(x);";

// generic マッチャー（言語指定なし）
String result = Comby.rewrite(source,
    "System.out.println(:[msg])",
    "logger.info(:[msg])");
// → "logger.info(\"hello\");\nlogger.info(x);"

// Java マッチャーを指定
String result = Comby.rewrite(source,
    "System.out.println(:[msg])",
    "logger.info(:[msg])",
    "java");
```

#### ファイルパスで言語を自動判定

```java
// ファイル内容を読み込んで変換
String source = Files.readString(Path.of("src/Main.java"));
String result = Comby.rewriteFile(source,
    "System.out.println(:[msg])",
    "logger.info(:[msg])",
    "src/Main.java");   // 拡張子 .java から自動的に Java マッチャーを使用
```

#### where ルールでフィルタリング

```java
// "null" への代入だけ書き換え
String result = Comby.rewrite(source,
    ":[x] = :[y]",
    ":[x] = Objects.requireNonNull(:[y])",
    "java",
    "where :[y] != \"null\"");  // null への代入は除外

// 複数条件
String result = Comby.rewrite(source,
    "call(:[a], :[b])",
    "call(:[b], :[a])",
    "java",
    "where :[a] != :[b], :[a] != \"\"");
```

#### マッチ情報の取得

```java
String source = """
    int add(int x, int y) { return x + y; }
    int mul(int x, int y) { return x * y; }
    """;

List<Match> matches = Comby.matches(source,
    "int :[[name]](:[params]) { :[body] }",
    "java");

for (Match m : matches) {
    String name   = m.environment().get("name").get().value();
    String params = m.environment().get("params").get().value();
    String body   = m.environment().get("body").get().value();
    int    line   = m.range().start().line();

    System.out.printf("L%d: %s(%s) → %s%n", line, name, params, body);
}
// L1: add(int x, int y) → return x + y;
// L2: mul(int x, int y) → return x * y;
```

#### 属性変換（リライトテンプレート内）

```java
// スネークケース → lowerCamelCase
String result = Comby.rewrite(source,
    "def :[[name]]():",
    "def :[name.lowerCamelCase]():",
    "python");

// フィールド名から定数名を生成
String result = Comby.rewrite(source,
    "private String :[[field]];",
    "private static final String :[field.UPPER_SNAKE_CASE] = \"\";",
    "java");

// マッチ位置の行番号を埋め込む
String result = Comby.rewrite(source,
    "TODO: :[msg\n]",
    "TODO(L:[x.line]): :[msg]",
    "java");
```

#### 複数ファイルの処理

```java
import java.nio.file.*;

List<Path> files = Files.walk(Path.of("src"))
    .filter(p -> p.toString().endsWith(".java"))
    .toList();

for (Path file : files) {
    String source = Files.readString(file);
    String result = Comby.rewriteFile(source,
        "Assert.assertEquals(:[a], :[b])",
        "assertEquals(:[a], :[b])",
        file.toString());
    if (!result.equals(source)) {
        Files.writeString(file, result);
        System.out.println("Updated: " + file);
    }
}
```

#### マッチ結果を JSON 的に集計

```java
Map<String, Long> callCounts = Comby
    .matchesFile(source, ":[[method]](:[_])", "src/Main.java")
    .stream()
    .collect(Collectors.groupingBy(
        m -> m.environment().get("method").get().value(),
        Collectors.counting()
    ));

callCounts.forEach((method, count) ->
    System.out.printf("%s: %d 回%n", method, count));
```
