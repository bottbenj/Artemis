import { BASE_API, PATCH } from '../../../constants';

/**
 * A class which encapsulates UI selectors and actions for static code analysis grading configuration page.
 */
export class CodeAnalysisGradingPage {
    visit(courseId: number, exerciseId: number) {
        cy.visit(`course-management/${courseId}/programming-exercises/${exerciseId}/grading/code-analysis`);
    }

    makeEveryScaCategoryInfluenceGrading() {
        cy.get('select').each((category) => {
            cy.wrap(category).select('GRADED');
        });
    }

    saveChanges() {
        cy.intercept(PATCH, BASE_API + 'programming-exercise/*/static-code-analysis-categories').as('scaConfigurationRequest');
        cy.get('#save-table-button').click();
        return cy.wait('@scaConfigurationRequest');
    }
}
