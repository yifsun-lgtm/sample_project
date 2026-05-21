import { Component, Input } from '@angular/core';
import { LoadingService } from '@core/services/loading.service';
import { Observable } from 'rxjs';

/**
 * ローディングスピナーコンポーネント
 * LoadingServiceと連動してローディング状態を表示
 */
@Component({
  selector: 'app-loading-spinner',
  templateUrl: './loading-spinner.component.html',
  styleUrls: ['./loading-spinner.component.scss']
})
export class LoadingSpinnerComponent {

  /** インラインモードで表示するかどうか */
  @Input() inline = false;

  /** ローディング状態のObservable */
  loading$: Observable<boolean>;

  constructor(private loadingService: LoadingService) {
    this.loading$ = this.loadingService.loading$;
  }
}
