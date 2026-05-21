@warehouses
Feature: 倉庫管理
  ProQuip倉庫一覧・詳細・レイアウトの動作検証

  Scenario: 倉庫一覧が3件表示される
    Given the user navigates to "/warehouses"
    Then the table should have 3 rows
    And the page should contain "倉庫"

  Scenario: 倉庫詳細にゾーン情報が表示される
    Given the user navigates to "/warehouses"
    When the user clicks the first row in the table
    Then the page should contain "ゾーン"

  Scenario: 倉庫レイアウトにゾーンセルが表示される
    Given the user navigates to "/warehouses"
    When the user clicks the first row in the table
    And the user clicks the "レイアウト" button
    Then the zone cells should be visible

  @bug @critical
  Scenario: レイアウト画面でゾーンをクリックするとサーバーエラーが発生する
    # BUG: 倉庫レイアウト画面でゾーンセルをクリックするとサーバーエラーが発生する
    Given the user navigates to "/warehouses"
    When the user clicks the first row in the table
    And the user clicks the "レイアウト" button
    And the user clicks a zone cell in the layout
    Then the zone detail should load without server error

  @bug @medium
  Scenario: 倉庫一覧の使用率・ゾーン数・ステータスカラムが空欄になっている
    # BUG: 倉庫一覧で使用率・ゾーン数・ステータスカラムが全行空欄
    Given the user navigates to "/warehouses"
    Then the table column 5 should not have empty cells
    And the table column 6 should not have empty cells
    And the table column 7 should not have empty cells
