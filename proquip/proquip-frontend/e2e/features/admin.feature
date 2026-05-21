@admin
Feature: 管理機能
  ProQuipユーザー管理・システム設定・監査ログの動作検証

  Scenario: ユーザー管理に10件のユーザーが表示される
    Given the user navigates to "/admin/users"
    Then the table should have 10 rows
    And the page should contain "ユーザー管理"

  Scenario: ユーザー管理のユーザー名・メールカラムが空でない
    Given the user navigates to "/admin/users"
    Then the table column 1 should not have empty cells
    And the table column 3 should not have empty cells

  Scenario: システム設定ページにデータが表示される
    Given the user navigates to "/admin/settings"
    Then the page should contain "システム設定"
    And the page should have more than 0 ".config-item" elements

  Scenario: 監査ログページが表示される
    Given the user navigates to "/admin/audit-log"
    Then the page should contain "監査ログ"
    And the table should have more than 0 rows

  Scenario: 監査ログのカラムにデータが表示される
    Given the user navigates to "/admin/audit-log"
    Then the table column 1 should not have empty cells
    And the table column 2 should not have empty cells
