@procurement
Feature: 調達管理
  ProQuip購買依頼・発注・承認・入荷・返品の動作検証（最多バグ発生モジュール）

  # --- 購買依頼 ---

  Scenario: 購買依頼一覧にデータが表示される
    Given the user navigates to "/procurement/requisitions"
    Then the table should have more than 0 rows
    And the page should contain "購買依頼"

  @bug @critical
  Scenario: 購買依頼の新規作成で保存時に500エラーが発生する
    # BUG: 購買依頼を作成して保存ボタンを押すとサーバーから500エラーが返される
    Given the user navigates to "/procurement/requisitions/new"
    When the user fills in the requisition form
    And the user clicks the "保存" button
    Then no server error dialog should appear

  @bug @high
  Scenario: 購買依頼作成ページでFormGroupエラーがコンソールに出力される
    # BUG: 購買依頼作成ページを開くとコンソールにFormGroup関連エラーが出力される
    Given the user navigates to "/procurement/requisitions/new"
    Then the console should have no errors matching "FormGroup"
    And the console should have no errors matching "Cannot find control"

  @bug @medium
  Scenario: 購買依頼一覧の作成日カラムが空欄になっている
    # BUG: 購買依頼一覧で作成日カラムが全行空欄
    Given the user navigates to "/procurement/requisitions"
    Then the table column 6 should not have empty cells

  @bug @low
  Scenario: 購買依頼のステータスが英語のまま表示される
    # BUG: ステータスが日本語に翻訳されず英語のまま表示される
    Given the user navigates to "/procurement/requisitions"
    Then the table column 5 values should all be translated

  # --- 発注 ---

  Scenario: 発注一覧にデータが表示される
    Given the user navigates to "/procurement/orders"
    Then the table should have more than 0 rows
    And the page should contain "発注"

  @bug @high
  Scenario: 発注作成ページでFormGroupエラーがコンソールに出力される
    # BUG: 発注作成ページを開くとコンソールにFormGroup関連エラーが8件出力される
    Given the user navigates to "/procurement/orders/new"
    Then the console should have no errors matching "FormGroup"
    And the console should have no errors matching "Cannot find control"

  @bug @medium
  Scenario: 発注一覧の発注日・納品予定日カラムが空欄になっている
    # BUG: 発注一覧で発注日・納品予定日カラムが全行空欄
    Given the user navigates to "/procurement/orders"
    Then the table column 4 should not have empty cells
    And the table column 5 should not have empty cells

  @bug @low
  Scenario: 発注のステータスが英語のまま表示される
    # BUG: ステータスが日本語に翻訳されず英語のまま表示される
    Given the user navigates to "/procurement/orders"
    Then the table column 3 values should all be translated

  # --- 承認 ---

  @bug @critical
  Scenario: 承認ページを開くとサーバーエラーが表示される
    # BUG: 承認ページにアクセスするとサーバーエラーが発生して画面が表示されない
    Then the approvals page should load without server error

  # --- 入荷 ---

  Scenario: 入荷ページが表示される
    Given the user navigates to "/procurement/goods-receipt"
    Then the page should contain "入荷"

  # --- 返品 ---

  @bug @critical
  Scenario: 返品ページを開くとサーバーエラーが表示される
    # BUG: 返品ページにアクセスするとサーバーエラーが発生して画面が表示されない
    Then the returns page should load without server error
