@suppliers
Feature: サプライヤー管理
  ProQuipサプライヤー一覧・詳細・フィルタの動作検証

  Background:
    Given the user navigates to "/suppliers"

  Scenario: サプライヤー一覧が表示される
    Then the table should have more than 0 rows
    And the page should contain "サプライヤー"

  Scenario: サプライヤー一覧のコード・会社名カラムが空でない
    Then the table column 2 should not have empty cells
    And the table column 3 should not have empty cells

  Scenario: サプライヤー詳細に会社情報と連絡先が表示される
    When the user clicks the first row in the table
    Then the page should contain "会社情報"
    And the page should contain "連絡先"

  Scenario: サプライヤー詳細の製品タブが正常に表示される
    When the user clicks the first row in the table
    And the user clicks the "製品" button
    Then the active tab panel should have content

  Scenario: サプライヤー詳細の契約タブが正常に表示される
    When the user clicks the first row in the table
    And the user clicks the "契約" button
    Then the active tab panel should have content

  Scenario: サプライヤー詳細の評価履歴タブが正常に表示される
    When the user clicks the first row in the table
    And the user clicks the "評価履歴" button
    Then the active tab panel should have content

  Scenario: サプライヤー詳細の認証タブが正常に表示される
    When the user clicks the first row in the table
    And the user clicks the "認証" button
    Then the active tab panel should have content

  Scenario: ステータスフィルタが存在する
    Then the select "ステータス" should have more than 1 options
