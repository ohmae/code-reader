# 設定画面の Compose 移行記録

実施日: 2026-10-06。移行プランの段階 2。

## 変更内容

- `SettingsActivity` は維持し、画面を `AppTheme` と `SettingsScreen` に置き換えた。
- Material 3 のツールバー、振動スイッチ、バージョン表示を実装した。
  Android 12 以降は Dynamic Color、それ以前は未カスタマイズの標準色を使う。
- 表示状態は設定画面で保持し、画面開始時に保存値を読み直す。
  保存処理は `Settings.vibrate` の setter で既存の SharedPreferences を更新する。
- 保存ファイル名、`VIBRATE_BOOLEAN`、初期値 true、レビュー用のキーを維持した。
- 振動項目とバージョン項目は長押しでコピーメニューを表示し、現在の要約をコピーする。
- 旧 `SettingsFragment`、レイアウト XML、Preference XML、`Settings.apply` を削除した。
  保存クラスは引き続き `PreferenceDataStore` を継承するため、Preference 依存は残す。
- ライト/ダークの Compose Preview を追加した。IDE の Preview 描画は未確認。

## 検証

以下のコマンドが成功した。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug ktlint dependencyGuard
```

ktlint の違反出力なし。Lint はエラーなし、既存の `orange_500` 未使用警告のみ。
追加した JUnit、Robolectric、Compose UI テスト依存は testImplementation であり、
releaseRuntimeClasspath に変化はない。Dependency Guard のベースライン更新は不要だった。

Robolectric の API 35 環境で以下の 6 テストを実行した。
ランナーは AndroidX Test の `AndroidJUnit4`、Context 取得は `ApplicationProvider`、
Activity 操作は `ActivityScenario` を使用する。SDK と言語の指定には `@Config` を使う。

- 既存の振動設定を読み、レビュー関連の値を維持する。
- 振動のオン/オフを既存キーへ保存し、Settings の再初期化後にも読み出せる。
- スイッチで設定を変更し、Activity 再生成後にも保持する。
- バージョンをコピーし、ツールバーの戻る操作で Activity を閉じる。
- 日本語の振動オフ要約をコピーし、長押しではスイッチを切り替えない。
- 画面が停止している間の設定変更を、再開時に表示へ反映する。

API 37 のエミュレータでも設定画面への遷移、オン/オフの切替、既存ファイルへの保存、
明暗の切替、横向き・文字サイズ 2 倍の表示を確認した。
比較画像は以下。Material 3 と Dynamic Color による配色・形状の変更は意図したもの。

| 条件 | 画像 |
| --- | --- |
| ライト・英語・縦・振動オン | [画像](light-en-portrait.png) |
| ダーク・英語・縦・振動オフ | [画像](dark-en-portrait.png) |
| ダーク・英語・横・文字サイズ 2 倍 | [画像](dark-en-large-landscape.png) |

実機での検出時の振動、TalkBack による読み上げ、Android 11 以下の標準色の表示は未確認。
検出側は従来と同じ `Settings.vibrate` getter を使用している。
