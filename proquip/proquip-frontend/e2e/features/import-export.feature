@import-export
Feature: インポート・エクスポート
  ProQuipインポート・エクスポート画面の表示確認

  Scenario: インポート・エクスポートページが表示される
    Given the user navigates to "/import-export"
    Then the page should contain "インポート"
    And the page should contain "エクスポート"
