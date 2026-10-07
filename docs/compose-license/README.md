# ライセンス画面の Compose 移行記録

確認日: 2026-10-07。移行プランの段階 3 を実装した。

## 実装

`LicenseActivity` は維持し、`AppTheme`、Material 3 の固定 `TopAppBar` と
`AndroidView` 内の標準 WebView に置き換えた。アプリバーは既存の Dynamic Color テーマを利用する。
`license.html` は変更せず、CSS の `prefers-color-scheme` による明暗表示を維持した。
JavaScript とズームは無効のまま、外部リンクは既存の `Launcher.openCustomTabs` を使用する。

埋め込み HTML を表示するだけの画面なので、nested scroll と状態保存・復元は実装しない。
WebView は `AndroidView.factory` で生成して HTML を読み込む。
再コンポーズでは再生成・再読込みせず、Activity 再生成時は先頭から表示する。
WebView の解放は `onRelease` の `destroy()` に集約する。
`onRenderProcessGone` は対象 WebView を親から取り外し、キーを更新して `true` を返す。
Compose の `key` を更新して `AndroidView` を再生成し、ローカル HTML を先頭から表示する。
旧 WebView は `AndroidView` の破棄時に `onRelease` で解放されるため、終了状態のフラグは不要。
[onRenderProcessGone の仕様](https://developer.android.com/reference/android/webkit/WebViewClient#onRenderProcessGone(android.webkit.WebView,%20android.webkit.RenderProcessGoneDetail))
に従い、終了したレンダラの WebView を再利用しない。
旧 XML、nested scroll の専用 View、状態保存クラスとそのテスト、不要な AppBar/Popup 用テーマを削除した。

## 自動検証

- `./gradlew :app:assembleDebug`: 成功。
- `./gradlew :app:testDebugUnitTest`: 全 9 件成功（ライセンス画面 3 件）。
- `./gradlew :app:lintDebug`: 成功。既存の `orange_500` の未使用警告のみ。
- `./gradlew ktlint`: 成功。出力にスタイル違反なし。
- `git diff --check`: 成功。

ライセンス画面のテストは `AndroidJUnit4`、`ActivityScenario`、`ApplicationProvider` を使用する。
Robolectric 固有 API は実行 SDK / Application の `@Config` のみ。
HTML の読み込み・ズーム無効・JavaScript 無効・戻る操作・Activity 再生成後の再読込みを確認した。
レンダラのクラッシュと OS による終了をコールバックで模し、連続した WebView 再生成も確認した。
実端末上でのレンダラ強制終了は未検証。
依存関係は変更していない。

## 端末検証

移行時に API 37 エミュレータで HTML の明暗表示と外部リンクによる Chrome の
`CustomTabActivity` 起動・復帰を確認した。
簡略化後も API 37 で HTML 表示と、本文スクロール中もアプリバーが固定されることを確認した。
API 26 の WebView と実機での操作は今回未確認。

| 記録 | 画像 |
| --- | --- |
| 先頭 | [画像](license-light-en-portrait.png) |
| 固定アプリバー・スクロール後 | [画像](license-light-en-scrolled.png) |
