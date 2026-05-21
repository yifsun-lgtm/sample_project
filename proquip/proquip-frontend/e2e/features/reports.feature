@reports
Feature: レポート
  ProQuip各種レポート画面の表示確認

  Scenario: 支出レポートにデータが表示される
    Given the user navigates to "/reports/spending"
    Then the page should contain "支出"
    And the table should have more than 0 rows

  Scenario: 在庫レポートにデータが表示される
    Given the user navigates to "/reports/inventory"
    Then the page should contain "在庫"
    And the table should have more than 0 rows

  Scenario: サプライヤーレポートにデータが表示される
    Given the user navigates to "/reports/suppliers"
    Then the page should contain "サプライヤー"
    And the table should have more than 0 rows
