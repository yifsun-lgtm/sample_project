@inventory
Feature: 在庫管理
  ProQuip在庫一覧・移動・調整・棚卸しの動作検証

  Scenario: 在庫一覧が表示される
    Given the user navigates to "/inventory"
    Then the table should have more than 0 rows
    And the page should contain "在庫"

  Scenario: 移動一覧が表示される
    Given the user navigates to "/inventory/transfers"
    Then the table should have more than 0 rows

  @bug @critical
  Scenario: 移動作成フォームの倉庫ドロップダウンが空で選択肢がない
    # BUG: 在庫移動の作成フォームで出庫元・入庫先の倉庫ドロップダウンに選択肢が表示されない
    Given the user navigates to "/inventory/transfers/new"
    Then the source warehouse dropdown should have options
    And the destination warehouse dropdown should have options

  @bug @high
  Scenario: 在庫調整ページで「在庫IDが無効です」と表示される（ルーティングバグ）
    # BUG: /inventory/adjustmentsにアクセスすると「在庫IDが無効です」と表示される
    Given the user navigates to "/inventory/adjustments"
    Then the page should show the adjustments interface

  @bug @high
  Scenario: 棚卸しページで「在庫IDが無効です」と表示される（ルーティングバグ）
    # BUG: /inventory/stocktakingにアクセスすると「在庫IDが無効です」と表示される
    Given the user navigates to "/inventory/stocktaking"
    Then the page should show the stocktaking interface
