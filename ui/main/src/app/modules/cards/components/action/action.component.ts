import {Component, Input, OnInit} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Action} from "@ofModel/thirds.model";
import {I18nService} from "@ofServices/i18n.service";
import {I18n} from "@ofModel/i18n.model";

@Component({
  selector: 'of-action',
  templateUrl: './action.component.html',
  styleUrls: ['./action.component.scss']
})
export class ActionComponent implements OnInit {

  @Input() readonly action:Action;
  @Input() readonly i18nPrefix:I18n;
  @Input() readonly actionUrlPath:string;
  constructor(private httpClient: HttpClient, private i18n: I18nService) {

  }

  ngOnInit() {
  }

  submit(){
    const protocol = 'http://';
    const domain = 'localhost';
    const port = '8080';
    const actionId = this.action.key;
    const resource = `${this.actionUrlPath}/${actionId}`
    const url = `${protocol}${domain}:${port}${resource}`;

this.httpClient.post(url
,this.action
    ).subscribe();
  }

}
