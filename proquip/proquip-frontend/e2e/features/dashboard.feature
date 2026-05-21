@dashboard
Feature: ダッシュボード
  ProQuipダッシュボード画面の表示確認とKPI・グラフ・テーブルの動作検証

  Background:
    Given the user navigates to "/dashboard"

  Scenario: ダッシュボードにKPIカードが表示される
    Then the page should contain "発注件数"
    And the page should contain "在庫アラート"
    And the page should contain "承認待ち"
    And the page should contain "予算消化率"

  Scenario: KPIカードの値が数値を表示する
    Then the KPI card "発注件数" should display a number
    And the KPI card "在庫アラート" should display a number
    And the KPI card "承認待ち" should display a number
    And the KPI card "予算消化率" should display a number

  Scenario: 月別発注金額推移チャートが表示される
    Then the heading "月別発注金額推移" should be visible

  Scenario: 最近の発注テーブルにデータが表示される
    Then the heading "最近の発注" should be visible
    And the table should have more than 0 rows

  @bug @medium
  Scenario: 発注日が「NaN年aN月aN日」と表示される行がある
    # BUG: 一部の発注データで日付フォーマットが壊れている
    Then the table column 3 should not contain "NaN"

  @bug @low
  Scenario: ステータス「SUBMITTED」が英語のまま表示される
    # BUG: ステータスが日本語に翻訳されず「SUBMITTED」と表示される
    Then the table column 5 values should all be translated
