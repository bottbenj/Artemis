import { ChangeDetectorRef, Component } from '@angular/core';
import { NotificationService } from 'app/shared/notification/notification.service';
import { UserSettingsDirective } from 'app/shared/user-settings/user-settings.directive';
import { JhiAlertService } from 'ng-jhipster';
import { reloadNotificationSideBarMessage } from 'app/shared/notification/notification-sidebar/notification-sidebar.component';
import { UserSettingsCategory } from 'app/shared/constants/user-settings.constants';
import { NotificationOptionCore } from 'app/shared/user-settings/notification-settings/notification-settings.default';
import { UserSettingsService } from 'app/shared/user-settings/user-settings.service';
import { UserSettings } from 'app/shared/user-settings/user-settings.model';

@Component({
    selector: 'jhi-notification-settings',
    templateUrl: 'notification-settings.component.html',
    styleUrls: ['../user-settings.scss'],
})
export class NotificationSettingsComponent extends UserSettingsDirective {
    constructor(notificationService: NotificationService, userSettingsService: UserSettingsService, changeDetector: ChangeDetectorRef, alertService: JhiAlertService) {
        super(userSettingsService, alertService, changeDetector);
    }

    userSettings: UserSettings<NotificationOptionCore>;
    optionCores: Array<NotificationOptionCore>;

    ngOnInit(): void {
        this.userSettingsCategory = UserSettingsCategory.NOTIFICATION_SETTINGS;
        this.changeEventMessage = reloadNotificationSideBarMessage;
        super.ngOnInit();
    }

    /**
     * Catches the toggle event from an user click
     * Toggles the respective option and mark it as changed (only changed option cores will be send to the server for saving)
     */
    toggleOption(event: any) {
        this.optionsChanged = true;
        const optionId = event.currentTarget.id;
        const foundOptionCore = this.optionCores.find((core) => core.optionSpecifier === optionId);
        if (!foundOptionCore) {
            return;
        }
        foundOptionCore!.webapp = !foundOptionCore!.webapp;
        foundOptionCore.changed = true;
    }
}
