/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {environment} from '@env/environment';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {AppState} from '@ofStore/index';
import {Store} from '@ngrx/store';
import {buildSettingsOrConfigSelector} from '@ofStore/selectors/settings.x.config.selectors';



@Injectable({
  providedIn: 'root'
})

export class RemoteLoggerService {

  private remoteLogsUrl;
  private isActive = false;
  private logs = [];

  constructor(private httpClient: HttpClient, private store: Store<AppState>) {

    this.remoteLogsUrl = `${environment.urls.remoteLogs}`;
    this.store.select(buildSettingsOrConfigSelector('remoteLoggingEnabled')).subscribe(
      remoteLoggingEnabled => this.setRemoteLoggerActive(remoteLoggingEnabled));
    this.regularlyFlush();
  }

  private regularlyFlush() {
    this.flush();
    setTimeout(() => this.regularlyFlush(), 5000);
  }

  public setRemoteLoggerActive(active: boolean) {
    if (active) {
      this.isActive = true;
      this.postLog('Remote log activated - ' + window.navigator.userAgent);
    } else {
      this.postLog('Remote log deactivated ');
      this.isActive = false;
    }
  }

  public postLog(logLine: string) {
    if (this.isActive) this.logs.push(logLine);
  }

  public flush() {
    if (this.logs.length > 0) {
      const logsToPush = this.logs;
      this.logs = [];
      this.httpClient.post<string[]>(`${this.remoteLogsUrl}`, {logs: logsToPush}).subscribe();
    }
  }

}