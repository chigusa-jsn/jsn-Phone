# jsn-Phone

マイクラ（NeoForge 1.21.1）の中で使えるスマートフォンMOD。

- 右クリック、または **Pキー** で開く
- **メッセージ**：LINEみたいなやりとり（オフラインの人にも送れる・既読つき）
- **つぶやき**：サーバーのみんなの掲示板（140文字・いいね・返信）
- **お知らせ**：進捗の達成・レベル突破・ボス討伐などが自動でのる（ほかのMODからものせられる）
- **フレンド**：申請・承認、フレンドのいる場所と距離、レーダー
- **時計・天気・メモ・設定**（テーマの色・壁紙・位置の共有・通知）
- ケースの色：作業台でスマホと染料を並べる（金インゴット＝金、鉄インゴット＝銀、アメジスト＝虹、水バケツ＝もとにもどす）

あれば使えるもの（なくても動く）：シンプルボイスチャット（電話）、Lightman's Currency（銀行）、jsn-Craft（jsn-Craft アプリ）

## ほかのMODからお知らせをのせる

```java
// jsn-Phone がなくても動くように、リフレクションで呼ぶ
Class.forName("com.chigusa.jsnphone.PhoneApi")
     .getMethod("news", ServerPlayer.class, String.class, String.class)
     .invoke(null, player, "〇〇 が △△ を手に入れた！", "item");
```
