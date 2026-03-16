# structural-rewriter — アーキテクチャドキュメント

## 概要

本プロジェクトは、[comby](https://github.com/comby-tools/comby) が提唱する
構造的コード検索・置換のアプローチに着想を得て、Java 21 + Maven で独自に設計・実装したツールです。
comby との互換性は目指していますが、内部実装や一部の動作は異なります。

- **groupId**: `io.github.takahino`
- **artifactId**: `structural-rewriter`
- **version**: `0.1.0`
- **対応言語マッチャー**: 18言語

「括弧バランスを保ちながらホール（`:[var]`）でコードをマッチする」という構造的マッチングが核心です。
正規表現と異なり、ネストした括弧・文字列リテラル・コメントを正しく透過します。

---

## ディレクトリ構成

```
src/
├── main/
│   ├── java/io/github/takahino/comby/
│   │   ├── Comby.java                          Java ライブラリ API（static facade）
│   │   ├── Main.java                           CLI エントリポイント
│   │   ├── cli/
│   │   │   └── CombyCommand.java              picocli CLI コマンド定義
│   │   ├── core/
│   │   │   ├── model/                         データモデル（不変）
│   │   │   │   ├── HoleSort.java
│   │   │   │   ├── HoleDimension.java
│   │   │   │   ├── TemplateNode.java
│   │   │   │   ├── Location.java
│   │   │   │   ├── Range.java
│   │   │   │   ├── CapturedValue.java
│   │   │   │   ├── MatchEnvironment.java
│   │   │   │   ├── Match.java
│   │   │   │   ├── RewriteResult.java
│   │   │   │   └── Specification.java
│   │   │   ├── template/
│   │   │   │   └── TemplateParser.java        テンプレート文字列 → List<TemplateNode>
│   │   │   ├── matcher/
│   │   │   │   ├── LanguageSyntax.java        言語ごとの構文定義
│   │   │   │   ├── LanguageMatcher.java       インターフェース
│   │   │   │   ├── AbstractLanguageMatcher.java  共通実装
│   │   │   │   ├── StringCursor.java          走査カーソル
│   │   │   │   ├── MatchEngine.java           コアマッチングエンジン
│   │   │   │   ├── StructuralToken.java       字句トークン（タイプ + オフセット）
│   │   │   │   ├── StructuralTokenizer.java   ANTLR トークナイザー インターフェース
│   │   │   │   ├── StructuralTokenType.java   トークン種別 enum
│   │   │   │   ├── languages/                 18言語マッチャー
│   │   │   │   │   ├── GenericMatcher.java
│   │   │   │   │   ├── JavaMatcher.java
│   │   │   │   │   ├── PythonMatcher.java
│   │   │   │   │   ├── JavaScriptMatcher.java
│   │   │   │   │   ├── TypeScriptMatcher.java
│   │   │   │   │   ├── CMatcher.java
│   │   │   │   │   ├── CppMatcher.java
│   │   │   │   │   ├── GoMatcher.java
│   │   │   │   │   ├── RustMatcher.java
│   │   │   │   │   ├── RubyMatcher.java
│   │   │   │   │   ├── PhpMatcher.java
│   │   │   │   │   ├── ScalaMatcher.java
│   │   │   │   │   ├── KotlinMatcher.java
│   │   │   │   │   ├── SwiftMatcher.java
│   │   │   │   │   ├── CSharpMatcher.java
│   │   │   │   │   ├── SqlMatcher.java
│   │   │   │   │   ├── BashMatcher.java
│   │   │   │   │   ├── OcamlMatcher.java
│   │   │   │   │   └── LanguageRegistry.java  拡張子 → Matcher 解決テーブル
│   │   │   │   └── tokenizer/                 ANTLR4 ベーストークナイザー実装
│   │   │   │       ├── CombyCLikeTokenizer.java   C/Java/JS/TS/PHP/Kotlin/Swift/C# 共通
│   │   │   │       ├── CombyGoTokenizer.java       Go（バックティック raw string）
│   │   │   │       ├── CombyNestedBlockTokenizer.java  Rust/Scala（ネストブロックコメント）
│   │   │   │       ├── CombyHashTokenizer.java     Bash/Ruby（# コメント）
│   │   │   │       ├── CombyPythonTokenizer.java   Python（トリプルクォート優先）
│   │   │   │       ├── CombySqlTokenizer.java      SQL（-- / /* */ コメント）
│   │   │   │       └── CombyOcamlTokenizer.java    OCaml（(* *) ネストコメント）
│   │   │   ├── rewrite/
│   │   │   │   └── RewriteEngine.java
│   │   │   └── rule/
│   │   │       ├── RuleNode.java
│   │   │       ├── RuleParser.java
│   │   │       └── RuleEngine.java
│   │   ├── pipeline/
│   │   │   ├── Pipeline.java                  Virtual Threads 並列処理
│   │   │   ├── FileProcessor.java
│   │   │   └── PipelineConfig.java
│   │   └── output/
│   │       ├── OutputFormatter.java           インターフェース
│   │       ├── TextFormatter.java
│   │       ├── JsonLinesFormatter.java
│   │       ├── DiffFormatter.java
│   │       └── InteractiveFormatter.java
│   └── antlr4/io/github/takahino/comby/core/matcher/
│       ├── CombyCLikeLexer.g4                 C スタイル言語共通 Lexer Grammar
│       ├── CombyGoLexer.g4                    Go バックティック Lexer Grammar
│       ├── CombyNestedBlockLexer.g4           Rust/Scala ネストコメント Lexer Grammar
│       ├── CombyHashLexer.g4                  Bash/Ruby # コメント Lexer Grammar
│       ├── CombyPythonLexer.g4                Python トリプルクォート優先 Lexer Grammar
│       ├── CombySqlLexer.g4                   SQL コメント Lexer Grammar
│       └── CombyOcamlLexer.g4                 OCaml ネストコメント Lexer Grammar
└── test/java/io/github/takahino/comby/
    ├── template/TemplateParserTest.java
    ├── matcher/
    │   ├── MatchEngineTest.java
    │   ├── HoleExtensionTest.java
    │   └── LanguageMatcherTest.java
    ├── rewrite/RewriteEngineTest.java
    ├── rule/RuleEngineTest.java
    └── integration/CombyIntegrationTest.java
```

---

## コアコンポーネント詳細

### 0. Java ライブラリ API (`Comby.java`)

ライブラリとして利用する際の static facade クラスです。
CLI を経由せずに Java コードから直接 comby の機能を呼び出せます。

```java
// マッチ
List<Match> matches = Comby.matches(source, ":[x] + :[y]");
List<Match> matches = Comby.matches(source, ":[x]", "java");

// リライト
String result = Comby.rewrite(source, "if (:[c])", "if (!(:[c]))");
String result = Comby.rewrite(source, ":[x]", ":[x.UPPERCASE]", "java");
String result = Comby.rewrite(source, ":[x]", ":[x]", "java", "where :[x] == \"foo\"");

// ファイルパスから言語自動検出
String result = Comby.rewriteFile(source, matchTmpl, rewriteTmpl, "Main.java");
List<Match> matches = Comby.matchesFile(source, matchTmpl, "script.py");
```

---

### 1. データモデル (`core/model/`)

すべて **不変** な record / sealed interface で設計されています。

#### `TemplateNode` — sealed interface

```
TemplateNode
├── Constant(String value)                          リテラル文字列
└── Hole(name, sort, dimension, regex)              キャプチャホール
```

Java 21 の sealed interface + pattern matching for switch を活用し、
ADT（代数的データ型）的な型安全な処理を実現しています。

#### `HoleSort` — ホール種別 enum

| 値 | 構文例 | 挙動 |
|---|---|---|
| `EVERYTHING` | `:[x]` | 括弧バランスを保ちながら停止条件まで消費（主要ホール） |
| `EXPRESSION` | （EVERYTHING の別名） | — |
| `ALPHANUM` | `:[[x]]` | 英数字・アンダースコアのみ |
| `NON_SPACE` | `:[x.]` | 空白以外の連続文字 |
| `LINE` | `:[x\n]` | 改行まで（改行を含まない） |
| `BLANK` | `:[ x]` | 空白文字のみ |
| `REGEX` | `:[x~\w+]` | 指定正規表現にマッチ |

#### `HoleDimension` — ホールの適用スコープ enum

| 値 | 構文例 | 挙動 |
|---|---|---|
| `CODE` | `:[x]` | コード部分（デフォルト） |
| `ESCAPABLE_STRING` | `:[x:e]` | エスケープ可能な文字列リテラル内 |
| `RAW_STRING` | `:[x:r]` | raw 文字列リテラル内（バックティック等） |
| `COMMENT` | `:[x:c]` | コメント内 |

#### `MatchEnvironment` — 不変バインディングマップ

`bind()` メソッドは新しいインスタンスを返します。
同一変数の複数出現（`:[x] + :[x]`）はバインド済み値との等値確認で処理されます。

#### `Specification` — 1つの変換仕様

```java
record Specification(String matchTemplate, String rewriteTemplate, String rule)
```

---

### 2. テンプレートパーサー (`core/template/TemplateParser.java`)

手書き再帰降下パーサで、テンプレート文字列を `List<TemplateNode>` に変換します。

#### ホール構文の解析規則

| 入力パターン | ノード種別 | 備考 |
|---|---|---|
| `:[x]` | `Hole(x, EVERYTHING, CODE)` | 最も一般的なホール |
| `:[[x]]` | `Hole(x, ALPHANUM, CODE)` | 二重括弧 |
| `:[x~\w+]` | `Hole(x, REGEX, CODE, regex="\w+")` | `~` の後が正規表現。`[...]` のネストを考慮して最終 `]` を終端とする |
| `:[x.]` | `Hole(x, NON_SPACE, CODE)` | `.` の後に続く文字がなければ NON_SPACE |
| `:[x.UPPERCASE]` | `Hole(x, EVERYTHING, CODE)` | `.` 以降が属性名（リライト時に変換） |
| `:[ x]` | `Hole(x, BLANK, CODE)` | 先頭スペースが BLANK ホールの識別子 |
| `:[x\n]` | `Hole(x, LINE, CODE)` | `\n` リテラルで LINE ホール |
| `:[_]` | `Hole(_, EVERYTHING, CODE)` | アンダースコアは捨て変数（bindしない） |
| `:[x:e]` | `Hole(x, EVERYTHING, ESCAPABLE_STRING)` | エスケープ可能文字列スコープ |
| `:[x:r]` | `Hole(x, EVERYTHING, RAW_STRING)` | raw 文字列スコープ |
| `:[x:c]` | `Hole(x, EVERYTHING, COMMENT)` | コメントスコープ |
| `...` | `Hole(_, EVERYTHING, CODE)` | ellipsis（`:[_]` の別名） |

**C++ scope resolution の扱い**:
`:::[[meth]]` と書くことで `::MethodName` にマッチできます。
最初の2コロンが定数として蓄積され、3番目の `:` がホール開始として認識されます。

---

### 3. ANTLR4 トークナイザー (`core/matcher/tokenizer/`)

文字列リテラル・コメントの境界を正確に検出するため、
ANTLR4 Lexer Grammar を使ったトークナイザーを言語ごとに実装しています。

#### アーキテクチャ

```
StructuralTokenizer (interface)
  └── tokenize(source) → List<StructuralToken>

StructuralToken
  ├── type: StructuralTokenType   (STRING, RAW_STRING, COMMENT, OTHER 等)
  ├── start: int                  開始オフセット
  └── end: int                    終了オフセット（exclusive）

StructuralTokenType (enum)
  ├── STRING           エスケープ可能な文字列リテラル
  ├── RAW_STRING       raw 文字列リテラル（バックティック等）
  ├── COMMENT          コメント
  └── OTHER            上記以外
```

#### トークナイザー一覧

| クラス | 使用言語 | Lexer Grammar |
|---|---|---|
| `CombyCLikeTokenizer` | Java, C, C++, JS, TS, PHP, Kotlin, Swift, C#, Generic | `CombyCLikeLexer.g4` |
| `CombyGoTokenizer` | Go | `CombyGoLexer.g4` |
| `CombyNestedBlockTokenizer` | Rust, Scala | `CombyNestedBlockLexer.g4` |
| `CombyHashTokenizer` | Bash, Ruby | `CombyHashLexer.g4` |
| `CombyPythonTokenizer` | Python | `CombyPythonLexer.g4` |
| `CombySqlTokenizer` | SQL | `CombySqlLexer.g4` |
| `CombyOcamlTokenizer` | OCaml | `CombyOcamlLexer.g4` |

#### `LanguageMatcher.tokenizer()` による統合

各言語マッチャーは `tokenizer()` を override して適切なトークナイザーを返します。
`AbstractLanguageMatcher` が `MatchEngine` にトークナイザーを渡し、
`MatchEngine` はトークナイズ結果を使ってスキップオフセットを計算します。

```java
// 例: JavaMatcher
@Override
public StructuralTokenizer tokenizer() {
    return new CombyCLikeTokenizer();
}
```

---

### 4. マッチングエンジン (`core/matcher/MatchEngine.java`)

**stateless 設計** — すべての状態は引数として渡します。
Virtual Threads からの並列呼び出しでもスレッド競合が発生しません。

#### 処理フロー（`findAll`）

```
findAll(source, templateStr, filePath)
  │
  ├─ tokenizer.tokenize(source)          ANTLR4 でトークナイズ
  │    └─ computeSkippedOffsets(tokens)  文字列・コメント内部の位置を事前計算
  │
  └─ while (startPos <= source.length())
       │
       ├─ skipped.contains(startPos) → skip（文字列・コメント内部からはマッチ開始しない）
       │
       └─ tryMatchAt(cursor, nodes)
            │
            └─ matchNodes(cursor, nodes, env)
                 │
                 ├─ Constant → cursor.consume(c.value())
                 │
                 └─ Hole → matchHole(cursor, h, stopCondition, env)
                               │
                               ├─ EVERYTHING → consumeEverything(cursor, stopCondition, ctx)
                               ├─ ALPHANUM   → consumeAlphanum(cursor)
                               ├─ NON_SPACE  → consumeNonSpace(cursor, stopCondition)
                               ├─ LINE       → consumeLine(cursor)
                               ├─ BLANK      → consumeBlank(cursor)
                               └─ REGEX      → consumeRegex(cursor, regex)
```

#### `consumeEverything` — コアアルゴリズム

EVERYTHING ホールの実装が comby の核心です。

```
while (cursor.hasMore()):
  if depth==0 && startsWith(stopCondition): break       // 停止条件
  if tryConsumeString(cursor): continue                 // 文字列リテラルをスキップ
  if tryConsumeComment(cursor): continue                // コメントをスキップ
  if isOpenDelim(c): depth++; advance                   // 開き括弧
  if isCloseDelim(c) && depth==0: break                 // 閉じ括弧（depth=0は消費しない）
  if isCloseDelim(c): depth--; advance                  // 閉じ括弧（depth>0）
  advance
```

**停止条件の計算** (`computeStopCondition`):
ホールの直後に来る `Constant` ノードの先頭文字列が自動的に停止条件になります。

```java
// "[1] :[2]" → :[1] の停止条件は " "（スペース）
// "foo(:[x])" → :[x] の停止条件は ")"
```

#### `computeSkippedOffsets` — 文字列・コメント内部のスキップ

`findAll` が文字列リテラル・コメントの内部からマッチを開始しないよう、
ANTLR4 トークナイズ結果から内部オフセットの集合を計算します。

```java
// STRING/RAW_STRING/COMMENT トークン: 開始+1 から 終了-1 をスキップ対象
// → findAll のスキャン開始位置がリテラル内部に入らない
```

---

### 5. 言語マッチャー (`core/matcher/languages/`)

各言語マッチャーは `LanguageSyntax` record で文字列・コメント・区切り文字を定義し、
`AbstractLanguageMatcher` が共通ロジック（findMatches → rule評価 → rewrite）を提供します。

#### `LanguageSyntax` の構成要素

```java
record LanguageSyntax(
    List<StringDelimiter> stringDelimiters,  // "..." / '...' / `...` 等
    List<CommentSyntax>   comments,          // // ... / /* ... */ / (* ... *) 等
    List<String>          openDelimiters,    // (, {, [
    List<String>          closeDelimiters    // ), }, ]
)
```

#### 言語別の主要な違い

| 言語 | 特徴的な定義 |
|---|---|
| **Generic** | `"..."(escapable)`, `'...'(escapable)`、コメントなし |
| **Java/C/C++** | `//` 行コメント, `/* */` ブロックコメント |
| **Go** | バックティック `` `...` ``（raw string、エスケープなし）|
| **Python** | `"""..."""` / `'''...'''` トリプルクォート（ANTLR で長いデリミタを優先） |
| **OCaml** | `(* ... *)` ネストコメント（ANTLR で深度カウント） |
| **Rust/Scala** | `/* ... */` ネストブロックコメント（ANTLR で深度カウント） |
| **SQL** | `--` 行コメント, `/* */` ブロックコメント |
| **Bash/Ruby** | `#` 行コメント |

#### `LanguageRegistry` — 拡張子解決

| 拡張子 | マッチャー |
|---|---|
| `.java` | JavaMatcher |
| `.py` | PythonMatcher |
| `.js` / `.jsx` | JavaScriptMatcher |
| `.ts` / `.tsx` | TypeScriptMatcher |
| `.c` / `.h` | CMatcher |
| `.cpp` / `.cc` / `.cxx` / `.hpp` | CppMatcher |
| `.go` | GoMatcher |
| `.rs` | RustMatcher |
| `.rb` | RubyMatcher |
| `.php` | PhpMatcher |
| `.scala` | ScalaMatcher |
| `.kt` | KotlinMatcher |
| `.swift` | SwiftMatcher |
| `.cs` | CSharpMatcher |
| `.sql` | SqlMatcher |
| `.sh` / `.bash` | BashMatcher |
| `.ml` / `.mli` | OcamlMatcher |
| （その他） | GenericMatcher |

---

### 6. リライトエンジン (`core/rewrite/RewriteEngine.java`)

マッチ結果のオフセット情報を使って、ソーステキストを置換します。
マッチは重複しないことを前提とし、オフセット昇順に処理します。

#### テンプレート変換属性

| 属性 | 例 | 変換内容 |
|---|---|---|
| `UPPERCASE` | `:[x.UPPERCASE]` | `"hello"` → `"HELLO"` |
| `lowercase` | `:[x.lowercase]` | `"HELLO"` → `"hello"` |
| `Capitalize` | `:[x.Capitalize]` | `"hello"` → `"Hello"` |
| `UpperCamelCase` | `:[x.UpperCamelCase]` | `"upper_camel_case"` → `"UpperCamelCase"` |
| `lowerCamelCase` | `:[x.lowerCamelCase]` | `"lower_camel_case"` → `"lowerCamelCase"` |
| `UPPER_SNAKE_CASE` | `:[x.UPPER_SNAKE_CASE]` | `"UpperSnakeCase"` → `"UPPER_SNAKE_CASE"` |
| `lines` | `:[x.lines]` | キャプチャ内の改行文字数を返す |
| `length` | `:[x.length]` | キャプチャの文字数を返す |
| `line` | `:[x.line]` | キャプチャの開始行番号を返す |
| `file` | `:[x.file]` | 処理中のファイルパスを返す |
| `id()` | `:[id()]` | UUID v4 ベースのランダムID生成 |

---

### 7. ルールエンジン (`core/rule/`)

`where` 句でマッチ結果をフィルタリングします。

#### `RuleNode` — sealed ADT

```
RuleNode
├── True_()                                   常に真
├── False_()                                  常に偽
├── Equal(left, right, leftIsVar, rightIsVar) ==
├── NotEqual(left, right, ...)                !=
├── Match_(variable, arms)                    パターンマッチ
├── Rewrite_(variable, template)              インプレースリライト
└── And(conditions)                           AND 結合（カンマ区切り）
```

#### ルール構文例

```
where :[x] == "foo"
where :[x] != :[y]
where :[x] == "foo", :[y] != "bar"
where "literal" == :[x]
rewrite :[body] { "new template" }
```

---

### 8. パイプライン (`pipeline/`)

Java 21 の **Virtual Threads** でファイルを並列処理します。

```
Pipeline.run()
  │
  ├─ collectFiles(directory)     拡張子フィルタ付きファイル収集
  │
  └─ processFiles(files)
       │
       └─ Executors.newVirtualThreadPerTaskExecutor()
            │
            └─ FileProcessor.process(path) × N ファイル（並列）
```

`PipelineConfig` record に変換仕様・言語マッチャー・出力設定・並列度を集約しています。

---

### 9. CLI (`cli/CombyCommand.java`)

picocli を使った CLI インターフェース。

#### 主要オプション

| オプション | 説明 |
|---|---|
| `MATCH REWRITE` | 位置引数：マッチテンプレートとリライトテンプレート |
| `-stdin` | 標準入力から読み込み |
| `-stdout` | 標準出力に書き込み |
| `-i` | ファイルをインプレース書き換え |
| `-d DIR` | ディレクトリを再帰検索 |
| `-matcher LANG` | 言語マッチャー指定（例: `java`, `python`） |
| `-rule RULE` | where ルール指定 |
| `-match-only` | マッチのみ表示（置換しない） |
| `-json-lines` | JSON Lines 形式で出力 |
| `-diff` | Unified diff 形式で出力 |
| `-review` | インタラクティブレビュー |
| `-count` | マッチ数のみ表示 |
| `-f EXT` | 対象拡張子フィルタ（例: `.java,.kt`） |
| `-config FILE` | TOML 設定ファイル |
| `-templates DIR` | TOML テンプレートディレクトリ |
| `-j N` | 並列ジョブ数 |

---

## データフロー

```
CLI 引数 / stdin
     │
     ▼
CombyCommand
     │  Specification(matchTemplate, rewriteTemplate, rule)
     ▼
AbstractLanguageMatcher.findMatches()
     │
     ├─ MatchEngine.findAll(source, templateStr)
     │    │
     │    ├─ TemplateParser.parse(matchTemplate) → List<TemplateNode>
     │    ├─ tokenizer.tokenize(source)           → List<StructuralToken>
     │    ├─ computeSkippedOffsets(tokens)        → Set<Integer>
     │    └─ tryMatchAt() × N                     → List<Match>
     │
     └─ RuleEngine.evaluate() でフィルタリング
          │
          ▼
     List<Match>（環境バインディング付き）
          │
          ▼
RewriteEngine.rewrite()
     │
     ├─ TemplateParser.parse(rewriteTemplate) → List<TemplateNode>
     └─ substitute() × N → rewritten source

     │
     ▼
OutputFormatter（Text / JsonLines / Diff / Interactive）
     │
     ▼
stdout / ファイル書き込み
```

**Java ライブラリとして使う場合**は `Comby` static facade を直接呼び出すことで、
CLI を経由せず同じパイプラインが実行されます。

---

## テスト構成

| テストクラス | テスト数 | 対応する comby オリジナルテスト |
|---|---|---|
| `TemplateParserTest` | 8 | — |
| `MatchEngineTest` | 21 | `test_generic.ml` |
| `HoleExtensionTest` | 15 | `test_hole_extensions.ml`, `test_regex_holes.ml` |
| `LanguageMatcherTest` | 23 | `test_string_literals.ml`, `test_c.ml`, `test_go.ml`, `test_python_string_literals.ml` |
| `RewriteEngineTest` | 12 | `test_rewrite_attributes.ml` |
| `RuleEngineTest` | 12 | `test_match_rule.ml` |
| `CombyIntegrationTest` | 14 | 公式ドキュメントサンプル + C++ scope resolution |
| **合計** | **105** | |

---

## 使用技術

| 技術 | バージョン | 用途 |
|---|---|---|
| Java | 21 | sealed classes, records, pattern matching, Virtual Threads |
| Maven | 3.9+ | ビルドツール |
| ANTLR4 | 4.13.2 | 文字列・コメント境界の字句解析 |
| picocli | 4.7.5 | CLI引数パース |
| jackson-databind | 2.17.1 | JSON Lines 出力 |
| toml4j | 0.7.2 | TOML 設定ファイル |
| java-diff-utils | 4.12 | Unified diff 生成 |
| JUnit Jupiter | 5.10.2 | テストフレームワーク |

### fat JAR ビルド

```bash
mvn package
echo 'if (x > 0) {}' | java -jar target/structural-rewriter-0.1.0-executable.jar 'if (:[c])' 'if (!(:[c]))'
```

```powershell
mvn package

'if (x > 0) {}' |
  java -jar target/structural-rewriter-0.1.0-executable.jar 'if (:[c])' 'if (!(:[c]))'
```

---

## 設計上の重要な決定事項

### ANTLR4 による文字列・コメント境界の検出

手書きスキャナーの代わりに ANTLR4 Lexer Grammar を使って
文字列リテラルとコメントの境界を正確に検出しています。

**背景**: Python のトリプルクォートや OCaml のネストコメントなど、
手書きスキャナーでは正確な境界検出が困難なケースが存在しました。
ANTLR4 によってこれらを言語ごとに正確に扱えるようになりました。

```
各言語マッチャー
  └── tokenizer() → StructuralTokenizer（ANTLR4ベース）
         └── tokenize(source) → List<StructuralToken>
               └── MatchEngine が computeSkippedOffsets() で使用
```

### stateless MatchEngine

`MatchEngine` はすべての状態を引数として受け渡す設計にしています。
これにより Virtual Threads からの並列呼び出し時にスレッド競合が発生しません。
`AbstractLanguageMatcher.findMatches()` が毎回 `new MatchEngine(syntax())` を生成します。

### Java 21 sealed interface + pattern matching

`TemplateNode`, `RuleNode` を sealed interface で定義することで、
網羅性チェック付きの `switch` 式が使えます。

```java
switch (node) {
    case TemplateNode.Constant c -> sb.append(c.value());
    case TemplateNode.Hole h    -> { /* キャプチャ処理 */ }
}
```

コンパイラが全ケース網羅を保証するため、新しいノード種別追加時の漏れを防ぎます。

### Static Facade API (`Comby.java`)

CLI とライブラリの両方のユースケースに対応するため、
`Comby` クラスに static メソッドとして主要操作を公開しています。
内部では同一パイプラインを使用するため、CLI との動作差異がありません。

### コメント透過マッチングの実装方針

comby 本家と同様に、コメント・文字列リテラルの内容は `consumeEverything` での
括弧深度計算から除外されます（デリミタが深度に影響しない）。

コメント・文字列リテラルのテキストはキャプチャ値にそのまま含まれます。
これは本家 comby と同じ動作です（コメント透過はブラケット深度のみに適用）。
