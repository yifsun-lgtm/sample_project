@pricing
Feature: 価格管理
  ProQuip価格リスト一覧・作成の動作検証

  Scenario: 価格リストにデータが表示される
    Given the user navigates to "/pricing"
    Then the table should have more than 0 rows
    And the page should contain "価格リスト"

  @bug @critical
  Scenario: 価格リストの新規作成で保存時に500エラーが発生する
    # BUG: 価格リストを作成して保存ボタンを押すとサーバーから500エラーが返される
    Given the user navigates to "/pricing"
    When the user clicks the "新規作成" button
    And the user fills in the price list form
    And the user clicks the "保存" button
    Then no server error dialog should appear

  @bug @medium
  Scenario: 価格リストの説明・有効開始日・有効終了日カラムが空欄になっている
    # BUG: 価格リスト一覧で説明・有効開始日・有効終了日カラムが全行空欄
    Given the user navigates to "/pricing"
    Then the table column 2 should not have empty cells
    And the table column 4 should not have empty cells
    And the table column 5 should not have empty cells
