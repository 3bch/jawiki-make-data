# jawiki-make-data

[日本語 Wikipedia](https://ja.wikipedia.org)
からカテゴリ別のデータを作成します

## 利用方法

* `./config/config.groovy` に出力したいカテゴリを設定する
* `./make-data.sh` を実行する
* `./resulｔ` に `<timestamp>-tar.xz` ファイルができる

  * `data` 以下に各カテゴリのドキュメントがあります
  * `config.groovy` に実行時の設定があります


### 利用における注意

* 実行には docker-compose が必要です
* 実行すると Docker 上で redis が起動します

  * 自動では stop しません
  * `./cache/redis` に redis 上のデータは保存されます

* 実行すると `./cache/wiki` 上に多くのテキストが展開されます

  * 自動では削除されません

* 初回実行時に [Wikipedia 日本語版データベース・ダンプ](https://dumps.wikimedia.org/jawiki/) を `./source` にダウンロードします

  * `cache.clear` に関わらず、ダウンロード済みであればそのファイルを利用します
  * データを最新にしたい場合は `./source` に落としたファイルを削除して実行してください

## 設定方法

* `cache.clear` `{boolean}`: キャッシュをクリアするか否か
* `cache.useXX` `{boolean}`: 各ステップでキャッシュを利用するか否か
* `data.includeAll` `{boolean}`: カテゴリ分けをせず、すべてのページを出力する `_all` を含めるか否か
* `data.defaultDepth` `{int}`: depth のデフォルト値
* `data.categories`: 各カテゴリ（名前は自由）それぞれについて以下を指定する

  * `title` `{String}`: 探索の起点とするカテゴリページのタイトル（日本語）
  * `depth` `{int}`: 指定したカテゴリページから何度までリンクを辿ったページをカテゴリのページとするか（省略可）

config.groovy のサンプル
```groovy
cache {
    clear = false

    use03 = true
    use04 = true
    use05 = true
    use06 = true
}

data {
    includeAll = true
    defaultDepth = 5

    categories {
        japan {
            title = '日本'
        }
        IT {
            title = '情報技術'
        }
        sports {
            title = 'スポーツ'
        }
        history {
            title = '歴史'
        }
    }
}
```
