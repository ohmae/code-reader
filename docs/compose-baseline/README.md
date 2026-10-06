# Compose 移行前の確認記録

記録日: 2026-10-06。CodeReader 0.5.4 の既存 Views UI。
検証環境は API 37 の `emulator-5554`、1080 × 2400、英語表示。
以下の画像は Compose 基盤追加前の APK から取得した。

| 画面 | 記録 |
| --- | --- |
| メイン・未検出・ライト・縦 | [画像](main-light-en-portrait.png) |
| メイン・未検出・ダーク・縦 | [画像](main-dark-en-portrait.png) |
| 設定・ライト・縦 | [画像](settings-light-en-portrait.png) |
| 設定・ダーク・縦 | [画像](settings-dark-en-portrait.png) |
| 設定・ライト・横 | [画像](settings-light-en-landscape.png) |
| ライセンス・ライト・縦 | [画像](license-light-en-portrait.png) |

## 確認した現行動作

- `.StartActivity` alias からメイン画面を起動し、カメラのプレビューと Scanning 表示を確認した。
  既存のカメラ権限は許可済みだった。初回要求の確認には含めない。
- メニューから設定・ライセンス画面を開ける。
  非モーダルのメニューは UI Automator の階層に含まれないため画像で項目を確認した。
- 設定は振動がオン、要約は「Vibrate when code is detected」、バージョンは 0.5.4。
- 明暗切替と設定画面の回転で画面を再生成できる。
- ライセンス HTML を表示でき、戻る操作でメインのプレビューに復帰する。
- 検証で変更した夜間モードと回転設定は元の値（night no、自動回転 1、回転 0）へ戻した。

## ソースから確認した、今後維持する仕様

- 結果は `ScanResult` 全体の等値性で重複排除し、検出順に追加する。
- 追加時に一覧の末尾へ移動し、最初の結果で Scanning 表示を隠す。
  2 件目で結果一覧を拡張する。結果行は 80dp、一覧は 160dp。
- 新規検出時の振動は設定が有効かつ端末が対応する場合に 30ms。
- 検出枠は 4 → 1.2 の拡縮を 1 秒間行い、さらに 0.5 秒後に読取りを再開する。
- 開く操作は URI 起動が失敗したら検索へフォールバックする。
  開く・コピー・共有でレビュー用の操作回数を加算してダイアログを閉じる。
- 結果一覧は ViewModel のメモリ保持で、永続履歴ではない。
- ON_RESUME でカメラを bind、ON_PAUSE で unbind する。
  ML Kit 処理完了後に `ImageProxy.close()` を呼ぶ。

これらはソースの確認であり、バーコードを使った実動作確認とは区別する。

## 後続の移行前に補完する確認

- [ ] 日本語の各画面、メイン・ライセンスの横画面、ライセンスのダーク表示
- [ ] 1 件・2 件以上の読取り結果と結果ダイアログの比較画像
- [ ] 実機での重複排除、検出枠の位置、振動、トーチ、バックグラウンド復帰
- [ ] 開く・検索フォールバック・コピー・共有、操作回数の更新
- [ ] 初回許可・拒否・再要求できない拒否・設定での許可変更と取消し
- [ ] 結果表示中とダイアログ表示中の回転、プロセス再生成
- [ ] 設定の変更・再起動後の保持、要約のコピー、ライセンスのスクロールとリンク
- [ ] 大きなフォント、TalkBack、ジェスチャー/3 ボタンナビゲーション

接続されているのはエミュレータのみで、実機のバーコード読取りは未確認。
この記録は段階 0 の一部であり、全項目の完了を意味しない。

## 段階 1 の実装・検証結果

- Compose compiler plugin は Kotlin と同じ 2.4.20。
- BOM 2026.09.00 により Compose UI 1.12.1、Material 3 1.4.0 を解決した。
  [公式セットアップ](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
  と参考アプリの宣言を確認して導入した。
- Lifecycle は 2.11.0 に更新。AGP、Kotlin、CameraX、ML Kit の版は維持した。
- `AppTheme` は Android 12 以降で Dynamic Color、それ以前で未カスタマイズの
  `darkColorScheme()` / `lightColorScheme()` を使用する。Compose にアプリ固有の色指定はない。
  Material 3 の標準 Typography を使用し、本文 16sp、補助本文 14sp、キャプション相当 12sp を利用できる。
- `PreviewAppTheme` のライト/ダーク Preview を追加し、debug のコンパイルを確認。
  IDE の Preview 描画は未確認。既存 Activity は Compose をまだ表示しない。
- `:app:assembleDebug`、`:app:lintDebug`、`ktlint` が成功。ktlint の違反出力なし。
- `orange_500` の未使用警告は既存リソースに対するもの。
- Dependency Guard は初回に意図した追加差分を検出。
  Compose と推移的な Emoji2 1.4.0、Window 1.5.0 などの追加・更新を確認後、
  `./dependency-guard-baseline.sh` で更新し、`./gradlew dependencyGuard` の成功を確認した。
- releaseRuntimeClasspath に `ui-tooling-preview` は含まれるが、debug 用の `ui-tooling` は含まれない。
- 導入後 APK のインストールと既存メイン画面の起動を確認した。
  今回はビルド基盤・未使用テーマの追加のため、単体テストの追加は行っていない。
