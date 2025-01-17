import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisSidePanelModule } from 'app/shared/side-panel/side-panel.module';
import { PageDiscussionSectionComponent } from 'app/overview/page-discussion-section/page-discussion-section.component';
import { RouterModule, Routes } from '@angular/router';
import { MetisModule } from 'app/shared/metis/metis.module';

const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        component: PageDiscussionSectionComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes), MetisModule, ArtemisSharedModule, ArtemisSidePanelModule],
    declarations: [PageDiscussionSectionComponent],
    exports: [PageDiscussionSectionComponent],
})
export class PageDiscussionSectionModule {}
