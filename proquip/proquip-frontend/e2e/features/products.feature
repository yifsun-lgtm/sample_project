@products
Feature: 製品管理
  ProQuip製品一覧・検索・フィルタ・詳細・作成・カテゴリ・バンドル管理の動作検証

  Scenario: 製品一覧が109件のアイテムとページネーション付きで表示される
    Given the user navigates to "/products"
    Then the page should contain "109件中"
    And the table should have more than 0 rows

  Scenario: SKU・製品名で検索できる
    Given the user navigates to "/products"
    When the user fills in "検索" with "Dell"
    And the user waits 1000 milliseconds
    Then the table should have more than 0 rows

  Scenario: カテゴリフィルタが機能する
    Given the user navigates to "/products"
    Then the select "カテゴリ" should have more than 1 options

  Scenario: 製品詳細に基本情報とタブが表示される
    Given the user navigates to "/products"
    When the user clicks the first row in the table
    Then the heading "製品詳細" should be visible
    And the page should contain "基本情報"
    And the page should contain "仕様"

  Scenario: 製品登録フォームに5ステップウィザードが表示される
    Given the user navigates to "/products/new"
    Then the heading "製品登録" should be visible
    And the page should contain "基本情報"
    And the page should contain "価格・在庫"
    And the page should contain "仕様"
    And the page should contain "画像・資料"
    And the page should contain "確認"

  Scenario: カテゴリ管理ページが表示される
    Given the user navigates to "/products"
    When the user clicks the "カテゴリ管理" button
    Then the page should contain "カテゴリ"

  Scenario: バンドル管理ページが表示される
    Given the user navigates to "/products"
    When the user clicks the "バンドル管理" button
    Then the page should contain "バンドル"

  @bug @medium
  Scenario: 全製品の在庫数カラムが空欄になっている
    # BUG: 製品一覧の在庫数カラムが全行空欄で表示される
    Given the user navigates to "/products"
    Then the table column 7 should not have empty cells

  @bug @low
  Scenario: 製品登録のメーカードロップダウンが実データと一致しない
    # BUG: ハードコードされたメーカー選択肢が実際のDBデータと異なる
    Given the user navigates to "/products/new"
    Then the select "メーカー" should have more than 1 options
