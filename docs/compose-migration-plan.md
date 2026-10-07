# Jetpack Compose 移行プラン

作成日: 2026-10-06。Code Reader と `../orientation-faker-private` の現在の実装を比較した計画。
このドキュメントは移行順序と完了条件を定める。

進捗（2026-10-07）: 段階 1 の基盤導入、段階 2 の設定画面、段階 3 のライセンス画面を実装した。
設定・ライセンス画面は Compose、メイン画面は Views のまま維持している。
設定の保存と画面操作は Robolectric で検証し、エミュレータでも表示を確認した。
詳細は[設定画面の移行記録](compose-settings/README.md)を参照。
IDE での Preview 描画は未確認。段階 0 の未確認項目は
[移行前の確認記録](compose-baseline/README.md)に記載した。

## 方針と到達点

設定 → ライセンス → 読み取り結果 → メイン画面 → 検出演出の順に、動作する単位で移行する。
各段階は独立した PR とし、必要なら段階内をさらに小さく分ける。
Google の[段階的な移行方針](https://developer.android.com/develop/ui/compose/migrate/strategy)に沿って、
移行中は Views と Compose を共存させる。

最初の到達点は、3 つの Activity を維持したまま画面 UI を Compose 化すること。
`PreviewView` と `WebView` は `AndroidView` で残してよい。
その後、参考アプリに合わせて Navigation 3 で単一 Activity に統合する。
カメラ解析の再実装、設定保存方式の変更、DI 導入、機能追加は別の作業とする。

Material 3 の部品を採用し、Compose のテーマではアプリ固有のカラー指定を廃止する。
Android 12 以降は `dynamicDarkColorScheme` / `dynamicLightColorScheme` を使用する。
それ以前は `darkColorScheme()` / `lightColorScheme()` をカスタマイズせず使用する。
XML テーマは既存 View・ダイアログ・起動ウィンドウのために移行中も維持する。

## 現状と参考にする実装

| 領域 | Code Reader の現状 | 参考アプリから取り入れる点 |
| --- | --- | --- |
| ビルド | AGP 9.4.1、Kotlin 2.4.20、minSdk 26、View Binding | Compose compiler plugin、BOM、`compose = true` |
| メイン | `MainActivity` に UI・検出通知・カメラ制御が集まっている | `ui/main/MainScreen.kt` の画面と表示部品の分離 |
| 状態 | `MainActivityViewModel` の `StateFlow<List<ScanResult>>` | `collectAsStateWithLifecycle`、状態・イベント・副作用の分離 |
| 設定 | `PreferenceFragmentCompat`。振動とバージョン表示のみ | `ui/settings/DetailedSettingsScreen.kt` の Compose 設定項目 |
| ライセンス | `NestedScrollingWebView` とローカル HTML | `ui/license/LicenseScreen.kt` の `Scaffold` + `AndroidView` |
| ダイアログ | 結果・権限の `DialogFragment` | `ui/dialog/` の状態に応じたダイアログ表示 |
| 画面遷移 | メインから設定・ライセンスの Activity を起動 | `ui/navigation/` の Navigation 3 と戻る操作 |
| テーマ | Material Components、明暗別 XML | `ui/theme/Theme.kt` の `AppTheme` と Material 3 |

参考アプリの Hilt、Room、DataStore、Accompanist、汎用ナビゲーショングラフは一括導入しない。
Code Reader は単一モジュールの小さなアプリなので、既存 ViewModel と通常の Factory で始める。
画面ごとの状態・イベントを必要な範囲で導入し、すべてのクリックを汎用イベント基盤に載せない。

## 段階 0: 現行動作と検証基準を記録する

- 明暗・日本語/英語・縦横画面で、メイン、結果ダイアログ、設定、ライセンスを記録する。
- メインは未検出、1 件、2 件以上を記録する。現状は結果領域 80dp、一覧 160dp で、
  2 件目に下側の空間を縮める。プレビュー自体の高さは一覧拡張と連動して縮まない。
- 初回許可、拒否、再要求できない拒否、アプリ情報での許可変更と復帰を確認する。
- 重複排除、検出順、末尾への自動スクロール、振動、トーチ、開く・コピー・共有を確認する。
- 回転とプロセス再生成は分けて記録する。現在の結果一覧は ViewModel のメモリ保持で、
  プロセス再生成後の履歴復元は保証されていない。この移行で永続履歴を追加しない。

完了条件: 比較用の画面と操作チェックリストが揃い、既存不具合と移行による退行を区別できる。
カメラの正常読取は実機で確認する。端末がない項目は未確認として残す。

## 段階 1: Compose のビルド基盤とテーマを追加する

対象: ルートと `app` の `build.gradle.kts`、`gradle/libs.versions.toml`、`ui/theme/`。

- Kotlin と同じ版の `org.jetbrains.kotlin.plugin.compose` を登録し、`compose = true` を有効化する。
  AGP の既存構成を維持し、旧来の Kotlin Android plugin や compiler extension 設定を追加しない。
- Compose BOM、Material 3、UI、`activity-compose`、`lifecycle-runtime-compose`、Preview を追加する。
  `ui-tooling` は debug のみに追加する。Compose で ViewModel を取得する段階で
  `lifecycle-viewmodel-compose` を追加する。
- BOM は参考アプリの宣言を候補に、実装時の利用可能版と互換性を確認する。
  Kotlin/AGP/CameraX/ML Kit の同時更新は行わない。
- `AppTheme` を追加し、Dynamic Color と未カスタマイズの標準カラースキームを切り替える。
  ライト/ダークの Preview を用意する。View Binding は有効なままにする。

完了条件: 既存画面の動作を保ち、Compose の Preview と debug ビルドが通る。
依存関係差分を確認してから Dependency Guard のベースラインを更新する。

## 段階 2: 設定画面を最初の Compose 画面にする

対象: `SettingsActivity.kt`、`setting/Settings.kt`、`ui/settings/`。

- `SettingsActivity` の中身を `setContent { AppTheme { SettingsScreen(...) } }` にする。
  `Scaffold`、`TopAppBar`、振動のスイッチ、バージョン表示を実装する。
- `Settings.vibrate` に書込み経路を追加する。既存の設定ファイル名、`VIBRATE_BOOLEAN`、
  初期値 true、レビュー関連の設定を保持する。
- 設定の表示状態と保存処理を分ける。画面開始時の再読込み、または設定変更の監視で整合を保つ。
- 設定項目のオン/オフ要約と、既存 Preference の要約コピー操作も引き継ぐ。
- 保存処理のテストとスイッチ操作の UI テストを必要最小限追加する。
  Android 依存がある保存処理は Robolectric など適切な実行環境で検証する。
- 移行後に `SettingsFragment`、`activity_settings.xml`、`xml/preferences.xml` を削除する。
  `PreferenceDataStore` を継承する保存クラスが残るため、Preference 依存はまだ削除しない。

完了条件: 既存インストールの設定を読め、変更が再起動後も保持され、検出時の振動にも反映される。
既存の Activity 起動と戻る操作を維持する。

## 段階 3: ライセンス画面を移行する

実装済み。検証内容は [ライセンス画面の移行記録](compose-license/README.md) を参照。

対象: `LicenseActivity.kt`、`view/NestedScrollingWebView.kt`、`ui/license/`。

- Activity は維持し、Compose のツールバーと `AndroidView` の WebView に置き換える。
- 既存 `license.html`、Custom Tabs へのリンク処理、ズーム設定を維持する。
  現在の HTML は CSS の `prefers-color-scheme` を使用しており、参考アプリの
  JavaScript `setTheme()` は実装されていない。その呼出しだけをコピーしない。
- TopAppBar は固定とし、本文は標準 WebView でスクロールする。
  埋め込み HTML の表示に用途を限定し、nested scroll と状態保存・復元は実装しない。
- WebView は初回生成時に HTML を読み込み、再コンポーズでの再生成・再読込みを避ける。
  Activity 再生成時は先頭から表示し、離脱時に読み込みを停止して View を解放する。

完了条件: 長文スクロール中もアプリバーが固定され、外部リンク・明暗表示が機能する。
確認後に `activity_license.xml` と不要になった旧 WebView 実装を削除する。

## 段階 4: メインの結果一覧だけを移行する

対象: `activity_main.xml`、`MainActivity.kt`、`MainActivityViewModel.kt`、`ui/result/`。

- RecyclerView 部分を `ComposeView` と `LazyColumn` に置き換える。
  カメラ、検出演出、メニュー、フラッシュ、結果ダイアログは現行実装を使う。
- `StateFlow` を lifecycle に合わせて購読し、結果順と重複排除を維持する。
  `ScanResult` 全体の等値性が現行の同一性であり、value だけをキーにしない。
  LazyColumn のキーは同一性を保つ保存可能な形式にする。
- 新しい結果が追加されたときだけ末尾へスクロールする。
  一覧の拡張はこの段階では既存の dummy と `ValueAnimator` に任せる。
- `ComposeView` に安定した ID と適切な composition の破棄方針を設定する。
  親 View が扱う Insets を Compose 側で二重適用しない。
- 重複排除と追加順を ViewModel の単体テストで検証する。
  UI は 0/1/複数件、長い値、行選択を Preview と必要な UI テストで確認する。

完了条件: 読取り → 一覧追加 → 結果ダイアログまでの経路が動く。
確認後に `ScanResultAdapter` と `item_result.xml` を削除する。

## 段階 5: 結果ダイアログを Compose 化する

対象: `result/ScanResultDialog.kt`、`ui/result/`、メインの選択状態。

- `AlertDialog` または Compose `Dialog` に置き換える。
  開く・コピー・共有は既存 `Launcher` / `ClipboardUtils` を使い、
  URI を開けない場合の検索フォールバックと `ReviewRequester.onAction()` を維持する。
- 選択結果を保存可能な表示状態として管理する。
  `ScanResult` は Parcelable なので、回転・再生成時のダイアログ復元に活用できる。
  表示状態を復元しても、外部起動・コピー・レビュー回数の加算は再実行しない。
- 戻る・外側タップによる閉じ方、連打による重複表示防止、長い値のスクロールを確認する。

完了条件: 3 操作が 1 回ずつ実行され、閉じた後も読取りを続けられる。
確認後に旧 `ScanResultDialog` と `dialog_result.xml` を削除する。

## 段階 6: メイン画面の土台を Compose にする

対象: `MainActivity.kt`、`OptionsMenuPresenter.kt`、`CodeScanner.kt`、`ui/main/`。

- `MainActivity` を `setContent` に切り替え、一覧、読取中表示、メニュー、トーチを Compose 化する。
  Activity は権限・アップデート・レビューなどの既存処理を当面担当する。
- カメラ部分は `AndroidView` で、`PreviewView`、静止画像の `ImageView`、
  `DetectedMarkerView` を重ねた専用 View コンテナを保持する。
  この段階では `DetectedPresenter` を使い、カメラと演出を同時に書き換えない。
- `CodeScanner` と Presenter を保持する単位を明確にする。
  再コンポーズで生成・開始し直さず、View が生成された後に接続する。
  後の画面離脱に備えて、Observer 解除、unbind、Analyzer 解除、Executor 終了、
  ML Kit scanner の close を対にする明示的な解放経路を設ける。
  非同期の camera provider 取得が解放後に完了しても再バインドしないようにする。
- 現行の ON_RESUME で bind、ON_PAUSE で unbind を保持する。
  `CodeAnalyzer` の ML Kit 完了後の `ImageProxy.close()` を保つ。
  実行中フレームと解放処理の競合も確認する。
- 結果一覧の拡張を Compose アニメーションへ置き換える。
  プレビューと静止画像と検出枠の領域・crop を揃え、0/1/2 件以上の配置を保持する。
- 上部はカメラ映像の表示を維持して操作ボタンを system bars から避け、
  下部・左右の Insets は一度だけ適用する。Main は単純な全体 padding に置き換えない。

完了条件: 再コンポーズ・回転・バックグラウンド復帰で重複カメラが生じず、
検出枠と静止画像の位置が揃う。確認後に `activity_main.xml` と `OptionsMenuPresenter` を削除する。

## 段階 7: 検出演出と権限ダイアログを移行する

この段階は「検出演出」と「権限 UI」の 2 PR に分ける。

- 検出コールバック中に Bitmap、角点、フレーム寸法、回転を取り出し、
  `ImageProxy` / `Barcode` 自体を Compose の状態に保持しない。
  フレームは引き続き Analyzer が閉じる。
- 静止画像と検出枠を Compose `Image` / `Canvas` とアニメーションに置き換える。
  既存の回転補正、center crop、枠の拡縮 4 → 1.2、1 秒の演出と 0.5 秒の待機を引き継ぐ。
- 停止・キャンセル・画面離脱時に古い Bitmap とコールバックを解放する。
  表示中の Bitmap を先に recycle しない。演出の途中終了でも読取り再開の状態を整える。
- 座標変換を独立した純粋ロジックとして切り出し、90/270 度の回転と縦横の crop をテストする。
- `PermissionDialog` を Compose 化する。まず既存の Activity Result 登録と
  `PermissionRequestLauncher` を使い、拒否判定を同時に変更しない。
  権限要求は操作・初回処理から行い、再コンポーズを理由に繰り返さない。
- 設定アプリから復帰した時点で権限を再確認する。設定での権限取消しとプロセス再生成も確認する。

完了条件: 枠がコードに追従し、演出終了後に読取りが再開する。
許可・拒否・設定アプリからの復帰が段階 0 の仕様を満たす。
確認後に `DetectedPresenter`、`DetectedMarkerView`、旧 `PermissionDialog` を削除する。

## 段階 8: Navigation 3 で単一 Activity に統合する

前提: 3 画面が Composable として利用でき、カメラを画面単位で解放できる。
UI の Compose 化だけを先にリリースする場合、この段階は後続に分離できる。

- `ui/navigation/` に Main / Settings / License の型付きキーと保存可能なバックスタックを追加する。
  Navigation 3 と必要な serialization 依存はここで導入する。
  [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) の標準機構を使い、
  参考アプリの汎用 Navigator を丸ごとコピーしない。
- 読取り結果の ViewModel は Main エントリの存続中保持し、
  設定・ライセンスから戻ったときに同じ結果を表示する。
- カメラの稼働条件を「Main が現在表示されている、Activity が RESUMED、権限あり」にする。
  Activity の lifecycle だけでは設定画面でもカメラが動き得るため、画面の表示状態も扱う。
- Updater の復帰処理、ReviewRequester の要求タイミング、権限の再確認を移す。
  既存の `onRestart()` は同一 Activity 内の画面遷移では呼ばれない点に注意する。
- 戻る、予測型 Back のキャンセル、遷移連打、回転・プロセス再生成後の遷移先を確認する。

完了条件: 設定・ライセンス表示中に解析が止まり、Main に戻ると結果を保持して再開する。
確認後に `SettingsActivity` / `LicenseActivity` と Manifest の宣言を削除する。
既存ランチャーの `.StartActivity` alias は保持する。

## 段階 9: 不要コード・依存関係を整理する

- View Binding を無効化し、Binding、Fragment、RecyclerView、ConstraintLayout、
  Material Components、Preference への参照を検索して不要な依存だけを削除する。
- Preference 依存を削除する前に `SharedPreferenceDataStore` の継承と
  段階 2 で削除した `Settings.apply` に代わる Fragment 接続は追加せず、
  SharedPreferences の小さなラッパーにする。
  設定ファイルとキーを維持する。Jetpack DataStore への変更は別計画とする。
- AppCompat は既存ユーティリティの参照を確認してから判断する。
  残る XML テーマは起動ウィンドウなどの用途を整理して維持する。
- `PreviewView` と WebView の依存は残す。Parcelable の必要性も保存状態を踏まえて判断する。
- Compose の依存関係をライセンス生成へ反映し、HTML と release の依存関係を一致させる。
- 移行後の構成に合わせて `AGENTS.md` を更新する。

完了条件: 不要な layout XML、Binding、旧 UI 実装がなく、段階 0 の操作をすべて満たす。
release の縮小・難読化ビルドでも起動、読取り、画面遷移、ライセンス表示を確認する。

## 各 PR の検証と戻し方

- アプリ変更時: `./gradlew :app:assembleDebug` と `./gradlew :app:lintDebug`。
- Kotlin/Gradle 変更時: `./gradlew ktlint`。終了コードだけでなく違反出力を確認する。
- 単体テスト追加時: `./gradlew :app:testDebugUnitTest`。
  Compose UI テストを androidTest に配置した場合は、接続端末で
  `./gradlew :app:connectedDebugAndroidTest` を実行する。
- 依存関係変更時: `./gradlew dependencyGuard`。意図した差分を確認後にのみ
  `./dependency-guard-baseline.sh` を実行する。
- 各 UI にライト/ダークの Preview を追加し、変更画面を日本語/英語、縦横、
  大きなフォント、TalkBack、ジェスチャー/3 ボタンナビゲーションで確認する。
  新しい操作ラベルは `values/strings.xml` と `values-ja/strings.xml` の両方に追加する。
- カメラ変更のある PR は実機の読取り・トーチ・復帰を確認する。
  UI 検証用のエミュレータだけでカメラの正常性を判断しない。
- 各段階は確認後に旧実装を削除し、PR 単位で戻せるようにする。
  設定データの形式を変えないため、コードを戻した場合も既存設定を読める。

Views を残す境界では、公式の
[AndroidView の相互運用 API](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)
を参考に、生成・更新・解放の責務を分ける。

最初の実装対象は段階 0 と段階 1。その後、段階 2 の設定画面を最初の完結した UI 移行とする。
