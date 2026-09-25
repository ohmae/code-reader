# Code Reader

## プロジェクト概要

- Code Reader は CameraX と ML Kit を使うバーコード読み取り用 Android アプリ。モジュールは `:app` のみ。
- Kotlin、Android Views、View Binding を使用する。パッケージ名と名前空間は `net.mm2d.codereader`。
- アプリの概要は `README.md` を参照する。SDK の設定と依存関係のバージョンは、変更時に `app/build.gradle.kts` と `gradle/libs.versions.toml` で確認する。

## 主なファイル

- `app/src/main/kotlin/net/mm2d/codereader/`: Activity、UI ロジック、ViewModel。
- `code/CodeScanner.kt` と `code/CodeAnalyzer.kt`: CameraX のライフサイクル管理と ML Kit による画像解析。
- `result/`: 読み取り結果とその UI。`permission/`: カメラ権限の処理。`setting/`: 設定。`util/` と `extension/`: 共通処理。
- `app/src/main/res/`: XML レイアウト、テーマ、文字列。画面に表示する文言はリソースに置き、翻訳対象の文言を追加・変更する場合は `values/strings.xml` と `values-ja/strings.xml` の両方を更新する。
- `app/src/main/AndroidManifest.xml`: アプリのコンポーネントと権限。`app/src/debug/` にはデバッグ専用の実装がある。

## 実装上の約束

- 周辺の Kotlin と XML の書き方に合わせる。`.editorconfig` は UTF-8、LF、スペース 4 個のインデント、1 行 120 文字を指定している。Kotlin にはリポジトリの ktlint 設定を適用する。
- 読み取り処理を変更する際は、カメラのバインドと解除、Executor の終了処理、`ImageProxy.close()` のタイミングを保つ。`CodeAnalyzer` は ML Kit の処理完了後に各フレームを閉じる。
- 新しいフレームワークや抽象化を加える前に、既存の View Binding、設定、権限処理の実装を確認する。
- デバイスなしで検証できるロジックには、対象を絞ったテストを追加する。現時点では `src/test` と `src/androidTest` にテストソースはない。

## 検証

- アプリを変更したら `./gradlew :app:assembleDebug`、Android Lint の確認には `./gradlew :app:lintDebug` を実行する。
- Kotlin または Gradle スクリプトを変更したら `./gradlew ktlint` を実行する。このタスクは `isIgnoreExitValue = true` なので、Gradle が成功しても出力に違反がないか確認する。
- テストを追加したら `./gradlew :app:testDebugUnitTest` を実行する。カメラ、権限、UI の変更は、利用可能ならエミュレータまたは実機でも該当する操作を確認し、実施できなかった確認は報告する。
- 依存関係は `gradle/libs.versions.toml` で管理する。変更した場合は `./gradlew dependencyGuard` を確認し、リリース用の依存関係が意図したものだと確認できた場合に限り `./dependency-guard-baseline.sh` でベースラインを更新する。
