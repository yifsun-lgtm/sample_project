import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProductService } from '@shared/services/product.service';
import { Category } from '@shared/models/product.model';

/**
 * カテゴリツリーノードのインターフェース
 */
interface CategoryTreeNode {
  category: Category;
  children: CategoryTreeNode[];
  expanded: boolean;
  level: number;
}

/**
 * カテゴリ管理コンポーネント
 * 階層カテゴリのCRUDと並べ替え
 *
 * 技術的負債: 再帰的なコンポーネントレンダリングによるパフォーマンス問題の可能性
 * 技術的負債: ドラッグアンドドロップではなく上下ボタンで並べ替え
 */
@Component({
  selector: 'app-category-management',
  templateUrl: './category-management.component.html',
  styleUrls: ['./category-management.component.scss']
})
export class CategoryManagementComponent implements OnInit {

  /** カテゴリ一覧（フラット） */
  categories: Category[] = [];

  /** ツリー構造のカテゴリ */
  categoryTree: CategoryTreeNode[] = [];

  /** フラット化した表示用リスト */
  flattenedTree: CategoryTreeNode[] = [];

  /** ローディング状態 */
  isLoading = true;

  /** カテゴリ編集フォーム */
  categoryForm!: FormGroup;

  /** 編集モード */
  editMode: 'create' | 'edit' | null = null;

  /** 編集中のカテゴリID */
  editingCategoryId: number | null = null;

  /** 親カテゴリ選択用リスト */
  parentOptions: { id: number | null; name: string; level: number }[] = [];

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 送信中フラグ */
  isSubmitting = false;

  /** 削除確認ダイアログ表示フラグ */
  showDeleteConfirm = false;

  /** 削除対象カテゴリ */
  deletingCategory: Category | null = null;

  constructor(
    private fb: FormBuilder,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadCategories();
  }

  /**
   * フォームを初期化
   */
  private initializeForm(): void {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      parentId: [null]
    });
  }

  /**
   * カテゴリ一覧をロード
   */
  loadCategories(): void {
    this.isLoading = true;
    this.productService.getCategories().subscribe(
      (categories) => {
        this.categories = categories;
        this.buildTree();
        this.buildParentOptions();
        this.isLoading = false;
      },
      (error) => {
        console.error('カテゴリ取得エラー:', error);
        this.errorMessage = 'カテゴリの取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * カテゴリのツリー構造を構築
   * 技術的負債: 大量のカテゴリがある場合のパフォーマンス問題
   */
  private buildTree(): void {
    const nodeMap = new Map<number, CategoryTreeNode>();

    // 全カテゴリのノードを作成
    this.categories.forEach(cat => {
      nodeMap.set(cat.id, {
        category: cat,
        children: [],
        expanded: true,
        level: 0
      });
    });

    // 親子関係を構築
    this.categoryTree = [];
    this.categories.forEach(cat => {
      const node = nodeMap.get(cat.id)!;
      if (cat.parentId && nodeMap.has(cat.parentId)) {
        const parentNode = nodeMap.get(cat.parentId)!;
        parentNode.children.push(node);
        node.level = parentNode.level + 1;
      } else {
        this.categoryTree.push(node);
      }
    });

    // レベルを再帰的に設定
    this.setLevels(this.categoryTree, 0);

    // フラット化
    this.flattenTree();
  }

  /**
   * レベルを再帰的に設定
   * 技術的負債: 深いネストでスタックオーバーフローの可能性
   */
  private setLevels(nodes: CategoryTreeNode[], level: number): void {
    nodes.forEach(node => {
      node.level = level;
      if (node.children.length > 0) {
        this.setLevels(node.children, level + 1);
      }
    });
  }

  /**
   * ツリーをフラットなリストに変換（表示用）
   */
  private flattenTree(): void {
    this.flattenedTree = [];
    this.flattenNodes(this.categoryTree);
  }

  /**
   * ノードを再帰的にフラット化
   */
  private flattenNodes(nodes: CategoryTreeNode[]): void {
    nodes.forEach(node => {
      this.flattenedTree.push(node);
      if (node.expanded && node.children.length > 0) {
        this.flattenNodes(node.children);
      }
    });
  }

  /**
   * 親カテゴリの選択肢を構築
   */
  private buildParentOptions(): void {
    this.parentOptions = [{ id: null, name: '（ルートカテゴリ）', level: 0 }];
    this.addParentOption(this.categoryTree);
  }

  /**
   * 親カテゴリ選択肢を再帰的に追加
   */
  private addParentOption(nodes: CategoryTreeNode[]): void {
    nodes.forEach(node => {
      this.parentOptions.push({
        id: node.category.id,
        name: '　'.repeat(node.level) + node.category.name,
        level: node.level
      });
      if (node.children.length > 0) {
        this.addParentOption(node.children);
      }
    });
  }

  /**
   * ノードの展開/折りたたみを切り替え
   */
  toggleExpand(node: CategoryTreeNode): void {
    node.expanded = !node.expanded;
    this.flattenTree();
  }

  /**
   * 新規作成モードを開始
   */
  startCreate(parentId: number | null = null): void {
    this.editMode = 'create';
    this.editingCategoryId = null;
    this.categoryForm.reset({
      name: '',
      description: '',
      parentId: parentId
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * 編集モードを開始
   */
  startEdit(category: Category): void {
    this.editMode = 'edit';
    this.editingCategoryId = category.id;
    this.categoryForm.patchValue({
      name: category.name,
      description: category.description,
      parentId: category.parentId
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * 編集をキャンセル
   */
  cancelEdit(): void {
    this.editMode = null;
    this.editingCategoryId = null;
    this.categoryForm.reset();
  }

  /**
   * カテゴリを保存（作成/更新）
   */
  saveCategory(): void {
    this.categoryForm.markAllAsTouched();
    if (this.categoryForm.invalid || this.isSubmitting) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const formData = this.categoryForm.value;

    if (this.editMode === 'create') {
      this.productService.createCategory({
        name: formData.name,
        description: formData.description || '',
        parentId: formData.parentId
      }).subscribe(
        () => {
          this.successMessage = 'カテゴリを作成しました。';
          this.isSubmitting = false;
          this.cancelEdit();
          this.loadCategories();
        },
        (error) => {
          console.error('カテゴリ作成エラー:', error);
          this.errorMessage = 'カテゴリの作成に失敗しました。';
          this.isSubmitting = false;
        }
      );
    } else if (this.editMode === 'edit' && this.editingCategoryId) {
      this.productService.updateCategory(this.editingCategoryId, {
        name: formData.name,
        description: formData.description || '',
        parentId: formData.parentId
      }).subscribe(
        () => {
          this.successMessage = 'カテゴリを更新しました。';
          this.isSubmitting = false;
          this.cancelEdit();
          this.loadCategories();
        },
        (error) => {
          console.error('カテゴリ更新エラー:', error);
          this.errorMessage = 'カテゴリの更新に失敗しました。';
          this.isSubmitting = false;
        }
      );
    }
  }

  /**
   * 削除確認を表示
   */
  confirmDelete(category: Category): void {
    this.deletingCategory = category;
    this.showDeleteConfirm = true;
  }

  /**
   * カテゴリを削除
   */
  deleteCategory(): void {
    if (!this.deletingCategory) return;

    const id = this.deletingCategory.id;
    this.productService.deleteCategory(id).subscribe(
      () => {
        this.successMessage = 'カテゴリを削除しました。';
        this.showDeleteConfirm = false;
        this.deletingCategory = null;
        this.loadCategories();
      },
      (error) => {
        console.error('カテゴリ削除エラー:', error);
        this.errorMessage = error.error?.error || 'カテゴリの削除に失敗しました。';
        this.showDeleteConfirm = false;
        this.deletingCategory = null;
      }
    );
  }

  /**
   * 削除確認をキャンセル
   */
  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deletingCategory = null;
  }

  /**
   * カテゴリを上に移動
   * 技術的負債: 実際のDnDではなくボタンによる並べ替え
   */
  moveUp(node: CategoryTreeNode): void {
    const siblings = this.getSiblings(node);
    const index = siblings.findIndex(n => n.category.id === node.category.id);
    if (index > 0) {
      const temp = siblings[index];
      siblings[index] = siblings[index - 1];
      siblings[index - 1] = temp;
      this.flattenTree();
    }
  }

  /**
   * カテゴリを下に移動
   * 技術的負債: 実際のDnDではなくボタンによる並べ替え
   */
  moveDown(node: CategoryTreeNode): void {
    const siblings = this.getSiblings(node);
    const index = siblings.findIndex(n => n.category.id === node.category.id);
    if (index < siblings.length - 1) {
      const temp = siblings[index];
      siblings[index] = siblings[index + 1];
      siblings[index + 1] = temp;
      this.flattenTree();
    }
  }

  /**
   * 同じ階層のノードを取得
   */
  private getSiblings(node: CategoryTreeNode): CategoryTreeNode[] {
    if (node.category.parentId === null) {
      return this.categoryTree;
    }

    // 親ノードを再帰的に検索
    const parent = this.findParentNode(this.categoryTree, node.category.id);
    return parent ? parent.children : this.categoryTree;
  }

  /**
   * 親ノードを再帰的に検索
   */
  private findParentNode(nodes: CategoryTreeNode[], childId: number): CategoryTreeNode | null {
    for (const n of nodes) {
      if (n.children.some(c => c.category.id === childId)) {
        return n;
      }
      const found = this.findParentNode(n.children, childId);
      if (found) return found;
    }
    return null;
  }

  /**
   * インデントのスタイルを返す
   */
  getIndentStyle(level: number): object {
    return { 'padding-left': (level * 28 + 16) + 'px' };
  }
}
